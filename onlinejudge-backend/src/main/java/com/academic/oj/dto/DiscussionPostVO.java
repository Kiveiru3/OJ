package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscussionPostVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private String title;
    private String content;
    private String contentPreview;
    private Long problemId;
    private String problemTitle;
    private Integer viewCount;
    private Integer likeCount;
    private Boolean liked;
    private Boolean editable;
    private Integer auditStatus;
    private String auditRemark;
    private LocalDateTime auditTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
