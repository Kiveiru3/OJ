package com.academic.oj.service;

import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.dto.SubmissionStatusDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.dto.UserPointRankingVO;
import com.academic.oj.dto.UserPointSummaryVO;
import com.academic.oj.entity.Submission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface SubmissionService {
    Submission submit(Long userId, SubmitDTO submitDTO);
    Submission getSubmissionEntityById(Long id);
    SubmissionStatusDTO getSubmissionStatusById(Long id);
    SubmissionVO getSubmissionById(Long id);
    Page<SubmissionVO> getSubmissionList(Long userId, Integer page, Integer size,
                                         Long problemId, String status, String language);
    List<UserPointRankingVO> getPointRanking(Integer size);
    UserPointSummaryVO getMyPointSummary(Long userId);
    List<Submission> getSubmissionsByUserId(Long userId, Integer page, Integer size);
    List<Submission> getSubmissionsByProblemId(Long problemId, Integer page, Integer size);
    void rejudgeSubmission(Long submissionId);
}

