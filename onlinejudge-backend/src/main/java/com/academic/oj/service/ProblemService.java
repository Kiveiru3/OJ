package com.academic.oj.service;

import com.academic.oj.dto.ProblemBatchImportResultDTO;
import com.academic.oj.dto.ProblemImportItemDTO;
import com.academic.oj.entity.Problem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface ProblemService {
    Page<Problem> getProblemList(Long userId, Integer page, Integer size, String difficulty, String keyword, boolean includeHidden);
    Problem getProblemById(Long id);
    Problem getProblemByIdForManage(Long id);
    Long createProblem(Problem problem);
    void updateProblem(Long id, Problem problem);
    void deleteProblem(Long id);
    ProblemBatchImportResultDTO batchImportProblems(Long operatorId, List<ProblemImportItemDTO> items, boolean skipExistingTitle);
}

