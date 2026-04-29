package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserInfoDTO;
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
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.RateLimitService;
import com.academic.oj.service.UserService;
import com.academic.oj.service.VerificationCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
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
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AdminOperationLogService adminOperationLogService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private VerificationCodeService verificationCodeService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void loginShouldReturnToken() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("123456");

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(12L);
        userInfo.setUsername("alice");
        when(userService.login(any(LoginDTO.class))).thenReturn(new TokenDTO("mock-token", userInfo));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("mock-token"))
                .andExpect(jsonPath("$.data.userInfo.username").value("alice"));

        verify(adminOperationLogService).record(12L, "AUTH", "LOGIN", "USER", 12L, "username=alice");
    }

    @Test
    void loginShouldValidatePayload() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void loginShouldReturn429WhenRateLimited() throws Exception {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("alice");
        dto.setPassword("123456");

        doThrow(new BusinessException(429, "登录过于频繁，请稍后再试"))
                .when(rateLimitService).checkLoginLimit(any());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("登录过于频繁，请稍后再试"));
    }
}
