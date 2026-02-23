package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.SubmissionStatusDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.entity.Submission;
import com.academic.oj.service.SubmissionService;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionControllerTest {

    @Mock
    private SubmissionService submissionService;

    @InjectMocks
    private SubmissionController submissionController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSubmissionByIdShouldRejectOtherUser() {
        setAuth(1L, "STUDENT");
        Submission entity = new Submission();
        entity.setId(10L);
        entity.setUserId(2L);
        when(submissionService.getSubmissionEntityById(10L)).thenReturn(entity);

        BusinessException ex = assertThrows(BusinessException.class, () -> submissionController.getSubmissionById(10L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getSubmissionByIdShouldAllowOwner() {
        setAuth(1L, "STUDENT");
        Submission entity = new Submission();
        entity.setId(11L);
        entity.setUserId(1L);
        SubmissionVO vo = new SubmissionVO();
        vo.setId(11L);

        when(submissionService.getSubmissionEntityById(11L)).thenReturn(entity);
        when(submissionService.getSubmissionById(11L)).thenReturn(vo);

        Long id = ((SubmissionVO) submissionController.getSubmissionById(11L).getData()).getId();

        assertEquals(11L, id);
        verify(submissionService).getSubmissionById(11L);
    }

    @Test
    void getSubmissionStatusShouldAllowOwner() {
        setAuth(1L, "STUDENT");
        Submission entity = new Submission();
        entity.setId(15L);
        entity.setUserId(1L);
        SubmissionStatusDTO statusDTO = new SubmissionStatusDTO();
        statusDTO.setId(15L);
        statusDTO.setStatus("ACCEPTED");

        when(submissionService.getSubmissionEntityById(15L)).thenReturn(entity);
        when(submissionService.getSubmissionStatusById(15L)).thenReturn(statusDTO);

        SubmissionStatusDTO result = (SubmissionStatusDTO) submissionController.getSubmissionStatus(15L).getData();
        assertEquals("ACCEPTED", result.getStatus());
        verify(submissionService).getSubmissionStatusById(15L);
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
