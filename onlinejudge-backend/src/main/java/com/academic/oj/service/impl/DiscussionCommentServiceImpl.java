package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.DiscussionCommentSaveDTO;
import com.academic.oj.dto.DiscussionCommentVO;
import com.academic.oj.entity.DiscussionComment;
import com.academic.oj.entity.DiscussionPost;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.DiscussionCommentMapper;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.DiscussionCommentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionCommentServiceImpl implements DiscussionCommentService {

    private final DiscussionCommentMapper discussionCommentMapper;
    private final DiscussionPostMapper discussionPostMapper;
    private final UserMapper userMapper;

    @Override
    public Page<DiscussionCommentVO> getCommentList(Long currentUserId, Long postId, Integer page, Integer size) {
        assertPostExists(postId);

        Page<DiscussionComment> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<DiscussionComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DiscussionComment::getPostId, postId)
                .orderByAsc(DiscussionComment::getCreateTime)
                .orderByAsc(DiscussionComment::getId);
        Page<DiscussionComment> commentPage = discussionCommentMapper.selectPage(pageObj, wrapper);

        Page<DiscussionCommentVO> resultPage = new Page<>(commentPage.getCurrent(), commentPage.getSize(), commentPage.getTotal());
        if (commentPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }

        List<Long> userIds = commentPage.getRecords().stream()
                .map(DiscussionComment::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));

        List<DiscussionCommentVO> records = commentPage.getRecords().stream()
                .map(comment -> toVO(comment, currentUserId, userMap.get(comment.getUserId())))
                .toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional
    public Long createComment(Long userId, Long postId, DiscussionCommentSaveDTO dto) {
        assertPostExists(postId);
        Long parentCommentId = dto.getParentCommentId();
        if (parentCommentId != null) {
            DiscussionComment parent = discussionCommentMapper.selectById(parentCommentId);
            if (parent == null || !postId.equals(parent.getPostId())) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Parent comment not found in this post");
            }
        }

        DiscussionComment comment = new DiscussionComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentCommentId(parentCommentId);
        comment.setContent(dto.getContent().trim());
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        discussionCommentMapper.insert(comment);
        return comment.getId();
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, boolean isAdmin, Long commentId) {
        DiscussionComment comment = discussionCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Comment not found");
        }
        if (!isAdmin && !userId.equals(comment.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
        Set<Long> deleteIds = collectDescendantIds(commentId);
        deleteIds.add(commentId);
        if (!deleteIds.isEmpty()) {
            discussionCommentMapper.deleteBatchIds(deleteIds);
        }
    }

    private void assertPostExists(Long postId) {
        DiscussionPost post = discussionPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Post not found");
        }
    }

    private DiscussionCommentVO toVO(DiscussionComment comment, Long currentUserId, User user) {
        DiscussionCommentVO vo = new DiscussionCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setParentCommentId(comment.getParentCommentId());
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setAvatar(user != null ? user.getAvatar() : null);
        vo.setRole(user != null ? user.getRole() : null);
        vo.setContent(comment.getContent());
        vo.setEditable(currentUserId != null && currentUserId.equals(comment.getUserId()));
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        return vo;
    }

    private Set<Long> collectDescendantIds(Long rootId) {
        Set<Long> descendants = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty()) {
            Long parentId = queue.poll();
            List<DiscussionComment> children = discussionCommentMapper.selectList(
                    new LambdaQueryWrapper<DiscussionComment>()
                            .eq(DiscussionComment::getParentCommentId, parentId)
            );
            if (children.isEmpty()) {
                continue;
            }
            List<Long> childIds = children.stream().map(DiscussionComment::getId).toList();
            descendants.addAll(childIds);
            queue.addAll(childIds);
        }
        return descendants;
    }
}
