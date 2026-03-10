package com.academic.oj.dto;

import lombok.Data;

@Data
public class RoleProfileDTO {
    private Long userId;
    private String role;

    // student
    private String studentNo;
    private String className;
    private String major;

    // teacher
    private String teacherNo;
    private String title;

    // admin
    private String adminCode;

    // shared
    private String department;
    private String realName;
    private String gender;
    private String bio;
}

