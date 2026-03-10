package com.academic.oj.controller;

import com.academic.oj.common.Result;
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
            @RequestParam(required = false) Long problemId) {
        Page<DiscussionPostVO> postPage = discussionService.getPostList(
                getCurrentUserId(), normalizePage(page), normalizeSize(size), keyword, problemId);
        return Result.success(postPage);
    }

    @GetMapping("/{id}")
    public Result<DiscussionPostVO> getPostDetail(@PathVariable Long id) {
        DiscussionPostVO vo = discussionService.getPostDetail(getCurrentUserId(), id);
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
}
