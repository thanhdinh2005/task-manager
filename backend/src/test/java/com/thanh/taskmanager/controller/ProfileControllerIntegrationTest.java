package com.thanh.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.taskmanager.BaseIntegrationTest;
import com.thanh.taskmanager.dto.request.auth.ChangePasswordRequest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class ProfileControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private static final String EMAIL     = "profile@test.com";
    private static final String PASSWORD  = "password123";
    private static final String FULL_NAME = "Profile User";

    private String accessToken;

    @BeforeEach
    void registerAndLogin() throws Exception {
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        // Register a real user
        mockMvc.perform(post("/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new RegisterRequest(EMAIL, PASSWORD, FULL_NAME))));

        // Login and capture the real access token
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(EMAIL, PASSWORD))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        accessToken = objectMapper
                .readTree(loginResponse)
                .path("data")
                .path("accessToken")
                .asText();
    }

    /** Performs an authenticated GET using the real JWT from login. */
    private org.springframework.test.web.servlet.ResultActions authenticatedGet()
            throws Exception {
        return mockMvc.perform(get("/me")
                .header("Authorization", "Bearer " + accessToken));
    }

    /** Performs an authenticated PUT with a JSON body. */
    private org.springframework.test.web.servlet.ResultActions authenticatedPut(
            Object body) throws Exception {
        return mockMvc.perform(put("/me/change-password")
                .with(csrf())
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /me
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /me")
    class GetProfileTests {

        @Test
        @DisplayName("Should return 200 with the authenticated user's profile")
        void getProfile_WhenAuthenticated_ShouldReturn200WithUserData() throws Exception {
            authenticatedGet()
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.email").value(EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(FULL_NAME))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("Should return 401 when request has no Authorization header")
        void getProfile_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Authorization header carries an invalid token")
        void getProfile_WhenInvalidToken_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/me")
                            .header("Authorization", "Bearer this.is.not.valid"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 when Authorization header is malformed (no Bearer prefix)")
        void getProfile_WhenMalformedAuthHeader_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/me")
                            .header("Authorization", accessToken))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /me/change-password
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /me/change-password")
    class ChangePasswordTests {

        private static final String NEW_PASSWORD = "newPassword456";

        @Test
        @DisplayName("Should return 200 with no body when password change succeeds")
        void changePassword_WhenValidRequest_ShouldReturn200WithNoBody() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);

            authenticatedPut(request)
                    .andExpect(status().isOk())
                    .andExpect(content().string(""));
        }

        @Test
        @DisplayName("Should allow login with new password after successful change")
        void changePassword_WhenSuccessful_NewPasswordShouldWork() throws Exception {
            // Change the password
            authenticatedPut(
                    new ChangePasswordRequest(PASSWORD, NEW_PASSWORD))
                    .andExpect(status().isOk());

            // Old password must no longer work
            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(EMAIL, PASSWORD))))
                    .andExpect(status().isUnauthorized());

            // New password must work
            mockMvc.perform(post("/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequest(EMAIL, NEW_PASSWORD))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
        }

        @Test
        @DisplayName("Should return 400 when current password is wrong")
        void changePassword_WhenCurrentPasswordWrong_ShouldReturn400() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", NEW_PASSWORD);

            authenticatedPut(request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when new password is the same as current password")
        void changePassword_WhenNewPasswordSameAsCurrent_ShouldReturn400() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, PASSWORD);

            authenticatedPut(request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when new password is blank")
        void changePassword_WhenNewPasswordBlank_ShouldReturn400() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, "");

            authenticatedPut(request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when request has no Authorization header")
        void changePassword_WhenUnauthenticated_ShouldReturn401() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, NEW_PASSWORD);

            mockMvc.perform(put("/me/change-password")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}