package com.thanh.taskmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thanh.taskmanager.BaseIntegrationTest;
import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.dto.request.task.TaskFilterParams;
import com.thanh.taskmanager.dto.request.task.UpdateTaskRequest;
import com.thanh.taskmanager.dto.request.task.UpdateTaskStatusRequest;
import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
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

class TaskControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private ProjectMemberRepository projectMemberRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    // ── user fixtures ─────────────────────────────────────────────────────────
    private static final String OWNER_EMAIL    = "task.owner@test.com";
    private static final String OWNER_PASSWORD = "password123";
    private static final String OWNER_NAME     = "Task Owner";

    private static final String MEMBER_EMAIL    = "task.member@test.com";
    private static final String MEMBER_PASSWORD = "password123";
    private static final String MEMBER_NAME     = "Task Member";

    private String ownerToken;
    private String memberToken;
    private Long   memberId;
    private Long   projectId;

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
        projectId   = createProjectAndGetId("Task Test Project");
        addMemberToProject(projectId, memberId);
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private ResultActions doGet(String url, String token) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }

    // GET with a JSON body — TaskFilterParams is a @RequestBody on a GET endpoint
    private ResultActions doGetWithBody(String url, String token, Object body) throws Exception {
        return mockMvc.perform(get(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
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

    private Long createTaskAndGetId(String title, String token) throws Exception {
        CreateTaskRequest request = new CreateTaskRequest(
                title, "desc", Priority.MEDIUM, null, LocalDate.now().plusDays(7));
        String response = doPost("/tasks/" + projectId, token, request)
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("id").asLong();
    }

    private TaskFilterParams defaultFilter() {
        return TaskFilterParams.builder().page(0).size(10).build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tasks/{projectId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /tasks/{projectId}")
    class CreateTaskTests {

        @Test
        @DisplayName("Should return 200 with task data when member creates a task with no assignee")
        void createTask_WhenValidRequestNoAssignee_ShouldReturn200() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "New Task", "desc", Priority.HIGH, null, LocalDate.now().plusDays(3));

            doPost("/tasks/" + projectId, ownerToken, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("New Task"))
                    .andExpect(jsonPath("$.data.priority").value("HIGH"))
                    .andExpect(jsonPath("$.data.status").value("TODO"))
                    .andExpect(jsonPath("$.data.commentCount").value(0))
                    .andExpect(jsonPath("$.data.assignee").doesNotExist()); // NON_NULL
        }

        @Test
        @DisplayName("Should return 200 with assignee populated when assignee is a project member")
        void createTask_WhenAssigneeIsValidMember_ShouldReturnTaskWithAssignee() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Assigned Task", "desc", Priority.LOW, memberId, null);

            doPost("/tasks/" + projectId, ownerToken, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assignee").exists());
        }

        @Test
        @DisplayName("Should default priority to MEDIUM when priority is omitted")
        void createTask_WhenPriorityOmitted_ShouldDefaultToMedium() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Default Priority Task", "desc", null, null, null);

            doPost("/tasks/" + projectId, ownerToken, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.priority").value("MEDIUM"));
        }

        @Test
        @DisplayName("Should return 403 when assignee is not a project member")
        void createTask_WhenAssigneeNotInProject_ShouldReturn403() throws Exception {
            // Register a user who is NOT added to the project
            String outsideToken = registerAndLogin("outside@test.com", "password123", "Outside");
            Long outsideId = extractUserId(outsideToken);

            CreateTaskRequest request = new CreateTaskRequest(
                    "Bad Task", "desc", Priority.MEDIUM, outsideId, null);

            doPost("/tasks/" + projectId, ownerToken, request)
                    .andExpect(status().isBadRequest()); // ASSIGNEE_NOT_IN_PROJECT → 400
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void createTask_WhenCallerNotMember_ShouldReturn403() throws Exception {
            String outsideToken = registerAndLogin("outside2@test.com", "password123", "Outside2");
            CreateTaskRequest request = new CreateTaskRequest(
                    "Unauthorized Task", "desc", Priority.MEDIUM, null, null);

            doPost("/tasks/" + projectId, outsideToken, request)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void createTask_WhenTitleBlank_ShouldReturn400() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "", "desc", Priority.MEDIUM, null, null);

            doPost("/tasks/" + projectId, ownerToken, request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void createTask_WhenUnauthenticated_ShouldReturn401() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Task", "desc", Priority.MEDIUM, null, null);

            mockMvc.perform(post("/tasks/" + projectId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/project/{projectId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/project/{projectId}")
    class GetTasksTests {

        @Test
        @DisplayName("Should return empty page when project has no tasks")
        void getTasks_WhenNoTasks_ShouldReturnEmptyPage() throws Exception {
            doGetWithBody("/tasks/project/" + projectId, ownerToken, defaultFilter())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should return page with tasks when tasks exist")
        void getTasks_WhenTasksExist_ShouldReturnPage() throws Exception {
            createTaskAndGetId("Task A", ownerToken);
            createTaskAndGetId("Task B", memberToken);

            doGetWithBody("/tasks/project/" + projectId, ownerToken, defaultFilter())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.page").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Should filter by status when status param is provided")
        void getTasks_WhenFilterByStatus_ShouldReturnFilteredPage() throws Exception {
            createTaskAndGetId("Todo Task", ownerToken);
            Long inProgressId = createTaskAndGetId("In Progress Task", ownerToken);

            // Advance the second task to IN_PROGRESS
            doPut("/tasks/status/" + inProgressId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS));

            TaskFilterParams filter = TaskFilterParams.builder()
                    .page(0).size(10).status(TodoStatus.TODO).build();

            doGetWithBody("/tasks/project/" + projectId, ownerToken, filter)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].status").value("TODO"));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void getTasks_WhenNotMember_ShouldReturn403() throws Exception {
            String outsideToken = registerAndLogin("out3@test.com", "password123", "Out3");

            doGetWithBody("/tasks/project/" + projectId, outsideToken, defaultFilter())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getTasks_WhenUnauthenticated_ShouldReturn401() throws Exception {
            mockMvc.perform(get("/tasks/project/" + projectId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(defaultFilter())))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/{taskId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/{taskId}")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should return 200 with task details when caller is a project member")
        void getTaskById_WhenValidMember_ShouldReturn200() throws Exception {
            Long taskId = createTaskAndGetId("Detail Task", ownerToken);

            doGet("/tasks/" + taskId, memberToken)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(taskId))
                    .andExpect(jsonPath("$.data.title").value("Detail Task"))
                    .andExpect(jsonPath("$.data.commentCount").isNumber());
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void getTaskById_WhenNotMember_ShouldReturn403() throws Exception {
            Long taskId = createTaskAndGetId("Hidden Task", ownerToken);
            String outsideToken = registerAndLogin("out4@test.com", "password123", "Out4");

            doGet("/tasks/" + taskId, outsideToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when task does not exist")
        void getTaskById_WhenTaskNotFound_ShouldReturn404() throws Exception {
            doGet("/tasks/9999", ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void getTaskById_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long taskId = createTaskAndGetId("Auth Task", ownerToken);

            mockMvc.perform(get("/tasks/" + taskId))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tasks/{taskId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /tasks/{taskId}")
    class UpdateTaskTests {

        @Test
        @DisplayName("Should return 200 with updated fields when any member updates the task")
        void updateTask_WhenValidMember_ShouldReturn200() throws Exception {
            Long taskId = createTaskAndGetId("Original Title", ownerToken);

            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated Title", "Updated desc", Priority.HIGH, null, null);

            doPut("/tasks/" + taskId, memberToken, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Updated Title"))
                    .andExpect(jsonPath("$.data.priority").value("HIGH"))
                    .andExpect(jsonPath("$.data.assignee").doesNotExist()); // unassigned
        }

        @Test
        @DisplayName("Should return 400 when new assignee is not a project member")
        void updateTask_WhenAssigneeNotInProject_ShouldReturn400() throws Exception {
            Long taskId = createTaskAndGetId("Task", ownerToken);
            String outsideToken = registerAndLogin("out5@test.com", "password123", "Out5");
            Long outsideId = extractUserId(outsideToken);

            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Task", "desc", Priority.MEDIUM, outsideId, null);

            doPut("/tasks/" + taskId, ownerToken, request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void updateTask_WhenNotMember_ShouldReturn403() throws Exception {
            Long taskId = createTaskAndGetId("Task", ownerToken);
            String outsideToken = registerAndLogin("out6@test.com", "password123", "Out6");

            doPut("/tasks/" + taskId, outsideToken,
                    new UpdateTaskRequest("Hijack", "desc", Priority.LOW, null, null))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when task does not exist")
        void updateTask_WhenTaskNotFound_ShouldReturn404() throws Exception {
            doPut("/tasks/9999", ownerToken,
                    new UpdateTaskRequest("X", "desc", Priority.LOW, null, null))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void updateTask_WhenTitleBlank_ShouldReturn400() throws Exception {
            Long taskId = createTaskAndGetId("Task", ownerToken);

            doPut("/tasks/" + taskId, ownerToken,
                    new UpdateTaskRequest("", "desc", Priority.LOW, null, null))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tasks/status/{taskId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /tasks/status/{taskId}")
    class UpdateTaskStatusTests {

        @Test
        @DisplayName("Should return 200 with updated status for valid transition (TODO → IN_PROGRESS)")
        void updateStatus_WhenValidTransition_ShouldReturn200() throws Exception {
            Long taskId = createTaskAndGetId("Status Task", ownerToken);

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("Should return 400 when transition is invalid (TODO → DONE)")
        void updateStatus_WhenInvalidTransition_ShouldReturn400() throws Exception {
            Long taskId = createTaskAndGetId("Status Task", ownerToken);

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.DONE))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when transitioning to the same status")
        void updateStatus_WhenSameStatus_ShouldReturn400() throws Exception {
            Long taskId = createTaskAndGetId("Status Task", ownerToken);

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.TODO))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should allow full forward progression: TODO → IN_PROGRESS → IN_REVIEW → DONE")
        void updateStatus_WhenFullForwardProgression_ShouldSucceed() throws Exception {
            Long taskId = createTaskAndGetId("Progression Task", ownerToken);

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS))
                    .andExpect(status().isOk());

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.IN_REVIEW))
                    .andExpect(status().isOk());

            doPut("/tasks/status/" + taskId, ownerToken,
                    new UpdateTaskStatusRequest(TodoStatus.DONE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("DONE"));
        }

        @Test
        @DisplayName("Should return 403 when caller is not a project member")
        void updateStatus_WhenNotMember_ShouldReturn403() throws Exception {
            Long taskId = createTaskAndGetId("Status Task", ownerToken);
            String outsideToken = registerAndLogin("out7@test.com", "password123", "Out7");

            doPut("/tasks/status/" + taskId, outsideToken,
                    new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /tasks/{taskId}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /tasks/{taskId}")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should return 204 when project owner deletes any task")
        void deleteTask_WhenProjectOwner_ShouldReturn204() throws Exception {
            // Task created by member, deleted by owner — owner privilege
            Long taskId = createTaskAndGetId("Member's Task", memberToken);

            doDelete("/tasks/" + taskId, ownerToken)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            doGet("/tasks/" + taskId, ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 204 when task creator deletes their own task")
        void deleteTask_WhenTaskCreator_ShouldReturn204() throws Exception {
            Long taskId = createTaskAndGetId("My Task", memberToken);

            doDelete("/tasks/" + taskId, memberToken)
                    .andExpect(status().isNoContent());

            doGet("/tasks/" + taskId, ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when member is neither owner nor creator")
        void deleteTask_WhenNeitherOwnerNorCreator_ShouldReturn403() throws Exception {
            // Third member tries to delete a task they didn't create
            String thirdToken = registerAndLogin("third@test.com", "password123", "Third");
            Long thirdId = extractUserId(thirdToken);
            addMemberToProject(projectId, thirdId);

            Long taskId = createTaskAndGetId("Owner's Task", ownerToken);

            doDelete("/tasks/" + taskId, thirdToken)
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 404 when task does not exist")
        void deleteTask_WhenTaskNotFound_ShouldReturn404() throws Exception {
            doDelete("/tasks/9999", ownerToken)
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 401 when unauthenticated")
        void deleteTask_WhenUnauthenticated_ShouldReturn401() throws Exception {
            Long taskId = createTaskAndGetId("Task", ownerToken);

            mockMvc.perform(delete("/tasks/" + taskId).with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}