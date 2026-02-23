package com.academic.oj.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ContestAnalyticsVO {
    private Long contestId;
    private Integer participantCount;
    private Integer activeParticipantCount;
    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Double acceptanceRate;
    private Map<String, Integer> statusDistribution;
    private Map<String, Integer> languageDistribution;
    private List<SolvedDistributionItemVO> solvedDistribution;
    private List<ContestRankingItemVO> topPerformers;
    private List<ProblemAnalyticsItemVO> problemStats;

    @Data
    public static class SolvedDistributionItemVO {
        private Integer solvedCount;
        private Integer userCount;
    }

    @Data
    public static class ProblemAnalyticsItemVO {
        private Long problemId;
        private String title;
        private String difficulty;
        private Integer totalSubmissions;
        private Integer acceptedSubmissions;
        private Integer acceptedUserCount;
        private Double passRate;
    }
}
