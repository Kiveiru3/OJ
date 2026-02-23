package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("contest_problem")
public class ContestProblem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contestId;
    private Long problemId;
}

