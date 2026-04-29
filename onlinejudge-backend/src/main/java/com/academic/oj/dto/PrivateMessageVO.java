package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateMessageVO {
    private Long id;
    private Long fromUserId;
    private Long toUserId;
    private String fromUsername;
    private String fromNickname;
    private String fromAvatar;
    private String toUsername;
    private String toNickname;
    private String toAvatar;
    private String content;
    private Integer readFlag;
    private Boolean mine;
    private LocalDateTime createTime;
}
