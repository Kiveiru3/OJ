package com.academic.oj.service.impl;

import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.dto.DiscussionPostVO;
import com.academic.oj.entity.DiscussionPost;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.DiscussionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionServiceImpl implements DiscussionService {

    private final DiscussionPostMapper discussionPostMapper;
    private final UserMapper userMapper;
    private final ProblemMapper problemMapper;

    @Override
    public Page<DiscussionPostVO> getPostList(Long currentUserId, Integer page, Integer size, String keyword, Long problemId) {
        Page<DiscussionPost> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<DiscussionPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(StringUtils.hasText(keyword), q ->
                q.like(DiscussionPost::getTitle, keyword).or().like(DiscussionPost::getContent, keyword));
        wrapper.eq(problemId != null && problemId > 0, DiscussionPost::getProblemId, problemId);
        wrapper.orderByDesc(DiscussionPost::getCreateTime);
        Page<DiscussionPost> postPage = discussionPostMapper.selectPage(pageObj, wrapper);

        Page<DiscussionPostVO> resultPage = new Page<>(postPage.getCurrent(), postPage.getSize(), postPage.getTotal());
        if (postPage.getRecords().isEmpty()) {
            resultPage.setRecords(Collections.emptyList());
            return resultPage;
        }

        List<Long> userIds = postPage.getRecords().stream()
                .map(DiscussionPost::getUserId).distinct().toList();
        List<Long> problemIds = postPage.getRecords().stream()
                .map(DiscussionPost::getProblemId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        Map<Long, User> userMap = loadUserMap(userIds);
        Map<Long, Problem> problemMap = loadProblemMap(problemIds);

        List<DiscussionPostVO> records = postPage.getRecords().stream()
                .map(post -> toListVO(post, currentUserId, userMap.get(post.getUserId()), problemMap.get(post.getProblemId())))
                .toList();
        resultPage.setRecords(records);
        return resultPage;
    }

    @Override
    @Transactional
    public DiscussionPostVO getPostDetail(Long currentUserId, Long postId) {
        DiscussionPost post = getPostOrThrow(postId);
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        post.setUpdateTime(LocalDateTime.now());
        discussionPostMapper.updateById(post);

        User author = userMapper.selectById(post.getUserId());
        Problem problem = post.getProblemId() != null ? problemMapper.selectById(post.getProblemId()) : null;
        return toDetailVO(post, currentUserId, author, problem);
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
        discussionPostMapper.deleteById(postId);
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

    private DiscussionPostVO toListVO(DiscussionPost post, Long currentUserId, User user, Problem problem) {
        DiscussionPostVO vo = baseVO(post, currentUserId, user, problem);
        vo.setContentPreview(buildPreview(post.getContent()));
        vo.setContent(null);
        return vo;
    }

    private DiscussionPostVO toDetailVO(DiscussionPost post, Long currentUserId, User user, Problem problem) {
        DiscussionPostVO vo = baseVO(post, currentUserId, user, problem);
        vo.setContent(post.getContent());
        vo.setContentPreview(buildPreview(post.getContent()));
        return vo;
    }

    private DiscussionPostVO baseVO(DiscussionPost post, Long currentUserId, User user, Problem problem) {
        DiscussionPostVO vo = new DiscussionPostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setUsername(user != null ? user.getUsername() : null);
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setRole(user != null ? user.getRole() : null);
        vo.setTitle(post.getTitle());
        vo.setProblemId(post.getProblemId());
        vo.setProblemTitle(problem != null ? problem.getTitle() : null);
        vo.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
        vo.setEditable(currentUserId != null && currentUserId.equals(post.getUserId()));
        vo.setCreateTime(post.getCreateTime());
        vo.setUpdateTime(post.getUpdateTime());
        return vo;
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
