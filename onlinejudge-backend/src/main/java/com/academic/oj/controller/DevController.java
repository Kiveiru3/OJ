package com.academic.oj.controller;

import com.academic.oj.common.Result;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.entity.User;
import com.academic.oj.mapper.UserMapper;
import com.academic.oj.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

/**
 * 开发工具控制器
 * 仅用于开发环境，提供便捷的开发功能
 */
@Slf4j
@RestController
@Profile("dev")
@RequestMapping("/dev")
@RequiredArgsConstructor
public class DevController {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 快速获取token（无需密码验证）
     * 仅用于开发环境
     */
    @GetMapping("/token/{username}")
    public Result<TokenDTO> getToken(@PathVariable String username) {
        log.warn("Dev endpoint used: /dev/token/{} - This should only be used in development!", username);
        
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return Result.error(404, "User not found: " + username);
        }

        if (user.getStatus() == 0) {
            return Result.error(403, "User account is disabled");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserInfoDTO userInfo = new UserInfoDTO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickname(user.getNickname());
        userInfo.setRole(user.getRole());

        return Result.success(new TokenDTO(token, userInfo));
    }
}

