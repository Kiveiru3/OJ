package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.TeacherAnalyticsVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.TeacherAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/teacher/analytics")
@RequiredArgsConstructor
public class TeacherAnalyticsController {

    private final TeacherAnalyticsService teacherAnalyticsService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/overview")
    public Result<TeacherAnalyticsVO> getOverview(@RequestParam(defaultValue = "7") Integer days) {
        requireTeacherOrAdmin();
        TeacherAnalyticsVO vo = teacherAnalyticsService.getOverview(days);
        adminOperationLogService.record(getCurrentUserId(), "TEACHER_ANALYTICS", "VIEW_OVERVIEW", "DASHBOARD", null,
                "days=" + (days == null ? 7 : days));
        return Result.success(vo);
    }

    @GetMapping("/overview/export")
    public Result<String> exportOverviewCsv(@RequestParam(defaultValue = "7") Integer days) {
        requireTeacherOrAdmin();
        int normalizedDays = days == null || days < 1 ? 7 : Math.min(days, 30);
        TeacherAnalyticsVO vo = teacherAnalyticsService.getOverview(normalizedDays);
        Long operatorId = getCurrentUserId();
        adminOperationLogService.record(operatorId, "TEACHER_ANALYTICS", "EXPORT_OVERVIEW", "DASHBOARD", null,
                "days=" + normalizedDays);
        return Result.success(buildCsv(vo));
    }

    private void requireTeacherOrAdmin() {
        if (!hasRole("TEACHER") && !hasRole("ADMIN")) {
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

    private String buildCsv(TeacherAnalyticsVO vo) {
        StringBuilder builder = new StringBuilder();
        builder.append("Metric,Value\n");
        builder.append("Students,").append(value(vo.getTotalStudents())).append('\n');
        builder.append("Teachers,").append(value(vo.getTotalTeachers())).append('\n');
        builder.append("Problems,").append(value(vo.getTotalProblems())).append('\n');
        builder.append("Submissions,").append(value(vo.getTotalSubmissions())).append('\n');
        builder.append("Accepted,").append(value(vo.getAcceptedSubmissions())).append('\n');
        builder.append("AcceptanceRate,").append(value(vo.getAcceptanceRate())).append("%\n");
        builder.append("Contests,").append(value(vo.getTotalContests())).append('\n');
        builder.append("ActiveContests,").append(value(vo.getActiveContests())).append('\n');
        builder.append("DiscussionPosts,").append(value(vo.getDiscussionPosts())).append('\n');
        builder.append("DiscussionComments,").append(value(vo.getDiscussionComments())).append("\n\n");

        builder.append("Date,TotalSubmissions,AcceptedSubmissions,AcceptanceRate\n");
        if (vo.getDailySubmissionTrend() != null) {
            vo.getDailySubmissionTrend().forEach(item -> builder
                    .append(csvEscape(item.getDate())).append(',')
                    .append(value(item.getTotalSubmissions())).append(',')
                    .append(value(item.getAcceptedSubmissions())).append(',')
                    .append(value(item.getAcceptanceRate())).append("%\n"));
        }
        return builder.toString();
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
