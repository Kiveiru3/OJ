package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.TeacherAnalyticsVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.TeacherAnalyticsService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherAnalyticsControllerTest {

    @Mock
    private TeacherAnalyticsService teacherAnalyticsService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private TeacherAnalyticsController teacherAnalyticsController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exportOverviewCsvShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class, () -> teacherAnalyticsController.exportOverviewCsv(7));
        assertEquals(403, ex.getCode());
    }

    @Test
    void exportOverviewCsvShouldReturnCsvAndRecordLog() {
        setAuth(2L, "TEACHER");
        TeacherAnalyticsVO vo = new TeacherAnalyticsVO();
        vo.setTotalStudents(10);
        vo.setTotalTeachers(2);
        vo.setTotalProblems(5);
        vo.setTotalSubmissions(100);
        vo.setAcceptedSubmissions(60);
        vo.setAcceptanceRate(60.0);
        vo.setTotalContests(3);
        vo.setActiveContests(1);
        vo.setDiscussionPosts(8);
        vo.setDiscussionComments(20);
        when(teacherAnalyticsService.getOverview(7)).thenReturn(vo);

        String csv = (String) teacherAnalyticsController.exportOverviewCsv(7).getData();

        assertTrue(csv.contains("指标,数值"));
        assertTrue(csv.contains("学生总数,10"));
        verify(adminOperationLogService).record(2L, "TEACHER_ANALYTICS", "EXPORT_OVERVIEW", "DASHBOARD", null, "days=7");
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
