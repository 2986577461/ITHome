package com.xiaoyan.controller.admin;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("total")
@AllArgsConstructor
public class TotalController {

    private MeterRegistry meterRegistry; // Micrometer 的度量注册中心

    /**
     * 获取 JVM 堆内存使用情况。
     * 提供已使用、最大可用和已提交的堆内存字节数，以及堆内存使用百分比。
     *
     * @return 包含JVM堆内存指标的Map：
     * - usedBytes: 堆内存已使用字节数
     * - maxBytes: 堆内存最大可用字节数
     * - usagePercentage: 堆内存使用百分比 (保留三位小数)
     */
    @GetMapping("/jvm/memory-heap")
    public Map<String, String> getJvmHeapMemory() {
        Map<String, Double> metrics = new HashMap<>();
        Gauge usedHeapGauge = meterRegistry.find("jvm.memory.used").tag("area", "heap").gauge();
        if (usedHeapGauge != null) {
            metrics.put("usedBytes", usedHeapGauge.value());
        }
        Gauge maxHeapGauge = meterRegistry.find("jvm.memory.max").tag("area", "heap").gauge();
        if (maxHeapGauge != null) {
            metrics.put("maxBytes", maxHeapGauge.value());
        }

        if (metrics.containsKey("usedBytes") && metrics.containsKey("maxBytes") && metrics.get("maxBytes") > 0) {
            double usage = metrics.get("usedBytes") / metrics.get("maxBytes");
            metrics.put("usagePercentage", usage);
        }
        Map<String, String> map = new HashMap<>();
        metrics.forEach((s, aDouble) -> map.put(s,
                BigDecimal.valueOf(aDouble).setScale(3, RoundingMode.HALF_UP).toPlainString()));
        return map;
    }

    /**
     * 获取 JVM 进程和系统 CPU 使用率。
     *
     * @return 包含CPU使用率指标的Map：
     * - processCpuUsage: 当前JVM进程的CPU使用率 (0.0-1.0，保留三位小数)
     * - systemCpuUsage: 整个系统的CPU使用率 (0.0-1.0，保留三位小数)
     */
    @GetMapping("/jvm/cpu-usage")
    public Map<String, String> getJvmCpuUsage() {
        Map<String, Double> cpuMetrics = new HashMap<>();
        Gauge processCpuGauge = meterRegistry.find("process.cpu.usage").gauge();
        if (processCpuGauge != null) {
            cpuMetrics.put("processCpuUsage", processCpuGauge.value());
        }
        Gauge systemCpuGauge = meterRegistry.find("system.cpu.usage").gauge();
        if (systemCpuGauge != null) {
            cpuMetrics.put("systemCpuUsage", systemCpuGauge.value());
        }
        Map<String, String> map = new HashMap<>();
        cpuMetrics.forEach((s, aDouble) -> map.put(s,
                BigDecimal.valueOf(aDouble).setScale(3, RoundingMode.HALF_UP).toPlainString()));
        return map;
    }

    /**
     * 获取 JVM 线程数统计。
     *
     * @return 包含线程数指标的Map：
     * - liveThreads: 当前活跃的线程数
     * - daemonThreads: 当前守护线程数
     * - peakThreads: JVM运行以来的峰值线程数
     */
    @GetMapping("/jvm/threads-count")
    public Map<String,Double> getJvmThreadsCount() {
        Map<String, Double> threadMetrics = new HashMap<>();
        Gauge liveThreadsGauge = meterRegistry.find("jvm.threads.live").gauge();
        if (liveThreadsGauge != null) {
            threadMetrics.put("liveThreads", liveThreadsGauge.value());
        }
        Gauge daemonThreadsGauge = meterRegistry.find("jvm.threads.daemon").gauge();
        if (daemonThreadsGauge != null) {
            threadMetrics.put("daemonThreads", daemonThreadsGauge.value());
        }
        Gauge peakThreadsGauge = meterRegistry.find("jvm.threads.peak").gauge();
        if (peakThreadsGauge != null) {
            threadMetrics.put("peakThreads", peakThreadsGauge.value());
        }
        return threadMetrics;
    }
}