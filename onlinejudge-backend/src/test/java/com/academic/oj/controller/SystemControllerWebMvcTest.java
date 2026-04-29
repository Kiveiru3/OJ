package com.academic.oj.controller;

import com.academic.oj.filter.JwtAuthenticationFilter;
import com.academic.oj.mapper.AdminOperationLogMapper;
import com.academic.oj.mapper.AdminProfileMapper;
import com.academic.oj.mapper.ContestMapper;
import com.academic.oj.mapper.ContestParticipantMapper;
import com.academic.oj.mapper.ContestProblemMapper;
import com.academic.oj.mapper.ContestScoreMapper;
import com.academic.oj.mapper.DiscussionCommentMapper;
import com.academic.oj.mapper.DiscussionPostLikeMapper;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.JudgeResultMapper;
import com.academic.oj.mapper.PrivateMessageMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.StudentProfileMapper;
import com.academic.oj.mapper.SubmissionCaseResultMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.SystemConfigMapper;
import com.academic.oj.mapper.TeacherProfileMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.mapper.UserFollowMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SystemController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(classes = {
        AdminOperationLogMapper.class,
        AdminProfileMapper.class,
        ContestMapper.class,
        ContestParticipantMapper.class,
        ContestProblemMapper.class,
        ContestScoreMapper.class,
        DiscussionCommentMapper.class,
        DiscussionPostLikeMapper.class,
        DiscussionPostMapper.class,
        JudgeResultMapper.class,
        PrivateMessageMapper.class,
        ProblemMapper.class,
        StudentProfileMapper.class,
        SubmissionCaseResultMapper.class,
        SubmissionMapper.class,
        SystemConfigMapper.class,
        TeacherProfileMapper.class,
        TestCaseMapper.class,
        UserFollowMapper.class,
        UserMapper.class
})
class SystemControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemConfigService systemConfigService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getPublicConfigsShouldReturnConfigMap() throws Exception {
        when(systemConfigService.getConfigMapByKeys(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of("site.name", "Online Judge"));

        mockMvc.perform(get("/system/public-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data['site.name']").value("Online Judge"));

        verify(systemConfigService).getConfigMapByKeys(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getPublicConfigsShouldAcceptCustomKeys() throws Exception {
        when(systemConfigService.getConfigMapByKeys(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of("site.announcement", "hello"));

        mockMvc.perform(get("/system/public-configs")
                        .param("keys", "site.announcement")
                        .param("keys", "site.name"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data['site.announcement']").value("hello"));
    }
}
