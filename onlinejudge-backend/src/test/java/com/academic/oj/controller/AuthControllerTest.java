package com.academic.oj.controller;

import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private AdminOperationLogService adminOperationLogService;

    @InjectMocks
    private AuthController authController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginShouldRecordAuditLog() {
        LoginDTO loginDTO = new LoginDTO();
        loginDTO.setUsername("alice");
        loginDTO.setPassword("123456");

        UserInfoDTO userInfoDTO = new UserInfoDTO();
        userInfoDTO.setId(7L);
        userInfoDTO.setUsername("alice");
        TokenDTO tokenDTO = new TokenDTO("mock-token", userInfoDTO);
        when(userService.login(loginDTO)).thenReturn(tokenDTO);

        TokenDTO result = (TokenDTO) authController.login(loginDTO).getData();

        assertEquals("mock-token", result.getToken());
        verify(adminOperationLogService).record(7L, "AUTH", "LOGIN", "USER", 7L, "username=alice");
    }

    @Test
    void logoutShouldRecordAuditLogWhenPrincipalIsUserId() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("9", null)
        );

        authController.logout();

        verify(adminOperationLogService).record(9L, "AUTH", "LOGOUT", "USER", 9L, "logout");
    }

    @Test
    void logoutShouldIgnoreInvalidPrincipal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("invalid", null)
        );

        authController.logout();

        verifyNoInteractions(adminOperationLogService);
    }
}
