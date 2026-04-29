package com.academic.oj.dto;

import lombok.Data;

@Data
public class UserPointRankingVO {
    private Integer rank;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer solvedCount;
    private Integer points;
}

