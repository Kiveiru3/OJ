package com.academic.oj.service.impl;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.TestCaseDTO;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.TestCase;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.service.TestCaseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseMapper testCaseMapper;
    private final ProblemMapper problemMapper;

    @Override
    public List<TestCaseDTO> getProblemTestCases(Long problemId) {
        ensureProblemExists(problemId);
        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .ne(TestCase::getIsSample, 1)
                .orderByAsc(TestCase::getId);
        return testCaseMapper.selectList(wrapper).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Long createTestCase(Long problemId, TestCaseDTO testCaseDTO) {
        ensureProblemExists(problemId);
        TestCase testCase = new TestCase();
        testCase.setProblemId(problemId);
        testCase.setInput(testCaseDTO.getInput());
        testCase.setOutput(testCaseDTO.getOutput());
        testCase.setIsSample(0);
        testCaseMapper.insert(testCase);
        return testCase.getId();
    }

    @Override
    @Transactional
    public void replaceProblemTestCases(Long problemId, List<TestCaseDTO> testCases) {
        ensureProblemExists(problemId);
        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .eq(TestCase::getIsSample, 0));
        if (testCases == null || testCases.isEmpty()) {
            return;
        }
        for (TestCaseDTO dto : testCases) {
            TestCase testCase = new TestCase();
            testCase.setProblemId(problemId);
            testCase.setInput(dto.getInput());
            testCase.setOutput(dto.getOutput());
            testCase.setIsSample(0);
            testCaseMapper.insert(testCase);
        }
    }

    @Override
    @Transactional
    public void updateTestCase(Long problemId, Long testCaseId, TestCaseDTO testCaseDTO) {
        ensureProblemExists(problemId);
        TestCase existing = getWritableTestCase(problemId, testCaseId);
        existing.setInput(testCaseDTO.getInput());
        existing.setOutput(testCaseDTO.getOutput());
        testCaseMapper.updateById(existing);
    }

    @Override
    @Transactional
    public void deleteTestCase(Long problemId, Long testCaseId) {
        ensureProblemExists(problemId);
        TestCase existing = getWritableTestCase(problemId, testCaseId);
        testCaseMapper.deleteById(existing.getId());
    }

    private void ensureProblemExists(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw new BusinessException("Problem not found");
        }
    }

    private TestCase getWritableTestCase(Long problemId, Long testCaseId) {
        TestCase testCase = testCaseMapper.selectById(testCaseId);
        if (testCase == null || !problemId.equals(testCase.getProblemId())) {
            throw new BusinessException("Test case not found");
        }
        if (Integer.valueOf(1).equals(testCase.getIsSample())) {
            throw new BusinessException("Sample test case is read-only");
        }
        return testCase;
    }

    private TestCaseDTO toDTO(TestCase testCase) {
        TestCaseDTO dto = new TestCaseDTO();
        dto.setId(testCase.getId());
        dto.setInput(testCase.getInput());
        dto.setOutput(testCase.getOutput());
        return dto;
    }
}
