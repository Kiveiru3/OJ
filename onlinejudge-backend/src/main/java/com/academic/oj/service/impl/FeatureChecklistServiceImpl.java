package com.academic.oj.service.impl;

import com.academic.oj.dto.FeatureChecklistItemVO;
import com.academic.oj.dto.FeatureChecklistModuleVO;
import com.academic.oj.dto.FeatureChecklistOverviewVO;
import com.academic.oj.service.FeatureChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeatureChecklistServiceImpl implements FeatureChecklistService {

    private final RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Override
    public FeatureChecklistOverviewVO getChecklist() {
        Set<String> endpointSet = loadEndpointSet();
        List<FeatureChecklistModuleVO> modules = new ArrayList<>();
        modules.add(buildModule("ADMIN", "Administrator", adminDefinitions(), endpointSet));
        modules.add(buildModule("TEACHER", "Teacher", teacherDefinitions(), endpointSet));
        modules.add(buildModule("STUDENT", "Student", studentDefinitions(), endpointSet));

        int total = modules.stream().mapToInt(FeatureChecklistModuleVO::getTotalFeatures).sum();
        int completed = modules.stream().mapToInt(FeatureChecklistModuleVO::getCompletedFeatures).sum();
        FeatureChecklistOverviewVO overview = new FeatureChecklistOverviewVO();
        overview.setModules(modules);
        overview.setTotalFeatures(total);
        overview.setCompletedFeatures(completed);
        overview.setCompletionRate(calculateRate(completed, total));
        return overview;
    }

    private FeatureChecklistModuleVO buildModule(String key, String name,
                                                 List<FeatureDefinition> definitions,
                                                 Set<String> endpointSet) {
        List<FeatureChecklistItemVO> items = new ArrayList<>();
        int completedCount = 0;
        for (FeatureDefinition definition : definitions) {
            FeatureChecklistItemVO item = new FeatureChecklistItemVO();
            item.setFeatureKey(definition.featureKey);
            item.setFeatureName(definition.featureName);

            List<String> required = definition.requirements.stream()
                    .map(EndpointRequirement::asText)
                    .toList();
            List<String> matched = definition.requirements.stream()
                    .filter(req -> exists(endpointSet, req))
                    .map(EndpointRequirement::asText)
                    .toList();

            boolean implemented = matched.size() == required.size();
            if (implemented) {
                completedCount++;
            }

            item.setRequiredEndpoints(required);
            item.setMatchedEndpoints(matched);
            item.setImplemented(implemented);
            items.add(item);
        }

        FeatureChecklistModuleVO module = new FeatureChecklistModuleVO();
        module.setModuleKey(key);
        module.setModuleName(name);
        module.setFeatures(items);
        module.setTotalFeatures(items.size());
        module.setCompletedFeatures(completedCount);
        module.setCompletionRate(calculateRate(completedCount, items.size()));
        return module;
    }

    private Set<String> loadEndpointSet() {
        Set<String> endpointSet = new LinkedHashSet<>();
        for (RequestMappingInfo info : requestMappingHandlerMapping.getHandlerMethods().keySet()) {
            Set<String> patterns = info.getPatternsCondition() == null
                    ? Set.of()
                    : info.getPatternsCondition().getPatterns();
            Set<RequestMethod> methods = info.getMethodsCondition() == null
                    ? Set.of()
                    : info.getMethodsCondition().getMethods();

            if (patterns.isEmpty()) {
                continue;
            }
            if (methods == null || methods.isEmpty()) {
                for (String pattern : patterns) {
                    endpointSet.add("ANY " + pattern);
                }
                continue;
            }
            for (RequestMethod method : methods) {
                for (String pattern : patterns) {
                    endpointSet.add(method.name() + " " + pattern);
                }
            }
        }
        return endpointSet;
    }

    private boolean exists(Set<String> endpointSet, EndpointRequirement requirement) {
        return endpointSet.contains(requirement.asText())
                || endpointSet.contains("ANY " + requirement.path);
    }

    private double calculateRate(int completed, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((completed * 1000.0) / total) / 10.0;
    }

    private List<FeatureDefinition> adminDefinitions() {
        return List.of(
                feature("admin_login", "Login", req("POST", "/auth/login")),
                feature("admin_profile", "Profile Management",
                        req("GET", "/user/info"), req("PUT", "/user/info"), req("POST", "/user/change-password")),
                feature("admin_user_manage", "User Management",
                        req("GET", "/user/list"), req("PUT", "/user/{id}/admin"), req("POST", "/user/{id}/reset-password")),
                feature("admin_problem_manage", "Problem Management",
                        req("POST", "/problem"), req("PUT", "/problem/{id}"), req("DELETE", "/problem/{id}")),
                feature("admin_system_config", "System Config", req("GET", "/admin/system/configs"), req("PUT", "/admin/system/config")),
                feature("admin_log_manage", "Log Management", req("GET", "/admin/system/logs")),
                feature("admin_runtime_monitor", "Runtime Monitor", req("GET", "/admin/system/monitor"))
        );
    }

    private List<FeatureDefinition> teacherDefinitions() {
        return List.of(
                feature("teacher_login", "Login", req("POST", "/auth/login")),
                feature("teacher_problem_manage", "Problem Management",
                        req("POST", "/problem"), req("PUT", "/problem/{id}")),
                feature("teacher_contest_manage", "Contest Management",
                        req("POST", "/contest"), req("PUT", "/contest/{id}"), req("GET", "/contest/list")),
                feature("teacher_score_analysis", "Score Analysis",
                        req("GET", "/teacher/analytics/overview"), req("GET", "/contest/{id}/analytics")),
                feature("teacher_data_export", "Data Export",
                        req("GET", "/contest/{id}/ranking/export"), req("GET", "/teacher/analytics/overview/export"))
        );
    }

    private List<FeatureDefinition> studentDefinitions() {
        return List.of(
                feature("student_register", "Register", req("POST", "/auth/register")),
                feature("student_login", "Login", req("POST", "/auth/login")),
                feature("student_profile", "Profile Management",
                        req("GET", "/user/info"), req("PUT", "/user/info"), req("POST", "/user/change-password")),
                feature("student_problem_practice", "Problem Practice",
                        req("GET", "/problem/list"), req("GET", "/problem/{id}")),
                feature("student_online_judge", "Online Judge",
                        req("POST", "/submission/submit"), req("GET", "/submission/{id}/status")),
                feature("student_contest_rank", "Contest & Ranking",
                        req("GET", "/contest/list"), req("POST", "/contest/{id}/join"), req("GET", "/contest/{id}/ranking")),
                feature("student_discussion", "Discussion",
                        req("GET", "/discussion/list"), req("GET", "/discussion/{id}"),
                        req("POST", "/discussion"), req("GET", "/discussion/{postId}/comments"))
        );
    }

    private FeatureDefinition feature(String key, String name, EndpointRequirement... requirements) {
        return new FeatureDefinition(key, name, List.of(requirements));
    }

    private EndpointRequirement req(String method, String path) {
        return new EndpointRequirement(method, path);
    }

    private record FeatureDefinition(String featureKey, String featureName, List<EndpointRequirement> requirements) {
    }

    private record EndpointRequirement(String method, String path) {
        String asText() {
            return method + " " + path;
        }
    }
}
