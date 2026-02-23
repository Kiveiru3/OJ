package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ContestDetailVO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long creatorId;
    private Integer status;
    private Integer participantCount;
    private Integer problemCount;
    private Boolean joined;
    private String contestStatus;
    private List<ContestProblemItemVO> problems;

    @Data
    public static class ContestProblemItemVO {
        private Long id;
        private String title;
        private String difficulty;
    }
}

