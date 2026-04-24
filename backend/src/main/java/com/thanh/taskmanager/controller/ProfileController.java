package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.auth.ChangePasswordRequest;
import com.thanh.taskmanager.dto.response.ApiResponse;
import com.thanh.taskmanager.dto.response.UserResponse;
import com.thanh.taskmanager.service.AuthService;
import com.thanh.taskmanager.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class ProfileController {
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(authService.getMe(currentUserId)));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }
}