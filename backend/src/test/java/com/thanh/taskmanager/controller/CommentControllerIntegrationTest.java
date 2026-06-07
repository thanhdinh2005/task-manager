package com.thanh.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.taskmanager.BaseIntegrationTest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CommentControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    private static final String OWNER_EMAIL    = "comment.owner@test.com";
    private static final String OWNER_PASSWORD = "password123";
    private static final String OWNER_NAME     = "Comment Owner";

    private static final String MEMBER_EMAIL    = "comment.member@test.com";
    private static final String MEMBER_PASSWORD = "password123";
    private static final String MEMBER_NAME     = "Comment Member";

    private String ownerToken;
    private String memberToken;
    private Long   memberId;
    private Long   taskId;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();

        ownerToken  = registerAndLogin(OWNER_EMAIL, OWNER_PASSWORD, OWNER_NAME);
        memberToken = registerAndLogin(MEMBER_EMAIL, MEMBER_PASSWORD, MEMBER_NAME);
        memberId    = extractUserId(memberToken);

        Long projectId = createProjectAndGetId("Comment Test Project");
        addMemberToProject(projectId, memberId);
        taskId = createTaskAndGetId(projectId, ownerToken);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private ResultActions doGet(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions doPost(String url, String token, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .with(csrf())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions doPut(String url, String token, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .with(csrf())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private ResultActions doDelete(String url, String token) throws Exception {
        return mockMvc.perform(delete(url)
                .with(csrf())
                .header("Authorization", "Bearer " + token));
    }

    // ── seed helpers ──────────────────────────────────────────────────────────

    private String registerAndLogin(String email, String password, String name) throws Exception {
        doPost("/auth/register", null,
                new RegisterRequest(email, password, name));
        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    private Long extractUserId(String token) throws Exception {
        String response = doGet("/me", token)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private Long createProjectAndGetId(String name) throws Exception {
        String response = doPost("/projects", ownerToken,
                new CreateProjectRequest(name, "desc"))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private void addMemberToProject(Long pId, Long uId) throws Exception {
        doPost("/projects/" + pId + "/members", ownerToken, new AddMemberRequest(uId));
    }

    private Long createTaskAndGetId(Long pId, String token) throws Exception {
        String response = doPost("/tasks/" + pId, token,
                new CreateTaskRequest("A Task", "desc", Priority.MEDIUM, null,
                        LocalDate.now().plusDays(7)))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    /** Posts a comment and returns its id. */
    private Long addCommentAndGetId(String content, String token) throws Exception {
        String response = doPost("/tasks/" + taskId + "/comments", token,
                new CreateCommentRequest(content))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/{taskId}/comments
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/{taskId}/comments")
    class GetCommentsTests {

        @Test
        @DisplayName("Should return empty list when task has no comments")
        void getComments_WhenNoComments_ShouldReturnEmptyList() throws Exception {
            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return all comments with author info when comments exist")
        void getComments_WhenCommentsExist_ShouldReturnList() throws Exception {
            addCommentAndGetId("First comment", ownerToken);
            addCommentAndGetId("Second comment", memberToken);

            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[0].content").isString());
        }

        @Test
        @DisplayName("Should return edited=false when comment has not been updated")
        void getComments_WhenNotEdited_ShouldReturnEditedFalse() throws Exception {
            addCommentAndGetId("Fresh comment", ownerToken);

            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].edited").value(false));
        }

        @Test
        @DisplayName("Should return edited=true after the comment has been updated")
        void getComments_WhenEdited_ShouldReturnEditedTrue() throws Exception {
            Long commentId = addCommentAndGetId("Original", ownerToken);

            // Update it so updatedAt != createdAt
            doPut("/comments/" + commentId, ownerToken,
                    new UpdateCommentRequest("Edited content"));

            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].edited").value(true))
                    .andExpect(jsonPath("$.data[0].content").value("Edited content"));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void getComments_WhenNotMember_ShouldReturn403() throws Exception {
            String outsideToken = registerAndLogin("out@test.com", "password123", "Outside");

            doGet("/tasks/" + taskId + "/comments", outsideToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when task does not exist")
        void getComments_WhenTaskNotFound_ShouldReturn404() throws Exception {
            doGet("/tasks/9999/comments", ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getComments_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/tasks/" + taskId + "/comments"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tasks/{taskId}/comments
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /tasks/{taskId}/comments")
    class AddCommentTests {

        @Test
        @DisplayName("Should return 201 CREATED with comment data when member posts a comment")
        void addComment_WhenValidRequest_ShouldReturn201() throws Exception {
            doPost("/tasks/" + taskId + "/comments", memberToken,
                    new CreateCommentRequest("Great progress!"))
                    .andExpect(status().isCreated())          // 201, not 200
                    .andExpect(jsonPath("$.data.content").value("Great progress!"))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("Should increment comment count visible on the task after posting")
        void addComment_ShouldIncrementTaskCommentCount() throws Exception {
            // Before: task has no comments
            doGet("/tasks/" + taskId, ownerToken)
                    .andExpect(jsonPath("$.data.commentCount").value(0));

            addCommentAndGetId("A comment", ownerToken);

            // After: comment count must be 1
            doGet("/tasks/" + taskId, ownerToken)
                    .andExpect(jsonPath("$.data.commentCount").value(1));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void addComment_WhenNotMember_ShouldReturn403() throws Exception {
            String outsideToken = registerAndLogin("out2@test.com", "password123", "Out2");

            doPost("/tasks/" + taskId + "/comments", outsideToken,
                    new CreateCommentRequest("Sneaky comment"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when task does not exist")
        void addComment_WhenTaskNotFound_ShouldReturn404() throws Exception {
            doPost("/tasks/9999/comments", ownerToken,
                    new CreateCommentRequest("Lost comment"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when content is blank")
        void addComment_WhenContentBlank_ShouldReturn400() throws Exception {
            doPost("/tasks/" + taskId + "/comments", ownerToken,
                    new CreateCommentRequest(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void addComment_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(post("/tasks/" + taskId + "/comments")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateCommentRequest("Anonymous"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /comments/{commentId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /comments/{commentId}")
    class UpdateCommentTests {

        @Test
        @DisplayName("Should return 200 with updated content when author edits their comment")
        void updateComment_WhenAuthor_ShouldReturn200() throws Exception {
            Long commentId = addCommentAndGetId("Original content", ownerToken);

            doPut("/comments/" + commentId, ownerToken,
                    new UpdateCommentRequest("Updated content"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").value("Updated content"));
        }

        @Test
        @DisplayName("Should return 403 when caller is not the comment author")
        void updateComment_WhenNotAuthor_ShouldReturn403() throws Exception {
            // Owner wrote the comment; member tries to edit it
            Long commentId = addCommentAndGetId("Owner's words", ownerToken);

            doPut("/comments/" + commentId, memberToken,
                    new UpdateCommentRequest("Tampered content"))
                    .andExpect(status().isForbidden());

            // Original content must be unchanged
            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(jsonPath("$.data[0].content").value("Owner's words"));
        }

        @Test
        @DisplayName("Should return 404 when comment does not exist")
        void updateComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
            doPut("/comments/9999", ownerToken,
                    new UpdateCommentRequest("Ghost update"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when new content is blank")
        void updateComment_WhenContentBlank_ShouldReturn400() throws Exception {
            Long commentId = addCommentAndGetId("Original", ownerToken);

            doPut("/comments/" + commentId, ownerToken,
                    new UpdateCommentRequest(""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void updateComment_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long commentId = addCommentAndGetId("Original", ownerToken);

            mockMvc.perform(put("/comments/" + commentId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateCommentRequest("Stealth edit"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /comments/{commentId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /comments/{commentId}")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should return 204 when author deletes their own comment")
        void deleteComment_WhenAuthor_ShouldReturn204() throws Exception {
            Long commentId = addCommentAndGetId("My comment", memberToken);

            doDelete("/comments/" + commentId, memberToken)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            // Comment list must now be empty
            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 204 when project owner deletes any comment")
        void deleteComment_WhenProjectOwner_ShouldReturn204() throws Exception {
            // Member wrote the comment; owner deletes it by project-owner privilege
            Long commentId = addCommentAndGetId("Member's comment", memberToken);

            doDelete("/comments/" + commentId, ownerToken)
                    .andExpect(status().isNoContent());

            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return 403 when a plain member tries to delete another user's comment")
        void deleteComment_WhenNeitherAuthorNorOwner_ShouldReturn403() throws Exception {
            // Owner wrote the comment; member (not author, not project owner) tries to delete
            Long commentId = addCommentAndGetId("Owner's comment", ownerToken);

            doDelete("/comments/" + commentId, memberToken)
                    .andExpect(status().isForbidden());

            // Comment must still exist
            doGet("/tasks/" + taskId + "/comments", ownerToken)
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 404 when comment does not exist")
        void deleteComment_WhenCommentNotFound_ShouldReturn404() throws Exception {
            doDelete("/comments/9999", ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void deleteComment_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long commentId = addCommentAndGetId("A comment", ownerToken);

            mockMvc.perform(delete("/comments/" + commentId).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}