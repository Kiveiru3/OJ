package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.dto.SubmissionStatusDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.entity.Submission;
import com.academic.oj.service.SubmissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/submit")
    public Result<Submission> submit(@Validated @RequestBody SubmitDTO submitDTO) {
        Long userId = getCurrentUserId();
        Submission submission = submissionService.submit(userId, submitDTO);
        return Result.success(submission);
    }

    @GetMapping("/{id}")
    public Result<SubmissionVO> getSubmissionById(@PathVariable Long id) {
        assertCanAccessSubmission(id);
        SubmissionVO submission = submissionService.getSubmissionById(id);
        return Result.success(submission);
    }

    @GetMapping("/{id}/status")
    public Result<SubmissionStatusDTO> getSubmissionStatus(@PathVariable Long id) {
        assertCanAccessSubmission(id);
        SubmissionStatusDTO status = submissionService.getSubmissionStatusById(id);
        return Result.success(status);
    }

    @GetMapping("/list")
    public Result<Page<SubmissionVO>> getSubmissionList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language) {
        Long userId = getCurrentUserId();
        Integer safePage = normalizePage(page);
        Integer safeSize = normalizeSize(size);
        Page<SubmissionVO> submissions = submissionService.getSubmissionList(userId, safePage, safeSize, problemId, status, language);
        return Result.success(submissions);
    }

    @GetMapping("/my")
    public Result<List<Submission>> getMySubmissions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Long userId = getCurrentUserId();
        List<Submission> submissions = submissionService.getSubmissionsByUserId(userId, normalizePage(page), normalizeSize(size));
        return Result.success(submissions);
    }

    @GetMapping("/problem/{problemId}")
    public Result<List<Submission>> getSubmissionsByProblem(
            @PathVariable Long problemId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        if (!hasRole("TEACHER") && !hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
        List<Submission> submissions = submissionService.getSubmissionsByProblemId(
                problemId, normalizePage(page), normalizeSize(size));
        return Result.success(submissions);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
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

    private Integer normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private Integer normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private void assertCanAccessSubmission(Long submissionId) {
        Long userId = getCurrentUserId();
        Submission entity = submissionService.getSubmissionEntityById(submissionId);
        if (entity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Submission not found");
        }
        if (!userId.equals(entity.getUserId()) && !hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
    }
}

