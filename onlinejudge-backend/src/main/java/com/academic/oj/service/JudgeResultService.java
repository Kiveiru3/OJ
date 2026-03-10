package com.academic.oj.service;

import com.academic.oj.dto.JudgeResultVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface JudgeResultService {
    Page<JudgeResultVO> getJudgeResultPage(Integer page, Integer size,
                                           Long userId, Long problemId, String status, String language);
}

