package com.academic.oj.service.impl;

import com.academic.oj.common.Constants;
import com.academic.oj.common.ResultCode;
import com.academic.oj.common.exception.BusinessException;
import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.RegisterDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.dto.UserListDTO;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.service.UserService;
import com.academic.oj.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Override
    public TokenDTO login(LoginDTO loginDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            log.warn("Login failed: user not found - {}", loginDTO.getUsername());
            throw new BusinessException("Invalid username or password");
        }

        String encodedPassword = user.getPassword();
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            log.error("User {} has null or empty password", loginDTO.getUsername());
            throw new BusinessException("Invalid username or password");
        }

        // 移除可能的空格、换行符等，并确保长度正确（BCrypt标准是60字符）
        encodedPassword = encodedPassword.trim();
        
        // 如果长度不是60，尝试截取前60个字符（可能是数据库中有额外字符）
        if (encodedPassword.length() != 60) {
            log.warn("Password hash length is {} (expected 60), truncating to 60 characters", encodedPassword.length());
            if (encodedPassword.length() > 60) {
                encodedPassword = encodedPassword.substring(0, 60);
            } else {
                log.error("Password hash too short: {} characters", encodedPassword.length());
                throw new BusinessException("Invalid username or password");
            }
        }
        
        // 检查密码长度（BCrypt密码应该是60个字符）
        int passwordLength = encodedPassword.length();
        if (passwordLength < 60) {
            log.error("Password for user {} is too short ({} chars), expected 60 for BCrypt", 
                    loginDTO.getUsername(), passwordLength);
            log.error("Password value: {}", encodedPassword);
            throw new BusinessException("Invalid username or password");
        }

        // 确保密码是BCrypt格式（以$2a$、$2b$或$2y$开头）
        if (!encodedPassword.startsWith("$2a$") && !encodedPassword.startsWith("$2b$") && !encodedPassword.startsWith("$2y$")) {
            log.error("Password for user {} is not in BCrypt format. First 10 chars: {}", 
                    loginDTO.getUsername(), encodedPassword.substring(0, Math.min(10, encodedPassword.length())));
            throw new BusinessException("Invalid username or password");
        }

        // 验证密码
        try {
            log.info("=== Password Verification Debug ===");
            log.info("Username: {}", loginDTO.getUsername());
            log.info("Password hash length: {}", passwordLength);
            log.info("Password hash prefix: {}", encodedPassword.substring(0, 7));
            log.info("Password hash suffix: {}", encodedPassword.substring(Math.max(0, passwordLength - 10)));
            log.info("Input password length: {}", loginDTO.getPassword() != null ? loginDTO.getPassword().length() : 0);
            
            boolean matches = passwordEncoder.matches(loginDTO.getPassword(), encodedPassword);
            log.info("Password match result: {}", matches);
            
            if (!matches) {
                log.error("=== Login Failed ===");
                log.error("Password mismatch for user: {}", loginDTO.getUsername());
                log.error("Stored hash: {}", encodedPassword);
                log.error("This usually means:");
                log.error("  1. The password entered is incorrect");
                log.error("  2. The password hash in database doesn't match the entered password");
                log.error("Solution: Use /dev/token/{} to get token without password", loginDTO.getUsername());
                throw new BusinessException("Invalid username or password");
            }
            
            log.info("Password verified successfully for user: {}", loginDTO.getUsername());
        } catch (IllegalArgumentException e) {
            log.error("Password encoding error for user {}: {}", loginDTO.getUsername(), e.getMessage());
            log.error("Password length: {}, First 20 chars: {}", 
                    passwordLength, encodedPassword.substring(0, Math.min(20, encodedPassword.length())));
            throw new BusinessException("Invalid username or password");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException("User account is disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());

        return new TokenDTO(token, userInfo);
    }

    @Override
    public void register(RegisterDTO registerDTO) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, registerDTO.getUsername());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException("Username already exists");
        }

        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, registerDTO.getEmail());
        if (userMapper.selectOne(wrapper) != null) {
            throw new BusinessException("Email already exists");
        }

        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setEmail(registerDTO.getEmail());
        user.setNickname(registerDTO.getNickname() != null ? registerDTO.getNickname() : registerDTO.getUsername());
        user.setRole(Constants.ROLE_STUDENT);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());

        userMapper.insert(user);
    }

    @Override
    public UserInfoDTO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());

        return userInfo;
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateUserInfo(Long userId, UserInfoDTO userInfo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("User not found");
        }

        if (userInfo.getEmail() != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getEmail, userInfo.getEmail());
            wrapper.ne(User::getId, userId);
            if (userMapper.selectOne(wrapper) != null) {
                throw new BusinessException("Email already exists");
            }
            user.setEmail(userInfo.getEmail());
        }

        if (userInfo.getNickname() != null) {
            user.setNickname(userInfo.getNickname());
        }

        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public Page<UserListDTO> getUserList(Integer page, Integer size, String keyword, String role, Integer status) {
        Page<User> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getEmail, keyword));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, normalizeRole(role));
        }
        if (status != null) {
            validateStatus(status);
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreateTime);
        Page<User> userPage = userMapper.selectPage(pageObj, wrapper);

        Page<UserListDTO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userPage.getRecords().stream().map(user -> {
            UserListDTO dto = new UserListDTO();
            dto.setId(user.getId());
            dto.setUsername(user.getUsername());
            dto.setNickname(user.getNickname());
            dto.setEmail(user.getEmail());
            dto.setRole(user.getRole());
            dto.setStatus(user.getStatus());
            dto.setCreateTime(user.getCreateTime());
            dto.setUpdateTime(user.getUpdateTime());
            return dto;
        }).toList());
        return resultPage;
    }

    @Override
    public void adminUpdateUser(Long operatorId, Long targetUserId, String role, Integer status) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }

        String normalizedRole = normalizeRole(role);
        validateStatus(status);

        if (operatorId.equals(targetUserId)) {
            if (!Constants.ROLE_ADMIN.equals(normalizedRole)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Cannot change your own role from ADMIN");
            }
            if (Integer.valueOf(0).equals(status)) {
                throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Cannot disable yourself");
            }
        }

        user.setRole(normalizedRole);
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void adminResetPassword(Long operatorId, Long targetUserId, String newPassword) {
        User user = userMapper.selectById(targetUserId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "User not found");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6 || newPassword.length() > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Password length must be between 6 and 50");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Role is required");
        }
        String normalized = role.trim().toUpperCase();
        if (!Constants.ROLE_ADMIN.equals(normalized)
                && !Constants.ROLE_TEACHER.equals(normalized)
                && !Constants.ROLE_STUDENT.equals(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid role");
        }
        return normalized;
    }

    private void validateStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "Invalid status");
        }
    }
}
