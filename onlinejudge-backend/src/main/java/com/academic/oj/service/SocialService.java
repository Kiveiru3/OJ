package com.academic.oj.service;

import com.academic.oj.dto.FollowUserVO;
import com.academic.oj.dto.PrivateMessageSendDTO;
import com.academic.oj.dto.PrivateMessageVO;
import com.academic.oj.dto.PrivateThreadVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface SocialService {

    boolean isFollowing(Long currentUserId, Long targetUserId);

    void follow(Long currentUserId, Long targetUserId);

    void unfollow(Long currentUserId, Long targetUserId);

    Page<FollowUserVO> getFollowing(Long currentUserId, Integer page, Integer size);

    Page<FollowUserVO> getFollowers(Long currentUserId, Integer page, Integer size);

    Long sendMessage(Long fromUserId, PrivateMessageSendDTO dto);

    Page<PrivateMessageVO> getMessages(Long currentUserId, Long peerUserId, Integer page, Integer size);

    Page<PrivateThreadVO> getThreads(Long currentUserId, Integer page, Integer size);

    void markConversationRead(Long currentUserId, Long peerUserId);
}
