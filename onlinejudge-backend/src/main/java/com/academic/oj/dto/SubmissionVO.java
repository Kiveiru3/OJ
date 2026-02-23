package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmissionVO {
    private Long id;
    private Long problemId;
    private String problemTitle;
    private String language;
    private String status;
    private Integer executeTime;
    private Integer executeMemory;
    private String code;
    private String errorMessage;
    private LocalDateTime submitTime;
}
