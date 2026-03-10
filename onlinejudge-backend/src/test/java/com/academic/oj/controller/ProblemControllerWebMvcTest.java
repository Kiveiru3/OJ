package com.academic.oj.controller;

import com.academic.oj.entity.Problem;
import com.academic.oj.filter.JwtAuthenticationFilter;
import com.academic.oj.mapper.AdminOperationLogMapper;
import com.academic.oj.mapper.AdminProfileMapper;
import com.academic.oj.mapper.ContestMapper;
import com.academic.oj.mapper.ContestParticipantMapper;
import com.academic.oj.mapper.ContestProblemMapper;
import com.academic.oj.mapper.ContestScoreMapper;
import com.academic.oj.mapper.DiscussionCommentMapper;
import com.academic.oj.mapper.DiscussionPostMapper;
import com.academic.oj.mapper.JudgeResultMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.StudentProfileMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.SystemConfigMapper;
import com.academic.oj.mapper.TeacherProfileMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ProblemService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProblemController.class)
@AutoConfigureMockMvc(addFilters = false)
@MockBean(classes = {
        AdminOperationLogMapper.class,
        AdminProfileMapper.class,
        ContestMapper.class,
        ContestParticipantMapper.class,
        ContestProblemMapper.class,
        ContestScoreMapper.class,
        DiscussionCommentMapper.class,
        DiscussionPostMapper.class,
        JudgeResultMapper.class,
        ProblemMapper.class,
        StudentProfileMapper.class,
        SubmissionMapper.class,
        SystemConfigMapper.class,
        TeacherProfileMapper.class,
        TestCaseMapper.class,
        UserMapper.class
})
class ProblemControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProblemService problemService;

    @MockBean
    private AdminOperationLogService adminOperationLogService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getProblemListShouldReturnPagedData() throws Exception {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setTitle("A + B");
        Page<Problem> pageData = new Page<>(1, 10, 1);
        pageData.setRecords(List.of(problem));
        when(problemService.getProblemList(null, 1, 10, null, null, false)).thenReturn(pageData);

        mockMvc.perform(get("/problem/list").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records[0].id").value(1))
                .andExpect(jsonPath("$.data.records[0].title").value("A + B"));
    }

    @Test
    void getProblemListShouldNormalizePagingAndHiddenFlag() throws Exception {
        when(problemService.getProblemList(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(new Page<>(1, 100, 0));

        mockMvc.perform(get("/problem/list")
                        .param("page", "0")
                        .param("size", "999")
                        .param("includeHidden", "true")
                        .param("difficulty", "EASY")
                        .param("keyword", "sum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long> userCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> difficultyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> hiddenCaptor = ArgumentCaptor.forClass(Boolean.class);
        verify(problemService).getProblemList(userCaptor.capture(), pageCaptor.capture(), sizeCaptor.capture(),
                difficultyCaptor.capture(), keywordCaptor.capture(), hiddenCaptor.capture());

        assertNull(userCaptor.getValue());
        assertEquals(1, pageCaptor.getValue());
        assertEquals(100, sizeCaptor.getValue());
        assertEquals("EASY", difficultyCaptor.getValue());
        assertEquals("sum", keywordCaptor.getValue());
        assertEquals(false, hiddenCaptor.getValue());
    }
}
