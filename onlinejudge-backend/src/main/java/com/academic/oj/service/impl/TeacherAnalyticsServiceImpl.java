package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.dto.TeacherAnalyticsVO;
import com.academic.oj.entity.Contest;
import com.academic.oj.entity.DiscussionComment;
import com.academic.oj.entity.DiscussionPost;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.ContestMapper;
import com.academic.oj.mapper.DiscussionCommentMapper;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.TeacherAnalyticsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeacherAnalyticsServiceImpl implements TeacherAnalyticsService {

    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;
    private final SubmissionMapper submissionMapper;
    private final ContestMapper contestMapper;
    private final DiscussionPostMapper discussionPostMapper;
    private final DiscussionCommentMapper discussionCommentMapper;

    @Override
    public TeacherAnalyticsVO getOverview(Integer recentDays) {
        int days = recentDays == null || recentDays < 1 ? 7 : Math.min(recentDays, 30);

        TeacherAnalyticsVO vo = new TeacherAnalyticsVO();
        vo.setTotalStudents(countUsersByRole(Constants.ROLE_STUDENT));
        vo.setTotalTeachers(countUsersByRole(Constants.ROLE_TEACHER));
        vo.setTotalProblems(safeInt(problemMapper.selectCount(new LambdaQueryWrapper<Problem>())));

        int totalSubmissions = safeInt(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()));
        int acceptedSubmissions = safeInt(submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getStatus, Constants.STATUS_ACCEPTED)));
        vo.setTotalSubmissions(totalSubmissions);
        vo.setAcceptedSubmissions(acceptedSubmissions);
        vo.setAcceptanceRate(calculateRate(acceptedSubmissions, totalSubmissions));

        int totalContests = safeInt(contestMapper.selectCount(new LambdaQueryWrapper<Contest>()));
        int activeContests = safeInt(contestMapper.selectCount(new LambdaQueryWrapper<Contest>()
                .eq(Contest::getStatus, 1)
                .le(Contest::getStartTime, LocalDateTime.now())
                .ge(Contest::getEndTime, LocalDateTime.now())));
        vo.setTotalContests(totalContests);
        vo.setActiveContests(activeContests);

        vo.setDiscussionPosts(safeInt(discussionPostMapper.selectCount(new LambdaQueryWrapper<DiscussionPost>())));
        vo.setDiscussionComments(safeInt(discussionCommentMapper.selectCount(new LambdaQueryWrapper<DiscussionComment>())));

        vo.setSubmissionStatusDistribution(loadSubmissionStatusDistribution());
        vo.setLanguageDistribution(loadLanguageDistribution());
        vo.setDailySubmissionTrend(loadDailySubmissionTrend(days));
        return vo;
    }

    private Integer countUsersByRole(String role) {
        return safeInt(userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getRole, role)));
    }

    private Map<String, Integer> loadSubmissionStatusDistribution() {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .select(Submission::getStatus));
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (submission.getStatus() == null) {
                continue;
            }
            map.put(submission.getStatus(), map.getOrDefault(submission.getStatus(), 0) + 1);
        }
        return map;
    }

    private Map<String, Integer> loadLanguageDistribution() {
        List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                .select(Submission::getLanguage));
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Submission submission : submissions) {
            if (submission.getLanguage() == null) {
                continue;
            }
            map.put(submission.getLanguage(), map.getOrDefault(submission.getLanguage(), 0) + 1);
        }
        return map;
    }

    private List<TeacherAnalyticsVO.DailySubmissionTrendItemVO> loadDailySubmissionTrend(int days) {
        List<TeacherAnalyticsVO.DailySubmissionTrendItemVO> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            int total = safeInt(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .ge(Submission::getCreateTime, start)
                    .lt(Submission::getCreateTime, end)));
            int accepted = safeInt(submissionMapper.selectCount(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getStatus, Constants.STATUS_ACCEPTED)
                    .ge(Submission::getCreateTime, start)
                    .lt(Submission::getCreateTime, end)));

            TeacherAnalyticsVO.DailySubmissionTrendItemVO item = new TeacherAnalyticsVO.DailySubmissionTrendItemVO();
            item.setDate(date.toString());
            item.setTotalSubmissions(total);
            item.setAcceptedSubmissions(accepted);
            item.setAcceptanceRate(calculateRate(accepted, total));
            trend.add(item);
        }
        return trend;
    }

    private int safeInt(Long value) {
        return value == null ? 0 : value.intValue();
    }

    private double calculateRate(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        double value = numerator * 100.0 / denominator;
        return Math.round(value * 10.0) / 10.0;
    }
}
