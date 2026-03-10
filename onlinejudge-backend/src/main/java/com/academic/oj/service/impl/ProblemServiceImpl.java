package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.ProblemBatchImportResultDTO;
import com.academic.oj.dto.ProblemImportItemDTO;
import com.academic.oj.dto.TestCaseDTO;
import com.academic.oj.entity.Problem;
import com.academic.oj.entity.Submission;
import com.academic.oj.entity.TestCase;
import com.academic.oj.mapper.ProblemMapper;
import com.academic.oj.mapper.SubmissionMapper;
import com.academic.oj.mapper.TestCaseMapper;
import com.academic.oj.service.ProblemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {
    private static final int MAX_BATCH_IMPORT_SIZE = 500;
    private static final int MAX_ERROR_ITEMS = 50;

    private final ProblemMapper problemMapper;
    private final TestCaseMapper testCaseMapper;
    private final SubmissionMapper submissionMapper;

    @Override
    public Page<Problem> getProblemList(Long userId, Integer page, Integer size, String difficulty, String keyword, boolean includeHidden) {
        Page<Problem> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<>();
        if (!includeHidden) {
            wrapper.eq(Problem::getStatus, 1);
        }
        
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(Problem::getDifficulty, difficulty);
        }
        
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Problem::getTitle, keyword)
                    .or().like(Problem::getDescription, keyword)
                    .or().like(Problem::getTags, keyword));
        }
        
        wrapper.orderByDesc(Problem::getCreateTime);
        Page<Problem> result = problemMapper.selectPage(pageObj, wrapper);
        fillSolvedFlag(result.getRecords(), userId);
        return result;
    }

    @Override
    public Problem getProblemById(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null || problem.getStatus() == 0) {
            throw new BusinessException("Problem not found");
        }
        return problem;
    }

    @Override
    public Problem getProblemByIdForManage(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BusinessException("Problem not found");
        }
        return problem;
    }

    @Override
    @Transactional
    public Long createProblem(Problem problem) {
        problem.setCreateTime(LocalDateTime.now());
        problem.setUpdateTime(LocalDateTime.now());
        if (problem.getStatus() == null) {
            problem.setStatus(1);
        }
        if (problem.getAcCount() == null) {
            problem.setAcCount(0);
        }
        if (problem.getSubmitCount() == null) {
            problem.setSubmitCount(0);
        }
        problemMapper.insert(problem);
        syncSampleTestCase(problem.getId(), problem.getSampleInput(), problem.getSampleOutput());
        return problem.getId();
    }

    @Override
    @Transactional
    public void updateProblem(Long id, Problem problem) {
        Problem existing = problemMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Problem not found");
        }
        
        problem.setId(id);
        problem.setUpdateTime(LocalDateTime.now());
        problemMapper.updateById(problem);
        syncSampleTestCase(id, problem.getSampleInput(), problem.getSampleOutput());
    }

    @Override
    @Transactional
    public void deleteProblem(Long id) {
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw new BusinessException("Problem not found");
        }
        testCaseMapper.delete(new LambdaQueryWrapper<TestCase>().eq(TestCase::getProblemId, id));
        problemMapper.deleteById(id);
    }

    @Override
    public ProblemBatchImportResultDTO batchImportProblems(Long operatorId, List<ProblemImportItemDTO> items, boolean skipExistingTitle) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException("Import list cannot be empty");
        }
        if (items.size() > MAX_BATCH_IMPORT_SIZE) {
            throw new BusinessException("Batch size cannot exceed " + MAX_BATCH_IMPORT_SIZE);
        }

        ProblemBatchImportResultDTO result = new ProblemBatchImportResultDTO();
        result.setTotal(items.size());
        result.setImported(0);
        result.setSkipped(0);
        result.setFailed(0);
        result.setErrors(new ArrayList<>());

        int index = 0;
        for (ProblemImportItemDTO item : items) {
            index++;
            try {
                String title = item == null ? null : trimToEmpty(item.getTitle());
                if (!StringUtils.hasText(title)) {
                    throw new BusinessException("title is required");
                }
                if (skipExistingTitle && existsProblemByTitle(title)) {
                    result.setSkipped(result.getSkipped() + 1);
                    continue;
                }

                Problem problem = buildProblemEntity(item, operatorId);
                Long problemId = createProblem(problem);
                insertHiddenTestCases(problemId, item.getTestCases());
                result.setImported(result.getImported() + 1);
            } catch (Exception ex) {
                result.setFailed(result.getFailed() + 1);
                if (result.getErrors().size() < MAX_ERROR_ITEMS) {
                    result.getErrors().add("[" + index + "] " + getSafeErrorMessage(ex));
                }
            }
        }
        return result;
    }

    private void syncSampleTestCase(Long problemId, String sampleInput, String sampleOutput) {
        if (problemId == null) {
            return;
        }

        LambdaQueryWrapper<TestCase> wrapper = new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .eq(TestCase::getIsSample, 1)
                .last("LIMIT 1");
        TestCase sampleCase = testCaseMapper.selectOne(wrapper);

        if (!StringUtils.hasText(sampleInput) && !StringUtils.hasText(sampleOutput)) {
            if (sampleCase != null) {
                testCaseMapper.deleteById(sampleCase.getId());
            }
            return;
        }

        if (sampleCase == null) {
            sampleCase = new TestCase();
            sampleCase.setProblemId(problemId);
            sampleCase.setIsSample(1);
            sampleCase.setInput(sampleInput == null ? "" : sampleInput);
            sampleCase.setOutput(sampleOutput == null ? "" : sampleOutput);
            testCaseMapper.insert(sampleCase);
            return;
        }

        sampleCase.setInput(sampleInput == null ? "" : sampleInput);
        sampleCase.setOutput(sampleOutput == null ? "" : sampleOutput);
        testCaseMapper.updateById(sampleCase);
    }

    private void fillSolvedFlag(List<Problem> problems, Long userId) {
        if (problems == null || problems.isEmpty()) {
            return;
        }
        if (userId == null) {
            problems.forEach(problem -> problem.setSolved(false));
            return;
        }

        List<Long> problemIds = problems.stream()
                .map(Problem::getId)
                .filter(id -> id != null && id > 0)
                .toList();
        if (problemIds.isEmpty()) {
            problems.forEach(problem -> problem.setSolved(false));
            return;
        }

        LambdaQueryWrapper<Submission> submissionWrapper = new LambdaQueryWrapper<Submission>()
                .select(Submission::getProblemId)
                .eq(Submission::getUserId, userId)
                .eq(Submission::getStatus, Constants.STATUS_ACCEPTED)
                .in(Submission::getProblemId, problemIds)
                .groupBy(Submission::getProblemId);
        List<Submission> acceptedList = submissionMapper.selectList(submissionWrapper);
        Set<Long> solvedProblemIds = acceptedList == null
                ? Collections.emptySet()
                : acceptedList.stream()
                .map(Submission::getProblemId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        problems.forEach(problem -> problem.setSolved(solvedProblemIds.contains(problem.getId())));
    }

    private Problem buildProblemEntity(ProblemImportItemDTO item, Long operatorId) {
        Problem problem = new Problem();
        problem.setTitle(trimToEmpty(item.getTitle()));
        problem.setDescription(trimToEmpty(item.getDescription()));
        problem.setInputFormat(trimToEmpty(item.getInputFormat()));
        problem.setOutputFormat(trimToEmpty(item.getOutputFormat()));
        problem.setSampleInput(item.getSampleInput());
        problem.setSampleOutput(item.getSampleOutput());
        problem.setHint(item.getHint());
        problem.setTimeLimit(item.getTimeLimit() == null || item.getTimeLimit() <= 0 ? 1000 : item.getTimeLimit());
        problem.setMemoryLimit(item.getMemoryLimit() == null || item.getMemoryLimit() <= 0 ? 262144 : item.getMemoryLimit());
        problem.setDifficulty(normalizeDifficulty(item.getDifficulty()));
        problem.setTags(trimToEmpty(item.getTags()));
        problem.setStatus(item.getStatus() == null ? 1 : (item.getStatus() == 0 ? 0 : 1));
        problem.setCreatorId(operatorId);
        return problem;
    }

    private String normalizeDifficulty(String value) {
        if (!StringUtils.hasText(value)) {
            return "MEDIUM";
        }
        String normalized = value.trim().toUpperCase();
        if ("EASY".equals(normalized) || "MEDIUM".equals(normalized) || "HARD".equals(normalized)) {
            return normalized;
        }
        return "MEDIUM";
    }

    private void insertHiddenTestCases(Long problemId, List<TestCaseDTO> testCases) {
        if (problemId == null || testCases == null || testCases.isEmpty()) {
            return;
        }
        for (TestCaseDTO dto : testCases) {
            if (dto == null) {
                continue;
            }
            TestCase testCase = new TestCase();
            testCase.setProblemId(problemId);
            testCase.setInput(dto.getInput() == null ? "" : dto.getInput());
            testCase.setOutput(dto.getOutput() == null ? "" : dto.getOutput());
            testCase.setIsSample(0);
            testCaseMapper.insert(testCase);
        }
    }

    private boolean existsProblemByTitle(String title) {
        Long count = problemMapper.selectCount(
                new LambdaQueryWrapper<Problem>().eq(Problem::getTitle, title)
        );
        return count != null && count > 0;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String getSafeErrorMessage(Exception ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return "unknown error";
        }
        return ex.getMessage();
    }
}


