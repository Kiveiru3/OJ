package com.academic.oj.service;

import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.dto.DiscussionPostVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface DiscussionService {
    Page<DiscussionPostVO> getPostList(Long currentUserId, boolean isAdmin, Integer page, Integer size,
                                       String keyword, Long problemId, Integer auditStatus);
    DiscussionPostVO getPostDetail(Long currentUserId, boolean isAdmin, Long postId);
    Long createPost(Long userId, DiscussionPostSaveDTO dto);
    void deletePost(Long userId, boolean isAdmin, Long postId);
    void auditPost(Long adminUserId, Long postId, Integer auditStatus, String auditRemark);
    void likePost(Long userId, Long postId);
    void unlikePost(Long userId, Long postId);
}
