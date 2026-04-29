package com.academic.oj.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserPublicProfileVO {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private RoleProfileDTO roleProfile;

    private Integer totalSubmissions;
    private Integer acceptedSubmissions;
    private Integer attemptedProblems;
    private Integer solvedProblems;
    private Double acceptanceRate;

    private List<UserDailySubmissionVO> dailySubmissionActivity;
    private List<UserProblemProgressVO> problemProgress;
    private List<SubmissionVO> recentSubmissions;
}
