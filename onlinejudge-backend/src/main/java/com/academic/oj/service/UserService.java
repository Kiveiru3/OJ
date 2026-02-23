package com.academic.oj.service;

import com.academic.oj.dto.LoginDTO;
import com.academic.oj.dto.RegisterDTO;
import com.academic.oj.dto.TokenDTO;
import com.academic.oj.dto.UserInfoDTO;
import com.academic.oj.dto.UserListDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface UserService {
    TokenDTO login(LoginDTO loginDTO);
    void register(RegisterDTO registerDTO);
    UserInfoDTO getUserInfo(Long userId);
    void updatePassword(Long userId, String oldPassword, String newPassword);
    void updateUserInfo(Long userId, UserInfoDTO userInfo);
    Page<UserListDTO> getUserList(Integer page, Integer size, String keyword, String role, Integer status);
    void adminUpdateUser(Long operatorId, Long targetUserId, String role, Integer status);
    void adminResetPassword(Long operatorId, Long targetUserId, String newPassword);
}

