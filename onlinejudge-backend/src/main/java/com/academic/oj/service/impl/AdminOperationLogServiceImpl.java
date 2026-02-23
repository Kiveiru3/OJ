package com.academic.oj.service.impl;

import com.academic.oj.dto.AdminOperationLogVO;
import com.academic.oj.entity.AdminOperationLog;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.AdminOperationLogMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.AdminOperationLogService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminOperationLogServiceImpl implements AdminOperationLogService {

    private final AdminOperationLogMapper adminOperationLogMapper;
    private final UserMapper userMapper;

    @Override
    public void record(Long operatorId, String module, String action,
                       String targetType, Long targetId, String detail) {
        String operatorUsername = null;
        if (operatorId != null) {
            User operator = userMapper.selectById(operatorId);
            operatorUsername = operator != null ? operator.getUsername() : null;
        }
        AdminOperationLog log = new AdminOperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorUsername(operatorUsername);
        log.setModule(module);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setCreateTime(LocalDateTime.now());
        adminOperationLogMapper.insert(log);
    }

    @Override
    public Page<AdminOperationLogVO> getLogPage(Integer page, Integer size, String module, String action, String keyword) {
        Page<AdminOperationLog> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<AdminOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(module), AdminOperationLog::getModule, module);
        wrapper.eq(StringUtils.hasText(action), AdminOperationLog::getAction, action);
        wrapper.and(StringUtils.hasText(keyword), q ->
                q.like(AdminOperationLog::getOperatorUsername, keyword)
                        .or().like(AdminOperationLog::getDetail, keyword)
                        .or().like(AdminOperationLog::getTargetType, keyword));
        wrapper.orderByDesc(AdminOperationLog::getCreateTime).orderByDesc(AdminOperationLog::getId);
        Page<AdminOperationLog> logPage = adminOperationLogMapper.selectPage(pageObj, wrapper);

        Page<AdminOperationLogVO> result = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        if (logPage.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        List<AdminOperationLogVO> records = logPage.getRecords().stream().map(this::toVO).toList();
        result.setRecords(records);
        return result;
    }

    private AdminOperationLogVO toVO(AdminOperationLog log) {
        AdminOperationLogVO vo = new AdminOperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorId(log.getOperatorId());
        vo.setOperatorUsername(log.getOperatorUsername());
        vo.setModule(log.getModule());
        vo.setAction(log.getAction());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
