package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.ProblemBatchImportDTO;
import com.academic.oj.dto.ProblemBatchImportResultDTO;
import com.academic.oj.entity.Problem;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ProblemService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
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
        boolean canViewHidden = Boolean.TRUE.equals(includeHidden) && (SecurityUtils.hasRole("TEACHER") || SecurityUtils.hasRole("ADMIN"));
        Page<Problem> problems = problemService.getProblemList(
                getCurrentUserIdSafely(), safePage, safeSize, difficulty, keyword, canViewHidden);
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

    @PostMapping("/batch-import")
    public Result<ProblemBatchImportResultDTO> batchImportProblems(@Validated @RequestBody ProblemBatchImportDTO dto) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        ProblemBatchImportResultDTO result = problemService.batchImportProblems(
                operatorId,
                dto.getProblems(),
                !Boolean.FALSE.equals(dto.getSkipExistingTitle())
        );
        adminOperationLogService.record(operatorId, "PROBLEM", "BATCH_IMPORT", "PROBLEM", null,
                "total=" + result.getTotal()
                        + ",imported=" + result.getImported()
                        + ",skipped=" + result.getSkipped()
                        + ",failed=" + result.getFailed());
        return Result.success(result);
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
        SecurityUtils.requireAnyRole("TEACHER", "ADMIN");
    }

    private void assertCanManageProblem(Long problemId) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }

        SecurityUtils.requireRole("TEACHER");

        Problem problem = problemService.getProblemByIdForManage(problemId);
        Long currentUserId = getCurrentUserId();
        if (problem.getCreatorId() != null && !currentUserId.equals(problem.getCreatorId())) {
            SecurityUtils.requireRole("ADMIN");
        }
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private Long getCurrentUserIdSafely() {
        return SecurityUtils.getCurrentUserIdOrNull();
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

