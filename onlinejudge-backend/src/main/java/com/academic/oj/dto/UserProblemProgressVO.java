package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProblemProgressVO {
    private Long problemId;
    private String problemTitle;
    private Integer submitCount;
    private Integer acceptedCount;
    private String latestStatus;
    private LocalDateTime lastSubmitTime;
    private Boolean passed;
}
