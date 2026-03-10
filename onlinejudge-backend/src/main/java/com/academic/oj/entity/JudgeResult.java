package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("judge_result")
public class JudgeResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long userId;
    private Long problemId;
    private String language;
    private String status;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String errorMessage;
    private LocalDateTime judgeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

