package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContestVO {
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
    private String contestStatus; // UPCOMING, RUNNING, ENDED
}

