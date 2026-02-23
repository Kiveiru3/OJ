package com.academic.oj.service;

import com.academic.oj.dto.DiscussionCommentSaveDTO;
import com.academic.oj.dto.DiscussionCommentVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface DiscussionCommentService {
    Page<DiscussionCommentVO> getCommentList(Long currentUserId, Long postId, Integer page, Integer size);
    Long createComment(Long userId, Long postId, DiscussionCommentSaveDTO dto);
    void deleteComment(Long userId, boolean isAdmin, Long commentId);
}
