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
import com.thanh.taskmanager.fixture.UserFixture;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.RefreshTokenRepository;
import com.thanh.taskmanager.repository.UserRepository;
import com.thanh.taskmanager.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthServiceImpl authService;

    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    // ── shared test data ──────────────────────────────────────────────────────
    private static final String EMAIL        = "test123@test.com";
    private static final String FULL_NAME    = "Test 123";
    private static final String PASSWORD     = "password123";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ENCODED_PWD  = "encodedPassword";
    private static final String ACCESS_TOKEN = "mock.access.token";

    private User mockUser;
    private RefreshToken mockRefreshToken;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private ChangePasswordRequest changePasswordRequest;

    @BeforeEach
    void setUp() {
        mockUser           = UserFixture.aUser().build();
        mockRefreshToken   = RefreshToken.create(mockUser, 604800000);
        registerRequest    = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);
        loginRequest       = new LoginRequest(EMAIL, PASSWORD);
        changePasswordRequest = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Register
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("Should throw EMAIL_ALREADY_EXISTS when email is taken")
        void register_WhenEmailAlreadyExists_ShouldThrowAppException() {
            given(userRepository.existsByEmail(EMAIL)).willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> authService.register(registerRequest));

            assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());

            verify(userRepository, never()).save(any(User.class));
            verifyNoInteractions(passwordEncoder, userMapper);
        }

        @Test
        @DisplayName("Should encode password and return UserResponse when email is available")
        void register_WhenValidRequest_ShouldReturnUserResponse() {
            UserResponse expectedResponse = UserResponse.builder()
                    .id(1L)
                    .fullName(FULL_NAME)
                    .email(EMAIL)
                    .build();

            given(userRepository.existsByEmail(EMAIL)).willReturn(false);
            given(passwordEncoder.encode(PASSWORD)).willReturn(ENCODED_PWD);
            given(userRepository.save(any(User.class))).willReturn(mockUser);
            given(userMapper.toResponse(mockUser)).willReturn(expectedResponse);

            UserResponse actual = authService.register(registerRequest);

            assertNotNull(actual);
            assertEquals(EMAIL, actual.getEmail());
            assertEquals(FULL_NAME, actual.getFullName());

            verify(userRepository).existsByEmail(EMAIL);
            verify(passwordEncoder).encode(PASSWORD);
            verify(userRepository).save(any(User.class));
            verify(userMapper).toResponse(mockUser);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Login
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class LoginTests {

        @Test
        @DisplayName("Should return TokenResponse with access token and expiry when credentials are valid")
        void login_WhenValidCredentials_ShouldReturnTokenResponse() {
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(15);

            Authentication authentication   = mock(Authentication.class);
            CustomUserDetails userDetails   = mock(CustomUserDetails.class);

            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willReturn(authentication);
            given(authentication.getPrincipal()).willReturn(userDetails);
            given(userDetails.getUser()).willReturn(mockUser);
            given(jwtService.generateAccessToken(mockUser)).willReturn(ACCESS_TOKEN);
            given(refreshTokenService.createRefreshToken(mockUser.getId())).willReturn(mockRefreshToken);
            given(jwtService.getExpirationTime()).willReturn(expiresAt);

            TokenResponse actual = authService.login(loginRequest);

            assertNotNull(actual);
            assertEquals(ACCESS_TOKEN, actual.getAccessToken());
            assertEquals(expiresAt, actual.getExpiresAt());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtService).generateAccessToken(mockUser);
            verify(refreshTokenService).createRefreshToken(mockUser.getId());
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException when password is wrong")
        void login_WhenPasswordIsWrong_ShouldThrowBadCredentialsException() {
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new BadCredentialsException("Invalid credentials"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessage("Invalid credentials");

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }

        @Test
        @DisplayName("Should propagate LockedException when account is locked")
        void login_WhenAccountIsLocked_ShouldThrowLockedException() {
            given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .willThrow(new LockedException("Account locked"));

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(LockedException.class)
                    .hasMessage("Account locked");

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).createRefreshToken(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GetMe
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMe")
    class GetMeTests {

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
        void getMe_WhenUserNotFound_ShouldThrowAppException() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> authService.getMe(99L));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("Should return mapped UserResponse when user exists")
        void getMe_WhenUserExists_ShouldReturnUserResponse() {
            // FIX: original test stubbed findById but forgot to stub userMapper.toResponse,
            // then asserted assertNull — meaning it was accidentally testing the unmapped null.
            UserResponse expectedResponse = UserResponse.builder()
                    .id(mockUser.getId())
                    .fullName(FULL_NAME)
                    .email(EMAIL)
                    .build();

            given(userRepository.findById(mockUser.getId())).willReturn(Optional.of(mockUser));
            given(userMapper.toResponse(mockUser)).willReturn(expectedResponse);

            UserResponse actual = authService.getMe(mockUser.getId());

            assertNotNull(actual);
            assertEquals(EMAIL, actual.getEmail());
            assertEquals(FULL_NAME, actual.getFullName());

            verify(userRepository).findById(mockUser.getId());
            verify(userMapper).toResponse(mockUser);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Change Password
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("changePassword")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
        void changePassword_WhenUserNotFound_ShouldThrowAppException() {
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> authService.changePassword(99L, changePasswordRequest));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Should throw WRONG_PASSWORD when current password does not match")
        void changePassword_WhenCurrentPasswordWrong_ShouldThrowAppException() {
            given(userRepository.findById(mockUser.getId())).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(PASSWORD, mockUser.getHashPassword())).willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> authService.changePassword(mockUser.getId(), changePasswordRequest));

            assertEquals(ErrorCode.WRONG_PASSWORD, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw SAME_PASSWORD when new password equals current password")
        void changePassword_WhenNewPasswordSameAsCurrent_ShouldThrowAppException() {
            given(userRepository.findById(mockUser.getId())).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(PASSWORD, mockUser.getHashPassword())).willReturn(true);
            given(passwordEncoder.matches(NEW_PASSWORD, mockUser.getHashPassword())).willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> authService.changePassword(mockUser.getId(), changePasswordRequest));

            assertEquals(ErrorCode.SAME_PASSWORD, exception.getErrorCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should encode new password and save user when request is valid")
        void changePassword_WhenValidRequest_ShouldEncodeAndSave() {
            given(userRepository.findById(mockUser.getId())).willReturn(Optional.of(mockUser));
            given(passwordEncoder.matches(PASSWORD, mockUser.getHashPassword())).willReturn(true);
            given(passwordEncoder.matches(NEW_PASSWORD, mockUser.getHashPassword())).willReturn(false);
            given(passwordEncoder.encode(NEW_PASSWORD)).willReturn(ENCODED_PWD);

            authService.changePassword(mockUser.getId(), changePasswordRequest);

            verify(passwordEncoder).encode(NEW_PASSWORD);
            verifyNoMoreInteractions(userRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Refresh Token
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("refreshToken")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should throw REFRESH_TOKEN_NOT_FOUND when token does not exist in DB")
        void refreshToken_WhenTokenNotFound_ShouldThrowException() {
            given(refreshTokenRepository.findByToken(anyString())).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> authService.refreshToken("unknown-token"));

            assertEquals(ErrorCode.REFRESH_TOKEN_NOT_FOUND, exception.getErrorCode());

            verifyNoInteractions(refreshTokenService, jwtService);
        }

        @Test
        @DisplayName("Should return new TokenResponse with rotated refresh token when token is valid")
        void refreshToken_WhenTokenValid_ShouldReturnRotatedTokenResponse() {
            given(refreshTokenRepository.findByToken(mockRefreshToken.getToken()))
                    .willReturn(Optional.of(mockRefreshToken));
            given(refreshTokenService.verifyExpiration(mockRefreshToken)).willReturn(mockRefreshToken);
            given(jwtService.generateAccessToken(mockUser)).willReturn(ACCESS_TOKEN);
            given(refreshTokenService.rotateRefreshToken(mockRefreshToken)).willReturn(mockRefreshToken);

            TokenResponse actual = authService.refreshToken(mockRefreshToken.getToken());

            assertNotNull(actual);
            assertEquals(ACCESS_TOKEN, actual.getAccessToken());
            assertEquals(mockRefreshToken.getToken(), actual.getRefreshToken());

            verify(refreshTokenService).verifyExpiration(mockRefreshToken);
            verify(jwtService).generateAccessToken(mockUser);
            verify(refreshTokenService).rotateRefreshToken(mockRefreshToken);
        }

        @Test
        @DisplayName("Should throw exception when token is expired")
        void refreshToken_WhenTokenExpired_ShouldThrowException() {
            given(refreshTokenRepository.findByToken(mockRefreshToken.getToken()))
                    .willReturn(Optional.of(mockRefreshToken));
            given(refreshTokenService.verifyExpiration(mockRefreshToken))
                    .willThrow(new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED));

            AppException exception = assertThrows(AppException.class,
                    () -> authService.refreshToken(mockRefreshToken.getToken()));

            assertEquals(ErrorCode.REFRESH_TOKEN_EXPIRED, exception.getErrorCode());

            verify(jwtService, never()).generateAccessToken(any());
            verify(refreshTokenService, never()).rotateRefreshToken(any());
        }
    }
}