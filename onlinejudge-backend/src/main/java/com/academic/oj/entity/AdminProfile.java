package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_profile")
public class AdminProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String adminCode;
    private String realName;
    private String department;
    private String bio;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

