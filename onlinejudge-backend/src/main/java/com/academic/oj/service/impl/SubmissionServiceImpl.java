package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.SubmitDTO;
import com.academic.oj.dto.SubmissionStatusDTO;
import com.academic.oj.dto.SubmissionVO;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.service.JudgeService;
import com.academic.oj.service.SubmissionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final SubmissionMapper submissionMapper;
    private final ProblemMapper problemMapper;
    private final JudgeService judgeService;
    @Resource(name = "taskExecutor")
    private Executor taskExecutor;

    @Override
    @Transactional
    public Submission submit(Long userId, SubmitDTO submitDTO) {
        validateSubmit(submitDTO);

        Submission submission = new Submission();
        submission.setUserId(userId);
        submission.setProblemId(submitDTO.getProblemId());
        submission.setCode(submitDTO.getCode());
        submission.setLanguage(submitDTO.getLanguage());
        submission.setStatus("PENDING");
        submission.setCreateTime(LocalDateTime.now());
        
        submissionMapper.insert(submission);
        increaseProblemSubmitCount(submitDTO.getProblemId());
        
        // 异步判题
        scheduleJudge(submission);
        
        return submission;
    }

    private void scheduleJudge(Submission submission) {
        Runnable task = () -> processJudge(submission);
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskExecutor.execute(task);
                }
            });
            return;
        }
        taskExecutor.execute(task);
    }

    public void processJudge(Submission submission) {
        try {
            Submission judging = new Submission();
            judging.setId(submission.getId());
            judging.setStatus("JUDGING");
            submissionMapper.updateById(judging);

            Submission result = judgeService.judge(submission);
            submissionMapper.updateById(result);
            if (Constants.STATUS_ACCEPTED.equals(result.getStatus())) {
                increaseProblemAcceptedCount(result.getProblemId());
            }
        } catch (Exception e) {
            log.error("Judge failed for submission: {}", submission.getId(), e);
            submission.setStatus("RUNTIME_ERROR");
            submission.setErrorMessage("Judge failed: " + e.getMessage());
            submissionMapper.updateById(submission);
        }
    }

    @Override
    public Submission getSubmissionEntityById(Long id) {
        return submissionMapper.selectById(id);
    }

    @Override
    public SubmissionStatusDTO getSubmissionStatusById(Long id) {
        Submission submission = getSubmissionEntityById(id);
        return toStatusDTO(submission);
    }

    @Override
    public SubmissionVO getSubmissionById(Long id) {
        Submission submission = getSubmissionEntityById(id);
        return toVO(submission, Collections.emptyMap());
    }

    @Override
    public Page<SubmissionVO> getSubmissionList(Long userId, Integer page, Integer size,
                                                Long problemId, String status, String language) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, userId);
        wrapper.eq(problemId != null, Submission::getProblemId, problemId);
        wrapper.eq(status != null && !status.isBlank(), Submission::getStatus, status);
        wrapper.eq(language != null && !language.isBlank(), Submission::getLanguage, language);
        wrapper.orderByDesc(Submission::getCreateTime);

        Page<Submission> submissionPage = submissionMapper.selectPage(pageObj, wrapper);
        Page<SubmissionVO> resultPage = new Page<>(
                submissionPage.getCurrent(),
                submissionPage.getSize(),
                submissionPage.getTotal()
        );
        Map<Long, String> problemTitleMap = loadProblemTitleMap(submissionPage.getRecords());
        resultPage.setRecords(submissionPage.getRecords().stream()
                .map(submission -> toVO(submission, problemTitleMap))
                .toList());
        return resultPage;
    }

    @Override
    public List<Submission> getSubmissionsByUserId(Long userId, Integer page, Integer size) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getUserId, userId);
        wrapper.orderByDesc(Submission::getCreateTime);
        return submissionMapper.selectPage(pageObj, wrapper).getRecords();
    }

    @Override
    public List<Submission> getSubmissionsByProblemId(Long problemId, Integer page, Integer size) {
        Page<Submission> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Submission::getProblemId, problemId);
        wrapper.orderByDesc(Submission::getCreateTime);
        return submissionMapper.selectPage(pageObj, wrapper).getRecords();
    }

    private Map<Long, String> loadProblemTitleMap(List<Submission> submissions) {
        Set<Long> problemIds = submissions.stream()
                .map(Submission::getProblemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        if (problemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Problem::getId, problemIds);
        return problemMapper.selectList(wrapper).stream()
                .collect(Collectors.toMap(Problem::getId, Problem::getTitle, (a, b) -> a));
    }

    private SubmissionVO toVO(Submission submission, Map<Long, String> problemTitleMap) {
        if (submission == null) {
            return null;
        }

        SubmissionVO vo = new SubmissionVO();
        vo.setId(submission.getId());
        vo.setProblemId(submission.getProblemId());
        vo.setLanguage(submission.getLanguage());
        vo.setStatus(submission.getStatus());
        vo.setExecuteTime(submission.getTimeUsed());
        vo.setExecuteMemory(submission.getMemoryUsed());
        vo.setCode(submission.getCode());
        vo.setErrorMessage(submission.getErrorMessage());
        vo.setSubmitTime(submission.getCreateTime());

        if (submission.getProblemId() != null) {
            String problemTitle = problemTitleMap.get(submission.getProblemId());
            if (problemTitle == null) {
                Problem problem = problemMapper.selectById(submission.getProblemId());
                problemTitle = problem != null ? problem.getTitle() : null;
            }
            vo.setProblemTitle(problemTitle);
        }

        return vo;
    }

    private SubmissionStatusDTO toStatusDTO(Submission submission) {
        if (submission == null) {
            return null;
        }
        SubmissionStatusDTO dto = new SubmissionStatusDTO();
        dto.setId(submission.getId());
        dto.setStatus(submission.getStatus());
        dto.setExecuteTime(submission.getTimeUsed());
        dto.setExecuteMemory(submission.getMemoryUsed());
        dto.setErrorMessage(submission.getErrorMessage());
        dto.setSubmitTime(submission.getCreateTime());
        return dto;
    }

    private void increaseProblemSubmitCount(Long problemId) {
        if (problemId == null) {
            return;
        }
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, problemId)
                .setSql("submit_count = COALESCE(submit_count, 0) + 1");
        problemMapper.update(null, updateWrapper);
    }

    private void increaseProblemAcceptedCount(Long problemId) {
        if (problemId == null) {
            return;
        }
        LambdaUpdateWrapper<Problem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Problem::getId, problemId)
                .setSql("ac_count = COALESCE(ac_count, 0) + 1");
        problemMapper.update(null, updateWrapper);
    }

    private void validateSubmit(SubmitDTO submitDTO) {
        Problem problem = problemMapper.selectById(submitDTO.getProblemId());
        if (problem == null || !Integer.valueOf(1).equals(problem.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "Problem not found");
        }

        String language = submitDTO.getLanguage();
        if (!Constants.LANGUAGE_JAVA.equals(language)
                && !Constants.LANGUAGE_CPP.equals(language)
                && !Constants.LANGUAGE_PYTHON.equals(language)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Unsupported language");
        }
    }
}

