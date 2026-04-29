package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.FollowUserVO;
import com.academic.oj.dto.PrivateMessageSendDTO;
import com.academic.oj.dto.PrivateMessageVO;
import com.academic.oj.dto.PrivateThreadVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.SocialService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/social")
@RequiredArgsConstructor
public class SocialController {

    private final SocialService socialService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/follow/status")
    public Result<Boolean> getFollowStatus(@RequestParam Long targetUserId) {
        boolean following = socialService.isFollowing(getCurrentUserId(), targetUserId);
        return Result.success(following);
    }

    @PostMapping("/follow/{targetUserId}")
    public Result<?> follow(@PathVariable Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        socialService.follow(currentUserId, targetUserId);
        adminOperationLogService.record(currentUserId, "SOCIAL", "FOLLOW", "USER", targetUserId, "follow user");
        return Result.success("Followed");
    }

    @DeleteMapping("/follow/{targetUserId}")
    public Result<?> unfollow(@PathVariable Long targetUserId) {
        Long currentUserId = getCurrentUserId();
        socialService.unfollow(currentUserId, targetUserId);
        adminOperationLogService.record(currentUserId, "SOCIAL", "UNFOLLOW", "USER", targetUserId, "unfollow user");
        return Result.success("Unfollowed");
    }

    @GetMapping("/follow/following")
    public Result<Page<FollowUserVO>> getFollowing(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(socialService.getFollowing(getCurrentUserId(), normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/follow/followers")
    public Result<Page<FollowUserVO>> getFollowers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(socialService.getFollowers(getCurrentUserId(), normalizePage(page), normalizeSize(size)));
    }

    @PostMapping("/message")
    public Result<Long> sendMessage(@Validated @RequestBody PrivateMessageSendDTO dto) {
        Long currentUserId = getCurrentUserId();
        Long id = socialService.sendMessage(currentUserId, dto);
        adminOperationLogService.record(currentUserId, "SOCIAL", "SEND_MESSAGE", "USER", dto.getToUserId(),
                "messageId=" + id);
        return Result.success(id);
    }

    @GetMapping("/message/list")
    public Result<Page<PrivateMessageVO>> getMessages(
            @RequestParam(required = false) Long peerUserId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(socialService.getMessages(
                getCurrentUserId(), peerUserId, normalizePage(page), normalizeSize(size)));
    }

    @GetMapping("/message/threads")
    public Result<Page<PrivateThreadVO>> getThreads(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return Result.success(socialService.getThreads(getCurrentUserId(), normalizePage(page), normalizeSize(size)));
    }

    @PutMapping("/message/read")
    public Result<?> markConversationRead(@RequestParam Long peerUserId) {
        socialService.markConversationRead(getCurrentUserId(), peerUserId);
        return Result.success("Read status updated");
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
