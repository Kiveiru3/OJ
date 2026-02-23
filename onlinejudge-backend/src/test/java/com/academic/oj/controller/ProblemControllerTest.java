package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.entity.Problem;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ProblemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProblemControllerTest {

    @Mock
    private ProblemService problemService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private ProblemController problemController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createProblemShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class, () -> problemController.createProblem(new Problem()));
        assertEquals(403, ex.getCode());
    }

    @Test
    void createProblemShouldAllowTeacher() {
        setAuth(2L, "TEACHER");
        when(problemService.createProblem(any(Problem.class))).thenReturn(100L);

        Long id = (Long) problemController.createProblem(new Problem()).getData();
        ArgumentCaptor<Problem> captor = ArgumentCaptor.forClass(Problem.class);
        verify(problemService).createProblem(captor.capture());

        assertEquals(100L, id);
        assertEquals(2L, captor.getValue().getCreatorId());
    }

    @Test
    void deleteProblemShouldRejectNonOwnerTeacher() {
        setAuth(3L, "TEACHER");
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setCreatorId(4L);
        when(problemService.getProblemByIdForManage(1L)).thenReturn(problem);

        BusinessException ex = assertThrows(BusinessException.class, () -> problemController.deleteProblem(1L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void deleteProblemShouldAllowOwnerTeacher() {
        setAuth(3L, "TEACHER");
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setCreatorId(3L);
        when(problemService.getProblemByIdForManage(1L)).thenReturn(problem);

        problemController.deleteProblem(1L);

        verify(problemService).deleteProblem(1L);
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
