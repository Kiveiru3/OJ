package com.academic.oj.controller;

import com.academic.oj.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private SystemController systemController;

    @Test
    void getPublicConfigsShouldUseDefaultKeysWhenInputIsNull() {
        when(systemConfigService.getConfigMapByKeys(List.of("site.name", "site.announcement", "contest.default_page_size")))
                .thenReturn(Map.of("site.name", "OJ"));

        Map<String, String> result = (Map<String, String>) systemController.getPublicConfigs(null).getData();

        assertEquals("OJ", result.get("site.name"));
        verify(systemConfigService).getConfigMapByKeys(List.of("site.name", "site.announcement", "contest.default_page_size"));
    }

    @Test
    void getPublicConfigsShouldNormalizeCustomKeys() {
        List<String> input = Arrays.asList(" ", "site.name", " site.name ", "site.announcement");
        when(systemConfigService.getConfigMapByKeys(List.of("site.name", "site.announcement")))
                .thenReturn(Map.of("site.name", "Online Judge"));

        systemController.getPublicConfigs(input);

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(systemConfigService).getConfigMapByKeys(captor.capture());
        assertEquals(List.of("site.name", "site.announcement"), captor.getValue());
    }
}
