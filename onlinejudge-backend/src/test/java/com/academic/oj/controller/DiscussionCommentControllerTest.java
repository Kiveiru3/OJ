package com.academic.oj.controller;

import com.academic.oj.dto.DiscussionCommentSaveDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.DiscussionCommentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscussionCommentControllerTest {

    @Mock
    private DiscussionCommentService discussionCommentService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private DiscussionCommentController discussionCommentController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCommentShouldRecordAuditLog() {
        setAuth(6L, "STUDENT");
        DiscussionCommentSaveDTO dto = new DiscussionCommentSaveDTO();
        dto.setContent("ok");
        when(discussionCommentService.createComment(6L, 20L, dto)).thenReturn(99L);

        Long id = (Long) discussionCommentController.createComment(20L, dto).getData();

        assertEquals(99L, id);
        verify(adminOperationLogService).record(6L, "DISCUSSION", "CREATE_COMMENT", "COMMENT", 99L, "postId=20");
    }

    @Test
    void deleteCommentByAdminShouldRecordAdminDetail() {
        setAuth(8L, "ADMIN");

        discussionCommentController.deleteComment(77L);

        verify(discussionCommentService).deleteComment(8L, true, 77L);
        verify(adminOperationLogService).record(8L, "DISCUSSION", "DELETE_COMMENT", "COMMENT", 77L, "admin delete comment");
    }

    private void setAuth(Long userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
