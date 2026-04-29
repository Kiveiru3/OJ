package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SubmissionStatusDTO {
    private Long id;
    private String status;
    private Integer executeTime;
    private Integer executeMemory;
    private String errorMessage;
    private LocalDateTime submitTime;
    private List<SubmissionCaseResultDTO> caseResults;
}
