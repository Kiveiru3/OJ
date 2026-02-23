package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.FeatureChecklistOverviewVO;
import com.academic.oj.dto.SystemMonitorVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.FeatureChecklistService;
import com.academic.oj.service.SystemConfigService;
import com.academic.oj.service.SystemMonitorService;
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
    private FeatureChecklistService featureChecklistService;

    @Mock
    private SystemMonitorService systemMonitorService;

    @InjectMocks
    private AdminSystemController adminSystemController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getFeatureChecklistShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class, () -> adminSystemController.getFeatureChecklist());
        assertEquals(403, ex.getCode());
    }

    @Test
    void getFeatureChecklistShouldAllowAdmin() {
        setAuth(2L, "ADMIN");
        FeatureChecklistOverviewVO vo = new FeatureChecklistOverviewVO();
        vo.setTotalFeatures(10);
        vo.setCompletedFeatures(8);
        vo.setCompletionRate(80.0);
        when(featureChecklistService.getChecklist()).thenReturn(vo);

        FeatureChecklistOverviewVO result = (FeatureChecklistOverviewVO) adminSystemController.getFeatureChecklist().getData();

        assertEquals(10, result.getTotalFeatures());
        verify(featureChecklistService).getChecklist();
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

    private void setAuth(Long userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
