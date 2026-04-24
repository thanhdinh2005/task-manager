package com.thanh.taskmanager.service;

import com.thanh.taskmanager.entity.RefreshToken;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.repository.RefreshTokenRepository;
import com.thanh.taskmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${application.security.jwt.refresh-expiration-ms}")
    private Long refreshTokenDurationMs;

    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        RefreshToken refreshToken = RefreshToken.create(user, refreshTokenDurationMs);
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isRevoked()) {
            refreshTokenRepository.deleteAllByUser(token.getUser());
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        return token;
    }

    @Transactional
    public RefreshToken rotateRefreshToken(RefreshToken existingToken) {
        refreshTokenRepository.deleteAllByUser(existingToken.getUser());

        RefreshToken newToken = RefreshToken.create(existingToken.getUser(), refreshTokenDurationMs);

        return refreshTokenRepository.save(newToken);
    }
}