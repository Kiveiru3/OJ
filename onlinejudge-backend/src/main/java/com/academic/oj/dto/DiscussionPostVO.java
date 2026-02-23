package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscussionPostVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private String title;
    private String content;
    private String contentPreview;
    private Long problemId;
    private String problemTitle;
    private Integer viewCount;
    private Boolean editable;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
