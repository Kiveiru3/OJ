package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.TestCaseDTO;
import com.academic.oj.service.TestCaseService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseControllerTest {

    @Mock
    private TestCaseService testCaseService;

    @InjectMocks
    private TestCaseController testCaseController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTestCaseShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        TestCaseDTO dto = new TestCaseDTO();
        dto.setInput("1");
        dto.setOutput("1");
        BusinessException ex = assertThrows(BusinessException.class, () -> testCaseController.createTestCase(1L, dto));
        assertEquals(403, ex.getCode());
    }

    @Test
    void createTestCaseShouldAllowTeacher() {
        setAuth(2L, "TEACHER");
        TestCaseDTO dto = new TestCaseDTO();
        dto.setInput("2");
        dto.setOutput("2");
        when(testCaseService.createTestCase(1L, dto)).thenReturn(88L);

        Long id = (Long) testCaseController.createTestCase(1L, dto).getData();

        assertEquals(88L, id);
        verify(testCaseService).createTestCase(1L, dto);
    }

    @Test
    void replaceTestCasesShouldAllowTeacher() {
        setAuth(2L, "TEACHER");
        TestCaseDTO dto = new TestCaseDTO();
        dto.setInput("1");
        dto.setOutput("1");

        testCaseController.replaceProblemTestCases(1L, List.of(dto));

        verify(testCaseService).replaceProblemTestCases(1L, List.of(dto));
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
