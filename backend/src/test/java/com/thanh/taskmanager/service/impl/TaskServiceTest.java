package com.thanh.taskmanager.service.impl;

import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.dto.request.task.TaskFilterParams;
import com.thanh.taskmanager.dto.request.task.UpdateTaskRequest;
import com.thanh.taskmanager.dto.request.task.UpdateTaskStatusRequest;
import com.thanh.taskmanager.dto.response.PageResponse;
import com.thanh.taskmanager.dto.response.TaskResponse;
import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.ProjectMember;
import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.fixture.ProjectFixture;
import com.thanh.taskmanager.fixture.TaskFixture;
import com.thanh.taskmanager.fixture.TestConstants;
import com.thanh.taskmanager.fixture.UserFixture;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @InjectMocks private TaskServiceImpl taskService;

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserMapper userMapper;

    private User mockOwner;
    private User mockMember;
    private Project mockProject;
    private Task mockTask;
    private ProjectMember mockOwnerMembership;
    private ProjectMember mockMemberMembership;

    @BeforeEach
    void setUp() {
        mockOwner   = UserFixture.aUser().withId(1L).build();
        mockMember  = UserFixture.aUser().withId(2L).build();
        mockProject = ProjectFixture.aProject()
                .withName(TestConstants.DEFAULT_PROJECT_NAME)
                .withOwner(mockOwner)
                .build();
        mockTask = TaskFixture.aTask()
                .withId(10L)
                .withProject(mockProject)
                .withCreatedBy(mockOwner)
                .build();

        mockOwnerMembership  = mock(ProjectMember.class);
        mockMemberMembership = mock(ProjectMember.class);
    }

    private void givenOwnerIsMember() {
        given(projectMemberRepository.findByProjectIdAndUserId(
                mockProject.getId(), mockOwner.getId()))
                .willReturn(Optional.of(mockOwnerMembership));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createTask
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createTask")
    class CreateTaskTests {

        private CreateTaskRequest requestWithoutAssignee() {
            return new CreateTaskRequest(
                    TestConstants.DEFAULT_TASK_TITLE,
                    TestConstants.DESCRIPTION,
                    Priority.MEDIUM,
                    null,
                    LocalDate.now().plusDays(7)
            );
        }

        private CreateTaskRequest requestWithAssignee(Long assigneeId) {
            return new CreateTaskRequest(
                    TestConstants.DEFAULT_TASK_TITLE,
                    TestConstants.DESCRIPTION,
                    Priority.HIGH,
                    assigneeId,
                    LocalDate.now().plusDays(7)
            );
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void createTask_WhenCallerNotMember_ShouldThrow() {
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.createTask(mockProject.getId(), 99L, requestWithoutAssignee()));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ASSIGNEE_NOT_IN_PROJECT when assignee is not a project member")
        void createTask_WhenAssigneeNotInProject_ShouldThrow() {
            Long outsideAssigneeId = 99L;
            givenOwnerIsMember();
            given(projectMemberRepository.existsByProjectIdAndUserId(mockProject.getId(), outsideAssigneeId))
                    .willReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.createTask(mockProject.getId(), mockOwner.getId(),
                            requestWithAssignee(outsideAssigneeId)));

            assertEquals(ErrorCode.ASSIGNEE_NOT_IN_PROJECT, ex.getErrorCode());
            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create task with no assignee and return response with commentCount=0")
        void createTask_WithNoAssignee_ShouldReturnResponse() {
            givenOwnerIsMember();
            given(taskRepository.save(any(Task.class))).willReturn(mockTask);

            TaskResponse response = taskService.createTask(
                    mockProject.getId(), mockOwner.getId(), requestWithoutAssignee());

            assertNotNull(response);
            assertEquals(0, response.getCommentCount());

            verify(taskRepository).save(any(Task.class));
            verify(projectMemberRepository, never()).existsByProjectIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should resolve assignee and create task when assignee is a valid member")
        void createTask_WithValidAssignee_ShouldReturnResponse() {
            givenOwnerIsMember();
            given(projectMemberRepository.existsByProjectIdAndUserId(
                    mockProject.getId(), mockMember.getId())).willReturn(true);
            given(userRepository.findById(mockMember.getId())).willReturn(Optional.of(mockMember));

            Task taskWithAssignee = TaskFixture.aTask()
                    .withId(11L)
                    .withProject(mockProject)
                    .withCreatedBy(mockOwner)
                    .withAssignee(mockMember)
                    .build();
            given(taskRepository.save(any(Task.class))).willReturn(taskWithAssignee);

            TaskResponse response = taskService.createTask(
                    mockProject.getId(), mockOwner.getId(), requestWithAssignee(mockMember.getId()));

            assertNotNull(response);
            verify(userRepository).findById(mockMember.getId());
            verify(taskRepository).save(any(Task.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTasks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTasks")
    class GetTasksTests {

        private TaskFilterParams defaultFilter() {
            return TaskFilterParams.builder()
                    .page(0)
                    .size(10)
                    .status(null)
                    .priority(null)
                    .assigneeId(null)
                    .build();
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void getTasks_WhenCallerNotMember_ShouldThrow() {
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.getTasks(mockProject.getId(), 99L, defaultFilter()));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
            verify(taskRepository, never()).findByProjectIdAndFilters(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should return empty PageResponse when project has no tasks")
        void getTasks_WhenNoTasks_ShouldReturnEmptyPage() {
            givenOwnerIsMember();

            Page<Task> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
            given(taskRepository.findByProjectIdAndFilters(
                    eq(mockProject.getId()), any(), any(), any(), any(Pageable.class)))
                    .willReturn(emptyPage);

            PageResponse<TaskResponse> result = taskService.getTasks(
                    mockProject.getId(), mockOwner.getId(), defaultFilter());

            assertThat(result.getContent()).isEmpty();
            assertEquals(0, result.getTotalElements());
            assertEquals(0, result.getTotalPages());
        }

        @Test
        @DisplayName("Should return PageResponse with mapped tasks and their comment counts")
        void getTasks_WhenTasksExist_ShouldReturnMappedPage() {
            givenOwnerIsMember();

            Page<Task> taskPage = new PageImpl<>(List.of(mockTask), PageRequest.of(0, 10), 1);
            given(taskRepository.findByProjectIdAndFilters(
                    eq(mockProject.getId()), any(), any(), any(), any(Pageable.class)))
                    .willReturn(taskPage);
            given(commentRepository.countByTaskId(mockTask.getId())).willReturn(3);

            PageResponse<TaskResponse> result = taskService.getTasks(
                    mockProject.getId(), mockOwner.getId(), defaultFilter());

            assertThat(result.getContent()).hasSize(1);
            assertEquals(3, result.getContent().getFirst().getCommentCount());
            assertEquals(1, result.getTotalElements());

            verify(commentRepository).countByTaskId(mockTask.getId());
        }

        @Test
        @DisplayName("Should sort by updatedAt DESC and respect page/size from filter")
        void getTasks_ShouldPassCorrectPageableToRepository() {
            TaskFilterParams filter = TaskFilterParams.builder()
                    .page(2).size(5).build();
            givenOwnerIsMember();

            Page<Task> emptyPage = new PageImpl<>(List.of());
            given(taskRepository.findByProjectIdAndFilters(
                    eq(mockProject.getId()), any(), any(), any(), any(Pageable.class)))
                    .willReturn(emptyPage);

            taskService.getTasks(mockProject.getId(), mockOwner.getId(), filter);

            verify(taskRepository).findByProjectIdAndFilters(
                    eq(mockProject.getId()), any(), any(), any(),
                    eq(PageRequest.of(2, 5, Sort.by(Sort.Direction.DESC, "updatedAt")))
            );
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getTaskById
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTaskById")
    class GetTaskByIdTests {

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void getTaskById_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.getTaskById(99L, mockOwner.getId()));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
            verify(projectMemberRepository, never()).findByProjectIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member of the task's project")
        void getTaskById_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.getTaskById(mockTask.getId(), 99L));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should return TaskResponse with comment count when caller is a valid member")
        void getTaskById_WhenValidMember_ShouldReturnResponse() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(commentRepository.countByTaskId(mockTask.getId())).willReturn(5);

            TaskResponse response = taskService.getTaskById(mockTask.getId(), mockOwner.getId());

            assertNotNull(response);
            assertEquals(mockTask.getId(), response.getId());
            assertEquals(5, response.getCommentCount());

            verify(commentRepository).countByTaskId(mockTask.getId());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTask
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTask")
    class UpdateTaskTests {

        private UpdateTaskRequest validRequest() {
            return new UpdateTaskRequest(
                    "Updated Title",
                    "Updated description",
                    Priority.HIGH,
                    null,
                    LocalDate.now().plusDays(14)
            );
        }

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void updateTask_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTask(99L, mockOwner.getId(), validRequest()));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void updateTask_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTask(mockTask.getId(), 99L, validRequest()));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should throw ASSIGNEE_NOT_IN_PROJECT when new assignee is not a member")
        void updateTask_WhenNewAssigneeNotInProject_ShouldThrow() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));

            Long outsideAssigneeId = 99L;
            given(projectMemberRepository.existsByProjectIdAndUserId(mockProject.getId(), outsideAssigneeId))
                    .willReturn(false);

            UpdateTaskRequest requestWithBadAssignee = new UpdateTaskRequest(
                    "Title", "Desc", Priority.MEDIUM, outsideAssigneeId, null);

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTask(mockTask.getId(), mockOwner.getId(), requestWithBadAssignee));

            assertEquals(ErrorCode.ASSIGNEE_NOT_IN_PROJECT, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should update task fields and return response when request is valid")
        void updateTask_WhenValidRequest_ShouldReturnUpdatedResponse() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(commentRepository.countByTaskId(mockTask.getId())).willReturn(2);

            TaskResponse response = taskService.updateTask(
                    mockTask.getId(), mockOwner.getId(), validRequest());

            assertNotNull(response);
            assertEquals("Updated Title", mockTask.getTitle());
            assertEquals(Priority.HIGH, mockTask.getPriority());
            assertNull(mockTask.getAssignee());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateTaskStatus
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateTaskStatus")
    class UpdateTaskStatusTests {

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void updateTaskStatus_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTaskStatus(99L, mockOwner.getId(),
                            new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS)));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void updateTaskStatus_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTaskStatus(mockTask.getId(), 99L,
                            new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS)));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should throw INVALID_STATUS_TRANSITION when transition is not allowed (e.g. TODO → DONE)")
        void updateTaskStatus_WhenInvalidTransition_ShouldThrow() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTaskStatus(mockTask.getId(), mockOwner.getId(),
                            new UpdateTaskStatusRequest(TodoStatus.DONE)));

            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should throw INVALID_STATUS_TRANSITION when transitioning to the same status")
        void updateTaskStatus_WhenSameStatus_ShouldThrow() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.updateTaskStatus(mockTask.getId(), mockOwner.getId(),
                            new UpdateTaskStatusRequest(TodoStatus.TODO))); // same as current

            assertEquals(ErrorCode.INVALID_STATUS_TRANSITION, ex.getErrorCode());
        }

        @Test
        @DisplayName("Should update status and return response for valid transition (TODO → IN_PROGRESS)")
        void updateTaskStatus_WhenValidTransition_ShouldReturnUpdatedResponse() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(commentRepository.countByTaskId(mockTask.getId())).willReturn(0);

            TaskResponse response = taskService.updateTaskStatus(
                    mockTask.getId(), mockOwner.getId(),
                    new UpdateTaskStatusRequest(TodoStatus.IN_PROGRESS));

            assertNotNull(response);
            assertEquals(TodoStatus.IN_PROGRESS, mockTask.getStatus());
        }

        @Test
        @DisplayName("Should allow backward transition (IN_PROGRESS → TODO)")
        void updateTaskStatus_BackwardTransition_ShouldSucceed() {
            Task inProgressTask = TaskFixture.aTask()
                    .withId(20L)
                    .withProject(mockProject)
                    .withCreatedBy(mockOwner)
                    .withStatus(TodoStatus.IN_PROGRESS)
                    .build();

            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.of(mockOwnerMembership));
            given(taskRepository.findByIdWithProject(inProgressTask.getId()))
                    .willReturn(Optional.of(inProgressTask));
            given(commentRepository.countByTaskId(inProgressTask.getId())).willReturn(0);

            taskService.updateTaskStatus(inProgressTask.getId(), mockOwner.getId(),
                    new UpdateTaskStatusRequest(TodoStatus.TODO));

            assertEquals(TodoStatus.TODO, inProgressTask.getStatus());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteTask
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteTask")
    class DeleteTaskTests {

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void deleteTask_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.deleteTask(99L, mockOwner.getId()));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
            verify(taskRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void deleteTask_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId())).willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.deleteTask(mockTask.getId(), 99L));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
            verify(taskRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when caller is a member but neither owner nor creator")
        void deleteTask_WhenNeitherOwnerNorCreator_ShouldThrow() {
            Task taskCreatedByOwner = TaskFixture.aTask()
                    .withId(30L)
                    .withProject(mockProject)
                    .withCreatedBy(mockOwner)
                    .build();

            given(taskRepository.findByIdWithProject(taskCreatedByOwner.getId()))
                    .willReturn(Optional.of(taskCreatedByOwner));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockMember.getId()))
                    .willReturn(Optional.of(mockMemberMembership));
            given(projectRepository.isProjectOwner(mockProject.getId(), mockMember.getId()))
                    .willReturn(false);

            AppException ex = assertThrows(AppException.class,
                    () -> taskService.deleteTask(taskCreatedByOwner.getId(), mockMember.getId()));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            verify(taskRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete when caller is the project owner (even if not the task creator)")
        void deleteTask_WhenProjectOwner_ShouldDelete() {
            Task taskCreatedByMember = TaskFixture.aTask()
                    .withId(31L)
                    .withProject(mockProject)
                    .withCreatedBy(mockMember)
                    .build();

            given(taskRepository.findByIdWithProject(taskCreatedByMember.getId()))
                    .willReturn(Optional.of(taskCreatedByMember));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.of(mockOwnerMembership));
            given(projectRepository.isProjectOwner(mockProject.getId(), mockOwner.getId()))
                    .willReturn(true);

            taskService.deleteTask(taskCreatedByMember.getId(), mockOwner.getId());

            verify(taskRepository).delete(taskCreatedByMember);
        }

        @Test
        @DisplayName("Should delete when caller is the task creator (even if not the project owner)")
        void deleteTask_WhenTaskCreator_ShouldDelete() {
            Task taskCreatedByMember = TaskFixture.aTask()
                    .withId(32L)
                    .withProject(mockProject)
                    .withCreatedBy(mockMember)
                    .build();

            given(taskRepository.findByIdWithProject(taskCreatedByMember.getId()))
                    .willReturn(Optional.of(taskCreatedByMember));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockMember.getId()))
                    .willReturn(Optional.of(mockMemberMembership));
            given(projectRepository.isProjectOwner(mockProject.getId(), mockMember.getId()))
                    .willReturn(false);

            taskService.deleteTask(taskCreatedByMember.getId(), mockMember.getId());

            verify(taskRepository).delete(taskCreatedByMember);
        }
    }
}