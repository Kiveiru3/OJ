package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("contest_participant")
public class ContestParticipant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long userId;
    private LocalDateTime createTime;
}

