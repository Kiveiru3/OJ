package com.academic.oj.service;

import com.academic.oj.dto.ContestRankingItemVO;
import com.academic.oj.dto.ContestSaveDTO;
import com.academic.oj.entity.Contest;
import com.academic.oj.entity.ContestParticipant;
import com.academic.oj.entity.ContestProblem;
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
import com.academic.oj.service.impl.ContestServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContestServiceImplTest {

    @Mock
    private ContestMapper contestMapper;
    @Mock
    private ContestProblemMapper contestProblemMapper;
    @Mock
    private ContestParticipantMapper contestParticipantMapper;
    @Mock
    private ContestScoreMapper contestScoreMapper;
    @Mock
    private ProblemMapper problemMapper;
    @Mock
    private SubmissionMapper submissionMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private ContestServiceImpl contestService;

    @Test
    void createContestShouldUseSystemDefaultPenaltyWhenInputIsNull() {
        ContestSaveDTO dto = new ContestSaveDTO();
        dto.setTitle("配置默认罚时测试");
        dto.setDescription("desc");
        dto.setStartTime(LocalDateTime.now().plusHours(1));
        dto.setEndTime(LocalDateTime.now().plusHours(2));
        dto.setProblemIds(List.of(1001L));
        dto.setStatus(1);
        dto.setPenaltyPerWrong(null);

        Problem problem = new Problem();
        problem.setId(1001L);
        when(problemMapper.selectBatchIds(any())).thenReturn(List.of(problem));
        when(systemConfigService.getConfigMapByKeys(List.of("contest.default_penalty_per_wrong")))
                .thenReturn(Map.of("contest.default_penalty_per_wrong", "35"));

        contestService.createContest(1L, dto);

        ArgumentCaptor<Contest> captor = ArgumentCaptor.forClass(Contest.class);
        verify(contestMapper).insert(captor.capture());
        assertEquals(35, captor.getValue().getPenaltyPerWrong());
    }

    @Test
    void getContestRankingAllShouldUseConfiguredPenaltyPerWrong() {
        Long contestId = 2001L;
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        Contest contest = buildContest(contestId, start, LocalDateTime.now().plusHours(2), null, 30);

        when(contestMapper.selectById(contestId)).thenReturn(contest);
        when(contestProblemMapper.selectList(any())).thenReturn(List.of(buildContestProblem(contestId, 9001L)));
        when(contestParticipantMapper.selectList(any())).thenReturn(List.of(
                buildParticipant(contestId, 1L),
                buildParticipant(contestId, 2L)
        ));

        List<Submission> submissions = List.of(
                buildSubmission(11L, 1L, 9001L, "WRONG_ANSWER", start.plusMinutes(5)),
                buildSubmission(12L, 1L, 9001L, "ACCEPTED", start.plusMinutes(10)),
                buildSubmission(13L, 2L, 9001L, "ACCEPTED", start.plusMinutes(20))
        );
        when(submissionMapper.selectList(any())).thenReturn(submissions);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                buildUser(1L, "u1"),
                buildUser(2L, "u2")
        ));

        List<ContestRankingItemVO> ranking = contestService.getContestRankingAll(contestId, false);

        assertEquals(2, ranking.size());
        assertEquals(2L, ranking.get(0).getUserId());
        assertEquals(20, ranking.get(0).getTotalPenalty());
        assertEquals(1L, ranking.get(1).getUserId());
        assertEquals(40, ranking.get(1).getTotalPenalty());
    }

    @Test
    void getContestRankingAllShouldHidePostFreezeSubmissionsForPublic() {
        Long contestId = 2002L;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusHours(2);
        LocalDateTime end = now.plusHours(1);
        LocalDateTime freeze = now.minusMinutes(30);
        Contest contest = buildContest(contestId, start, end, freeze, 20);

        when(contestMapper.selectById(contestId)).thenReturn(contest);
        when(contestProblemMapper.selectList(any())).thenReturn(List.of(buildContestProblem(contestId, 9002L)));
        when(contestParticipantMapper.selectList(any())).thenReturn(List.of(
                buildParticipant(contestId, 1L),
                buildParticipant(contestId, 2L)
        ));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(
                buildUser(1L, "u1"),
                buildUser(2L, "u2")
        ));

        List<Submission> submissions = List.of(
                buildSubmission(21L, 2L, 9002L, "ACCEPTED", now.minusMinutes(10))
        );
        when(submissionMapper.selectList(any())).thenReturn(submissions);

        List<ContestRankingItemVO> publicRanking = contestService.getContestRankingAll(contestId, false);
        List<ContestRankingItemVO> adminRanking = contestService.getContestRankingAll(contestId, true);

        assertEquals(0, publicRanking.stream()
                .filter(item -> Long.valueOf(2L).equals(item.getUserId()))
                .findFirst()
                .orElseThrow()
                .getAcceptedCount());
        assertEquals(1, adminRanking.stream()
                .filter(item -> Long.valueOf(2L).equals(item.getUserId()))
                .findFirst()
                .orElseThrow()
                .getAcceptedCount());
    }

    private Contest buildContest(Long id, LocalDateTime start, LocalDateTime end, LocalDateTime freeze, Integer penalty) {
        Contest contest = new Contest();
        contest.setId(id);
        contest.setTitle("contest-" + id);
        contest.setStartTime(start);
        contest.setEndTime(end);
        contest.setScoreboardFreezeTime(freeze);
        contest.setPenaltyPerWrong(penalty);
        contest.setStatus(1);
        contest.setCreatorId(100L);
        return contest;
    }

    private ContestProblem buildContestProblem(Long contestId, Long problemId) {
        ContestProblem cp = new ContestProblem();
        cp.setId(problemId);
        cp.setContestId(contestId);
        cp.setProblemId(problemId);
        return cp;
    }

    private ContestParticipant buildParticipant(Long contestId, Long userId) {
        ContestParticipant participant = new ContestParticipant();
        participant.setContestId(contestId);
        participant.setUserId(userId);
        return participant;
    }

    private Submission buildSubmission(Long id, Long userId, Long problemId, String status, LocalDateTime createTime) {
        Submission submission = new Submission();
        submission.setId(id);
        submission.setUserId(userId);
        submission.setProblemId(problemId);
        submission.setStatus(status);
        submission.setCreateTime(createTime);
        return submission;
    }

    private User buildUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        return user;
    }
}
