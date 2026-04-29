package com.academic.oj.dto;

import lombok.Data;

@Data
public class SubmissionCaseResultDTO {
    private Integer caseNo;
    private Integer isSample;
    private String status;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String inputPreview;
    private String expectedPreview;
    private String actualPreview;
    private String errorMessage;
}

