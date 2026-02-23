package com.academic.oj.controller;

import com.academic.oj.dto.DiscussionPostSaveDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.DiscussionService;
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
class DiscussionControllerTest {

    @Mock
    private DiscussionService discussionService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private DiscussionController discussionController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPostShouldRecordAuditLog() {
        setAuth(3L, "STUDENT");
        DiscussionPostSaveDTO dto = new DiscussionPostSaveDTO();
        dto.setTitle("hello");
        dto.setContent("content");
        when(discussionService.createPost(3L, dto)).thenReturn(66L);

        Long id = (Long) discussionController.createPost(dto).getData();

        assertEquals(66L, id);
        verify(adminOperationLogService).record(3L, "DISCUSSION", "CREATE_POST", "POST", 66L, "title=hello");
    }

    @Test
    void deletePostByAdminShouldRecordAdminDetail() {
        setAuth(5L, "ADMIN");

        discussionController.deletePost(88L);

        verify(discussionService).deletePost(5L, true, 88L);
        verify(adminOperationLogService).record(5L, "DISCUSSION", "DELETE_POST", "POST", 88L, "admin delete post");
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
