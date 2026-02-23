package com.academic.oj.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeatureChecklistOverviewVO {
    private Integer totalFeatures;
    private Integer completedFeatures;
    private Double completionRate;
    private List<FeatureChecklistModuleVO> modules;
}
