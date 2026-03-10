package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.AdminOperationLogVO;
import com.academic.oj.dto.JudgeResultVO;
import com.academic.oj.dto.SystemMonitorVO;
import com.academic.oj.dto.SystemConfigUpdateDTO;
import com.academic.oj.entity.SystemConfig;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.JudgeResultService;
import com.academic.oj.service.SystemMonitorService;
import com.academic.oj.service.SystemConfigService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SystemConfigService systemConfigService;
    private final AdminOperationLogService adminOperationLogService;
    private final SystemMonitorService systemMonitorService;
    private final JudgeResultService judgeResultService;

    @GetMapping("/configs")
    public Result<List<SystemConfig>> getConfigs() {
        requireAdmin();
        return Result.success(systemConfigService.getAllConfigs());
    }

    @PutMapping("/config")
    public Result<?> upsertConfig(@Validated @RequestBody SystemConfigUpdateDTO dto) {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        systemConfigService.upsertConfig(operatorId, dto.getConfigKey().trim(),
                dto.getConfigValue() == null ? "" : dto.getConfigValue(), dto.getDescription());
        adminOperationLogService.record(operatorId, "SYSTEM_CONFIG", "UPSERT", "CONFIG", null,
                "key=" + dto.getConfigKey());
        return Result.success("Config saved");
    }

    @GetMapping("/logs")
    public Result<Page<AdminOperationLogVO>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        requireAdmin();
        Page<AdminOperationLogVO> result = adminOperationLogService.getLogPage(
                normalizePage(page), normalizeSize(size), module, action, keyword);
        return Result.success(result);
    }

    @GetMapping("/monitor")
    public Result<SystemMonitorVO> getSystemMonitor() {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        SystemMonitorVO monitor = systemMonitorService.getMonitor();
        adminOperationLogService.record(operatorId, "SYSTEM_MONITOR", "VIEW", "MONITOR", null, "view monitor");
        return Result.success(monitor);
    }

    @GetMapping("/judge-results")
    public Result<Page<JudgeResultVO>> getJudgeResults(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String language) {
        requireAdmin();
        Page<JudgeResultVO> result = judgeResultService.getJudgeResultPage(
                normalizePage(page), normalizeSize(size), userId, problemId, status, language);
        return Result.success(result);
    }

    private void requireAdmin() {
        SecurityUtils.requireRole("ADMIN");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private Integer normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private Integer normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
