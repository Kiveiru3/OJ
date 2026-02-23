package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AdminOperationLogVO {
    private Long id;
    private Long operatorId;
    private String operatorUsername;
    private String module;
    private String action;
    private String targetType;
    private Long targetId;
    private String detail;
    private LocalDateTime createTime;
}
