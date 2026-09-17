package com.xiaoyan.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 全局共享的异步任务线程池。
 *
 * <p>线程池声明为 static，整个 JVM 只有一个实例，所有 Service 共用，
 * 避免各处 {@code new Thread()} 或各自建池导致线程数失控。</p>
 */
@Component
@Slf4j
public class AsyncExecutors implements DisposableBean {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 8;
    private static final int QUEUE_CAPACITY = 100;
    private static final long KEEP_ALIVE_SECONDS = 60L;

    private static final ThreadPoolExecutor UPLOAD_EXECUTOR = new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAX_POOL_SIZE,
            KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(QUEUE_CAPACITY),
            new NamedThreadFactory("async-upload-"),
            // 队列满时交给调用线程执行：宁可拖慢当前请求，也不静默丢掉任务
            new ThreadPoolExecutor.CallerRunsPolicy());

    /** 文件上传等 IO 密集型异步任务共用的线程池。 */
    public static ExecutorService uploadExecutor() {
        return UPLOAD_EXECUTOR;
    }

    @Override
    public void destroy() {
        UPLOAD_EXECUTOR.shutdown();
        try {
            if (!UPLOAD_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                UPLOAD_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            UPLOAD_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {

        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, prefix + counter.getAndIncrement());
            // 守护线程，避免 JVM 退出时被未完成的上传任务卡住
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(
                    (t, e) -> log.error("异步任务执行异常, thread={}", t.getName(), e));
            return thread;
        }
    }
}
