package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class JudgeResultVO {
    private Long id;
    private Long submissionId;
    private Long userId;
    private String username;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String status;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String errorMessage;
    private LocalDateTime judgeTime;
    private LocalDateTime updateTime;
}

