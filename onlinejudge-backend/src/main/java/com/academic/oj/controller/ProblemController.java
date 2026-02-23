package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.entity.Problem;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ProblemService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/problem")
@RequiredArgsConstructor
public class ProblemController {

    private final ProblemService problemService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/list")
    public Result<Page<Problem>> getProblemList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") Boolean includeHidden) {
        Integer safePage = normalizePage(page);
        Integer safeSize = normalizeSize(size);
        boolean canViewHidden = Boolean.TRUE.equals(includeHidden) && (hasRole("TEACHER") || hasRole("ADMIN"));
        Page<Problem> problems = problemService.getProblemList(safePage, safeSize, difficulty, keyword, canViewHidden);
        return Result.success(problems);
    }

    @GetMapping("/{id}")
    public Result<Problem> getProblemById(@PathVariable Long id) {
        Problem problem = problemService.getProblemById(id);
        return Result.success(problem);
    }

    @PostMapping
    public Result<Long> createProblem(@RequestBody Problem problem) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        problem.setCreatorId(operatorId);
        Long id = problemService.createProblem(problem);
        adminOperationLogService.record(operatorId, "PROBLEM", "CREATE", "PROBLEM", id,
                "title=" + problem.getTitle());
        return Result.success(id);
    }

    @PostMapping("/create")
    public Result<Long> createProblemCompat(@RequestBody Problem problem) {
        return createProblem(problem);
    }

    @PutMapping("/{id}")
    public Result<?> updateProblem(@PathVariable Long id, @RequestBody Problem problem) {
        requireTeacherOrAdmin();
        assertCanManageProblem(id);
        Long operatorId = getCurrentUserId();
        problemService.updateProblem(id, problem);
        adminOperationLogService.record(operatorId, "PROBLEM", "UPDATE", "PROBLEM", id,
                "title=" + problem.getTitle());
        return Result.success("Problem updated successfully");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteProblem(@PathVariable Long id) {
        requireTeacherOrAdmin();
        assertCanManageProblem(id);
        Long operatorId = getCurrentUserId();
        problemService.deleteProblem(id);
        adminOperationLogService.record(operatorId, "PROBLEM", "DELETE", "PROBLEM", id, "deleted");
        return Result.success("Problem deleted successfully");
    }

    private void requireTeacherOrAdmin() {
        if (!hasRole("TEACHER") && !hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
    }

    private void requireAdmin() {
        if (!hasRole("ADMIN")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
    }

    private void assertCanManageProblem(Long problemId) {
        if (hasRole("ADMIN")) {
            return;
        }

        if (!hasRole("TEACHER")) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }

        Problem problem = problemService.getProblemByIdForManage(problemId);
        Long currentUserId = getCurrentUserId();
        if (problem.getCreatorId() != null && !currentUserId.equals(problem.getCreatorId())) {
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

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return Long.parseLong(authentication.getName());
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
}

