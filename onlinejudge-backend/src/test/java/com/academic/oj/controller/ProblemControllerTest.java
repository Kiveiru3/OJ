package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.ProblemBatchImportDTO;
import com.academic.oj.dto.ProblemBatchImportResultDTO;
import com.academic.oj.dto.ProblemImportItemDTO;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
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

    @Test
    void batchImportShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> problemController.batchImportProblems(buildBatchImportDTO()));
        assertEquals(403, ex.getCode());
    }

    @Test
    void batchImportShouldAllowTeacher() {
        setAuth(2L, "TEACHER");
        ProblemBatchImportResultDTO serviceResult = new ProblemBatchImportResultDTO();
        serviceResult.setTotal(1);
        serviceResult.setImported(1);
        serviceResult.setSkipped(0);
        serviceResult.setFailed(0);
        when(problemService.batchImportProblems(anyLong(), anyList(), anyBoolean())).thenReturn(serviceResult);

        ProblemBatchImportResultDTO result = (ProblemBatchImportResultDTO) problemController
                .batchImportProblems(buildBatchImportDTO())
                .getData();

        assertEquals(1, result.getImported());
        verify(problemService).batchImportProblems(anyLong(), anyList(), anyBoolean());
        verify(adminOperationLogService).record(2L, "PROBLEM", "BATCH_IMPORT", "PROBLEM", null,
                "total=1,imported=1,skipped=0,failed=0");
    }

    private ProblemBatchImportDTO buildBatchImportDTO() {
        ProblemImportItemDTO item = new ProblemImportItemDTO();
        item.setTitle("A+B");
        item.setDescription("sum");
        item.setDifficulty("EASY");
        item.setTimeLimit(1000);
        item.setMemoryLimit(262144);

        ProblemBatchImportDTO dto = new ProblemBatchImportDTO();
        dto.setProblems(List.of(item));
        dto.setSkipExistingTitle(true);
        return dto;
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
