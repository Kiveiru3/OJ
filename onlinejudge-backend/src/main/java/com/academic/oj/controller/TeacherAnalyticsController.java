package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.TeacherAnalyticsVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.TeacherAnalyticsService;
import com.academic.oj.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
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
        SecurityUtils.requireAnyRole("TEACHER", "ADMIN");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private String buildCsv(TeacherAnalyticsVO vo) {
        StringBuilder builder = new StringBuilder();
        builder.append("指标,数值\n");
        builder.append("学生总数,").append(value(vo.getTotalStudents())).append('\n');
        builder.append("教师总数,").append(value(vo.getTotalTeachers())).append('\n');
        builder.append("题目总数,").append(value(vo.getTotalProblems())).append('\n');
        builder.append("提交总数,").append(value(vo.getTotalSubmissions())).append('\n');
        builder.append("通过总数,").append(value(vo.getAcceptedSubmissions())).append('\n');
        builder.append("通过率,").append(value(vo.getAcceptanceRate())).append("%\n");
        builder.append("竞赛总数,").append(value(vo.getTotalContests())).append('\n');
        builder.append("进行中竞赛,").append(value(vo.getActiveContests())).append('\n');
        builder.append("讨论帖数,").append(value(vo.getDiscussionPosts())).append('\n');
        builder.append("评论数,").append(value(vo.getDiscussionComments())).append("\n\n");

        builder.append("日期,总提交数,通过数,通过率\n");
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
