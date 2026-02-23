package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.UserListDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.UserService;
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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private UserController userController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserListShouldRejectStudent() {
        setAuth(1L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class, () -> userController.getUserList(1, 10, null, null, null));
        assertEquals(403, ex.getCode());
    }

    @Test
    void getUserListShouldAllowAdmin() {
        setAuth(1L, "ADMIN");
        Page<UserListDTO> page = new Page<>(1, 10, 1);
        UserListDTO dto = new UserListDTO();
        dto.setId(99L);
        page.setRecords(List.of(dto));
        when(userService.getUserList(1, 10, null, null, null)).thenReturn(page);

        Page<UserListDTO> result = (Page<UserListDTO>) userController.getUserList(1, 10, null, null, null).getData();
        assertEquals(1, result.getRecords().size());
        verify(userService).getUserList(1, 10, null, null, null);
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
