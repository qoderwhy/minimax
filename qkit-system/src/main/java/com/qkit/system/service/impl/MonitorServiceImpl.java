package com.qkit.system.service.impl;

import com.qkit.common.api.R;
import com.qkit.system.domain.vo.ServerInfoVO;
import com.qkit.system.service.MonitorService;
import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class MonitorServiceImpl implements MonitorService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public R<ServerInfoVO> getServerInfo() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        ServerInfoVO.CpuInfo cpuInfo = buildCpuInfo(osBean);
        ServerInfoVO.MemoryInfo memoryInfo = buildMemoryInfo(osBean, memoryBean);
        ServerInfoVO.ServerDetail serverDetail = buildServerDetail();
        ServerInfoVO.JvmInfo jvmInfo = buildJvmInfo(runtimeBean);
        List<ServerInfoVO.DiskInfo> diskInfoList = buildDiskInfo();

        return R.ok(new ServerInfoVO(cpuInfo, memoryInfo, serverDetail, jvmInfo, diskInfoList));
    }

    private ServerInfoVO.CpuInfo buildCpuInfo(OperatingSystemMXBean osBean) {
        int cores = osBean.getAvailableProcessors();
        double cpuLoad = osBean.getSystemCpuLoad();
        double processLoad = osBean.getProcessCpuLoad();
        // 系统使用率取系统 CPU 负载，用户使用率取进程 CPU 负载
        double systemUsage = Math.max(0, cpuLoad) * 100;
        double userUsage = Math.max(0, processLoad) * 100;
        double freeRate = Math.max(0, 100 - systemUsage);
        return new ServerInfoVO.CpuInfo(cores, userUsage, systemUsage, freeRate);
    }

    private ServerInfoVO.MemoryInfo buildMemoryInfo(OperatingSystemMXBean osBean, MemoryMXBean memoryBean) {
        long totalMemory = osBean.getTotalPhysicalMemorySize();
        long freeMemory = osBean.getFreePhysicalMemorySize();
        long usedMemory = totalMemory - freeMemory;
        double usageRate = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0;

        long jvmTotal = memoryBean.getHeapMemoryUsage().getMax();
        long jvmUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long jvmFree = jvmTotal - jvmUsed;
        double jvmUsageRate = jvmTotal > 0 ? (double) jvmUsed / jvmTotal * 100 : 0;

        return new ServerInfoVO.MemoryInfo(
                totalMemory, usedMemory, freeMemory, usageRate,
                jvmTotal, jvmUsed, jvmFree, jvmUsageRate
        );
    }

    private ServerInfoVO.ServerDetail buildServerDetail() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return new ServerInfoVO.ServerDetail(
                    addr.getHostName(),
                    addr.getHostAddress(),
                    System.getProperty("os.name"),
                    System.getProperty("os.arch")
            );
        } catch (Exception e) {
            return new ServerInfoVO.ServerDetail(
                    System.getProperty("host.name", "unknown"),
                    "unknown",
                    System.getProperty("os.name"),
                    System.getProperty("os.arch")
            );
        }
    }

    private ServerInfoVO.JvmInfo buildJvmInfo(RuntimeMXBean runtimeBean) {
        long startTimeMs = runtimeBean.getStartTime();
        LocalDateTime startTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTimeMs), ZoneId.systemDefault());
        long runTimeMs = runtimeBean.getUptime();
        Duration duration = Duration.ofMillis(runTimeMs);
        String runTime = String.format("%d天%d小时%d分钟",
                duration.toDays(),
                duration.toHoursPart(),
                duration.toMinutesPart());

        return new ServerInfoVO.JvmInfo(
                ManagementFactory.getRuntimeMXBean().getVmName(),
                System.getProperty("java.version"),
                startTime.format(FORMATTER),
                runTime,
                System.getProperty("java.home"),
                System.getProperty("user.dir"),
                runtimeBean.getInputArguments().toString()
        );
    }

    private List<ServerInfoVO.DiskInfo> buildDiskInfo() {
        List<ServerInfoVO.DiskInfo> diskList = new ArrayList<>();
        for (File root : File.listRoots()) {
            try {
                Path path = root.toPath();
                var store = Files.getFileStore(path);
                long total = store.getTotalSpace();
                long free = store.getUsableSpace();
                long used = total - free;
                double usageRate = total > 0 ? (double) used / total * 100 : 0;
                diskList.add(new ServerInfoVO.DiskInfo(
                        path.toString(),
                        store.type(),
                        store.name(),
                        total, free, used, usageRate
                ));
            } catch (Exception ignored) {
            }
        }
        return diskList;
    }
}
