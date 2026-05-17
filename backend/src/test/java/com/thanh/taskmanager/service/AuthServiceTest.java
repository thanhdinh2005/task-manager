package com.thanh.taskmanager.service;

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
import com.thanh.taskmanager.service.impl.AuthServiceImpl;
import com.thanh.taskmanager.service.impl.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

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

    private User mockUser;
    private RefreshToken mockRefreshToken;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private ChangePasswordRequest changePasswordRequest;

    private final String emailTest = "test123@test.com";
    private final String fullNameTest = "Test 123";
    private final String passwordTest = "password123";
    private final String encodedPassword = "encodedPassword";
    private final String mockAccessToken = "mock.token.test";
    private final String newPassword = "newPassword";

    // Setup
    @BeforeEach
    void setUp() {
        mockUser = User.register(emailTest, fullNameTest, passwordTest, passwordEncoder);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "hashPassword", encodedPassword);

        mockRefreshToken = RefreshToken.create(mockUser, 604800000);

        registerRequest = new RegisterRequest(emailTest, passwordTest, fullNameTest);
        loginRequest = new LoginRequest(emailTest, passwordTest);
        changePasswordRequest = new ChangePasswordRequest(passwordTest, newPassword);
    }

    //
    // Register
    //
    @Test
    @DisplayName("Should throw App exception when register email already exists")
    void register_WhenEmailAlreadyExists_ShouldThrowAppException() {
        given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(registerRequest));

        assertEquals(ErrorCode.EMAIL_ALREADY_EXISTS, exception.getErrorCode());

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should return UserResponse when register successfully")
    void register_WhenValidRequest_ShouldReturnUserResponse() {
        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .fullName(fullNameTest)
                .email(emailTest)
                .build();

        given(userRepository.existsByEmail(registerRequest.getEmail())).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn(encodedPassword);
        given(userRepository.save(any(User.class))).willReturn(mockUser);
        given(userMapper.toResponse(mockUser)).willReturn(expectedResponse);

        UserResponse actualResponse = authService.register(registerRequest);

        assertNotNull(actualResponse);
        assertEquals(expectedResponse.getEmail(), actualResponse.getEmail());

        verify(userRepository, times(1)).existsByEmail(registerRequest.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
        verify(userMapper, times(1)).toResponse(mockUser);
    }

    //
    // Login
    //
    @Test
    @DisplayName("Should return TokenResponse when login successfully")
    void login_WhenValidCredentials_ShouldReturnTokenResponse() {
        LocalDateTime mockExpiresAt = LocalDateTime.now().plusMinutes(15);

        Authentication authentication = mock(Authentication.class);
        CustomUserDetails userDetails = mock(CustomUserDetails.class);

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userDetails.getUser()).willReturn(mockUser);

        given(jwtService.generateAccessToken(mockUser)).willReturn(mockAccessToken);
        given(refreshTokenService.createRefreshToken(mockUser.getId())).willReturn(mockRefreshToken);
        given(jwtService.getExpirationTime()).willReturn(mockExpiresAt);

        TokenResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals(mockAccessToken, response.getAccessToken());
        assertEquals(mockExpiresAt, response.getExpiresAt());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should throw BadCredentialException when credentials are invalid")
    void login_WhenValidCredentials_ShouldThrowBadCredentialsException() {

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new BadCredentialsException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    @DisplayName("Should throw LockedException when account is locked")
    void login_WhenValidCredentials_ShouldThrowLockedException() {

        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .willThrow(new LockedException("Account locked"));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LockedException.class)
                .hasMessage("Account locked");

        verify(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    //
    // GetMe
    //
    @Test
    @DisplayName("Should throw AppException when user not found")
    void getMe_WhenUserNotFound_ShouldThrowAppException() {
        Long userId = 99L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> {
            authService.getMe(userId);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should return UserResponse")
    void getMe_WhenUserExists_ShouldReturnUserResponse() {
        UserResponse expectedResponse = UserResponse.builder()
                .id(1L)
                .fullName(fullNameTest)
                .email(emailTest)
                .build();

        given(userRepository.findById(mockUser.getId())).willReturn(Optional.ofNullable(mockUser));

        UserResponse actualResponse = authService.getMe(mockUser.getId());

        assertNull(actualResponse);

        verify(userRepository, times(1)).findById(anyLong());
        verify(userMapper, times(1)).toResponse(mockUser);
    }

    //
    // Change password
    //
    @Test
    @DisplayName("Should successfully when valid request")
    void changePassword_WhenValidRequest_ShouldChangePasswordSuccessfully() {
        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), mockUser.getHashPassword())).willReturn(true);
        given(passwordEncoder.matches(changePasswordRequest.getNewPassword(), mockUser.getHashPassword())).willReturn(false);

        authService.changePassword(1L, changePasswordRequest);

        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when user not found")
    void changePassword_WhenUserNotFound_ShouldThrowAppException() {
        Long userId = 99L;
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> authService.changePassword(userId, changePasswordRequest));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }



}
