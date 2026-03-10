package com.academic.oj.service;

import com.academic.oj.common.Constants;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.config.JudgeProperties;
import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.entity.JudgeResult;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.mapper.JudgeResultMapper;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.service.impl.SubmissionServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceImplTest {

    @Mock
    private SubmissionMapper submissionMapper;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private JudgeResultMapper judgeResultMapper;

    @Mock
    private JudgeService judgeService;

    @Mock
    private Executor taskExecutor;

    @Mock
    private JudgeProperties judgeProperties;

    @Mock
    private JudgeProperties.Retry retryPolicy;

    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(submissionService, "taskExecutor", taskExecutor);
        initTableInfoIfNecessary(Submission.class, "submission");
        initTableInfoIfNecessary(Problem.class, "problem");
        initTableInfoIfNecessary(JudgeResult.class, "judgeResult");
    }

    private void initTableInfoIfNecessary(Class<?> entityClass, String namespace) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, namespace + ".xml");
        assistant.setCurrentNamespace(namespace);
        TableInfoHelper.initTableInfo(assistant, entityClass);
    }

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
        Submission submissionTask = new Submission();
        submissionTask.setId(100L);

        Submission dbSubmission = new Submission();
        dbSubmission.setId(100L);
        dbSubmission.setUserId(7L);
        dbSubmission.setProblemId(8L);
        dbSubmission.setLanguage(Constants.LANGUAGE_JAVA);
        dbSubmission.setStatus(Constants.STATUS_PENDING);

        Submission judged = new Submission();
        judged.setId(100L);
        judged.setUserId(7L);
        judged.setProblemId(8L);
        judged.setLanguage(Constants.LANGUAGE_JAVA);
        judged.setStatus(Constants.STATUS_ACCEPTED);

        when(submissionMapper.selectById(100L)).thenReturn(
                dbSubmission, dbSubmission, dbSubmission, dbSubmission
        );
        when(submissionMapper.update(isNull(), any())).thenReturn(1);
        when(judgeService.judge(any(Submission.class))).thenReturn(judged);
        when(judgeResultMapper.selectOne(any())).thenReturn(null);

        submissionService.processJudge(submissionTask);

        verify(submissionMapper).updateById(judged);
        verify(problemMapper).update(any(), any());
    }

    @Test
    void processJudgeShouldSkipWhenSubmissionAlreadyClaimed() {
        Submission submissionTask = new Submission();
        submissionTask.setId(101L);

        Submission current = new Submission();
        current.setId(101L);
        current.setStatus(Constants.STATUS_JUDGING);

        when(submissionMapper.selectById(101L)).thenReturn(current);
        when(submissionMapper.update(isNull(), any())).thenReturn(0);

        submissionService.processJudge(submissionTask);

        verify(judgeService, never()).judge(any(Submission.class));
        verify(submissionMapper, never()).updateById(any(Submission.class));
    }

    @Test
    void processJudgeShouldRetryTransientRuntimeError() {
        Submission submissionTask = new Submission();
        submissionTask.setId(102L);

        Submission dbSubmission = new Submission();
        dbSubmission.setId(102L);
        dbSubmission.setUserId(9L);
        dbSubmission.setProblemId(12L);
        dbSubmission.setLanguage(Constants.LANGUAGE_JAVA);
        dbSubmission.setStatus(Constants.STATUS_PENDING);

        Submission transientError = new Submission();
        transientError.setId(102L);
        transientError.setUserId(9L);
        transientError.setProblemId(12L);
        transientError.setLanguage(Constants.LANGUAGE_JAVA);
        transientError.setStatus(Constants.STATUS_RUNTIME_ERROR);
        transientError.setErrorMessage("Sandbox error: docker unavailable");

        Submission accepted = new Submission();
        accepted.setId(102L);
        accepted.setUserId(9L);
        accepted.setProblemId(12L);
        accepted.setLanguage(Constants.LANGUAGE_JAVA);
        accepted.setStatus(Constants.STATUS_ACCEPTED);

        when(submissionMapper.selectById(102L)).thenReturn(
                dbSubmission, dbSubmission, dbSubmission, dbSubmission, dbSubmission
        );
        when(submissionMapper.update(isNull(), any())).thenReturn(1);
        when(judgeProperties.getRetry()).thenReturn(retryPolicy);
        when(retryPolicy.getMaxAttempts()).thenReturn(2);
        when(retryPolicy.getBackoffMs()).thenReturn(0L);
        when(judgeService.judge(any(Submission.class))).thenReturn(transientError, accepted);
        when(judgeResultMapper.selectOne(any())).thenReturn(null);

        submissionService.processJudge(submissionTask);

        verify(judgeService, times(2)).judge(any(Submission.class));
        verify(submissionMapper).updateById(accepted);
    }
}
