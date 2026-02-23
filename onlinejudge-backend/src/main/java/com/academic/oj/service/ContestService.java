package com.academic.oj.service;

import com.academic.oj.dto.ContestAnalyticsVO;
import com.academic.oj.dto.ContestDetailVO;
import com.academic.oj.dto.ContestRankingItemVO;
import com.academic.oj.dto.ContestSaveDTO;
import com.academic.oj.dto.ContestVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ContestService {
    Page<ContestVO> getContestList(Long userId, Integer page, Integer size, String keyword, boolean canViewHidden);
    ContestDetailVO getContestDetail(Long userId, Long contestId, boolean canViewHidden);
    Long createContest(Long creatorId, ContestSaveDTO dto);
    void updateContest(Long operatorId, Long contestId, boolean isAdmin, ContestSaveDTO dto);
    void joinContest(Long userId, Long contestId);
    Page<ContestRankingItemVO> getContestRanking(Long contestId, Integer page, Integer size, boolean canViewHidden);
    List<ContestRankingItemVO> getContestRankingAll(Long contestId, boolean canViewHidden);
    ContestAnalyticsVO getContestAnalytics(Long contestId, boolean canViewHidden);
}
