package com.academic.oj.dto;

import lombok.Data;

import java.util.List;

@Data
public class FeatureChecklistItemVO {
    private String featureKey;
    private String featureName;
    private Boolean implemented;
    private List<String> requiredEndpoints;
    private List<String> matchedEndpoints;
}
