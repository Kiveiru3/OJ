package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.TestCaseDTO;
import com.academic.oj.service.TestCaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/problem/{problemId}/test-cases")
public class TestCaseController {

    private final TestCaseService testCaseService;

    @GetMapping
    public Result<List<TestCaseDTO>> getProblemTestCases(@PathVariable Long problemId) {
        requireTeacherOrAdmin();
        return Result.success(testCaseService.getProblemTestCases(problemId));
    }

    @PostMapping
    public Result<Long> createTestCase(@PathVariable Long problemId,
                                       @Validated @RequestBody TestCaseDTO testCaseDTO) {
        requireTeacherOrAdmin();
        Long id = testCaseService.createTestCase(problemId, testCaseDTO);
        return Result.success(id);
    }

    @PutMapping
    public Result<?> replaceProblemTestCases(@PathVariable Long problemId,
                                             @RequestBody List<TestCaseDTO> testCases) {
        requireTeacherOrAdmin();
        testCaseService.replaceProblemTestCases(problemId, testCases);
        return Result.success("Test cases replaced");
    }

    @PutMapping("/{testCaseId}")
    public Result<?> updateTestCase(@PathVariable Long problemId,
                                    @PathVariable Long testCaseId,
                                    @Validated @RequestBody TestCaseDTO testCaseDTO) {
        requireTeacherOrAdmin();
        testCaseService.updateTestCase(problemId, testCaseId, testCaseDTO);
        return Result.success("Test case updated");
    }

    @DeleteMapping("/{testCaseId}")
    public Result<?> deleteTestCase(@PathVariable Long problemId,
                                    @PathVariable Long testCaseId) {
        requireTeacherOrAdmin();
        testCaseService.deleteTestCase(problemId, testCaseId);
        return Result.success("Test case deleted");
    }

    private void requireTeacherOrAdmin() {
        if (!hasRole("TEACHER") && !hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
    }

    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        String expectedAuthority = "ROLE_" + role;
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthority.equals(authority.getAuthority()));
    }
}
