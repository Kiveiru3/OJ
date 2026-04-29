package com.academic.oj.controller;

import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.entity.Submission;
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
import com.academic.oj.service.RateLimitService;
import com.academic.oj.service.SubmissionService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubmissionController.class)
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
class SubmissionControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubmissionService submissionService;

    @MockBean
    private RateLimitService rateLimitService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitShouldUseCurrentUserAndRateLimit() throws Exception {
        setAuth(5L, "STUDENT");
        SubmitDTO dto = new SubmitDTO();
        dto.setProblemId(1001L);
        dto.setLanguage("JAVA");
        dto.setCode("public class Main { public static void main(String[] args) {} }");

        Submission submission = new Submission();
        submission.setId(88L);
        submission.setUserId(5L);
        when(submissionService.submit(5L, dto)).thenReturn(submission);

        mockMvc.perform(post("/submission/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(88));

        verify(rateLimitService).checkSubmitLimit(5L);
        verify(submissionService).submit(5L, dto);
    }

    @Test
    void getSubmissionListShouldNormalizePaging() throws Exception {
        setAuth(9L, "STUDENT");
        Page<SubmissionVO> pageData = new Page<>(1, 100, 0);
        when(submissionService.getSubmissionList(any(), any(), any(), any(), any(), any()))
                .thenReturn(pageData);

        mockMvc.perform(get("/submission/list")
                        .param("page", "0")
                        .param("size", "999")
                        .param("problemId", "33")
                        .param("status", "ACCEPTED")
                        .param("language", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Long> problemCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> statusCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> languageCaptor = ArgumentCaptor.forClass(String.class);
        verify(submissionService).getSubmissionList(userCaptor.capture(), pageCaptor.capture(), sizeCaptor.capture(),
                problemCaptor.capture(), statusCaptor.capture(), languageCaptor.capture());

        assertEquals(9L, userCaptor.getValue());
        assertEquals(1, pageCaptor.getValue());
        assertEquals(100, sizeCaptor.getValue());
        assertEquals(33L, problemCaptor.getValue());
        assertEquals("ACCEPTED", statusCaptor.getValue());
        assertEquals("JAVA", languageCaptor.getValue());
    }

    @Test
    void getSubmissionsByProblemShouldRejectStudent() throws Exception {
        setAuth(9L, "STUDENT");

        mockMvc.perform(get("/submission/problem/44")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void rejudgeShouldRejectStudent() throws Exception {
        setAuth(9L, "STUDENT");

        mockMvc.perform(post("/submission/66/rejudge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));

        verify(submissionService, never()).rejudgeSubmission(66L);
    }

    @Test
    void rejudgeShouldAllowTeacher() throws Exception {
        setAuth(7L, "TEACHER");

        mockMvc.perform(post("/submission/66/rejudge"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(submissionService).rejudgeSubmission(66L);
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
