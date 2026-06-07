package com.thanh.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.taskmanager.BaseIntegrationTest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.project.UpdateProjectRequest;
import com.thanh.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    // ── user fixtures ─────────────────────────────────────────────────────────
    private static final String OWNER_EMAIL    = "owner@test.com";
    private static final String OWNER_PASSWORD = "password123";
    private static final String OWNER_NAME     = "Project Owner";

    private static final String MEMBER_EMAIL    = "member@test.com";
    private static final String MEMBER_PASSWORD = "password123";
    private static final String MEMBER_NAME     = "Project Member";

    // ── project fixtures ──────────────────────────────────────────────────────
    private static final String PROJECT_NAME = "My Project";
    private static final String DESCRIPTION  = "Project description";

    // ── tokens & ids resolved at runtime ─────────────────────────────────────
    private String ownerToken;
    private String memberToken;
    private Long   memberId;

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
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private ResultActions asOwner(ResultActions... ignored) { return null; }

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

    /**
     * Registers a user then logs in, returning the real JWT access token.
     * All downstream requests use this token so SecurityUtils.getCurrentUserId()
     * resolves to the correct user — @WithMockUser cannot do this.
     */
    private String registerAndLogin(String email, String password, String fullName) throws Exception {
        doPost("/auth/register", null,
                new RegisterRequest(email, password, fullName));

        String response = mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("accessToken").asText();
    }

    /**
     * Creates a project as the owner and returns its id.
     * Used by nested test classes that need an existing project to operate on.
     */
    private Long createProjectAndGetId(String name) throws Exception {
        String response = doPost("/projects", ownerToken,
                new CreateProjectRequest(name, DESCRIPTION))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    /**
     * Adds memberId to the project as a plain member. Returns the response body.
     */
    private void addMemberToProject(Long projectId) throws Exception {
        doPost("/projects/" + projectId + "/members", ownerToken,
                new AddMemberRequest(memberId));
    }

    /**
     * Extracts the user id embedded in the JWT claims via the /me endpoint.
     */
    private Long extractUserId(String token) throws Exception {
        String response = mockMvc.perform(get("/me")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /projects
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /projects")
    class CreateProjectTests {

        @Test
        @DisplayName("Should return 200 with project data and memberCount=1 when request is valid")
        void createProject_WhenValidRequest_ShouldReturn200() throws Exception {
            doPost("/projects", ownerToken,
                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(PROJECT_NAME))
                    .andExpect(jsonPath("$.data.memberCount").value(1))
                    .andExpect(jsonPath("$.data.taskCount").value(0))
                    .andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @DisplayName("Should return 409 when owner already has a project with the same name")
        void createProject_WhenDuplicateName_ShouldReturn409() throws Exception {
            doPost("/projects", ownerToken,
                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION));

            doPost("/projects", ownerToken,
                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should allow two different users to own projects with the same name")
        void createProject_WhenSameNameDifferentOwners_ShouldReturn200() throws Exception {
            doPost("/projects", ownerToken,
                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION))
                    .andExpect(status().isOk());

            // memberToken user creates a project with the same name — different owner, no conflict
            doPost("/projects", memberToken,
                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 when project name is blank")
        void createProject_WhenNameBlank_ShouldReturn400() throws Exception {
            doPost("/projects", ownerToken,
                    new CreateProjectRequest("", DESCRIPTION))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when request is unauthenticated")
        void createProject_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(post("/projects")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateProjectRequest(PROJECT_NAME, DESCRIPTION))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /projects/me
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /projects/me")
    class GetMyProjectsTests {

        @Test
        @DisplayName("Should return empty list when user has no projects")
        void getMyProjects_WhenNoProjects_ShouldReturnEmptyList() throws Exception {
            doGet("/projects/me", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("Should return all projects the user belongs to (owned and joined)")
        void getMyProjects_WhenProjectsExist_ShouldReturnList() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            // Owner sees 1 project
            doGet("/projects/me", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value(PROJECT_NAME));

            // Member also sees the project they were added to
            doGet("/projects/me", memberToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].name").value(PROJECT_NAME));
        }

        @Test
        @DisplayName("Should return correct memberCount and taskCount")
        void getMyProjects_ShouldReturnCorrectCounts() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doGet("/projects/me", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].memberCount").value(2))
                    .andExpect(jsonPath("$.data[0].taskCount").value(0));
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getMyProjects_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/projects/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /projects/{projectId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /projects/{projectId}")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should return 200 with project details when caller is a member")
        void getProjectById_WhenValidMember_ShouldReturn200() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doGet("/projects/" + projectId, ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(projectId))
                    .andExpect(jsonPath("$.data.name").value(PROJECT_NAME))
                    .andExpect(jsonPath("$.data.memberCount").value(1));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void getProjectById_WhenNotMember_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            // memberToken user was never added to this project
            doGet("/projects/" + projectId, memberToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getProjectById_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            mockMvc.perform(get("/projects/" + projectId))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /projects/{projectId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /projects/{projectId}")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should return 200 with updated project when owner sends valid request")
        void updateProject_WhenOwnerValidRequest_ShouldReturn200() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doPut("/projects/" + projectId, ownerToken,
                    new UpdateProjectRequest("Updated Name", "Updated desc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Updated Name"));
        }

        @Test
        @DisplayName("Should return 403 when a plain member tries to update the project")
        void updateProject_WhenNotOwner_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doPut("/projects/" + projectId, memberToken,
                    new UpdateProjectRequest("Hijacked Name", "desc"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 409 when the new name already exists for this owner")
        void updateProject_WhenNameConflict_ShouldReturn409() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            createProjectAndGetId("Other Project");

            doPut("/projects/" + projectId, ownerToken,
                    new UpdateProjectRequest("Other Project", DESCRIPTION))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 200 when updating with the same name (only description changes)")
        void updateProject_WhenSameName_ShouldReturn200() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doPut("/projects/" + projectId, ownerToken,
                    new UpdateProjectRequest(PROJECT_NAME, "New description only"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value(PROJECT_NAME));
        }

        @Test
        @DisplayName("Should return 400 when new name is blank")
        void updateProject_WhenNameBlank_ShouldReturn400() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doPut("/projects/" + projectId, ownerToken,
                    new UpdateProjectRequest("", DESCRIPTION))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /projects/{projectId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /projects/{projectId}")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should return 204 with no body when owner deletes the project")
        void deleteProject_WhenOwner_ShouldReturn204() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doDelete("/projects/" + projectId, ownerToken)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            // Project must no longer be fetchable
            doGet("/projects/" + projectId, ownerToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when a plain member tries to delete the project")
        void deleteProject_WhenNotOwner_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doDelete("/projects/" + projectId, memberToken)
                    .andExpect(status().isForbidden());

            doGet("/projects/" + projectId, ownerToken)
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void deleteProject_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            mockMvc.perform(delete("/projects/" + projectId).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /projects/{projectId}/members
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /projects/{projectId}/members")
    class GetMembersTests {

        @Test
        @DisplayName("Should return member list when caller is a project member")
        void getMembers_WhenValidMember_ShouldReturnList() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doGet("/projects/" + projectId + "/members", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(2)))
                    .andExpect(jsonPath("$.data[*].email",
                            containsInAnyOrder(OWNER_EMAIL, MEMBER_EMAIL)));
        }

        @Test
        @DisplayName("Should return single owner when no other members have been added")
        void getMembers_WhenOnlyOwner_ShouldReturnSingleEntry() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doGet("/projects/" + projectId + "/members", ownerToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].email").value(OWNER_EMAIL));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a member")
        void getMembers_WhenNotMember_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doGet("/projects/" + projectId + "/members", memberToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getMembers_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            mockMvc.perform(get("/projects/" + projectId + "/members"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /projects/{projectId}/members
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /projects/{projectId}/members")
    class AddMemberTests {

        @Test
        @DisplayName("Should return 200 with MemberResponse when owner adds a valid user")
        void addMember_WhenOwnerAddsValidUser_ShouldReturn200() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doPost("/projects/" + projectId + "/members", ownerToken,
                    new AddMemberRequest(memberId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.email").value(MEMBER_EMAIL));

            // Verify member count increased
            doGet("/projects/" + projectId + "/members", ownerToken)
                    .andExpect(jsonPath("$.data", hasSize(2)));
        }

        @Test
        @DisplayName("Should return 403 when a plain member tries to add someone")
        void addMember_WhenNotOwner_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            String thirdToken = registerAndLogin("third@test.com", "password123", "Third User");
            Long thirdId = extractUserId(thirdToken);

            doPost("/projects/" + projectId + "/members", memberToken,
                    new AddMemberRequest(thirdId))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 409 when target user is already a member")
        void addMember_WhenAlreadyMember_ShouldReturn409() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doPost("/projects/" + projectId + "/members", ownerToken,
                    new AddMemberRequest(memberId))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Should return 404 when target user does not exist")
        void addMember_WhenUserNotFound_ShouldReturn404() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            doPost("/projects/" + projectId + "/members", ownerToken,
                    new AddMemberRequest(9999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void addMember_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);

            mockMvc.perform(post("/projects/" + projectId + "/members")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddMemberRequest(memberId))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /projects/{projectId}/members/{targetUserId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /projects/{projectId}/members/{targetUserId}")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should return 204 when owner removes a valid member")
        void removeMember_WhenOwnerRemovesValidMember_ShouldReturn204() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            doDelete("/projects/" + projectId + "/members/" + memberId, ownerToken)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            // Member count must drop back to 1
            doGet("/projects/" + projectId + "/members", ownerToken)
                    .andExpect(jsonPath("$.data", hasSize(1)));
        }

        @Test
        @DisplayName("Should return 403 when a plain member tries to remove someone")
        void removeMember_WhenNotOwner_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            // member tries to remove owner — not allowed
            Long ownerId = extractUserId(ownerToken);
            doDelete("/projects/" + projectId + "/members/" + ownerId, memberToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when owner tries to remove themselves")
        void removeMember_WhenOwnerRemovesSelf_ShouldReturn400() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            Long ownerId   = extractUserId(ownerToken);

            doDelete("/projects/" + projectId + "/members/" + ownerId, ownerToken)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when target user is not a member of the project")
        void removeMember_WhenTargetNotMember_ShouldReturn403() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            // memberId was never added to this project

            doDelete("/projects/" + projectId + "/members/" + memberId, ownerToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void removeMember_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long projectId = createProjectAndGetId(PROJECT_NAME);
            addMemberToProject(projectId);

            mockMvc.perform(delete("/projects/" + projectId + "/members/" + memberId)
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}