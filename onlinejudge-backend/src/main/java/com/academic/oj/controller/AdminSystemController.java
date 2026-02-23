package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.AdminOperationLogVO;
import com.academic.oj.dto.FeatureChecklistOverviewVO;
import com.academic.oj.dto.SystemMonitorVO;
import com.academic.oj.dto.SystemConfigUpdateDTO;
import com.academic.oj.entity.SystemConfig;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.FeatureChecklistService;
import com.academic.oj.service.SystemMonitorService;
import com.academic.oj.service.SystemConfigService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final SystemConfigService systemConfigService;
    private final AdminOperationLogService adminOperationLogService;
    private final FeatureChecklistService featureChecklistService;
    private final SystemMonitorService systemMonitorService;

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

    @GetMapping("/feature-checklist")
    public Result<FeatureChecklistOverviewVO> getFeatureChecklist() {
        requireAdmin();
        return Result.success(featureChecklistService.getChecklist());
    }

    @GetMapping("/monitor")
    public Result<SystemMonitorVO> getSystemMonitor() {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        SystemMonitorVO monitor = systemMonitorService.getMonitor();
        adminOperationLogService.record(operatorId, "SYSTEM_MONITOR", "VIEW", "MONITOR", null, "view monitor");
        return Result.success(monitor);
    }

    private void requireAdmin() {
        if (!hasRole("ADMIN")) {
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
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED.getCode(), "Unauthorized");
        }
        return Long.parseLong(authentication.getName());
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
