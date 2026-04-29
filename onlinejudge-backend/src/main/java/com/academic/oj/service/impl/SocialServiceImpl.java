package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.FollowUserVO;
import com.academic.oj.dto.PrivateMessageSendDTO;
import com.academic.oj.dto.PrivateMessageVO;
import com.academic.oj.dto.PrivateThreadVO;
import com.academic.oj.entity.PrivateMessage;
import com.academic.oj.entity.User;
import com.academic.oj.entity.UserFollow;
import com.academic.oj.mapper.PrivateMessageMapper;
import com.academic.oj.mapper.UserFollowMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.SocialService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialServiceImpl implements SocialService {

    private final UserFollowMapper userFollowMapper;
    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;

    @Override
    public boolean isFollowing(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null || currentUserId.equals(targetUserId)) {
            return false;
        }
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFollowingId, targetUserId));
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void follow(Long currentUserId, Long targetUserId) {
        assertValidTargetUser(currentUserId, targetUserId);
        UserFollow existed = userFollowMapper.selectOne(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFollowingId, targetUserId)
                .last("LIMIT 1"));
        if (existed != null) {
            return;
        }

        UserFollow relation = new UserFollow();
        relation.setFollowerId(currentUserId);
        relation.setFollowingId(targetUserId);
        relation.setCreateTime(LocalDateTime.now());
        relation.setUpdateTime(LocalDateTime.now());
        userFollowMapper.insert(relation);
    }

    @Override
    @Transactional
    public void unfollow(Long currentUserId, Long targetUserId) {
        if (currentUserId == null || targetUserId == null || currentUserId.equals(targetUserId)) {
            return;
        }
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, currentUserId)
                .eq(UserFollow::getFollowingId, targetUserId));
    }

    @Override
    public Page<FollowUserVO> getFollowing(Long currentUserId, Integer page, Integer size) {
        Page<UserFollow> followPage = userFollowMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowerId, currentUserId)
                        .orderByDesc(UserFollow::getCreateTime)
                        .orderByDesc(UserFollow::getId));
        return toFollowPage(followPage, true);
    }

    @Override
    public Page<FollowUserVO> getFollowers(Long currentUserId, Integer page, Integer size) {
        Page<UserFollow> followPage = userFollowMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowingId, currentUserId)
                        .orderByDesc(UserFollow::getCreateTime)
                        .orderByDesc(UserFollow::getId));
        return toFollowPage(followPage, false);
    }

    @Override
    @Transactional
    public Long sendMessage(Long fromUserId, PrivateMessageSendDTO dto) {
        if (dto == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid message request");
        }
        Long toUserId = dto.getToUserId();
        assertValidTargetUser(fromUserId, toUserId);
        String content = dto.getContent() == null ? "" : dto.getContent().trim();
        if (!StringUtils.hasText(content)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Message content is required");
        }

        PrivateMessage message = new PrivateMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setReadFlag(0);
        message.setCreateTime(LocalDateTime.now());
        message.setUpdateTime(LocalDateTime.now());
        privateMessageMapper.insert(message);
        return message.getId();
    }

    @Override
    public Page<PrivateMessageVO> getMessages(Long currentUserId, Long peerUserId, Integer page, Integer size) {
        if (peerUserId != null && peerUserId > 0) {
            getUserOrThrow(peerUserId);
        } else {
            peerUserId = null;
        }

        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        if (peerUserId == null) {
            wrapper.and(q -> q.eq(PrivateMessage::getFromUserId, currentUserId)
                    .or()
                    .eq(PrivateMessage::getToUserId, currentUserId));
        } else {
            Long finalPeerUserId = peerUserId;
            wrapper.and(q -> q
                    .nested(n -> n.eq(PrivateMessage::getFromUserId, currentUserId)
                            .eq(PrivateMessage::getToUserId, finalPeerUserId))
                    .or()
                    .nested(n -> n.eq(PrivateMessage::getFromUserId, finalPeerUserId)
                            .eq(PrivateMessage::getToUserId, currentUserId)));
        }
        wrapper.orderByDesc(PrivateMessage::getCreateTime).orderByDesc(PrivateMessage::getId);

        Page<PrivateMessage> messagePage = privateMessageMapper.selectPage(new Page<>(page, size), wrapper);
        return toMessagePage(currentUserId, messagePage);
    }

    @Override
    public Page<PrivateThreadVO> getThreads(Long currentUserId, Integer page, Integer size) {
        List<PrivateMessage> messages = privateMessageMapper.selectList(new LambdaQueryWrapper<PrivateMessage>()
                .and(q -> q.eq(PrivateMessage::getFromUserId, currentUserId)
                        .or()
                        .eq(PrivateMessage::getToUserId, currentUserId))
                .orderByDesc(PrivateMessage::getCreateTime)
                .orderByDesc(PrivateMessage::getId));

        if (messages.isEmpty()) {
            Page<PrivateThreadVO> empty = new Page<>(page, size, 0);
            empty.setRecords(Collections.emptyList());
            return empty;
        }

        Map<Long, PrivateThreadVO> threadMap = new LinkedHashMap<>();
        Map<Long, Integer> unreadMap = new HashMap<>();
        for (PrivateMessage message : messages) {
            Long peerUserId = currentUserId.equals(message.getFromUserId())
                    ? message.getToUserId()
                    : message.getFromUserId();
            if (peerUserId == null) {
                continue;
            }

            if (!threadMap.containsKey(peerUserId)) {
                PrivateThreadVO thread = new PrivateThreadVO();
                thread.setPeerUserId(peerUserId);
                thread.setLastMessage(message.getContent());
                thread.setLastMessageId(message.getId());
                thread.setLastMessageTime(message.getCreateTime());
                threadMap.put(peerUserId, thread);
            }

            if (currentUserId.equals(message.getToUserId()) && Integer.valueOf(0).equals(message.getReadFlag())) {
                unreadMap.merge(peerUserId, 1, Integer::sum);
            }
        }

        List<Long> peerIds = threadMap.keySet().stream().toList();
        Map<Long, User> userMap = loadUserMap(peerIds);
        for (Map.Entry<Long, PrivateThreadVO> entry : threadMap.entrySet()) {
            Long peerId = entry.getKey();
            PrivateThreadVO thread = entry.getValue();
            User peer = userMap.get(peerId);
            thread.setPeerUsername(peer != null ? peer.getUsername() : null);
            thread.setPeerNickname(peer != null ? peer.getNickname() : null);
            thread.setPeerAvatar(peer != null ? peer.getAvatar() : null);
            thread.setPeerRole(peer != null ? peer.getRole() : null);
            thread.setUnreadCount(unreadMap.getOrDefault(peerId, 0));
        }

        List<PrivateThreadVO> allThreads = new ArrayList<>(threadMap.values());
        int total = allThreads.size();
        int from = Math.max((page - 1) * size, 0);
        int to = Math.min(from + size, total);

        List<PrivateThreadVO> records = from >= total
                ? Collections.emptyList()
                : allThreads.subList(from, to);

        Page<PrivateThreadVO> result = new Page<>(page, size, total);
        result.setRecords(records);
        return result;
    }

    @Override
    @Transactional
    public void markConversationRead(Long currentUserId, Long peerUserId) {
        if (peerUserId == null || peerUserId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "peerUserId is required");
        }
        getUserOrThrow(peerUserId);
        privateMessageMapper.update(null, new LambdaUpdateWrapper<PrivateMessage>()
                .eq(PrivateMessage::getFromUserId, peerUserId)
                .eq(PrivateMessage::getToUserId, currentUserId)
                .eq(PrivateMessage::getReadFlag, 0)
                .set(PrivateMessage::getReadFlag, 1)
                .set(PrivateMessage::getUpdateTime, LocalDateTime.now()));
    }

    private Page<FollowUserVO> toFollowPage(Page<UserFollow> source, boolean toFollowing) {
        Page<FollowUserVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        if (source.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }
        List<Long> userIds = source.getRecords().stream()
                .map(item -> toFollowing ? item.getFollowingId() : item.getFollowerId())
                .toList();
        Map<Long, User> userMap = loadUserMap(userIds);
        List<FollowUserVO> records = source.getRecords().stream().map(item -> {
            Long userId = toFollowing ? item.getFollowingId() : item.getFollowerId();
            User user = userMap.get(userId);
            FollowUserVO vo = new FollowUserVO();
            vo.setUserId(userId);
            vo.setUsername(user != null ? user.getUsername() : null);
            vo.setNickname(user != null ? user.getNickname() : null);
            vo.setAvatar(user != null ? user.getAvatar() : null);
            vo.setRole(user != null ? user.getRole() : null);
            vo.setFollowTime(item.getCreateTime());
            return vo;
        }).toList();
        result.setRecords(records);
        return result;
    }

    private Page<PrivateMessageVO> toMessagePage(Long currentUserId, Page<PrivateMessage> source) {
        Page<PrivateMessageVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        if (source.getRecords().isEmpty()) {
            result.setRecords(Collections.emptyList());
            return result;
        }

        List<Long> userIds = source.getRecords().stream()
                .flatMap(msg -> List.of(msg.getFromUserId(), msg.getToUserId()).stream())
                .distinct()
                .toList();
        Map<Long, User> userMap = loadUserMap(userIds);

        List<PrivateMessageVO> records = source.getRecords().stream().map(message -> {
            User fromUser = userMap.get(message.getFromUserId());
            User toUser = userMap.get(message.getToUserId());
            PrivateMessageVO vo = new PrivateMessageVO();
            vo.setId(message.getId());
            vo.setFromUserId(message.getFromUserId());
            vo.setToUserId(message.getToUserId());
            vo.setFromUsername(fromUser != null ? fromUser.getUsername() : null);
            vo.setFromNickname(fromUser != null ? fromUser.getNickname() : null);
            vo.setFromAvatar(fromUser != null ? fromUser.getAvatar() : null);
            vo.setToUsername(toUser != null ? toUser.getUsername() : null);
            vo.setToNickname(toUser != null ? toUser.getNickname() : null);
            vo.setToAvatar(toUser != null ? toUser.getAvatar() : null);
            vo.setContent(message.getContent());
            vo.setReadFlag(message.getReadFlag());
            vo.setMine(currentUserId.equals(message.getFromUserId()));
            vo.setCreateTime(message.getCreateTime());
            return vo;
        }).toList();
        result.setRecords(records);
        return result;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private void assertValidTargetUser(Long currentUserId, Long targetUserId) {
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "targetUserId is invalid");
        }
        if (currentUserId.equals(targetUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "You cannot target yourself");
        }
        getUserOrThrow(targetUserId);
    }

    private User getUserOrThrow(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }
        return user;
    }
}
