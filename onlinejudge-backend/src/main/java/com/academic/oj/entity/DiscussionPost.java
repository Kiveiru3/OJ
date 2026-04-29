package com.academic.oj.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("discussion_post")
public class DiscussionPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private Long problemId;
    private Integer viewCount;
    private Integer likeCount;
    /**
     * 0-pending, 1-approved, 2-rejected
     */
    private Integer auditStatus;
    private Long auditUserId;
    private String auditRemark;
    private LocalDateTime auditTime;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
