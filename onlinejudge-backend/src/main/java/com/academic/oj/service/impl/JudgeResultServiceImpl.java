package com.academic.oj.service.impl;

import com.academic.oj.dto.JudgeResultVO;
import com.academic.oj.entity.JudgeResult;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.JudgeResultMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.JudgeResultService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JudgeResultServiceImpl implements JudgeResultService {

    private final JudgeResultMapper judgeResultMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;

    @Override
    public Page<JudgeResultVO> getJudgeResultPage(Integer page, Integer size,
                                                  Long userId, Long problemId, String status, String language) {
        Page<JudgeResult> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<JudgeResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null && userId > 0, JudgeResult::getUserId, userId);
        wrapper.eq(problemId != null && problemId > 0, JudgeResult::getProblemId, problemId);
        wrapper.eq(StringUtils.hasText(status), JudgeResult::getStatus, status);
        wrapper.eq(StringUtils.hasText(language), JudgeResult::getLanguage, language);
        wrapper.orderByDesc(JudgeResult::getUpdateTime).orderByDesc(JudgeResult::getId);

        Page<JudgeResult> resultPage = judgeResultMapper.selectPage(pageObj, wrapper);
        Page<JudgeResultVO> voPage = new Page<>(resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());

        if (resultPage.getRecords().isEmpty()) {
            voPage.setRecords(Collections.emptyList());
            return voPage;
        }

        Set<Long> userIds = resultPage.getRecords().stream()
                .map(JudgeResult::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> usernameMap = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        Set<Long> problemIds = resultPage.getRecords().stream()
                .map(JudgeResult::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> problemTitleMap = problemIds.isEmpty() ? Collections.emptyMap()
                : problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));

        voPage.setRecords(resultPage.getRecords().stream().map(item -> {
            JudgeResultVO vo = new JudgeResultVO();
            vo.setId(item.getId());
            vo.setSubmissionId(item.getSubmissionId());
            vo.setUserId(item.getUserId());
            vo.setUsername(usernameMap.get(item.getUserId()));
            vo.setProblemId(item.getProblemId());
            vo.setProblemTitle(problemTitleMap.get(item.getProblemId()));
            vo.setLanguage(item.getLanguage());
            vo.setStatus(item.getStatus());
            vo.setTimeUsed(item.getTimeUsed());
            vo.setMemoryUsed(item.getMemoryUsed());
            vo.setErrorMessage(item.getErrorMessage());
            vo.setJudgeTime(item.getJudgeTime());
            vo.setUpdateTime(item.getUpdateTime());
            return vo;
        }).toList());
        return voPage;
    }
}

