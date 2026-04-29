package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.DiscussionAuditDTO;
import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.dto.DiscussionPostVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.DiscussionService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discussion")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionService discussionService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/list")
    public Result<Page<DiscussionPostVO>> getPostList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long problemId,
            @RequestParam(required = false) Integer auditStatus) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        Page<DiscussionPostVO> postPage = discussionService.getPostList(
                getCurrentUserIdSafely(), isAdmin, normalizePage(page), normalizeSize(size), keyword, problemId, auditStatus);
        return Result.success(postPage);
    }

    @GetMapping("/{id}")
    public Result<DiscussionPostVO> getPostDetail(@PathVariable Long id) {
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        DiscussionPostVO vo = discussionService.getPostDetail(getCurrentUserIdSafely(), isAdmin, id);
        return Result.success(vo);
    }

    @PostMapping
    public Result<Long> createPost(@Validated @RequestBody DiscussionPostSaveDTO dto) {
        Long operatorId = getCurrentUserId();
        Long id = discussionService.createPost(operatorId, dto);
        adminOperationLogService.record(operatorId, "DISCUSSION", "CREATE_POST", "POST", id,
                "title=" + dto.getTitle());
        return Result.success(id);
    }

    @DeleteMapping("/{id}")
    public Result<?> deletePost(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        boolean isAdmin = SecurityUtils.hasRole("ADMIN");
        discussionService.deletePost(operatorId, isAdmin, id);
        adminOperationLogService.record(operatorId, "DISCUSSION", "DELETE_POST", "POST", id,
                isAdmin ? "admin delete post" : "owner delete post");
        return Result.success("Deleted");
    }

    @PutMapping("/{id}/audit")
    public Result<?> auditPost(@PathVariable Long id, @Validated @RequestBody DiscussionAuditDTO dto) {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        discussionService.auditPost(operatorId, id, dto.getAuditStatus(), dto.getAuditRemark());
        adminOperationLogService.record(operatorId, "DISCUSSION", "AUDIT_POST", "POST", id,
                "auditStatus=" + dto.getAuditStatus());
        return Result.success("Audited");
    }

    @PostMapping("/{id}/like")
    public Result<?> likePost(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        discussionService.likePost(operatorId, id);
        adminOperationLogService.record(operatorId, "DISCUSSION", "LIKE_POST", "POST", id, "like post");
        return Result.success("Liked");
    }

    @DeleteMapping("/{id}/like")
    public Result<?> unlikePost(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        discussionService.unlikePost(operatorId, id);
        adminOperationLogService.record(operatorId, "DISCUSSION", "UNLIKE_POST", "POST", id, "unlike post");
        return Result.success("Unliked");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private void requireAdmin() {
        SecurityUtils.requireRole("ADMIN");
    }

    private Long getCurrentUserIdSafely() {
        return SecurityUtils.getCurrentUserIdOrNull();
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
}
