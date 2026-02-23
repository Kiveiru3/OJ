package com.academic.oj.service;

import com.academic.oj.dto.AdminOperationLogVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface AdminOperationLogService {
    void record(Long operatorId, String module, String action,
                String targetType, Long targetId, String detail);
    Page<AdminOperationLogVO> getLogPage(Integer page, Integer size, String module, String action, String keyword);
}
