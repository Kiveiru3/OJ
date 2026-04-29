package com.academic.oj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ContestRankingItemVO {
    private Integer rank;
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private Integer acceptedCount;
    private Integer totalPenalty;
    private Integer totalSubmissions;
    private LocalDateTime lastAcceptedTime;
}
