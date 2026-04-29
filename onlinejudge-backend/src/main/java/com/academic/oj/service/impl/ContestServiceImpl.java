package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.ContestAnalyticsVO;
import com.academic.oj.dto.ContestDetailVO;
import com.academic.oj.dto.ContestRankingItemVO;
import com.academic.oj.dto.ContestSaveDTO;
import com.academic.oj.dto.ContestVO;
import com.academic.oj.entity.Contest;
import com.academic.oj.entity.ContestParticipant;
import com.academic.oj.entity.ContestProblem;
import com.academic.oj.entity.ContestScore;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.ContestMapper;
import com.academic.oj.mapper.ContestParticipantMapper;
import com.academic.oj.mapper.ContestProblemMapper;
import com.academic.oj.mapper.ContestScoreMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.ContestService;
import com.academic.oj.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {
    private static final String CONFIG_KEY_DEFAULT_PENALTY = "contest.default_penalty_per_wrong";
    private static final int DEFAULT_PENALTY_PER_WRONG = 20;

    private final ContestMapper contestMapper;
    private final ContestProblemMapper contestProblemMapper;
    private final ContestParticipantMapper contestParticipantMapper;
    private final ContestScoreMapper contestScoreMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final UserMapper userMapper;
    private final SystemConfigService systemConfigService;

    @Override
    public Page<ContestVO> getContestList(Long userId, Integer page, Integer size, String keyword, boolean canViewHidden) {
        Page<Contest> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), Contest::getTitle, keyword);
        wrapper.eq(!canViewHidden, Contest::getStatus, 1);
        wrapper.orderByDesc(Contest::getStartTime);
        Page<Contest> contestPage = contestMapper.selectPage(pageObj, wrapper);

        Page<ContestVO> resultPage = new Page<>(contestPage.getCurrent(), contestPage.getSize(), contestPage.getTotal());
        List<Contest> contests = contestPage.getRecords();
        if (contests.isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }

        List<Long> contestIds = contests.stream().map(Contest::getId).toList();
        Map<Long, Integer> participantCountMap = loadParticipantCountMap(contestIds);
        Map<Long, Integer> problemCountMap = loadProblemCountMap(contestIds);
        Set<Long> joinedContestIds = loadJoinedContestIds(userId, contestIds);

        List<ContestVO> records = contests.stream()
                .map(contest -> toContestVO(contest, participantCountMap, problemCountMap, joinedContestIds, canViewHidden))
                .toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    public ContestDetailVO getContestDetail(Long userId, Long contestId, boolean canViewHidden) {
        Contest contest = getContestOrThrow(contestId, canViewHidden);
        List<ContestProblem> contestProblems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId)
                        .orderByAsc(ContestProblem::getId)
        );

        List<Long> problemIds = contestProblems.stream().map(ContestProblem::getProblemId).toList();
        Map<Long, Problem> problemMap = loadProblemMap(problemIds);

        ContestDetailVO detailVO = new ContestDetailVO();
        detailVO.setId(contest.getId());
        detailVO.setTitle(contest.getTitle());
        detailVO.setDescription(contest.getDescription());
        detailVO.setStartTime(contest.getStartTime());
        detailVO.setEndTime(contest.getEndTime());
        detailVO.setScoreboardFreezeTime(contest.getScoreboardFreezeTime());
        detailVO.setPenaltyPerWrong(resolvePenaltyPerWrong(contest));
        detailVO.setCreatorId(contest.getCreatorId());
        detailVO.setStatus(contest.getStatus());
        detailVO.setContestStatus(calculateContestStatus(contest.getStartTime(), contest.getEndTime()));
        detailVO.setRankingFrozen(isRankingFrozen(contest, canViewHidden));
        detailVO.setParticipantCount(loadParticipantCountMap(List.of(contestId)).getOrDefault(contestId, 0));
        detailVO.setProblemCount(problemIds.size());
        detailVO.setJoined(isJoined(userId, contestId));
        detailVO.setProblems(contestProblems.stream()
                .map(cp -> toProblemItemVO(problemMap.get(cp.getProblemId())))
                .filter(item -> item != null)
                .toList());
        return detailVO;
    }

    @Override
    @Transactional
    public Long createContest(Long creatorId, ContestSaveDTO dto) {
        List<Long> validProblemIds = validateContestSaveDTO(dto);

        Contest contest = new Contest();
        contest.setTitle(dto.getTitle().trim());
        contest.setDescription(dto.getDescription());
        contest.setStartTime(dto.getStartTime());
        contest.setEndTime(dto.getEndTime());
        contest.setScoreboardFreezeTime(dto.getScoreboardFreezeTime());
        contest.setPenaltyPerWrong(resolvePenaltyPerWrongForSave(dto.getPenaltyPerWrong(), null));
        contest.setCreatorId(creatorId);
        contest.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        contest.setCreateTime(LocalDateTime.now());
        contest.setUpdateTime(LocalDateTime.now());
        contestMapper.insert(contest);

        replaceContestProblems(contest.getId(), validProblemIds);
        return contest.getId();
    }

    @Override
    @Transactional
    public void updateContest(Long operatorId, Long contestId, boolean isAdmin, ContestSaveDTO dto) {
        List<Long> validProblemIds = validateContestSaveDTO(dto);

        Contest existing = getContestOrThrow(contestId, true);
        if (!isAdmin && !operatorId.equals(existing.getCreatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }

        Contest contest = new Contest();
        contest.setId(contestId);
        contest.setTitle(dto.getTitle().trim());
        contest.setDescription(dto.getDescription());
        contest.setStartTime(dto.getStartTime());
        contest.setEndTime(dto.getEndTime());
        contest.setScoreboardFreezeTime(dto.getScoreboardFreezeTime());
        contest.setPenaltyPerWrong(resolvePenaltyPerWrongForSave(dto.getPenaltyPerWrong(), existing.getPenaltyPerWrong()));
        contest.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        contest.setUpdateTime(LocalDateTime.now());
        contestMapper.updateById(contest);

        replaceContestProblems(contestId, validProblemIds);
    }

    @Override
    @Transactional
    public void deleteContest(Long operatorId, Long contestId, boolean isAdmin) {
        Contest existing = getContestOrThrow(contestId, true);
        if (!isAdmin && !operatorId.equals(existing.getCreatorId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }

        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        contestParticipantMapper.delete(new LambdaQueryWrapper<ContestParticipant>()
                .eq(ContestParticipant::getContestId, contestId));
        contestScoreMapper.delete(new LambdaQueryWrapper<ContestScore>()
                .eq(ContestScore::getContestId, contestId));
        contestMapper.deleteById(contestId);
    }

    @Override
    @Transactional
    public void joinContest(Long userId, Long contestId) {
        Contest contest = getContestOrThrow(contestId, false);
        if (Integer.valueOf(0).equals(contest.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Contest is hidden");
        }

        if (LocalDateTime.now().isAfter(contest.getEndTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Contest already ended");
        }

        if (isJoined(userId, contestId)) {
            return;
        }

        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(contestId);
        participant.setUserId(userId);
        contestParticipantMapper.insert(participant);
    }

    @Override
    public Page<ContestRankingItemVO> getContestRanking(Long contestId, Integer page, Integer size, boolean canViewHidden) {
        List<ContestRankingItemVO> ranking = getContestRankingAll(contestId, canViewHidden);
        long total = ranking.size();
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        int fromIndex = (safePage - 1) * safeSize;
        int toIndex = Math.min(fromIndex + safeSize, ranking.size());
        List<ContestRankingItemVO> records = fromIndex >= ranking.size()
                ? Collections.emptyList()
                : ranking.subList(fromIndex, toIndex);

        Page<ContestRankingItemVO> pageObj = new Page<>(safePage, safeSize, total);
        pageObj.setRecords(records);
        return pageObj;
    }

    @Override
    public Page<ContestRankingItemVO> getContestScoreSnapshot(Long contestId, Integer page, Integer size, boolean canViewHidden) {
        getContestOrThrow(contestId, canViewHidden);
        int safePage = page == null || page < 1 ? 1 : page;
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);

        Page<ContestScore> scorePage = contestScoreMapper.selectPage(
                new Page<>(safePage, safeSize),
                new LambdaQueryWrapper<ContestScore>()
                        .eq(ContestScore::getContestId, contestId)
                        .orderByAsc(ContestScore::getRankNo)
                        .orderByAsc(ContestScore::getId)
        );

        if (scorePage.getRecords().isEmpty()) {
            return getContestRanking(contestId, safePage, safeSize, canViewHidden);
        }

        List<Long> userIds = scorePage.getRecords().stream()
                .map(ContestScore::getUserId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, User> userMap = loadUserMap(userIds);

        Page<ContestRankingItemVO> resultPage = new Page<>(safePage, safeSize, scorePage.getTotal());
        resultPage.setRecords(scorePage.getRecords().stream().map(score -> {
            ContestRankingItemVO vo = new ContestRankingItemVO();
            vo.setRank(score.getRankNo());
            vo.setUserId(score.getUserId());
            User user = userMap.get(score.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
            vo.setNickname(user == null ? null : user.getNickname());
            vo.setAvatar(user == null ? null : user.getAvatar());
            vo.setAcceptedCount(score.getAcceptedCount());
            vo.setTotalPenalty(score.getTotalPenalty());
            vo.setTotalSubmissions(score.getTotalSubmissions());
            vo.setLastAcceptedTime(score.getLastAcceptedTime());
            return vo;
        }).toList());
        return resultPage;
    }

    @Override
    public List<ContestRankingItemVO> getContestRankingAll(Long contestId, boolean canViewHidden) {
        Contest contest = getContestOrThrow(contestId, canViewHidden);
        List<Long> problemIds = contestProblemMapper.selectList(
                        new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId))
                .stream().map(ContestProblem::getProblemId).toList();

        if (problemIds.isEmpty()) {
            persistContestScores(contestId, Collections.emptyList());
            return Collections.emptyList();
        }

        List<Long> participantIds = contestParticipantMapper.selectList(
                        new LambdaQueryWrapper<ContestParticipant>().eq(ContestParticipant::getContestId, contestId))
                .stream().map(ContestParticipant::getUserId).distinct().toList();

        if (participantIds.isEmpty()) {
            persistContestScores(contestId, Collections.emptyList());
            return Collections.emptyList();
        }

        LocalDateTime rankingEnd = resolveRankingEnd(contest, canViewHidden);

        LambdaQueryWrapper<Submission> submissionWrapper = new LambdaQueryWrapper<>();
        submissionWrapper.in(Submission::getUserId, participantIds)
                .in(Submission::getProblemId, problemIds)
                .ge(Submission::getCreateTime, contest.getStartTime())
                .le(Submission::getCreateTime, rankingEnd)
                .orderByAsc(Submission::getCreateTime)
                .orderByAsc(Submission::getId);
        List<Submission> submissions = filterSubmissionsByWindow(
                submissionMapper.selectList(submissionWrapper), contest.getStartTime(), rankingEnd);
        List<ContestRankingItemVO> ranking = buildRanking(contest, participantIds, submissions);
        persistContestScores(contestId, ranking);
        return ranking;
    }

    @Override
    public ContestAnalyticsVO getContestAnalytics(Long contestId, boolean canViewHidden) {
        Contest contest = getContestOrThrow(contestId, canViewHidden);
        List<ContestProblem> contestProblems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId));
        List<Long> problemIds = contestProblems.stream().map(ContestProblem::getProblemId).distinct().toList();
        List<Long> participantIds = contestParticipantMapper.selectList(
                        new LambdaQueryWrapper<ContestParticipant>().eq(ContestParticipant::getContestId, contestId))
                .stream().map(ContestParticipant::getUserId).distinct().toList();

        ContestAnalyticsVO vo = new ContestAnalyticsVO();
        vo.setContestId(contestId);
        vo.setParticipantCount(participantIds.size());
        vo.setStatusDistribution(new LinkedHashMap<>());
        vo.setLanguageDistribution(new LinkedHashMap<>());
        vo.setSolvedDistribution(Collections.emptyList());
        vo.setTopPerformers(Collections.emptyList());
        vo.setProblemStats(Collections.emptyList());
        vo.setActiveParticipantCount(0);
        vo.setTotalSubmissions(0);
        vo.setAcceptedSubmissions(0);
        vo.setAcceptanceRate(0.0);

        if (problemIds.isEmpty() || participantIds.isEmpty()) {
            return vo;
        }

        LocalDateTime rankingEnd = resolveRankingEnd(contest, canViewHidden);
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .in(Submission::getUserId, participantIds)
                        .in(Submission::getProblemId, problemIds)
                        .ge(Submission::getCreateTime, contest.getStartTime())
                        .le(Submission::getCreateTime, rankingEnd)
                        .orderByAsc(Submission::getCreateTime)
                        .orderByAsc(Submission::getId)
        );
        submissions = filterSubmissionsByWindow(submissions, contest.getStartTime(), rankingEnd);

        int totalSubmissions = submissions.size();
        int acceptedSubmissions = 0;
        Set<Long> activeUsers = new HashSet<>();
        Map<String, Integer> statusDist = new LinkedHashMap<>();
        Map<String, Integer> languageDist = new LinkedHashMap<>();
        Map<Long, ProblemAnalyticsAccumulator> problemStatMap = new LinkedHashMap<>();
        Map<Long, Problem> problemMap = loadProblemMap(problemIds);
        for (Long problemId : problemIds) {
            problemStatMap.put(problemId, new ProblemAnalyticsAccumulator());
        }

        for (Submission submission : submissions) {
            activeUsers.add(submission.getUserId());
            incrementMapCount(statusDist, submission.getStatus());
            incrementMapCount(languageDist, submission.getLanguage());

            ProblemAnalyticsAccumulator acc = problemStatMap.computeIfAbsent(
                    submission.getProblemId(), k -> new ProblemAnalyticsAccumulator());
            acc.totalSubmissions++;
            if (Constants.STATUS_ACCEPTED.equals(submission.getStatus())) {
                acceptedSubmissions++;
                acc.acceptedSubmissions++;
                acc.acceptedUsers.add(submission.getUserId());
            }
        }

        List<ContestRankingItemVO> ranking = buildRanking(contest, participantIds, submissions);
        Map<Integer, Integer> solvedMap = new LinkedHashMap<>();
        for (ContestRankingItemVO item : ranking) {
            incrementMapCount(solvedMap, item.getAcceptedCount());
        }
        List<ContestAnalyticsVO.SolvedDistributionItemVO> solvedDistribution = solvedMap.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByKey().reversed())
                .map(entry -> {
                    ContestAnalyticsVO.SolvedDistributionItemVO item = new ContestAnalyticsVO.SolvedDistributionItemVO();
                    item.setSolvedCount(entry.getKey());
                    item.setUserCount(entry.getValue());
                    return item;
                })
                .toList();

        List<ContestAnalyticsVO.ProblemAnalyticsItemVO> problemStats = problemIds.stream()
                .map(problemId -> toProblemAnalyticsItem(problemMap.get(problemId), problemStatMap.get(problemId)))
                .toList();

        vo.setActiveParticipantCount(activeUsers.size());
        vo.setTotalSubmissions(totalSubmissions);
        vo.setAcceptedSubmissions(acceptedSubmissions);
        vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));
        vo.setStatusDistribution(statusDist);
        vo.setLanguageDistribution(languageDist);
        vo.setSolvedDistribution(solvedDistribution);
        vo.setTopPerformers(ranking.stream().limit(10).toList());
        vo.setProblemStats(problemStats);
        return vo;
    }

    private ContestRankingItemVO toRankingItem(Long userId, UserStats stats, User user, int rank) {
        ContestRankingItemVO vo = new ContestRankingItemVO();
        vo.setRank(rank);
        vo.setUserId(userId);
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setAvatar(user != null ? user.getAvatar() : null);
        vo.setAcceptedCount(stats.acceptedCount);
        vo.setTotalPenalty(stats.totalPenalty);
        vo.setTotalSubmissions(stats.totalSubmissions);
        vo.setLastAcceptedTime(stats.lastAcceptedTime);
        return vo;
    }

    private ContestAnalyticsVO.ProblemAnalyticsItemVO toProblemAnalyticsItem(
            Problem problem, ProblemAnalyticsAccumulator acc) {
        ContestAnalyticsVO.ProblemAnalyticsItemVO item = new ContestAnalyticsVO.ProblemAnalyticsItemVO();
        item.setProblemId(problem != null ? problem.getId() : null);
        item.setTitle(problem != null ? problem.getTitle() : null);
        item.setDifficulty(problem != null ? problem.getDifficulty() : null);
        int totalSubmissions = acc == null ? 0 : acc.totalSubmissions;
        int acceptedSubmissions = acc == null ? 0 : acc.acceptedSubmissions;
        int acceptedUserCount = acc == null ? 0 : acc.acceptedUsers.size();
        item.setTotalSubmissions(totalSubmissions);
        item.setAcceptedSubmissions(acceptedSubmissions);
        item.setAcceptedUserCount(acceptedUserCount);
        item.setPassRate(calculateRate(acceptedSubmissions, totalSubmissions));
        return item;
    }

    private List<ContestRankingItemVO> buildRanking(Contest contest, List<Long> participantIds, List<Submission> submissions) {
        int penaltyPerWrong = resolvePenaltyPerWrong(contest);
        Map<Long, UserStats> statsMap = new LinkedHashMap<>();
        for (Long userId : participantIds) {
            statsMap.put(userId, new UserStats());
        }

        for (Submission submission : submissions) {
            UserStats userStats = statsMap.get(submission.getUserId());
            if (userStats == null) {
                continue;
            }
            userStats.totalSubmissions++;

            ProblemStats problemStats = userStats.problemStats.computeIfAbsent(submission.getProblemId(), id -> new ProblemStats());
            if (problemStats.solved) {
                continue;
            }
            if (Constants.STATUS_ACCEPTED.equals(submission.getStatus())) {
                problemStats.solved = true;
                userStats.acceptedCount++;
                long minutes = Math.max(0, Duration.between(contest.getStartTime(), submission.getCreateTime()).toMinutes());
                userStats.totalPenalty += (int) minutes + problemStats.wrongAttempts * penaltyPerWrong;
                userStats.lastAcceptedTime = maxTime(userStats.lastAcceptedTime, submission.getCreateTime());
            } else {
                problemStats.wrongAttempts++;
            }
        }

        Map<Long, User> userMap = loadUserMap(participantIds);
        List<Map.Entry<Long, UserStats>> sorted = new ArrayList<>(statsMap.entrySet());
        sorted.sort(Comparator
                .comparingInt((Map.Entry<Long, UserStats> entry) -> entry.getValue().acceptedCount).reversed()
                .thenComparingInt(entry -> entry.getValue().totalPenalty)
                .thenComparing(entry -> entry.getValue().lastAcceptedTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(Map.Entry::getKey));

        List<ContestRankingItemVO> ranking = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<Long, UserStats> entry : sorted) {
            ranking.add(toRankingItem(entry.getKey(), entry.getValue(), userMap.get(entry.getKey()), rank));
            rank++;
        }
        return ranking;
    }

    private List<Long> validateContestSaveDTO(ContestSaveDTO dto) {
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid contest time");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "End time must be after start time");
        }
        if (dto.getScoreboardFreezeTime() != null) {
            if (dto.getScoreboardFreezeTime().isBefore(dto.getStartTime())
                    || dto.getScoreboardFreezeTime().isAfter(dto.getEndTime())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "Scoreboard freeze time must be between start and end time");
            }
        }
        if (dto.getStatus() != null && !Integer.valueOf(0).equals(dto.getStatus()) && !Integer.valueOf(1).equals(dto.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid contest status");
        }
        if (dto.getPenaltyPerWrong() != null && (dto.getPenaltyPerWrong() < 0 || dto.getPenaltyPerWrong() > 120)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid penalty per wrong submission");
        }

        List<Long> validProblemIds = sanitizeProblemIds(dto.getProblemIds());
        if (validProblemIds.isEmpty()) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "At least one problem is required");
        }
        ensureProblemsExist(validProblemIds);
        return validProblemIds;
    }

    private List<Long> sanitizeProblemIds(List<Long> problemIds) {
        if (problemIds == null) {
            return Collections.emptyList();
        }
        return problemIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private void ensureProblemsExist(List<Long> distinctIds) {
        List<Problem> problems = problemMapper.selectBatchIds(distinctIds);
        if (problems.size() != distinctIds.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Some problems do not exist");
        }
    }

    private void replaceContestProblems(Long contestId, List<Long> problemIds) {
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>().eq(ContestProblem::getContestId, contestId));
        Set<Long> deduplicated = new java.util.LinkedHashSet<>(problemIds);
        for (Long problemId : deduplicated) {
            ContestProblem contestProblem = new ContestProblem();
            contestProblem.setContestId(contestId);
            contestProblem.setProblemId(problemId);
            contestProblemMapper.insert(contestProblem);
        }
    }

    private Contest getContestOrThrow(Long contestId, boolean canViewHidden) {
        Contest contest = contestMapper.selectById(contestId);
        if (contest == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Contest not found");
        }
        if (!canViewHidden && Integer.valueOf(0).equals(contest.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
        return contest;
    }

    private Set<Long> loadJoinedContestIds(Long userId, List<Long> contestIds) {
        if (userId == null || contestIds.isEmpty()) {
            return Collections.emptySet();
        }
        return contestParticipantMapper.selectList(
                        new LambdaQueryWrapper<ContestParticipant>()
                                .eq(ContestParticipant::getUserId, userId)
                                .in(ContestParticipant::getContestId, contestIds))
                .stream()
                .map(ContestParticipant::getContestId)
                .collect(Collectors.toSet());
    }

    private boolean isJoined(Long userId, Long contestId) {
        if (userId == null) {
            return false;
        }
        Long count = contestParticipantMapper.selectCount(
                new LambdaQueryWrapper<ContestParticipant>()
                        .eq(ContestParticipant::getContestId, contestId)
                        .eq(ContestParticipant::getUserId, userId)
        );
        return count != null && count > 0;
    }

    private Map<Long, Integer> loadParticipantCountMap(List<Long> contestIds) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (Long contestId : contestIds) {
            Long count = contestParticipantMapper.selectCount(
                    new LambdaQueryWrapper<ContestParticipant>()
                            .eq(ContestParticipant::getContestId, contestId)
            );
            countMap.put(contestId, count == null ? 0 : count.intValue());
        }
        return countMap;
    }

    private Map<Long, Integer> loadProblemCountMap(List<Long> contestIds) {
        Map<Long, Integer> countMap = new HashMap<>();
        for (Long contestId : contestIds) {
            Long count = contestProblemMapper.selectCount(
                    new LambdaQueryWrapper<ContestProblem>()
                            .eq(ContestProblem::getContestId, contestId)
            );
            countMap.put(contestId, count == null ? 0 : count.intValue());
        }
        return countMap;
    }

    private ContestVO toContestVO(Contest contest,
                                  Map<Long, Integer> participantCountMap,
                                  Map<Long, Integer> problemCountMap,
                                  Set<Long> joinedContestIds,
                                  boolean canViewHidden) {
        ContestVO vo = new ContestVO();
        vo.setId(contest.getId());
        vo.setTitle(contest.getTitle());
        vo.setDescription(contest.getDescription());
        vo.setStartTime(contest.getStartTime());
        vo.setEndTime(contest.getEndTime());
        vo.setScoreboardFreezeTime(contest.getScoreboardFreezeTime());
        vo.setPenaltyPerWrong(resolvePenaltyPerWrong(contest));
        vo.setCreatorId(contest.getCreatorId());
        vo.setStatus(contest.getStatus());
        vo.setParticipantCount(participantCountMap.getOrDefault(contest.getId(), 0));
        vo.setProblemCount(problemCountMap.getOrDefault(contest.getId(), 0));
        vo.setJoined(joinedContestIds.contains(contest.getId()));
        vo.setContestStatus(calculateContestStatus(contest.getStartTime(), contest.getEndTime()));
        vo.setRankingFrozen(isRankingFrozen(contest, canViewHidden));
        return vo;
    }

    private ContestDetailVO.ContestProblemItemVO toProblemItemVO(Problem problem) {
        if (problem == null) {
            return null;
        }
        ContestDetailVO.ContestProblemItemVO itemVO = new ContestDetailVO.ContestProblemItemVO();
        itemVO.setId(problem.getId());
        itemVO.setTitle(problem.getTitle());
        itemVO.setDifficulty(problem.getDifficulty());
        return itemVO;
    }

    private Map<Long, Problem> loadProblemMap(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, problem -> problem, (a, b) -> a));
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private int resolvePenaltyPerWrong(Contest contest) {
        return resolvePenaltyPerWrong(contest == null ? null : contest.getPenaltyPerWrong());
    }

    private int resolvePenaltyPerWrong(Integer value) {
        if (value == null) {
            return DEFAULT_PENALTY_PER_WRONG;
        }
        return Math.max(0, Math.min(value, 120));
    }

    private int resolvePenaltyPerWrongForSave(Integer inputValue, Integer fallbackValue) {
        if (inputValue != null) {
            return resolvePenaltyPerWrong(inputValue);
        }
        if (fallbackValue != null) {
            return resolvePenaltyPerWrong(fallbackValue);
        }
        return resolvePenaltyPerWrong(loadDefaultPenaltyPerWrongFromConfig());
    }

    private Integer loadDefaultPenaltyPerWrongFromConfig() {
        Map<String, String> configMap = systemConfigService.getConfigMapByKeys(List.of(CONFIG_KEY_DEFAULT_PENALTY));
        String rawValue = configMap.get(CONFIG_KEY_DEFAULT_PENALTY);
        if (!StringUtils.hasText(rawValue)) {
            return DEFAULT_PENALTY_PER_WRONG;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return DEFAULT_PENALTY_PER_WRONG;
        }
    }

    private LocalDateTime resolveRankingEnd(Contest contest, boolean canViewHidden) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime naturalEnd = now.isBefore(contest.getEndTime()) ? now : contest.getEndTime();
        if (!isRankingFrozen(contest, canViewHidden)) {
            return naturalEnd;
        }
        LocalDateTime freezeTime = contest.getScoreboardFreezeTime();
        if (freezeTime == null) {
            return naturalEnd;
        }
        return freezeTime.isBefore(naturalEnd) ? freezeTime : naturalEnd;
    }

    private boolean isRankingFrozen(Contest contest, boolean canViewHidden) {
        if (contest == null || canViewHidden) {
            return false;
        }
        LocalDateTime freezeTime = contest.getScoreboardFreezeTime();
        if (freezeTime == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(freezeTime) && now.isBefore(contest.getEndTime());
    }

    private List<Submission> filterSubmissionsByWindow(List<Submission> submissions,
                                                        LocalDateTime startTime,
                                                        LocalDateTime endTime) {
        if (submissions == null || submissions.isEmpty()) {
            return Collections.emptyList();
        }
        return submissions.stream()
                .filter(submission -> submission.getCreateTime() != null)
                .filter(submission -> !submission.getCreateTime().isBefore(startTime))
                .filter(submission -> !submission.getCreateTime().isAfter(endTime))
                .sorted(Comparator.comparing(Submission::getCreateTime)
                        .thenComparing(Submission::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private String calculateContestStatus(LocalDateTime startTime, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startTime)) {
            return "UPCOMING";
        }
        if (now.isAfter(endTime)) {
            return "ENDED";
        }
        return "RUNNING";
    }

    private LocalDateTime maxTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private double calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        double value = numerator * 100.0 / denominator;
        return Math.round(value * 10.0) / 10.0;
    }

    private <K> void incrementMapCount(Map<K, Integer> map, K key) {
        if (key == null) {
            return;
        }
        map.put(key, map.getOrDefault(key, 0) + 1);
    }

    private void persistContestScores(Long contestId, List<ContestRankingItemVO> ranking) {
        contestScoreMapper.delete(new LambdaQueryWrapper<ContestScore>()
                .eq(ContestScore::getContestId, contestId));
        if (ranking == null || ranking.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (ContestRankingItemVO item : ranking) {
            ContestScore score = new ContestScore();
            score.setContestId(contestId);
            score.setUserId(item.getUserId());
            score.setRankNo(item.getRank());
            score.setAcceptedCount(item.getAcceptedCount());
            score.setTotalPenalty(item.getTotalPenalty());
            score.setTotalSubmissions(item.getTotalSubmissions());
            score.setLastAcceptedTime(item.getLastAcceptedTime());
            score.setCreateTime(now);
            score.setUpdateTime(now);
            contestScoreMapper.insert(score);
        }
    }

    private static class UserStats {
        private int acceptedCount = 0;
        private int totalPenalty = 0;
        private int totalSubmissions = 0;
        private LocalDateTime lastAcceptedTime = null;
        private final Map<Long, ProblemStats> problemStats = new HashMap<>();
    }

    private static class ProblemStats {
        private boolean solved = false;
        private int wrongAttempts = 0;
    }

    private static class ProblemAnalyticsAccumulator {
        private int totalSubmissions = 0;
        private int acceptedSubmissions = 0;
        private final Set<Long> acceptedUsers = new HashSet<>();
    }
}
