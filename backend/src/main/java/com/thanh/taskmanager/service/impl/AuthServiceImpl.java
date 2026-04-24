package com.thanh.taskmanager.service.impl;

import com.thanh.taskmanager.dto.request.auth.ChangePasswordRequest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.response.TokenResponse;
import com.thanh.taskmanager.dto.response.UserResponse;
import com.thanh.taskmanager.entity.RefreshToken;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.RefreshTokenRepository;
import com.thanh.taskmanager.repository.UserRepository;
import com.thanh.taskmanager.security.CustomUserDetails;
import com.thanh.taskmanager.service.AuthService;
import com.thanh.taskmanager.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.register(request.getEmail(), request.getPassword(), request.getFullName(), passwordEncoder);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.getUser();

        String token = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());
        LocalDateTime expires = jwtService.getExpirationTime();

        return TokenResponse.builder()
                .accessToken(token)
                .refreshToken(refreshToken.getToken())
                .expiresAt(expires)
                .build();
    }

    @Override
    public UserResponse getMe(Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toResponse(currentUser);
    }

    @Override
    @Transactional
    public void changePassword(Long currentUserId, ChangePasswordRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        currentUser.changePassword(request.getCurrentPassword(), request.getNewPassword(), passwordEncoder);
    }

    @Override
    public TokenResponse refreshToken(String refreshToken) {
        return refreshTokenRepository.findByToken(refreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(validToken -> {
                    var user = validToken.getUser();
                    String newAccessToken = jwtService.generateAccessToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.rotateRefreshToken(validToken);

                    return TokenResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(newRefreshToken.getToken())
                            .expiresAt(jwtService.getExpirationTime())
                            .build();
                })
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }
}
