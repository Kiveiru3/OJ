package com.academic.oj.dto;

import lombok.Data;

@Data
public class UserPointSummaryVO {
    private Integer rank;
    private Long userId;
    private Integer solvedCount;
    private Integer points;
}

