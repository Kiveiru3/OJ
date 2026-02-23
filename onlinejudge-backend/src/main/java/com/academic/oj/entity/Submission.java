package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提交记录实体
 */
@Data
@TableName("submission")
public class Submission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long problemId;
    private String language;  // JAVA, CPP, PYTHON
    private String code;
    private String status;  // PENDING, ACCEPTED, WRONG_ANSWER, etc.
    private Integer timeUsed;  // 毫秒
    private Integer memoryUsed;  // MB
    private String errorMessage;
    private LocalDateTime createTime;
}

