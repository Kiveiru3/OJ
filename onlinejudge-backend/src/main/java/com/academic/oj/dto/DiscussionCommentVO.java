package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiscussionCommentVO {
    private Long id;
    private Long postId;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
    private String content;
    private Boolean editable;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
