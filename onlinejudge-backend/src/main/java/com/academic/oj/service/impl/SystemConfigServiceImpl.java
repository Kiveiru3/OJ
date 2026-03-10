package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.entity.SystemConfig;
import com.academic.oj.mapper.SystemConfigMapper;
import com.academic.oj.service.SystemConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {
    private static final String KEY_SITE_NAME = "site.name";
    private static final String KEY_SITE_ANNOUNCEMENT = "site.announcement";
    private static final String KEY_CONTEST_DEFAULT_PAGE_SIZE = "contest.default_page_size";
    private static final String KEY_CONTEST_DEFAULT_PENALTY = "contest.default_penalty_per_wrong";

    private static final int MAX_SITE_NAME_LENGTH = 80;
    private static final int MAX_ANNOUNCEMENT_LENGTH = 2000;
    private static final int MIN_CONTEST_PAGE_SIZE = 1;
    private static final int MAX_CONTEST_PAGE_SIZE = 100;
    private static final int MIN_CONTEST_PENALTY = 0;
    private static final int MAX_CONTEST_PENALTY = 120;

    private final SystemConfigMapper systemConfigMapper;

    @Override
    public List<SystemConfig> getAllConfigs() {
        return systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .orderByAsc(SystemConfig::getConfigKey));
    }

    @Override
    public Map<String, String> getConfigMapByKeys(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<String> normalizedKeys = keys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SystemConfig> configs = systemConfigMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .in(SystemConfig::getConfigKey, normalizedKeys));
        if (configs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> configMap = new LinkedHashMap<>();
        for (SystemConfig config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    @Override
    @Transactional
    public void upsertConfig(Long operatorId, String configKey, String configValue, String description) {
        String normalizedKey = normalizeRequired(configKey, "Config key is required");
        String normalizedValue = configValue == null ? "" : configValue;
        String normalizedDescription = description == null ? null : description.trim();

        validateConfig(normalizedKey, normalizedValue);

        SystemConfig existing = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, normalizedKey)
                .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(normalizedKey);
            config.setConfigValue(normalizedValue);
            config.setDescription(normalizedDescription);
            config.setUpdateUserId(operatorId);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            systemConfigMapper.insert(config);
            return;
        }

        existing.setConfigValue(normalizedValue);
        existing.setDescription(normalizedDescription);
        existing.setUpdateUserId(operatorId);
        existing.setUpdateTime(now);
        systemConfigMapper.updateById(existing);
    }

    private String normalizeRequired(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
        }
        return value.trim();
    }

    private void validateConfig(String key, String value) {
        if (KEY_SITE_NAME.equals(key)) {
            String siteName = normalizeRequired(value, "site.name must not be blank");
            if (siteName.length() > MAX_SITE_NAME_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "site.name length must be <= " + MAX_SITE_NAME_LENGTH);
            }
            return;
        }

        if (KEY_SITE_ANNOUNCEMENT.equals(key)) {
            if (value != null && value.length() > MAX_ANNOUNCEMENT_LENGTH) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        "site.announcement length must be <= " + MAX_ANNOUNCEMENT_LENGTH);
            }
            return;
        }

        if (KEY_CONTEST_DEFAULT_PAGE_SIZE.equals(key)) {
            Integer parsed = parseIntValue(value, key);
            if (parsed < MIN_CONTEST_PAGE_SIZE || parsed > MAX_CONTEST_PAGE_SIZE) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        key + " must be between " + MIN_CONTEST_PAGE_SIZE + " and " + MAX_CONTEST_PAGE_SIZE);
            }
            return;
        }

        if (KEY_CONTEST_DEFAULT_PENALTY.equals(key)) {
            Integer parsed = parseIntValue(value, key);
            if (parsed < MIN_CONTEST_PENALTY || parsed > MAX_CONTEST_PENALTY) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(),
                        key + " must be between " + MIN_CONTEST_PENALTY + " and " + MAX_CONTEST_PENALTY);
            }
        }
    }

    private Integer parseIntValue(String value, String keyName) {
        String raw = normalizeRequired(value, keyName + " must not be blank");
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), keyName + " must be an integer");
        }
    }
}
