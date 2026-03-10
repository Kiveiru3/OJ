package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.AdminResetPasswordDTO;
import com.academic.oj.dto.AdminUserUpdateDTO;
import com.academic.oj.dto.ChangePasswordDTO;
import com.academic.oj.dto.RoleProfileDTO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.dto.UserListDTO;
import com.academic.oj.service.AdminOperationLogService;
import com.academic.oj.service.UserService;
import com.academic.oj.util.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AdminOperationLogService adminOperationLogService;

    @GetMapping("/info")
    public Result<UserInfoDTO> getUserInfo() {
        Long userId = getCurrentUserId();
        UserInfoDTO userInfo = userService.getUserInfo(userId);
        return Result.success(userInfo);
    }

    @GetMapping("/role-profile")
    public Result<RoleProfileDTO> getMyRoleProfile() {
        Long userId = getCurrentUserId();
        return Result.success(userService.getRoleProfile(userId));
    }

    @PutMapping("/role-profile")
    public Result<?> updateMyRoleProfile(@RequestBody RoleProfileDTO profileDTO) {
        Long userId = getCurrentUserId();
        userService.updateRoleProfile(userId, profileDTO);
        return Result.success("Role profile updated");
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        Long userId = getCurrentUserId();
        userService.updatePassword(userId, oldPassword, newPassword);
        return Result.success("Password updated successfully");
    }

    @PutMapping("/info")
    public Result<?> updateUserInfo(@RequestBody UserInfoDTO userInfo) {
        Long userId = getCurrentUserId();
        userService.updateUserInfo(userId, userInfo);
        return Result.success("User info updated successfully");
    }

    @PutMapping("/update")
    public Result<?> updateUserInfoCompat(@RequestBody UserInfoDTO userInfo) {
        return updateUserInfo(userInfo);
    }

    @PostMapping("/change-password")
    public Result<?> changePassword(@Validated @RequestBody ChangePasswordDTO passwordDTO) {
        Long userId = getCurrentUserId();
        userService.updatePassword(userId, passwordDTO.getOldPassword(), passwordDTO.getNewPassword());
        return Result.success("Password updated successfully");
    }

    @GetMapping("/list")
    public Result<Page<UserListDTO>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        requireAdmin();
        Page<UserListDTO> userPage = userService.getUserList(
                normalizePage(page), normalizeSize(size), keyword, role, status);
        return Result.success(userPage);
    }

    @PutMapping("/{id}/admin")
    public Result<?> adminUpdateUser(@PathVariable Long id, @Validated @RequestBody AdminUserUpdateDTO dto) {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        userService.adminUpdateUser(operatorId, id, dto.getRole(), dto.getStatus());
        adminOperationLogService.record(operatorId, "USER_MANAGE", "UPDATE_USER", "USER", id,
                "role=" + dto.getRole() + ",status=" + dto.getStatus());
        return Result.success("User updated");
    }

    @PostMapping("/{id}/reset-password")
    public Result<?> adminResetPassword(@PathVariable Long id, @Validated @RequestBody AdminResetPasswordDTO dto) {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        userService.adminResetPassword(operatorId, id, dto.getNewPassword());
        adminOperationLogService.record(operatorId, "USER_MANAGE", "RESET_PASSWORD", "USER", id, "password reset");
        return Result.success("Password reset successfully");
    }

    @GetMapping("/{id}/role-profile")
    public Result<RoleProfileDTO> adminGetRoleProfile(@PathVariable Long id) {
        requireAdmin();
        return Result.success(userService.getRoleProfile(id));
    }

    @PutMapping("/{id}/role-profile")
    public Result<?> adminUpdateRoleProfile(@PathVariable Long id, @RequestBody RoleProfileDTO profileDTO) {
        requireAdmin();
        Long operatorId = getCurrentUserId();
        userService.updateRoleProfile(id, profileDTO);
        adminOperationLogService.record(operatorId, "USER_MANAGE", "UPDATE_ROLE_PROFILE", "USER", id, "update role profile");
        return Result.success("Role profile updated");
    }

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    private void requireAdmin() {
        SecurityUtils.requireRole("ADMIN");
    }

    private Integer normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private Integer normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }
}

