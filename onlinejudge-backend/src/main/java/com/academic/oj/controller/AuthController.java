package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.RegisterDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminOperationLogService adminOperationLogService;

    @PostMapping("/login")
    public Result<TokenDTO> login(@Validated @RequestBody LoginDTO loginDTO) {
        TokenDTO tokenDTO = userService.login(loginDTO);
        if (tokenDTO != null && tokenDTO.getUserInfo() != null) {
            Long userId = tokenDTO.getUserInfo().getId();
            String username = tokenDTO.getUserInfo().getUsername();
            safeRecord(userId, "AUTH", "LOGIN", "USER", userId, "username=" + username);
        }
        return Result.success(tokenDTO);
    }

    @PostMapping("/register")
    public Result<?> register(@Validated @RequestBody RegisterDTO registerDTO) {
        userService.register(registerDTO);
        return Result.success("Registration successful");
    }

    @PostMapping("/logout")
    public Result<?> logout() {
        // JWT is stateless; frontend just clears token.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            try {
                Long userId = Long.parseLong(authentication.getName());
                safeRecord(userId, "AUTH", "LOGOUT", "USER", userId, "logout");
            } catch (NumberFormatException ignored) {
                // Ignore invalid principal format.
            }
        }
        return Result.success("Logout successful");
    }

    private void safeRecord(Long operatorId, String module, String action,
                            String targetType, Long targetId, String detail) {
        try {
            adminOperationLogService.record(operatorId, module, action, targetType, targetId, detail);
        } catch (Exception ignored) {
            // Audit failures should not block auth flow.
        }
    }
}

