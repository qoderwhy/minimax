package com.qkit.system.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "服务器监控信息 VO")
public record ServerInfoVO(
        CpuInfo cpu,
        MemoryInfo memory,
        ServerDetail server,
        JvmInfo jvm,
        List<DiskInfo> disks
) {
    @Schema(description = "CPU 信息")
    public record CpuInfo(
            @Schema(description = "核心数") int cores,
            @Schema(description = "用户使用率(%)") double userUsage,
            @Schema(description = "系统使用率(%)") double systemUsage,
            @Schema(description = "当前空闲率(%)") double freeRate
    ) {}

    @Schema(description = "内存信息")
    public record MemoryInfo(
            @Schema(description = "总内存(字节)") long totalMemory,
            @Schema(description = "已用内存(字节)") long usedMemory,
            @Schema(description = "剩余内存(字节)") long freeMemory,
            @Schema(description = "使用率(%)") double usageRate,
            @Schema(description = "JVM 总内存(字节)") long jvmTotalMemory,
            @Schema(description = "JVM 已用内存(字节)") long jvmUsedMemory,
            @Schema(description = "JVM 剩余内存(字节)") long jvmFreeMemory,
            @Schema(description = "JVM 使用率(%)") double jvmUsageRate
    ) {}

    @Schema(description = "服务器详情")
    public record ServerDetail(
            @Schema(description = "服务器名称") String name,
            @Schema(description = "服务器 IP") String ip,
            @Schema(description = "操作系统") String os,
            @Schema(description = "系统架构") String arch
    ) {}

    @Schema(description = "JVM 信息")
    public record JvmInfo(
            @Schema(description = "Java 名称") String javaName,
            @Schema(description = "Java 版本") String javaVersion,
            @Schema(description = "启动时间") String startTime,
            @Schema(description = "运行时长") String runTime,
            @Schema(description = "安装路径") String installPath,
            @Schema(description = "项目路径") String projectPath,
            @Schema(description = "运行参数") String runArgs
    ) {}

    @Schema(description = "磁盘信息")
    public record DiskInfo(
            @Schema(description = "盘符路径") String path,
            @Schema(description = "文件系统") String fileSystem,
            @Schema(description = "盘符类型") String type,
            @Schema(description = "总大小(字节)") long totalSize,
            @Schema(description = "可用大小(字节)") long freeSize,
            @Schema(description = "已用大小(字节)") long usedSize,
            @Schema(description = "已用百分比(%)") double usageRate
    ) {}
}
