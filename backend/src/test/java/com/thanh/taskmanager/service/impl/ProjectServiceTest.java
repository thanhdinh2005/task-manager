package com.thanh.taskmanager.service.impl;

import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.project.UpdateProjectRequest;
import com.thanh.taskmanager.dto.response.MemberResponse;
import com.thanh.taskmanager.dto.response.ProjectResponse;
import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.ProjectMember;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.fixture.ProjectFixture;
import com.thanh.taskmanager.fixture.TestConstants;
import com.thanh.taskmanager.fixture.UserFixture;
import com.thanh.taskmanager.mapper.MemberMapper;
import com.thanh.taskmanager.mapper.ProjectMapper;
import com.thanh.taskmanager.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectServiceTest {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private MemberMapper memberMapper;

    private Project mockProject;
    private User mockOwner;
    private User mockOtherUser;

    @BeforeEach
    void setup() {
        mockOwner = UserFixture.aUser().withId(1L).build();
        mockOtherUser = UserFixture.aUser().withId(2L).build();
        mockProject = ProjectFixture.aProject()
                .withName(TestConstants.DEFAULT_PROJECT_NAME)
                .withOwner(mockOwner)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Create Project
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createProject")
    class CreateProjectTests {

        @Test
        @DisplayName("Should return ProjectResponse when request is valid")
        void createProject_WhenValidRequest_ShouldReturnResponse() {
            CreateProjectRequest request = new CreateProjectRequest(
                    TestConstants.DEFAULT_PROJECT_NAME, TestConstants.DESCRIPTION);

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(TestConstants.DEFAULT_PROJECT_NAME)
                    .memberCount(1)
                    .build();

            given(userRepository.findById(mockOwner.getId())).willReturn(Optional.of(mockOwner));
            given(projectRepository.existsByNameAndCreatedById(request.getName(), mockOwner.getId())).willReturn(false);
            given(projectRepository.save(any(Project.class))).willReturn(mockProject);
            given(projectMapper.toResponse(mockProject, 1, 0)).willReturn(expectedResponse);

            ProjectResponse actual = projectService.createProject(mockOwner.getId(), request);

            assertNotNull(actual);
            assertEquals(expectedResponse.getName(), actual.getName());
            assertEquals(1, actual.getMemberCount());

            verify(userRepository).findById(mockOwner.getId());
            verify(projectRepository).existsByNameAndCreatedById(request.getName(), mockOwner.getId());
            verify(projectRepository).save(any(Project.class));
            verify(projectMemberRepository).save(any(ProjectMember.class));
            verify(projectMapper).toResponse(mockProject, 1, 0);
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when user does not exist")
        void createProject_WhenUserNotFound_ShouldThrowException() {
            CreateProjectRequest request = new CreateProjectRequest(
                    TestConstants.DEFAULT_PROJECT_NAME, TestConstants.DESCRIPTION);

            given(userRepository.findById(anyLong())).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.createProject(99L, request));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());

            verify(projectRepository, never()).existsByNameAndCreatedById(anyString(), anyLong());
            verify(projectRepository, never()).save(any(Project.class));
            verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        }

        @Test
        @DisplayName("Should throw PROJECT_NAME_EXISTS when duplicate name for same owner")
        void createProject_WhenProjectNameExists_ShouldThrowException() {
            CreateProjectRequest request = new CreateProjectRequest(
                    TestConstants.DEFAULT_PROJECT_NAME, TestConstants.DESCRIPTION);

            given(userRepository.findById(mockOwner.getId())).willReturn(Optional.of(mockOwner));
            given(projectRepository.existsByNameAndCreatedById(request.getName(), mockOwner.getId())).willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.createProject(mockOwner.getId(), request));

            assertEquals(ErrorCode.PROJECT_NAME_EXISTS, exception.getErrorCode());

            verify(projectRepository, never()).save(any(Project.class));
            verify(projectMemberRepository, never()).save(any(ProjectMember.class));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get My Projects
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyProjects")
    class GetMyProjectsTests {

        @Test
        @DisplayName("Should return empty list when user has no projects")
        void getMyProjects_WhenProjectsEmpty_ShouldReturnEmptyList() {
            given(projectRepository.findProjectsByUserId(mockOwner.getId()))
                    .willReturn(Collections.emptyList());

            List<ProjectResponse> responses = projectService.getMyProjects(mockOwner.getId());

            assertThat(responses).isEmpty();

            verify(projectRepository).findProjectsByUserId(mockOwner.getId());
            verifyNoInteractions(projectMemberRepository, taskRepository, projectMapper);
        }

        @Test
        @DisplayName("Should return mapped responses with correct member and task counts")
        void getMyProjects_WhenValidRequest_ShouldReturnResponse() {
            Long projectId = mockProject.getId();
            long expectedMembers = 5L;
            long expectedTasks = 10L;

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(TestConstants.DEFAULT_PROJECT_NAME)
                    .memberCount((int) expectedMembers)
                    .taskCount((int) expectedTasks)
                    .build();

            List<Object[]> memberCounts = Collections.singletonList(new Object[]{projectId, expectedMembers});
            List<Object[]> taskCounts = Collections.singletonList(new Object[]{projectId, expectedTasks});

            given(projectRepository.findProjectsByUserId(mockOwner.getId()))
                    .willReturn(List.of(mockProject));
            given(projectMemberRepository.countMembersByProjectIds(List.of(projectId)))
                    .willReturn(memberCounts);
            given(taskRepository.countTasksByProjectIds(List.of(projectId)))
                    .willReturn(taskCounts);
            given(projectMapper.toResponse(mockProject, (int) expectedMembers, (int) expectedTasks))
                    .willReturn(expectedResponse);

            List<ProjectResponse> actual = projectService.getMyProjects(mockOwner.getId());

            assertThat(actual).hasSize(1);
            assertThat(actual.getFirst().getName()).isEqualTo(TestConstants.DEFAULT_PROJECT_NAME);
            assertThat(actual.getFirst().getMemberCount()).isEqualTo((int) expectedMembers);
            assertThat(actual.getFirst().getTaskCount()).isEqualTo((int) expectedTasks);

            verify(projectRepository).findProjectsByUserId(mockOwner.getId());
            verify(projectMemberRepository).countMembersByProjectIds(List.of(projectId));
            verify(taskRepository).countTasksByProjectIds(List.of(projectId));
            verifyNoMoreInteractions(projectRepository, projectMemberRepository, taskRepository);
        }

        @Test
        @DisplayName("Should default member/task counts to 0 when project ids are absent from count maps")
        void getMyProjects_WhenCountsNotReturnedForProject_ShouldDefaultToZero() {
            Long projectId = mockProject.getId();

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(TestConstants.DEFAULT_PROJECT_NAME)
                    .memberCount(0)
                    .taskCount(0)
                    .build();

            given(projectRepository.findProjectsByUserId(mockOwner.getId()))
                    .willReturn(List.of(mockProject));
            given(projectMemberRepository.countMembersByProjectIds(List.of(projectId)))
                    .willReturn(Collections.emptyList());
            given(taskRepository.countTasksByProjectIds(List.of(projectId)))
                    .willReturn(Collections.emptyList());
            given(projectMapper.toResponse(mockProject, 0, 0))
                    .willReturn(expectedResponse);

            List<ProjectResponse> actual = projectService.getMyProjects(mockOwner.getId());

            assertThat(actual).hasSize(1);
            assertThat(actual.getFirst().getMemberCount()).isZero();
            assertThat(actual.getFirst().getTaskCount()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Project By Id
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getProjectById")
    class GetProjectByIdTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when user is not a member")
        void getProjectById_WhenUserNotMember_ShouldThrowException() {
            Long projectId = mockProject.getId();
            Long outsiderId = 99L;

            given(projectMemberRepository.findByProjectIdAndUserId(projectId, outsiderId))
                    .willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.getProjectById(projectId, outsiderId));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, exception.getErrorCode());

            verify(projectMemberRepository).findByProjectIdAndUserId(projectId, outsiderId);
            verifyNoInteractions(projectRepository, taskRepository, projectMapper);
        }

        @Test
        @DisplayName("Should return ProjectResponse when user is a valid member")
        void getProjectById_WhenValidMember_ShouldReturnResponse() {
            Long projectId = mockProject.getId();
            int memberCount = 3;
            int taskCount = 7;

            ProjectMember membership = mock(ProjectMember.class);
            given(membership.getProject()).willReturn(mockProject);

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(TestConstants.DEFAULT_PROJECT_NAME)
                    .memberCount(memberCount)
                    .taskCount(taskCount)
                    .build();

            given(projectMemberRepository.findByProjectIdAndUserId(projectId, mockOwner.getId()))
                    .willReturn(Optional.of(membership));
            given(projectMemberRepository.countByProjectId(projectId)).willReturn(memberCount);
            given(taskRepository.countByProjectId(projectId)).willReturn(taskCount);
            given(projectMapper.toResponse(mockProject, memberCount, taskCount))
                    .willReturn(expectedResponse);

            ProjectResponse actual = projectService.getProjectById(projectId, mockOwner.getId());

            assertNotNull(actual);
            assertEquals(TestConstants.DEFAULT_PROJECT_NAME, actual.getName());
            assertEquals(memberCount, actual.getMemberCount());
            assertEquals(taskCount, actual.getTaskCount());

            verify(projectMemberRepository).findByProjectIdAndUserId(projectId, mockOwner.getId());
            verify(projectMemberRepository).countByProjectId(projectId);
            verify(taskRepository).countByProjectId(projectId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Update Project
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateProject")
    class UpdateProjectTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_OWNER when user is not the owner")
        void updateProject_WhenUserNotOwner_ShouldThrowException() {
            Long projectId = mockProject.getId();
            UpdateProjectRequest request = new UpdateProjectRequest("New Name", TestConstants.DESCRIPTION);

            given(projectRepository.findByProjectIdAndOwnerId(projectId, mockOtherUser.getId()))
                    .willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.updateProject(projectId, mockOtherUser.getId(), request));

            assertEquals(ErrorCode.NOT_PROJECT_OWNER, exception.getErrorCode());

            verify(projectRepository, never()).existsByNameAndCreatedById(anyString(), anyLong());
        }

        @Test
        @DisplayName("Should throw PROJECT_NAME_EXISTS when new name already taken by owner")
        void updateProject_WhenNewNameExists_ShouldThrowException() {
            Long projectId = mockProject.getId();
            String differentName = "Another Project";
            UpdateProjectRequest request = new UpdateProjectRequest(differentName, TestConstants.DESCRIPTION);

            given(projectRepository.findByProjectIdAndOwnerId(projectId, mockOwner.getId()))
                    .willReturn(Optional.of(mockProject));
            given(projectRepository.existsByNameAndCreatedById(differentName, mockOwner.getId()))
                    .willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.updateProject(projectId, mockOwner.getId(), request));

            assertEquals(ErrorCode.PROJECT_NAME_EXISTS, exception.getErrorCode());
        }

        @Test
        @DisplayName("Should update and return response when same name is kept")
        void updateProject_WhenSameNameKept_ShouldSkipDuplicateCheck() {
            Long projectId = mockProject.getId();
            // Name unchanged — duplicate check must be skipped
            UpdateProjectRequest request = new UpdateProjectRequest(
                    TestConstants.DEFAULT_PROJECT_NAME, "Updated description");

            int memberCount = 2;
            int taskCount = 4;

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(TestConstants.DEFAULT_PROJECT_NAME)
                    .memberCount(memberCount)
                    .taskCount(taskCount)
                    .build();

            given(projectRepository.findByProjectIdAndOwnerId(projectId, mockOwner.getId()))
                    .willReturn(Optional.of(mockProject));
            given(projectMemberRepository.countByProjectId(projectId)).willReturn(memberCount);
            given(taskRepository.countByProjectId(projectId)).willReturn(taskCount);
            given(projectMapper.toResponse(mockProject, memberCount, taskCount)).willReturn(expectedResponse);

            ProjectResponse actual = projectService.updateProject(projectId, mockOwner.getId(), request);

            assertNotNull(actual);
            assertEquals(TestConstants.DEFAULT_PROJECT_NAME, actual.getName());

            verify(projectRepository, never()).existsByNameAndCreatedById(anyString(), anyLong());
        }

        @Test
        @DisplayName("Should update and return response when new name is available")
        void updateProject_WhenNewNameAvailable_ShouldReturnUpdatedResponse() {
            Long projectId = mockProject.getId();
            String newName = "New Available Name";
            UpdateProjectRequest request = new UpdateProjectRequest(newName, TestConstants.DESCRIPTION);

            int memberCount = 2;
            int taskCount = 3;

            ProjectResponse expectedResponse = ProjectResponse.builder()
                    .name(newName)
                    .memberCount(memberCount)
                    .taskCount(taskCount)
                    .build();

            given(projectRepository.findByProjectIdAndOwnerId(projectId, mockOwner.getId()))
                    .willReturn(Optional.of(mockProject));
            given(projectRepository.existsByNameAndCreatedById(newName, mockOwner.getId()))
                    .willReturn(false);
            given(projectMemberRepository.countByProjectId(projectId)).willReturn(memberCount);
            given(taskRepository.countByProjectId(projectId)).willReturn(taskCount);
            given(projectMapper.toResponse(mockProject, memberCount, taskCount)).willReturn(expectedResponse);

            ProjectResponse actual = projectService.updateProject(projectId, mockOwner.getId(), request);

            assertNotNull(actual);
            assertEquals(newName, actual.getName());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Delete Project
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteProject")
    class DeleteProjectTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_OWNER when caller is not owner")
        void deleteProject_WhenUserNotOwner_ShouldThrowException() {
            Long projectId = mockProject.getId();

            given(projectRepository.isProjectOwner(projectId, mockOtherUser.getId())).willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.deleteProject(projectId, mockOtherUser.getId()));

            assertEquals(ErrorCode.NOT_PROJECT_OWNER, exception.getErrorCode());

            verify(commentRepository, never()).deleteAllByProjectId(anyLong());
            verify(taskRepository, never()).deleteAllByProjectId(anyLong());
            verify(projectMemberRepository, never()).deleteAllByProjectId(anyLong());
            verify(projectRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("Should delete all related data in correct order when owner calls delete")
        void deleteProject_WhenOwnerRequest_ShouldDeleteInOrder() {
            Long projectId = mockProject.getId();

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);

            projectService.deleteProject(projectId, mockOwner.getId());

            // Verify cascade deletion order: comments → tasks → members → project
            var inOrder = inOrder(commentRepository, taskRepository, projectMemberRepository, projectRepository);
            inOrder.verify(commentRepository).deleteAllByProjectId(projectId);
            inOrder.verify(taskRepository).deleteAllByProjectId(projectId);
            inOrder.verify(projectMemberRepository).deleteAllByProjectId(projectId);
            inOrder.verify(projectRepository).deleteById(projectId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Get Members
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMembers")
    class GetMembersTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not a member")
        void getMembers_WhenUserNotMember_ShouldThrowException() {
            Long projectId = mockProject.getId();
            Long outsiderId = 99L;

            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, outsiderId))
                    .willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.getMembers(projectId, outsiderId));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, exception.getErrorCode());

            verify(projectMemberRepository, never()).findMembersByProjectId(anyLong());
        }

        @Test
        @DisplayName("Should return empty list when project has no members (edge case)")
        void getMembers_WhenNoMembersFound_ShouldReturnEmptyList() {
            Long projectId = mockProject.getId();

            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, mockOwner.getId()))
                    .willReturn(true);
            given(projectMemberRepository.findMembersByProjectId(projectId))
                    .willReturn(Collections.emptyList());

            List<MemberResponse> actual = projectService.getMembers(projectId, mockOwner.getId());

            assertThat(actual).isEmpty();
            verify(memberMapper, never()).toResponse(any());
        }

        @Test
        @DisplayName("Should return mapped member list when user is a valid member")
        void getMembers_WhenValidMember_ShouldReturnMemberList() {
            Long projectId = mockProject.getId();

            ProjectMember memberEntry = mock(ProjectMember.class);
            MemberResponse memberResponse = MemberResponse.builder().build();

            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, mockOwner.getId()))
                    .willReturn(true);
            given(projectMemberRepository.findMembersByProjectId(projectId))
                    .willReturn(List.of(memberEntry));
            given(memberMapper.toResponse(memberEntry)).willReturn(memberResponse);

            List<MemberResponse> actual = projectService.getMembers(projectId, mockOwner.getId());

            assertThat(actual).hasSize(1);
            assertThat(actual.getFirst()).isEqualTo(memberResponse);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add Member
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_OWNER when caller is not the owner")
        void addMember_WhenCallerNotOwner_ShouldThrowException() {
            Long projectId = mockProject.getId();
            AddMemberRequest request = new AddMemberRequest(mockOtherUser.getId());

            given(projectRepository.isProjectOwner(projectId, mockOtherUser.getId())).willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.addMember(projectId, mockOtherUser.getId(), request));

            assertEquals(ErrorCode.NOT_PROJECT_OWNER, exception.getErrorCode());

            verify(userRepository, never()).findById(anyLong());
            verify(projectMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw PROJECT_NOT_FOUND when project does not exist")
        void addMember_WhenProjectNotFound_ShouldThrowException() {
            Long projectId = 999L;
            AddMemberRequest request = new AddMemberRequest(mockOtherUser.getId());

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectRepository.findById(projectId)).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.addMember(projectId, mockOwner.getId(), request));

            assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Should throw USER_NOT_FOUND when target user does not exist")
        void addMember_WhenTargetUserNotFound_ShouldThrowException() {
            Long projectId = mockProject.getId();
            AddMemberRequest request = new AddMemberRequest(999L);

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectRepository.findById(projectId)).willReturn(Optional.of(mockProject));
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.addMember(projectId, mockOwner.getId(), request));

            assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Should throw USER_ALREADY_MEMBER when user is already in the project")
        void addMember_WhenUserAlreadyMember_ShouldThrowException() {
            Long projectId = mockProject.getId();
            AddMemberRequest request = new AddMemberRequest(mockOtherUser.getId());

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectRepository.findById(projectId)).willReturn(Optional.of(mockProject));
            given(userRepository.findById(mockOtherUser.getId())).willReturn(Optional.of(mockOtherUser));
            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, mockOtherUser.getId()))
                    .willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.addMember(projectId, mockOwner.getId(), request));

            assertEquals(ErrorCode.USER_ALREADY_MEMBER, exception.getErrorCode());

            verify(projectMemberRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save and return MemberResponse when request is valid")
        void addMember_WhenValidRequest_ShouldReturnMemberResponse() {
            Long projectId = mockProject.getId();
            AddMemberRequest request = new AddMemberRequest(mockOtherUser.getId());

            ProjectMember savedMember = mock(ProjectMember.class);
            MemberResponse expectedResponse = MemberResponse.builder().build();

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectRepository.findById(projectId)).willReturn(Optional.of(mockProject));
            given(userRepository.findById(mockOtherUser.getId())).willReturn(Optional.of(mockOtherUser));
            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, mockOtherUser.getId()))
                    .willReturn(false);
            given(projectMemberRepository.save(any(ProjectMember.class))).willReturn(savedMember);
            given(memberMapper.toResponse(savedMember)).willReturn(expectedResponse);

            MemberResponse actual = projectService.addMember(projectId, mockOwner.getId(), request);

            assertNotNull(actual);
            assertThat(actual).isEqualTo(expectedResponse);

            verify(projectMemberRepository).save(any(ProjectMember.class));
            verify(memberMapper).toResponse(savedMember);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Remove Member
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("Should throw NOT_PROJECT_OWNER when caller is not the owner")
        void removeMember_WhenCallerNotOwner_ShouldThrowException() {
            Long projectId = mockProject.getId();

            given(projectRepository.isProjectOwner(projectId, mockOtherUser.getId())).willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.removeMember(projectId, mockOtherUser.getId(), mockOwner.getId()));

            assertEquals(ErrorCode.NOT_PROJECT_OWNER, exception.getErrorCode());

            verify(projectMemberRepository, never()).deleteByProjectIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw CANNOT_REMOVE_YOURSELF when owner tries to remove themselves")
        void removeMember_WhenOwnerRemovesSelf_ShouldThrowException() {
            Long projectId = mockProject.getId();

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.removeMember(projectId, mockOwner.getId(), mockOwner.getId()));

            assertEquals(ErrorCode.CANNOT_REMOVE_YOURSELF, exception.getErrorCode());

            verify(projectMemberRepository, never()).deleteByProjectIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when target user is not a member")
        void removeMember_WhenTargetNotMember_ShouldThrowException() {
            Long projectId = mockProject.getId();
            Long outsiderId = 99L;

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, outsiderId))
                    .willReturn(false);

            AppException exception = assertThrows(AppException.class,
                    () -> projectService.removeMember(projectId, mockOwner.getId(), outsiderId));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, exception.getErrorCode());

            verify(projectMemberRepository, never()).deleteByProjectIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Should delete membership when all conditions pass")
        void removeMember_WhenValidRequest_ShouldDeleteMembership() {
            Long projectId = mockProject.getId();
            Long targetUserId = mockOtherUser.getId();

            given(projectRepository.isProjectOwner(projectId, mockOwner.getId())).willReturn(true);
            given(projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUserId))
                    .willReturn(true);

            projectService.removeMember(projectId, mockOwner.getId(), targetUserId);

            verify(projectMemberRepository).deleteByProjectIdAndUserId(projectId, targetUserId);
        }
    }
}