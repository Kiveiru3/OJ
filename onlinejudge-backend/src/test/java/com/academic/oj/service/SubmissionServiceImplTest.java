package com.academic.oj.service;

import com.academic.oj.common.Constants;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.service.impl.SubmissionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private JudgeService judgeService;

    @Mock
    private Executor taskExecutor;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Test
    void submitShouldPersistAndScheduleJudgeTask() {
        SubmitDTO dto = new SubmitDTO();
        dto.setProblemId(1L);
        dto.setLanguage(Constants.LANGUAGE_JAVA);
        dto.setCode("public class Main {}");
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setStatus(1);
        when(problemMapper.selectById(1L)).thenReturn(problem);

        Submission result = submissionService.submit(7L, dto);

        assertEquals("PENDING", result.getStatus());
        verify(submissionMapper).insert(any(Submission.class));
        verify(problemMapper).update(any(), any());
        verify(taskExecutor).execute(any(Runnable.class));
    }

    @Test
    void submitShouldRejectUnsupportedLanguage() {
        SubmitDTO dto = new SubmitDTO();
        dto.setProblemId(1L);
        dto.setLanguage("GO");
        dto.setCode("package main");

        Problem problem = new Problem();
        problem.setId(1L);
        problem.setStatus(1);
        when(problemMapper.selectById(1L)).thenReturn(problem);

        assertThrows(BusinessException.class, () -> submissionService.submit(7L, dto));
    }

    @Test
    void processJudgeShouldIncreaseAcceptedCount() {
        Submission submission = new Submission();
        submission.setId(100L);
        submission.setProblemId(8L);

        Submission judged = new Submission();
        judged.setId(100L);
        judged.setProblemId(8L);
        judged.setStatus(Constants.STATUS_ACCEPTED);

        when(judgeService.judge(submission)).thenReturn(judged);

        submissionService.processJudge(submission);

        verify(submissionMapper).updateById(judged);
        verify(problemMapper).update(any(), any());
    }
}
