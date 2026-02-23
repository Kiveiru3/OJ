package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/system")
@RequiredArgsConstructor
public class SystemController {

    private static final List<String> DEFAULT_PUBLIC_KEYS = List.of(
            "site.name",
            "site.announcement",
            "contest.default_page_size"
    );

    private final SystemConfigService systemConfigService;

    @GetMapping("/public-configs")
    public Result<Map<String, String>> getPublicConfigs(@RequestParam(required = false) List<String> keys) {
        List<String> targetKeys = buildTargetKeys(keys);
        return Result.success(systemConfigService.getConfigMapByKeys(targetKeys));
    }

    private List<String> buildTargetKeys(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return DEFAULT_PUBLIC_KEYS;
        }
        List<String> filtered = keys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
        return filtered.isEmpty() ? DEFAULT_PUBLIC_KEYS : filtered;
    }
}
