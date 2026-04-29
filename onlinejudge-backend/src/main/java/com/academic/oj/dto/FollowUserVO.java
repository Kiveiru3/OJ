package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowUserVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime followTime;
}
