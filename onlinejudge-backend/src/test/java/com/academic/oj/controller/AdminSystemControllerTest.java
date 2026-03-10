package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.JudgeResultVO;
import com.academic.oj.dto.SystemMonitorVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.JudgeResultService;
import com.academic.oj.service.SystemConfigService;
import com.academic.oj.service.SystemMonitorService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
class AdminSystemControllerTest {

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @Mock
    private SystemMonitorService systemMonitorService;

    @Mock
    private JudgeResultService judgeResultService;

    @InjectMocks
    private AdminSystemController adminSystemController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSystemMonitorShouldAllowAdmin() {
        setAuth(2L, "ADMIN");
        SystemMonitorVO monitorVO = new SystemMonitorVO();
        monitorVO.setTotalUsers(20L);
        when(systemMonitorService.getMonitor()).thenReturn(monitorVO);

        SystemMonitorVO result = (SystemMonitorVO) adminSystemController.getSystemMonitor().getData();

        assertEquals(20L, result.getTotalUsers());
        verify(systemMonitorService).getMonitor();
        verify(adminOperationLogService).record(2L, "SYSTEM_MONITOR", "VIEW", "MONITOR", null, "view monitor");
    }

    @Test
    void getJudgeResultsShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> adminSystemController.getJudgeResults(1, 20, null, null, null, null));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getJudgeResultsShouldAllowAdmin() {
        setAuth(2L, "ADMIN");
        Page<JudgeResultVO> page = new Page<>(1, 20, 1);
        JudgeResultVO vo = new JudgeResultVO();
        vo.setId(11L);
        page.setRecords(List.of(vo));
        when(judgeResultService.getJudgeResultPage(1, 20, 10L, 1001L, "ACCEPTED", "java"))
                .thenReturn(page);

        Page<JudgeResultVO> result = (Page<JudgeResultVO>) adminSystemController
                .getJudgeResults(1, 20, 10L, 1001L, "ACCEPTED", "java")
                .getData();

        assertEquals(1, result.getRecords().size());
        verify(judgeResultService).getJudgeResultPage(1, 20, 10L, 1001L, "ACCEPTED", "java");
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
