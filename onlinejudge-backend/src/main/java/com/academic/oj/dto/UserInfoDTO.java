package com.academic.oj.dto;

import lombok.Data;

@Data
public class UserInfoDTO {
    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String role;
}

