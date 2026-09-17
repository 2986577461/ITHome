package com.xiaoyan.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 事务同步工具。
 */
@Slf4j
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在事务提交后执行一段逻辑；当前没有事务时立即执行。
     *
     * <p>用于缓存失效、OSS 文件清理这类「必须等数据真正落库之后才能做」的动作。
     * 回调抛出的异常只记录日志、不向外抛：此时事务已经提交、数据不会回滚，
     * 抛出去只会让调用方拿到一个 500，反而掩盖了真正的问题。</p>
     */
    public static void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runQuietly(action);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runQuietly(action);
            }
        });
    }

    private static void runQuietly(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            log.error("事务提交后的清理动作失败", e);
        }
    }
}
