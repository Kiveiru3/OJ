package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemMonitorVO {
    private LocalDateTime generatedAt;

    private Long totalUsers;
    private Long enabledUsers;
    private Long newUsersToday;

    private Long totalProblems;
    private Long totalSubmissions;
    private Long acceptedSubmissions;
    private Long pendingSubmissions;
    private Long submissionsToday;
    private Double acceptanceRate;

    private Long totalContests;
    private Long runningContests;
    private Long operationLogs24h;

    private LocalDateTime oldestPendingSubmissionTime;
    private Long oldestPendingMinutes;
    private String queueStatus; // NORMAL, WARNING, CRITICAL

    private Long uptimeSeconds;
    private Long jvmUsedMemoryMb;
    private Long jvmFreeMemoryMb;
    private Long jvmMaxMemoryMb;
    private Integer threadCount;
    private Integer availableProcessors;
}
