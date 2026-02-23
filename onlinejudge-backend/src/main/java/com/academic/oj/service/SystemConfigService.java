package com.academic.oj.service;

import com.academic.oj.entity.SystemConfig;

import java.util.List;
import java.util.Map;

public interface SystemConfigService {
    List<SystemConfig> getAllConfigs();
    Map<String, String> getConfigMapByKeys(List<String> keys);
    void upsertConfig(Long operatorId, String configKey, String configValue, String description);
}
