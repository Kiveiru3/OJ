package com.academic.oj.service;

import com.academic.oj.entity.Problem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface ProblemService {
    Page<Problem> getProblemList(Integer page, Integer size, String difficulty, String keyword, boolean includeHidden);
    Problem getProblemById(Long id);
    Problem getProblemByIdForManage(Long id);
    Long createProblem(Problem problem);
    void updateProblem(Long id, Problem problem);
    void deleteProblem(Long id);
}

