package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.dto.SystemMonitorVO;
import com.academic.oj.entity.AdminOperationLog;
import com.academic.oj.entity.Contest;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.AdminOperationLogMapper;
import com.academic.oj.mapper.ContestMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.SystemMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SystemMonitorServiceImpl implements SystemMonitorService {

    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final ContestMapper contestMapper;
    private final AdminOperationLogMapper adminOperationLogMapper;

    private final Instant applicationStartTime = Instant.now();

    @Override
    public SystemMonitorVO getMonitor() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime logs24hStart = now.minusHours(24);

        long totalUsers = safeLong(userMapper.selectCount(new LambdaQueryWrapper<User>()));
        long enabledUsers = safeLong(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)));
        long newUsersToday = safeLong(userMapper.selectCount(new LambdaQueryWrapper<User>()
                .ge(User::getCreateTime, todayStart)));

        long totalProblems = safeLong(problemMapper.selectCount(new LambdaQueryWrapper<>()));
        long totalSubmissions = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<>()));
        long acceptedSubmissions = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, Constants.STATUS_ACCEPTED)));
        long pendingSubmissions = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, Constants.STATUS_PENDING)));
        long submissionsToday = safeLong(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                .ge(Submission::getCreateTime, todayStart)));

        long totalContests = safeLong(contestMapper.selectCount(new LambdaQueryWrapper<Contest>()));
        long runningContests = safeLong(contestMapper.selectCount(new LambdaQueryWrapper<Contest>()
                .eq(Contest::getStatus, 1)
                .le(Contest::getStartTime, now)
                .ge(Contest::getEndTime, now)));
        long operationLogs24h = safeLong(adminOperationLogMapper.selectCount(new LambdaQueryWrapper<AdminOperationLog>()
                .ge(AdminOperationLog::getCreateTime, logs24hStart)));

        Submission oldestPending = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getStatus, Constants.STATUS_PENDING)
                .orderByAsc(Submission::getCreateTime)
                .last("LIMIT 1"));

        LocalDateTime oldestPendingTime = oldestPending == null ? null : oldestPending.getCreateTime();
        long oldestPendingMinutes = oldestPendingTime == null ? 0 : Math.max(0, Duration.between(oldestPendingTime, now).toMinutes());

        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        SystemMonitorVO vo = new SystemMonitorVO();
        vo.setGeneratedAt(now);

        vo.setTotalUsers(totalUsers);
        vo.setEnabledUsers(enabledUsers);
        vo.setNewUsersToday(newUsersToday);

        vo.setTotalProblems(totalProblems);
        vo.setTotalSubmissions(totalSubmissions);
        vo.setAcceptedSubmissions(acceptedSubmissions);
        vo.setPendingSubmissions(pendingSubmissions);
        vo.setSubmissionsToday(submissionsToday);
        vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));

        vo.setTotalContests(totalContests);
        vo.setRunningContests(runningContests);
        vo.setOperationLogs24h(operationLogs24h);

        vo.setOldestPendingSubmissionTime(oldestPendingTime);
        vo.setOldestPendingMinutes(oldestPendingMinutes);
        vo.setQueueStatus(resolveQueueStatus(pendingSubmissions, oldestPendingMinutes));

        vo.setUptimeSeconds(Duration.between(applicationStartTime, Instant.now()).getSeconds());
        vo.setJvmMaxMemoryMb(toMb(maxMemory));
        vo.setJvmFreeMemoryMb(toMb(freeMemory));
        vo.setJvmUsedMemoryMb(toMb(usedMemory));
        vo.setThreadCount(threadMXBean.getThreadCount());
        vo.setAvailableProcessors(runtime.availableProcessors());
        return vo;
    }

    private String resolveQueueStatus(long pendingSubmissions, long oldestPendingMinutes) {
        if (pendingSubmissions <= 0) {
            return "NORMAL";
        }
        if (oldestPendingMinutes >= 30) {
            return "CRITICAL";
        }
        if (oldestPendingMinutes >= 5) {
            return "WARNING";
        }
        return "NORMAL";
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private long safeLong(Long value) {
        return value == null ? 0 : value;
    }

    private long toMb(long bytes) {
        return bytes / 1024 / 1024;
    }
}
