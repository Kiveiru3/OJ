package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.ContestSaveDTO;
import com.academic.oj.dto.ContestRankingItemVO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.ContestService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
class ContestControllerTest {

    @Mock
    private ContestService contestService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private ContestController contestController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getContestScoreSnapshotShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> contestController.getContestScoreSnapshot(3L, 1, 20));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getContestScoreSnapshotShouldAllowTeacher() {
        setAuth(7L, "TEACHER");
        Page<ContestRankingItemVO> page = new Page<>(1, 20, 1);
        ContestRankingItemVO item = new ContestRankingItemVO();
        item.setRank(1);
        item.setUserId(9L);
        page.setRecords(List.of(item));
        when(contestService.getContestScoreSnapshot(3L, 1, 20, true)).thenReturn(page);

        Page<ContestRankingItemVO> result = (Page<ContestRankingItemVO>) contestController
                .getContestScoreSnapshot(3L, 1, 20)
                .getData();

        assertEquals(1, result.getRecords().size());
        verify(contestService).getContestScoreSnapshot(3L, 1, 20, true);
    }

    @Test
    void createContestShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> contestController.createContest(buildContestSaveDTO()));
        assertEquals(403, ex.getCode());
    }

    @Test
    void createContestShouldAllowTeacher() {
        setAuth(7L, "TEACHER");
        ContestSaveDTO dto = buildContestSaveDTO();
        when(contestService.createContest(7L, dto)).thenReturn(88L);

        Long result = (Long) contestController.createContest(dto).getData();

        assertEquals(88L, result);
        verify(contestService).createContest(7L, dto);
        verify(adminOperationLogService).record(7L, "CONTEST", "CREATE", "CONTEST", 88L, "title=" + dto.getTitle());
    }

    private ContestSaveDTO buildContestSaveDTO() {
        ContestSaveDTO dto = new ContestSaveDTO();
        dto.setTitle("Core Contest");
        dto.setDescription("for permission test");
        dto.setStartTime(LocalDateTime.of(2026, 3, 10, 10, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 3, 10, 12, 0, 0));
        dto.setProblemIds(List.of(1001L, 1002L));
        dto.setStatus(1);
        dto.setPenaltyPerWrong(20);
        return dto;
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
