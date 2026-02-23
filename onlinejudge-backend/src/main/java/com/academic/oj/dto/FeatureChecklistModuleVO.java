package com.academic.oj.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeatureChecklistModuleVO {
    private String moduleKey;
    private String moduleName;
    private Integer totalFeatures;
    private Integer completedFeatures;
    private Double completionRate;
    private List<FeatureChecklistItemVO> features;
}
