package com.academic.oj.service;

import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.dto.DiscussionPostVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface DiscussionService {
    Page<DiscussionPostVO> getPostList(Long currentUserId, Integer page, Integer size, String keyword, Long problemId);
    DiscussionPostVO getPostDetail(Long currentUserId, Long postId);
    Long createPost(Long userId, DiscussionPostSaveDTO dto);
    void deletePost(Long userId, boolean isAdmin, Long postId);
}
