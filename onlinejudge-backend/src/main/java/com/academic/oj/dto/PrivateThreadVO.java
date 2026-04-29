package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PrivateThreadVO {
    private Long peerUserId;
    private String peerUsername;
    private String peerNickname;
    private String peerAvatar;
    private String peerRole;
    private String lastMessage;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
