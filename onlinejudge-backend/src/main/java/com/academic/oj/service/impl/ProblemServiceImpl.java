package com.academic.oj.service.impl;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.TestCase;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.service.ProblemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;

    @Override
    public Page<Problem> getProblemList(Integer page, Integer size, String difficulty, String keyword, boolean includeHidden) {
        Page<Problem> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (!includeHidden) {
            wrapper.eq(Problem::getStatus, 1);
        }
        
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(Problem::getDifficulty, difficulty);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Problem::getTitle, keyword)
                    .or().like(Problem::getDescription, keyword)
                    .or().like(Problem::getTags, keyword));
        }
        
        wrapper.orderByDesc(Problem::getCreateTime);
        return problemMapper.selectPage(pageObj, wrapper);
    }

    @Override
    public Problem getProblemById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || problem.getStatus() == 0) {
            throw new BusinessException("Problem not found");
        }
        return problem;
    }

    @Override
    public Problem getProblemByIdForManage(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BusinessException("Problem not found");
        }
        return problem;
    }

    @Override
    @Transactional
    public Long createProblem(Problem problem) {
        problem.setCreateTime(LocalDateTime.now());
        problem.setUpdateTime(LocalDateTime.now());
        if (problem.getStatus() == null) {
            problem.setStatus(1);
        }
        if (problem.getAcCount() == null) {
            problem.setAcCount(0);
        }
        if (problem.getSubmitCount() == null) {
            problem.setSubmitCount(0);
        }
        problemMapper.insert(problem);
        syncSampleTestCase(problem.getId(), problem.getSampleInput(), problem.getSampleOutput());
        return problem.getId();
    }

    @Override
    @Transactional
    public void updateProblem(Long id, Problem problem) {
        Problem existing = problemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Problem not found");
        }
        
        problem.setId(id);
        problem.setUpdateTime(LocalDateTime.now());
        problemMapper.updateById(problem);
        syncSampleTestCase(id, problem.getSampleInput(), problem.getSampleOutput());
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BusinessException("Problem not found");
        }
        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>().eq(TestCase::getProblemId, id));
        problemMapper.deleteById(id);
    }

    private void syncSampleTestCase(Long problemId, String sampleInput, String sampleOutput) {
        if (problemId == null) {
            return;
        }

        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .eq(TestCase::getIsSample, 1)
                .last("LIMIT 1");
        TestCase sampleCase = testCaseMapper.selectOne(wrapper);

        if (!StringUtils.hasText(sampleInput) && !StringUtils.hasText(sampleOutput)) {
            if (sampleCase != null) {
                testCaseMapper.deleteById(sampleCase.getId());
            }
            return;
        }

        if (sampleCase == null) {
            sampleCase = new TestCase();
            sampleCase.setProblemId(problemId);
            sampleCase.setIsSample(1);
            sampleCase.setInput(sampleInput == null ? "" : sampleInput);
            sampleCase.setOutput(sampleOutput == null ? "" : sampleOutput);
            testCaseMapper.insert(sampleCase);
            return;
        }

        sampleCase.setInput(sampleInput == null ? "" : sampleInput);
        sampleCase.setOutput(sampleOutput == null ? "" : sampleOutput);
        testCaseMapper.updateById(sampleCase);
    }
}


