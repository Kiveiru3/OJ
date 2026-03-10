package com.academic.oj.controller;

import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.RoleProfileDTO;
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

    @Test
    void getMyRoleProfileShouldUseCurrentUser() {
        setAuth(8L, "STUDENT");
        RoleProfileDTO dto = new RoleProfileDTO();
        dto.setUserId(8L);
        dto.setRole("STUDENT");
        dto.setStudentNo("20261234");
        when(userService.getRoleProfile(8L)).thenReturn(dto);

        RoleProfileDTO result = (RoleProfileDTO) userController.getMyRoleProfile().getData();

        assertEquals("20261234", result.getStudentNo());
        verify(userService).getRoleProfile(8L);
    }

    @Test
    void adminGetRoleProfileShouldRejectStudent() {
        setAuth(8L, "STUDENT");
        BusinessException ex = assertThrows(BusinessException.class, () -> userController.adminGetRoleProfile(2L));
        assertEquals(403, ex.getCode());
    }

    @Test
    void adminUpdateRoleProfileShouldAllowAdmin() {
        setAuth(1L, "ADMIN");
        RoleProfileDTO dto = new RoleProfileDTO();
        dto.setRole("TEACHER");
        dto.setTeacherNo("T2026");
        dto.setDepartment("Computer Science");

        userController.adminUpdateRoleProfile(2L, dto);

        verify(userService).updateRoleProfile(2L, dto);
        verify(adminOperationLogService).record(1L, "USER_MANAGE", "UPDATE_ROLE_PROFILE", "USER", 2L, "update role profile");
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
