package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("submission_case_result")
public class SubmissionCaseResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Integer caseNo;
    private Integer isSample;
    private String status;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String inputPreview;
    private String expectedPreview;
    private String actualPreview;
    private String errorMessage;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

