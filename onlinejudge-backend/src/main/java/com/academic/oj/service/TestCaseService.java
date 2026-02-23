package com.academic.oj.service;

import com.academic.oj.dto.TestCaseDTO;

import java.util.List;

public interface TestCaseService {
    List<TestCaseDTO> getProblemTestCases(Long problemId);
    Long createTestCase(Long problemId, TestCaseDTO testCaseDTO);
    void replaceProblemTestCases(Long problemId, List<TestCaseDTO> testCases);
    void updateTestCase(Long problemId, Long testCaseId, TestCaseDTO testCaseDTO);
    void deleteTestCase(Long problemId, Long testCaseId);
}
