package com.academic.oj.service.impl;

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
        SystemConfig existing = systemConfigMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .last("LIMIT 1"));

        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            SystemConfig config = new SystemConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setDescription(description);
            config.setUpdateUserId(operatorId);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            systemConfigMapper.insert(config);
            return;
        }

        existing.setConfigValue(configValue);
        existing.setDescription(description);
        existing.setUpdateUserId(operatorId);
        existing.setUpdateTime(now);
        systemConfigMapper.updateById(existing);
    }
}
