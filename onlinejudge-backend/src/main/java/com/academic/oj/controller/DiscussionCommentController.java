package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.DiscussionCommentSaveDTO;
import com.academic.oj.dto.DiscussionCommentVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.DiscussionCommentService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/discussion")
@RequiredArgsConstructor
public class DiscussionCommentController {

    private final DiscussionCommentService discussionCommentService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/{postId}/comments")
    public Result<Page<DiscussionCommentVO>> getCommentList(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<DiscussionCommentVO> result = discussionCommentService.getCommentList(
                getCurrentUserIdSafely(), postId, normalizePage(page), normalizeSize(size));
        return Result.success(result);
    }

    @PostMapping("/{postId}/comments")
    public Result<Long> createComment(@PathVariable Long postId, @Validated @RequestBody DiscussionCommentSaveDTO dto) {
        Long operatorId = getCurrentUserId();
        Long id = discussionCommentService.createComment(operatorId, postId, dto);
        adminOperationLogService.record(operatorId, "DISCUSSION", "CREATE_COMMENT", "COMMENT", id,
                "postId=" + postId);
        return Result.success(id);
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<?> deleteComment(@PathVariable Long commentId) {
        Long operatorId = getCurrentUserId();
        boolean isAdmin = hasRole("ADMIN");
        discussionCommentService.deleteComment(operatorId, isAdmin, commentId);
        adminOperationLogService.record(operatorId, "DISCUSSION", "DELETE_COMMENT", "COMMENT", commentId,
                isAdmin ? "admin delete comment" : "owner delete comment");
        return Result.success("Deleted");
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
