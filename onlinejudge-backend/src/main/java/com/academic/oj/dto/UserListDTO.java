package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserListDTO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String role;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
