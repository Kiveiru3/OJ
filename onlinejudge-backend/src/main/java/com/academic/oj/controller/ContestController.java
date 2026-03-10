package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.ContestAnalyticsVO;
import com.academic.oj.dto.ContestDetailVO;
import com.academic.oj.dto.ContestRankingItemVO;
import com.academic.oj.dto.ContestSaveDTO;
import com.academic.oj.dto.ContestVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ContestService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/contest")
@RequiredArgsConstructor
public class ContestController {

    private final ContestService contestService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/list")
    public Result<Page<ContestVO>> getContestList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        boolean canViewHidden = SecurityUtils.hasRole("TEACHER") || SecurityUtils.hasRole("ADMIN");
        Page<ContestVO> contestPage = contestService.getContestList(
                getCurrentUserId(), normalizePage(page), normalizeSize(size), keyword, canViewHidden);
        return Result.success(contestPage);
    }

    @GetMapping("/{id}")
    public Result<ContestDetailVO> getContestDetail(@PathVariable Long id) {
        boolean canViewHidden = SecurityUtils.hasRole("TEACHER") || SecurityUtils.hasRole("ADMIN");
        ContestDetailVO detail = contestService.getContestDetail(getCurrentUserId(), id, canViewHidden);
        return Result.success(detail);
    }

    @PostMapping
    public Result<Long> createContest(@Validated @RequestBody ContestSaveDTO dto) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        Long id = contestService.createContest(operatorId, dto);
        adminOperationLogService.record(operatorId, "CONTEST", "CREATE", "CONTEST", id,
                "title=" + dto.getTitle());
        return Result.success(id);
    }

    @PutMapping("/{id}")
    public Result<?> updateContest(@PathVariable Long id, @Validated @RequestBody ContestSaveDTO dto) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        contestService.updateContest(operatorId, id, SecurityUtils.hasRole("ADMIN"), dto);
        adminOperationLogService.record(operatorId, "CONTEST", "UPDATE", "CONTEST", id,
                "title=" + dto.getTitle());
        return Result.success("Contest updated");
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteContest(@PathVariable Long id) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        contestService.deleteContest(operatorId, id, SecurityUtils.hasRole("ADMIN"));
        adminOperationLogService.record(operatorId, "CONTEST", "DELETE", "CONTEST", id, "deleted");
        return Result.success("Contest deleted");
    }

    @PostMapping("/{id}/join")
    public Result<?> joinContest(@PathVariable Long id) {
        contestService.joinContest(getCurrentUserId(), id);
        return Result.success("Joined contest");
    }

    @GetMapping("/{id}/ranking")
    public Result<Page<ContestRankingItemVO>> getContestRanking(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        boolean canViewHidden = SecurityUtils.hasRole("TEACHER") || SecurityUtils.hasRole("ADMIN");
        Page<ContestRankingItemVO> rankingPage = contestService.getContestRanking(
                id, normalizePage(page), normalizeSize(size), canViewHidden);
        return Result.success(rankingPage);
    }

    @GetMapping("/{id}/score-snapshot")
    public Result<Page<ContestRankingItemVO>> getContestScoreSnapshot(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        requireTeacherOrAdmin();
        Page<ContestRankingItemVO> scorePage = contestService.getContestScoreSnapshot(
                id, normalizePage(page), normalizeSize(size), true);
        return Result.success(scorePage);
    }

    @GetMapping("/{id}/analytics")
    public Result<ContestAnalyticsVO> getContestAnalytics(@PathVariable Long id) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        ContestAnalyticsVO analytics = contestService.getContestAnalytics(id, true);
        adminOperationLogService.record(operatorId, "CONTEST", "VIEW_ANALYTICS", "CONTEST", id, "view analytics");
        return Result.success(analytics);
    }

    @GetMapping("/{id}/ranking/export")
    public Result<String> exportContestRanking(@PathVariable Long id) {
        requireTeacherOrAdmin();
        Long operatorId = getCurrentUserId();
        List<ContestRankingItemVO> ranking = contestService.getContestRankingAll(id, true);
        adminOperationLogService.record(operatorId, "CONTEST", "EXPORT_RANKING", "CONTEST", id,
                "rows=" + ranking.size());
        return Result.success(buildCsv(ranking));
    }

    private void requireTeacherOrAdmin() {
        SecurityUtils.requireAnyRole("TEACHER", "ADMIN");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private Integer normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private Integer normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private String buildCsv(List<ContestRankingItemVO> ranking) {
        StringBuilder builder = new StringBuilder();
        builder.append("Rank,UserId,Username,Nickname,Solved,Penalty,Submissions,LastAcceptedTime\n");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (ContestRankingItemVO item : ranking) {
            builder.append(value(item.getRank())).append(',')
                    .append(value(item.getUserId())).append(',')
                    .append(csvEscape(item.getUsername())).append(',')
                    .append(csvEscape(item.getNickname())).append(',')
                    .append(value(item.getAcceptedCount())).append(',')
                    .append(value(item.getTotalPenalty())).append(',')
                    .append(value(item.getTotalSubmissions())).append(',')
                    .append(csvEscape(item.getLastAcceptedTime() == null ? "" : formatter.format(item.getLastAcceptedTime())))
                    .append('\n');
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
