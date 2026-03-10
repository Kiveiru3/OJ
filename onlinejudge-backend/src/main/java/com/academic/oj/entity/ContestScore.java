package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_score")
public class ContestScore {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long userId;
    private Integer rankNo;
    private Integer acceptedCount;
    private Integer totalPenalty;
    private Integer totalSubmissions;
    private LocalDateTime lastAcceptedTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

