package com.thanh.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.repository.*;
import com.thanh.taskmanager.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
public class AuthControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private static final String EMAIL     = "integration@test.com";
    private static final String PASSWORD  = "password123";
    private static final String FULL_NAME = "Integration User";

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should register user and return 200 with user data")
        void register_WhenValidRequest_ShouldReturn200AndPersistUser() throws Exception {
            RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.email").value(EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(FULL_NAME))
                    .andExpect(jsonPath("$.data.id").isNumber());

            assert userRepository.existsByEmail(EMAIL);
        }

        @Test
        @DisplayName("Should return 409 when email is already registered")
        void register_WhenEmailAlreadyExists_ShouldReturn409() throws Exception {
            RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void register_WhenEmailInvalid_ShouldReturn400() throws Exception {
            RegisterRequest request = new RegisterRequest("not-an-email", PASSWORD, FULL_NAME);

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when required fields are blank")
        void register_WhenFieldsBlank_ShouldReturn400() throws Exception {
            RegisterRequest request = new RegisterRequest("", "", "");

            mockMvc.perform(post("/auth/register")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @BeforeEach
        void registerUser() throws Exception {
            RegisterRequest request = new RegisterRequest(EMAIL, PASSWORD, FULL_NAME);
            mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));
        }

        @Test
        @DisplayName("Should return 200 with access and refresh tokens when credentials are valid")
        void login_WhenValidCredentials_ShouldReturn200WithTokens() throws Exception {
            LoginRequest request = new LoginRequest(EMAIL, PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 401 when password is wrong")
        void login_WhenWrongPassword_ShouldReturn401() throws Exception {
            LoginRequest request = new LoginRequest(EMAIL, "wrongpassword");

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("Should return 401 when email does not exist")
        void login_WhenEmailNotFound_ShouldReturn401() throws Exception {
            LoginRequest request = new LoginRequest("nobody@test.com", PASSWORD);

            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)
                            )
                    )
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /auth/refresh
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/refresh")
    class RefreshTokenTests {

        private String validRefreshToken;

        @BeforeEach
        void registerAndLogin() throws Exception {
            // Register
            mockMvc.perform(post("/auth/register")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(
                            new RegisterRequest(EMAIL, PASSWORD, FULL_NAME))));

            String loginResponse = mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(EMAIL, PASSWORD))
                            )
                    )
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            validRefreshToken = objectMapper
                    .readTree(loginResponse)
                    .path("data")
                    .path("refreshToken")
                    .asText();
        }

        @Test
        @DisplayName("Should return 200 with new tokens when refresh token is valid")
        void refresh_WhenValidToken_ShouldReturn200WithNewTokens() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .with(csrf())
                            .param("token", validRefreshToken)
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 404 when refresh token does not exist")
        void refresh_WhenUnknownToken_ShouldReturn404() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .with(csrf())
                            .param("token", "completely-unknown-token")
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("Should return 400 when token param is missing")
        void refresh_WhenMissingParam_ShouldReturn400() throws Exception {
            mockMvc.perform(post("/auth/refresh")
                            .with(csrf()))
                    .andExpect(status().isBadRequest());
        }
    }
}
