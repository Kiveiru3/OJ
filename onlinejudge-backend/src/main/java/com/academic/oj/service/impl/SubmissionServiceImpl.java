package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.config.JudgeProperties;
import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.dto.SubmissionCaseResultDTO;
import com.academic.oj.dto.SubmissionStatusDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.dto.UserPointRankingVO;
import com.academic.oj.dto.UserPointSummaryVO;
import com.academic.oj.entity.JudgeResult;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.SubmissionCaseResult;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.JudgeResultMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionCaseResultMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.JudgeService;
import com.academic.oj.service.SubmissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final SubmissionCaseResultMapper submissionCaseResultMapper;
    private final JudgeResultMapper judgeResultMapper;
    private final ProblemMapper problemMapper;
    private final UserMapper userMapper;
    private final JudgeService judgeService;
    private final JudgeProperties judgeProperties;
    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    @Override
    @Transactional
    public Submission submit(Long userId, SubmitDTO submitDTO) {
        validateSubmit(submitDTO);

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(submitDTO.getProblemId());
        submission.setCode(submitDTO.getCode());
        submission.setLanguage(submitDTO.getLanguage());
        submission.setStatus("PENDING");
        submission.setCreateTime(LocalDateTime.now());
        
        submissionMapper.insert(submission);
        persistJudgeResult(submission, null);
        increaseProblemSubmitCount(submitDTO.getProblemId());
        
        // 异步判题
        scheduleJudge(submission);
        
        return submission;
    }

    private void scheduleJudge(Submission submission) {
        Runnable task = () -> processJudge(submission);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(task);
                }
            });
            return;
        }
        taskExecutor.execute(task);
    }

    public void processJudge(Submission submission) {
        Submission current = submissionMapper.selectById(submission.getId());
        if (current == null) {
            log.warn("Submission not found when judge task starts: {}", submission.getId());
            return;
        }

        // Idempotent claim: only one worker can move PENDING -> JUDGING.
        LambdaUpdateWrapper<Submission> claimWrapper = new LambdaUpdateWrapper<>();
        claimWrapper.eq(Submission::getId, submission.getId())
                .eq(Submission::getStatus, Constants.STATUS_PENDING)
                .set(Submission::getStatus, Constants.STATUS_JUDGING);
        int claimed = submissionMapper.update(null, claimWrapper);
        if (claimed == 0) {
            log.info("Skip duplicate judge task for submission={}, currentStatus={}", submission.getId(), current.getStatus());
            return;
        }

        Submission judgingSubmission = submissionMapper.selectById(submission.getId());
        if (judgingSubmission == null) {
            log.warn("Submission disappeared after claim: {}", submission.getId());
            return;
        }

        try {
            judgingSubmission.setStatus(Constants.STATUS_JUDGING);
            persistJudgeResult(judgingSubmission, null);

            int maxAttempts = resolveMaxJudgeAttempts();
            Submission result = null;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                Submission attemptSubmission = submissionMapper.selectById(judgingSubmission.getId());
                if (attemptSubmission == null) {
                    log.warn("Submission missing before judge attempt. submissionId={}, attempt={}",
                            judgingSubmission.getId(), attempt);
                    return;
                }
                attemptSubmission.setStatus(Constants.STATUS_JUDGING);
                attemptSubmission.setErrorMessage(null);

                try {
                    result = judgeService.judge(attemptSubmission);
                } catch (Exception ex) {
                    log.error("Judge execution exception. submissionId={}, attempt={}", attemptSubmission.getId(), attempt, ex);
                    attemptSubmission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                    attemptSubmission.setErrorMessage("Judge failed: " + ex.getMessage());
                    result = attemptSubmission;
                }

                if (!shouldRetryJudge(result, attempt, maxAttempts)) {
                    break;
                }

                log.warn("Retry judge for submission={}, attempt={}/{}, status={}, message={}",
                        judgingSubmission.getId(), attempt, maxAttempts,
                        result == null ? "null" : result.getStatus(),
                        result == null ? "" : result.getErrorMessage());
                sleepBeforeRetry(attempt);
            }

            if (result == null) {
                judgingSubmission.setStatus(Constants.STATUS_RUNTIME_ERROR);
                judgingSubmission.setErrorMessage("Judge failed: empty result");
                result = judgingSubmission;
            }
            submissionMapper.updateById(result);
            persistCaseResults(result);
            persistJudgeResult(result, LocalDateTime.now());
            if (Constants.STATUS_ACCEPTED.equals(result.getStatus())) {
                increaseProblemAcceptedCount(result.getProblemId());
            }
        } catch (Exception e) {
            log.error("Judge failed for submission: {}", judgingSubmission.getId(), e);
            judgingSubmission.setStatus(Constants.STATUS_RUNTIME_ERROR);
            judgingSubmission.setErrorMessage("Judge failed: " + e.getMessage());
            submissionMapper.updateById(judgingSubmission);
            persistCaseResults(judgingSubmission);
            persistJudgeResult(judgingSubmission, LocalDateTime.now());
        }
    }

    private int resolveMaxJudgeAttempts() {
        if (judgeProperties == null || judgeProperties.getRetry() == null || judgeProperties.getRetry().getMaxAttempts() == null) {
            return 1;
        }
        return Math.max(1, judgeProperties.getRetry().getMaxAttempts());
    }

    private long resolveRetryBackoffMs() {
        if (judgeProperties == null || judgeProperties.getRetry() == null || judgeProperties.getRetry().getBackoffMs() == null) {
            return 0L;
        }
        return Math.max(0L, judgeProperties.getRetry().getBackoffMs());
    }

    private boolean shouldRetryJudge(Submission result, int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return false;
        }
        if (result == null) {
            return true;
        }
        if (!Constants.STATUS_RUNTIME_ERROR.equals(result.getStatus())) {
            return false;
        }
        String message = result.getErrorMessage();
        if (message == null || message.isBlank()) {
            return true;
        }
        String normalized = message.trim().toLowerCase();
        return normalized.startsWith("judge failed:")
                || normalized.startsWith("judge error:")
                || normalized.startsWith("sandbox judge failed:")
                || normalized.startsWith("sandbox error:")
                || normalized.startsWith("java toolchain unavailable:")
                || normalized.startsWith("c++ toolchain unavailable:")
                || normalized.startsWith("python runtime unavailable:")
                || normalized.contains("docker");
    }

    private void sleepBeforeRetry(int attempt) {
        long backoffMs = resolveRetryBackoffMs();
        if (backoffMs <= 0) {
            return;
        }
        long sleepMs = backoffMs * Math.max(1, attempt);
        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Judge retry sleep interrupted");
        }
    }

    @Override
    public Submission getSubmissionEntityById(Long id) {
        return submissionMapper.selectById(id);
    }

    @Override
    public SubmissionStatusDTO getSubmissionStatusById(Long id) {
        Submission submission = getSubmissionEntityById(id);
        return toStatusDTO(submission);
    }

    @Override
    public SubmissionVO getSubmissionById(Long id) {
        Submission submission = getSubmissionEntityById(id);
        return toVO(submission, Collections.emptyMap());
    }

    @Override
    public Page<SubmissionVO> getSubmissionList(Long userId, Integer page, Integer size,
                                                Long problemId, String status, String language) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, userId);
        wrapper.eq(problemId != null, Submission::getProblemId, problemId);
        wrapper.eq(status != null && !status.isBlank(), Submission::getStatus, status);
        wrapper.eq(language != null && !language.isBlank(), Submission::getLanguage, language);
        wrapper.orderByDesc(Submission::getCreateTime);

        Page<Submission> submissionPage = submissionMapper.selectPage(pageObj, wrapper);
        Page<SubmissionVO> resultPage = new Page<>(
                submissionPage.getCurrent(),
                submissionPage.getSize(),
                submissionPage.getTotal()
        );
        Map<Long, String> problemTitleMap = loadProblemTitleMap(submissionPage.getRecords());
        resultPage.setRecords(submissionPage.getRecords().stream()
                .map(submission -> toVO(submission, problemTitleMap))
                .toList());
        return resultPage;
    }

    @Override
    public List<UserPointRankingVO> getPointRanking(Integer size) {
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        List<UserScoreAggregate> ranking = buildUserScoreAggregates();
        if (ranking.isEmpty()) {
            return Collections.emptyList();
        }
        int limit = Math.min(safeSize, ranking.size());
        List<UserPointRankingVO> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            UserScoreAggregate item = ranking.get(i);
            UserPointRankingVO vo = new UserPointRankingVO();
            vo.setRank(i + 1);
            vo.setUserId(item.userId);
            vo.setUsername(item.username);
            vo.setNickname(item.nickname);
            vo.setAvatar(item.avatar);
            vo.setSolvedCount(item.solvedCount);
            vo.setPoints(item.points);
            result.add(vo);
        }
        return result;
    }

    @Override
    public UserPointSummaryVO getMyPointSummary(Long userId) {
        UserPointSummaryVO summary = new UserPointSummaryVO();
        summary.setUserId(userId);
        summary.setRank(0);
        summary.setSolvedCount(0);
        summary.setPoints(0);
        if (userId == null) {
            return summary;
        }

        List<UserScoreAggregate> ranking = buildUserScoreAggregates();
        for (int i = 0; i < ranking.size(); i++) {
            UserScoreAggregate item = ranking.get(i);
            if (userId.equals(item.userId)) {
                summary.setRank(i + 1);
                summary.setSolvedCount(item.solvedCount);
                summary.setPoints(item.points);
                return summary;
            }
        }
        return summary;
    }

    @Override
    public List<Submission> getSubmissionsByUserId(Long userId, Integer page, Integer size) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, userId);
        wrapper.orderByDesc(Submission::getCreateTime);
        return submissionMapper.selectPage(pageObj, wrapper).getRecords();
    }

    @Override
    public List<Submission> getSubmissionsByProblemId(Long problemId, Integer page, Integer size) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getProblemId, problemId);
        wrapper.orderByDesc(Submission::getCreateTime);
        return submissionMapper.selectPage(pageObj, wrapper).getRecords();
    }

    @Override
    @Transactional
    public void rejudgeSubmission(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Submission not found");
        }

        if (Constants.STATUS_ACCEPTED.equals(submission.getStatus())) {
            decreaseProblemAcceptedCount(submission.getProblemId());
        }

        submission.setStatus(Constants.STATUS_PENDING);
        submission.setTimeUsed(null);
        submission.setMemoryUsed(null);
        submission.setErrorMessage(null);
        submissionMapper.updateById(submission);
        submissionCaseResultMapper.delete(
                new LambdaQueryWrapper<SubmissionCaseResult>()
                        .eq(SubmissionCaseResult::getSubmissionId, submission.getId())
        );
        persistJudgeResult(submission, null);
        scheduleJudge(submission);
    }

    private Map<Long, String> loadProblemTitleMap(List<Submission> submissions) {
        Set<Long> problemIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (problemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Problem::getId, problemIds);
        return problemMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));
    }

    private SubmissionVO toVO(Submission submission, Map<Long, String> problemTitleMap) {
        if (submission == null) {
            return null;
        }

        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setProblemId(submission.getProblemId());
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setExecuteTime(submission.getTimeUsed());
        vo.setExecuteMemory(submission.getMemoryUsed());
        vo.setCode(submission.getCode());
        vo.setErrorMessage(submission.getErrorMessage());
        vo.setSubmitTime(submission.getCreateTime());

        if (submission.getProblemId() != null) {
            String problemTitle = problemTitleMap.get(submission.getProblemId());
            if (problemTitle == null) {
                Problem problem = problemMapper.selectById(submission.getProblemId());
                problemTitle = problem != null ? problem.getTitle() : null;
            }
            vo.setProblemTitle(problemTitle);
        }

        return vo;
    }

    private SubmissionStatusDTO toStatusDTO(Submission submission) {
        if (submission == null) {
            return null;
        }
        SubmissionStatusDTO dto = new SubmissionStatusDTO();
        dto.setId(submission.getId());
        dto.setStatus(submission.getStatus());
        dto.setExecuteTime(submission.getTimeUsed());
        dto.setExecuteMemory(submission.getMemoryUsed());
        dto.setErrorMessage(submission.getErrorMessage());
        dto.setSubmitTime(submission.getCreateTime());
        dto.setCaseResults(loadCaseResults(submission.getId()));
        return dto;
    }

    private void increaseProblemSubmitCount(Long problemId) {
        if (problemId == null) {
            return;
        }
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, problemId)
                .setSql("submit_count = COALESCE(submit_count, 0) + 1");
        problemMapper.update(null, updateWrapper);
    }

    private void increaseProblemAcceptedCount(Long problemId) {
        if (problemId == null) {
            return;
        }
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, problemId)
                .setSql("ac_count = COALESCE(ac_count, 0) + 1");
        problemMapper.update(null, updateWrapper);
    }

    private void decreaseProblemAcceptedCount(Long problemId) {
        if (problemId == null) {
            return;
        }
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, problemId)
                .setSql("ac_count = GREATEST(COALESCE(ac_count, 0) - 1, 0)");
        problemMapper.update(null, updateWrapper);
    }

    private void validateSubmit(SubmitDTO submitDTO) {
        Problem problem = problemMapper.selectById(submitDTO.getProblemId());
        if (problem == null || !Integer.valueOf(1).equals(problem.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Problem not found");
        }

        String language = submitDTO.getLanguage();
        if (!Constants.LANGUAGE_JAVA.equals(language)
                && !Constants.LANGUAGE_CPP.equals(language)
                && !Constants.LANGUAGE_PYTHON.equals(language)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Unsupported language");
        }
    }

    private void persistJudgeResult(Submission submission, LocalDateTime judgeTime) {
        if (submission == null || submission.getId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        JudgeResult entity = judgeResultMapper.selectOne(
                new LambdaQueryWrapper<JudgeResult>()
                        .eq(JudgeResult::getSubmissionId, submission.getId())
                        .last("LIMIT 1")
        );
        boolean isNew = entity == null;
        if (isNew) {
            entity = new JudgeResult();
            entity.setSubmissionId(submission.getId());
            entity.setCreateTime(now);
        }

        entity.setUserId(submission.getUserId());
        entity.setProblemId(submission.getProblemId());
        entity.setLanguage(submission.getLanguage());
        entity.setStatus(submission.getStatus());
        entity.setTimeUsed(submission.getTimeUsed());
        entity.setMemoryUsed(submission.getMemoryUsed());
        entity.setErrorMessage(submission.getErrorMessage());
        entity.setJudgeTime(judgeTime);
        entity.setUpdateTime(now);

        if (isNew) {
            judgeResultMapper.insert(entity);
        } else {
            judgeResultMapper.updateById(entity);
        }
    }

    private void persistCaseResults(Submission submission) {
        if (submission == null || submission.getId() == null) {
            return;
        }
        submissionCaseResultMapper.delete(
                new LambdaQueryWrapper<SubmissionCaseResult>()
                        .eq(SubmissionCaseResult::getSubmissionId, submission.getId())
        );
        List<SubmissionCaseResultDTO> caseResults = submission.getCaseResults();
        if (caseResults == null || caseResults.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SubmissionCaseResultDTO item : caseResults) {
            SubmissionCaseResult entity = new SubmissionCaseResult();
            entity.setSubmissionId(submission.getId());
            entity.setCaseNo(item.getCaseNo());
            entity.setIsSample(item.getIsSample());
            entity.setStatus(item.getStatus());
            entity.setTimeUsed(item.getTimeUsed());
            entity.setMemoryUsed(item.getMemoryUsed());
            entity.setInputPreview(item.getInputPreview());
            entity.setExpectedPreview(item.getExpectedPreview());
            entity.setActualPreview(item.getActualPreview());
            entity.setErrorMessage(item.getErrorMessage());
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            submissionCaseResultMapper.insert(entity);
        }
    }

    private List<SubmissionCaseResultDTO> loadCaseResults(Long submissionId) {
        if (submissionId == null) {
            return Collections.emptyList();
        }
        List<SubmissionCaseResult> entities = submissionCaseResultMapper.selectList(
                new LambdaQueryWrapper<SubmissionCaseResult>()
                        .eq(SubmissionCaseResult::getSubmissionId, submissionId)
                        .orderByAsc(SubmissionCaseResult::getCaseNo)
        );
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(item -> {
            SubmissionCaseResultDTO dto = new SubmissionCaseResultDTO();
            dto.setCaseNo(item.getCaseNo());
            dto.setIsSample(item.getIsSample());
            dto.setStatus(item.getStatus());
            dto.setTimeUsed(item.getTimeUsed());
            dto.setMemoryUsed(item.getMemoryUsed());
            dto.setInputPreview(item.getInputPreview());
            dto.setExpectedPreview(item.getExpectedPreview());
            dto.setActualPreview(item.getActualPreview());
            dto.setErrorMessage(item.getErrorMessage());
            return dto;
        }).toList();
    }

    private List<UserScoreAggregate> buildUserScoreAggregates() {
        List<Submission> acceptedSubmissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStatus, Constants.STATUS_ACCEPTED)
                        .select(Submission::getUserId, Submission::getProblemId, Submission::getCreateTime, Submission::getId)
        );
        if (acceptedSubmissions.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> problemIds = acceptedSubmissions.stream()
                .map(Submission::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> difficultyMap = problemIds.isEmpty() ? Collections.emptyMap()
                : problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getDifficulty, (a, b) -> a));

        acceptedSubmissions.sort(Comparator
                .comparing(Submission::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Submission::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<Long, Set<Long>> solvedMap = new HashMap<>();
        Map<Long, Integer> pointsMap = new HashMap<>();
        for (Submission submission : acceptedSubmissions) {
            if (submission.getUserId() == null || submission.getProblemId() == null) {
                continue;
            }
            Set<Long> solvedProblems = solvedMap.computeIfAbsent(submission.getUserId(), key -> new HashSet<>());
            if (!solvedProblems.add(submission.getProblemId())) {
                continue;
            }
            String difficulty = difficultyMap.get(submission.getProblemId());
            int add = difficultyPoints(difficulty);
            pointsMap.put(submission.getUserId(), pointsMap.getOrDefault(submission.getUserId(), 0) + add);
        }

        if (pointsMap.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, User> userMap = userMapper.selectBatchIds(pointsMap.keySet()).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        List<UserScoreAggregate> ranking = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : pointsMap.entrySet()) {
            Long userId = entry.getKey();
            int points = entry.getValue();
            int solvedCount = solvedMap.getOrDefault(userId, Collections.emptySet()).size();
            User user = userMap.get(userId);
            UserScoreAggregate item = new UserScoreAggregate();
            item.userId = userId;
            item.username = user == null ? null : user.getUsername();
            item.nickname = user == null ? null : user.getNickname();
            item.avatar = user == null ? null : user.getAvatar();
            item.solvedCount = solvedCount;
            item.points = points;
            ranking.add(item);
        }

        ranking.sort(Comparator
                .comparingInt((UserScoreAggregate item) -> item.points).reversed()
                .thenComparingInt((UserScoreAggregate item) -> item.solvedCount).reversed()
                .thenComparing(item -> item.userId, Comparator.nullsLast(Comparator.naturalOrder())));
        return ranking;
    }

    private int difficultyPoints(String difficulty) {
        if (difficulty == null) {
            return 15;
        }
        String v = difficulty.trim().toUpperCase();
        if ("EASY".equals(v)) {
            return 10;
        }
        if ("MEDIUM".equals(v)) {
            return 20;
        }
        if ("HARD".equals(v)) {
            return 40;
        }
        return 15;
    }

    private static class UserScoreAggregate {
        private Long userId;
        private String username;
        private String nickname;
        private String avatar;
        private int solvedCount;
        private int points;
    }
}

