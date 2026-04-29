package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.dto.DiscussionPostVO;
import com.academic.oj.entity.DiscussionPost;
import com.academic.oj.entity.DiscussionPostLike;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.DiscussionPostLikeMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.DiscussionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {
    private static final Integer AUDIT_PENDING = 0;
    private static final Integer AUDIT_APPROVED = 1;
    private static final Integer AUDIT_REJECTED = 2;

    private final DiscussionPostMapper discussionPostMapper;
    private final DiscussionPostLikeMapper discussionPostLikeMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;

    @Override
    public Page<DiscussionPostVO> getPostList(Long currentUserId, boolean isAdmin, Integer page, Integer size,
                                              String keyword, Long problemId, Integer auditStatus) {
        Page<DiscussionPost> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<DiscussionPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), q ->
                q.like(DiscussionPost::getTitle, keyword).or().like(DiscussionPost::getContent, keyword));
        wrapper.eq(problemId != null && problemId > 0, DiscussionPost::getProblemId, problemId);
        if (isAdmin) {
            wrapper.eq(auditStatus != null && auditStatus >= 0, DiscussionPost::getAuditStatus, auditStatus);
        } else if (currentUserId != null) {
            wrapper.and(q -> q.eq(DiscussionPost::getAuditStatus, AUDIT_APPROVED)
                    .or()
                    .eq(DiscussionPost::getUserId, currentUserId));
        } else {
            wrapper.eq(DiscussionPost::getAuditStatus, AUDIT_APPROVED);
        }
        wrapper.orderByDesc(DiscussionPost::getLikeCount)
                .orderByDesc(DiscussionPost::getCreateTime);
        Page<DiscussionPost> postPage = discussionPostMapper.selectPage(pageObj, wrapper);

        Page<DiscussionPostVO> resultPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (postPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }

        List<Long> userIds = postPage.getRecords().stream()
                .map(DiscussionPost::getUserId).distinct().toList();
        List<Long> postIds = postPage.getRecords().stream()
                .map(DiscussionPost::getId)
                .distinct()
                .toList();
        List<Long> problemIds = postPage.getRecords().stream()
                .map(DiscussionPost::getProblemId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, Problem> problemMap = loadProblemMap(problemIds);
        Set<Long> likedPostIds = loadLikedPostIds(currentUserId, postIds);

        List<DiscussionPostVO> records = postPage.getRecords().stream()
                .map(post -> toListVO(post, currentUserId, likedPostIds,
                        userMap.get(post.getUserId()), problemMap.get(post.getProblemId())))
                .toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional
    public DiscussionPostVO getPostDetail(Long currentUserId, boolean isAdmin, Long postId) {
        DiscussionPost post = getPostOrThrow(postId);
        if (!canReadPost(post, currentUserId, isAdmin)) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        post.setUpdateTime(LocalDateTime.now());
        discussionPostMapper.updateById(post);

        User author = userMapper.selectById(post.getUserId());
        Problem problem = post.getProblemId() != null ? problemMapper.selectById(post.getProblemId()) : null;
        return toDetailVO(post, currentUserId, loadLikedPostIds(currentUserId, List.of(post.getId())), author, problem);
    }

    @Override
    @Transactional
    public Long createPost(Long userId, DiscussionPostSaveDTO dto) {
        Long problemId = dto.getProblemId();
        if (problemId != null && problemId > 0) {
            Problem problem = problemMapper.selectById(problemId);
            if (problem == null) {
                throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Problem not found");
            }
        }

        DiscussionPost post = new DiscussionPost();
        post.setUserId(userId);
        post.setTitle(dto.getTitle().trim());
        post.setContent(dto.getContent().trim());
        post.setProblemId(problemId != null && problemId > 0 ? problemId : null);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setAuditStatus(AUDIT_PENDING);
        post.setAuditUserId(null);
        post.setAuditRemark(null);
        post.setAuditTime(null);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        discussionPostMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional
    public void deletePost(Long userId, boolean isAdmin, Long postId) {
        DiscussionPost post = getPostOrThrow(postId);
        if (!isAdmin && !userId.equals(post.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Forbidden");
        }
        discussionPostLikeMapper.delete(new LambdaQueryWrapper<DiscussionPostLike>()
                .eq(DiscussionPostLike::getPostId, postId));
        discussionPostMapper.deleteById(postId);
    }

    @Override
    @Transactional
    public void auditPost(Long adminUserId, Long postId, Integer auditStatus, String auditRemark) {
        if (!AUDIT_APPROVED.equals(auditStatus) && !AUDIT_REJECTED.equals(auditStatus)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid audit status");
        }
        DiscussionPost post = getPostOrThrow(postId);
        post.setAuditStatus(auditStatus);
        post.setAuditUserId(adminUserId);
        post.setAuditRemark(StringUtils.hasText(auditRemark) ? auditRemark.trim() : null);
        post.setAuditTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        discussionPostMapper.updateById(post);
    }

    @Override
    @Transactional
    public void likePost(Long userId, Long postId) {
        DiscussionPost post = getPostOrThrow(postId);
        if (!AUDIT_APPROVED.equals(post.getAuditStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN.getCode(), "Post is not available for like");
        }
        if (userId.equals(post.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "You cannot like your own post");
        }

        LambdaQueryWrapper<DiscussionPostLike> wrapper = new LambdaQueryWrapper<DiscussionPostLike>()
                .eq(DiscussionPostLike::getPostId, postId)
                .eq(DiscussionPostLike::getUserId, userId);
        if (discussionPostLikeMapper.selectCount(wrapper) > 0) {
            return;
        }

        DiscussionPostLike like = new DiscussionPostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        like.setCreateTime(LocalDateTime.now());
        discussionPostLikeMapper.insert(like);

        LambdaUpdateWrapper<DiscussionPost> updateWrapper = new LambdaUpdateWrapper<DiscussionPost>()
                .eq(DiscussionPost::getId, postId)
                .setSql("like_count = COALESCE(like_count, 0) + 1");
        discussionPostMapper.update(null, updateWrapper);
    }

    @Override
    @Transactional
    public void unlikePost(Long userId, Long postId) {
        LambdaQueryWrapper<DiscussionPostLike> wrapper = new LambdaQueryWrapper<DiscussionPostLike>()
                .eq(DiscussionPostLike::getPostId, postId)
                .eq(DiscussionPostLike::getUserId, userId);
        int deleted = discussionPostLikeMapper.delete(wrapper);
        if (deleted <= 0) {
            return;
        }

        LambdaUpdateWrapper<DiscussionPost> updateWrapper = new LambdaUpdateWrapper<DiscussionPost>()
                .eq(DiscussionPost::getId, postId)
                .setSql("like_count = GREATEST(COALESCE(like_count, 0) - 1, 0)");
        discussionPostMapper.update(null, updateWrapper);
    }

    private DiscussionPost getPostOrThrow(Long postId) {
        DiscussionPost post = discussionPostMapper.selectById(postId);
        if (post == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Post not found");
        }
        return post;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private Map<Long, Problem> loadProblemMap(List<Long> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return problemMapper.selectBatchIds(problemIds).stream()
                .collect(Collectors.toMap(Problem::getId, problem -> problem, (a, b) -> a));
    }

    private Set<Long> loadLikedPostIds(Long currentUserId, List<Long> postIds) {
        if (currentUserId == null || postIds == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(discussionPostLikeMapper.selectList(new LambdaQueryWrapper<DiscussionPostLike>()
                        .select(DiscussionPostLike::getPostId)
                        .eq(DiscussionPostLike::getUserId, currentUserId)
                        .in(DiscussionPostLike::getPostId, postIds))
                .stream()
                .map(DiscussionPostLike::getPostId)
                .toList());
    }

    private DiscussionPostVO toListVO(DiscussionPost post, Long currentUserId, Set<Long> likedPostIds, User user, Problem problem) {
        DiscussionPostVO vo = baseVO(post, currentUserId, likedPostIds, user, problem);
        vo.setContentPreview(buildPreview(post.getContent()));
        vo.setContent(null);
        return vo;
    }

    private DiscussionPostVO toDetailVO(DiscussionPost post, Long currentUserId, Set<Long> likedPostIds, User user, Problem problem) {
        DiscussionPostVO vo = baseVO(post, currentUserId, likedPostIds, user, problem);
        vo.setContent(post.getContent());
        vo.setContentPreview(buildPreview(post.getContent()));
        return vo;
    }

    private DiscussionPostVO baseVO(DiscussionPost post, Long currentUserId, Set<Long> likedPostIds, User user, Problem problem) {
        DiscussionPostVO vo = new DiscussionPostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setAvatar(user != null ? user.getAvatar() : null);
        vo.setRole(user != null ? user.getRole() : null);
        vo.setTitle(post.getTitle());
        vo.setProblemId(post.getProblemId());
        vo.setProblemTitle(problem != null ? problem.getTitle() : null);
        vo.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
        vo.setLikeCount(post.getLikeCount() == null ? 0 : post.getLikeCount());
        vo.setLiked(likedPostIds != null && likedPostIds.contains(post.getId()));
        vo.setEditable(currentUserId != null && currentUserId.equals(post.getUserId()));
        vo.setAuditStatus(post.getAuditStatus() == null ? AUDIT_PENDING : post.getAuditStatus());
        vo.setAuditRemark(post.getAuditRemark());
        vo.setAuditTime(post.getAuditTime());
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
    }

    private boolean canReadPost(DiscussionPost post, Long currentUserId, boolean isAdmin) {
        Integer status = post.getAuditStatus() == null ? AUDIT_PENDING : post.getAuditStatus();
        if (AUDIT_APPROVED.equals(status)) {
            return true;
        }
        if (isAdmin) {
            return true;
        }
        return currentUserId != null && currentUserId.equals(post.getUserId());
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String compact = content.replace('\r', ' ').replace('\n', ' ').trim();
        if (compact.length() <= 160) {
            return compact;
        }
        return compact.substring(0, 160) + "...";
    }
}
