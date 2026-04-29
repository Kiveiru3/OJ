package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.RegisterDTO;
import com.academic.oj.dto.SendVerificationCodeDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.VerificationCodeDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.RateLimitService;
import com.academic.oj.service.UserService;
import com.academic.oj.service.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AdminOperationLogService adminOperationLogService;
    private final RateLimitService rateLimitService;
    private final VerificationCodeService verificationCodeService;

    @PostMapping("/login")
    public Result<TokenDTO> login(@Validated @RequestBody LoginDTO loginDTO, HttpServletRequest request) {
        rateLimitService.checkLoginLimit(extractClientIp(request));
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

    @PostMapping("/verification-code")
    public Result<VerificationCodeDTO> sendVerificationCode(@Validated @RequestBody SendVerificationCodeDTO dto) {
        return Result.success(verificationCodeService.sendPhoneCode(dto.getPhone()));
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

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

