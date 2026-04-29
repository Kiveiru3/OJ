package com.academic.oj.service;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.entity.SystemConfig;
import com.academic.oj.mapper.SystemConfigMapper;
import com.academic.oj.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigServiceImplTest {

    @Mock
    private SystemConfigMapper systemConfigMapper;

    @InjectMocks
    private SystemConfigServiceImpl systemConfigService;

    @Test
    void upsertConfigShouldRejectInvalidContestPageSize() {
        assertThrows(BusinessException.class, () ->
                systemConfigService.upsertConfig(1L, "contest.default_page_size", "abc", "bad value"));
        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
    }

    @Test
    void upsertConfigShouldRejectInvalidContestPenalty() {
        assertThrows(BusinessException.class, () ->
                systemConfigService.upsertConfig(1L, "contest.default_penalty_per_wrong", "121", "bad value"));
        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
    }

    @Test
    void upsertConfigShouldRejectBlankSiteName() {
        assertThrows(BusinessException.class, () ->
                systemConfigService.upsertConfig(1L, "site.name", "   ", "blank"));
        verify(systemConfigMapper, never()).insert(any(SystemConfig.class));
        verify(systemConfigMapper, never()).updateById(any(SystemConfig.class));
    }

    @Test
    void upsertConfigShouldInsertNormalizedKeyAndDescription() {
        when(systemConfigMapper.selectOne(any())).thenReturn(null);

        systemConfigService.upsertConfig(9L, " contest.default_page_size ", "20", "  default page size  ");

        ArgumentCaptor<SystemConfig> captor = ArgumentCaptor.forClass(SystemConfig.class);
        verify(systemConfigMapper).insert(captor.capture());
        SystemConfig config = captor.getValue();
        assertEquals("contest.default_page_size", config.getConfigKey());
        assertEquals("20", config.getConfigValue());
        assertEquals("default page size", config.getDescription());
        assertEquals(9L, config.getUpdateUserId());
    }
}
