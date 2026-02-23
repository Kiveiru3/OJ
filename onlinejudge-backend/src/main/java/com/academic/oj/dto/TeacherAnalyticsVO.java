package com.academic.oj.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TeacherAnalyticsVO {
    private Integer totalStudents;
    private Integer totalTeachers;
    private Integer totalProblems;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
    private Integer totalContests;
    private Integer activeContests;
    private Integer discussionPosts;
    private Integer discussionComments;
    private Map<String, Integer> submissionStatusDistribution;
    private Map<String, Integer> languageDistribution;
    private List<DailySubmissionTrendItemVO> dailySubmissionTrend;

    @Data
    public static class DailySubmissionTrendItemVO {
        private String date;
        private Integer totalSubmissions;
        private Integer acceptedSubmissions;
        private Double acceptanceRate;
    }
}
