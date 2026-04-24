package com.thanh.taskmanager.service;

import com.thanh.taskmanager.dto.request.auth.ChangePasswordRequest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.response.TokenResponse;
import com.thanh.taskmanager.dto.response.UserResponse;

public interface AuthService {

    // Đăng ký tài khoản mới — trả về thông tin user vừa tạo
    UserResponse register(RegisterRequest request);

    // Đăng nhập — trả về JWT token
    TokenResponse login(LoginRequest request);

    // Lấy thông tin user đang đăng nhập
    UserResponse getMe(Long currentUserId);

    // Đổi mật khẩu — void vì không cần trả data
    void changePassword(Long currentUserId, ChangePasswordRequest request);

    TokenResponse refreshToken(String refreshToken);
}
