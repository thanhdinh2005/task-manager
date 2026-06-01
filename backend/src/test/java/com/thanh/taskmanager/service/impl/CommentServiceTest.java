package com.thanh.taskmanager.service.impl;

import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.response.CommentResponse;
import com.thanh.taskmanager.entity.Comment;
import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.ProjectMember;
import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.entity.enums.Role;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.fixture.CommentFixture;
import com.thanh.taskmanager.fixture.ProjectFixture;
import com.thanh.taskmanager.fixture.TaskFixture;
import com.thanh.taskmanager.fixture.TestConstants;
import com.thanh.taskmanager.fixture.UserFixture;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.CommentRepository;
import com.thanh.taskmanager.repository.ProjectMemberRepository;
import com.thanh.taskmanager.repository.TaskRepository;
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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks private CommentServiceImpl commentService;

    @Mock private TaskRepository taskRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ProjectMemberRepository projectMemberRepository;
    @Mock private UserMapper userMapper;

    // ── shared fixtures ───────────────────────────────────────────────────────
    private User mockOwner;
    private User mockMember;
    private Project mockProject;
    private Task mockTask;
    private Comment mockComment;

    private ProjectMember mockOwnerMembership;
    private ProjectMember mockMemberMembership;

    @BeforeEach
    void setUp() {
        mockOwner  = UserFixture.aUser().withId(1L).build();
        mockMember = UserFixture.aUser().withId(2L).build();

        mockProject = ProjectFixture.aProject()
                .withName(TestConstants.DEFAULT_PROJECT_NAME)
                .withOwner(mockOwner)
                .build();

        mockTask = TaskFixture.aTask()
                .withId(10L)
                .withProject(mockProject)
                .withCreatedBy(mockOwner)
                .build();

        mockComment = CommentFixture.aComment()
                .withId(100L)
                .withTask(mockTask)
                .withAuthor(mockOwner)
                .withContent(TestConstants.DEFAULT_COMMENT_CONTENT)
                .build();

        mockOwnerMembership  = mock(ProjectMember.class);
        mockMemberMembership = mock(ProjectMember.class);
    }

    private void givenOwnerIsMember() {
        given(projectMemberRepository.findByProjectIdAndUserId(
                mockProject.getId(), mockOwner.getId()))
                .willReturn(Optional.of(mockOwnerMembership));
    }

    private void givenMemberIsMember() {
        given(projectMemberRepository.findByProjectIdAndUserId(
                mockProject.getId(), mockMember.getId()))
                .willReturn(Optional.of(mockMemberMembership));
    }

    /**
     * Used only in addComment — the service calls member.getUser() on the
     * result to pass to Comment.create(). Stubs must be declared here, not
     * in @BeforeEach, to avoid "unnecessary stubbing" failures in other tests.
     */
    private void givenOwnerIsMemberWithUser() {
        given(mockOwnerMembership.getUser()).willReturn(mockOwner);
        givenOwnerIsMember();
    }

    private void givenMemberIsMemberWithUser() {
        given(mockMemberMembership.getUser()).willReturn(mockMember);
        givenMemberIsMember();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getComments
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getComments")
    class GetCommentsTests {

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void getComments_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.getComments(99L, mockOwner.getId()));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
            verifyNoInteractions(projectMemberRepository, commentRepository);
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not in the project")
        void getComments_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.getComments(mockTask.getId(), 99L));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should return empty list when task has no comments")
        void getComments_WhenNoComments_ShouldReturnEmptyList() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.findByTaskIdWithAuthor(mockTask.getId()))
                    .willReturn(Collections.emptyList());

            List<CommentResponse> result = commentService.getComments(mockTask.getId(), mockOwner.getId());

            assertThat(result).isEmpty();
            verifyNoInteractions(userMapper);
        }

        @Test
        @DisplayName("Should return mapped CommentResponse list when comments exist")
        void getComments_WhenCommentsExist_ShouldReturnMappedList() {
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.findByTaskIdWithAuthor(mockTask.getId()))
                    .willReturn(List.of(mockComment));

            List<CommentResponse> result = commentService.getComments(mockTask.getId(), mockOwner.getId());

            assertThat(result).hasSize(1);
            assertEquals(mockComment.getId(), result.getFirst().getId());
            assertEquals(TestConstants.DEFAULT_COMMENT_CONTENT, result.getFirst().getContent());
        }

        @Test
        @DisplayName("Should set edited=false when createdAt equals updatedAt")
        void getComments_WhenCommentNotEdited_ShouldReturnEditedFalse() {
            // Default fixture has createdAt == updatedAt → edited = false
            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.findByTaskIdWithAuthor(mockTask.getId()))
                    .willReturn(List.of(mockComment));

            List<CommentResponse> result = commentService.getComments(mockTask.getId(), mockOwner.getId());

            assertFalse(result.getFirst().isEdited());
        }

        @Test
        @DisplayName("Should set edited=true when updatedAt differs from createdAt")
        void getComments_WhenCommentEdited_ShouldReturnEditedTrue() {
            Comment editedComment = CommentFixture.aComment()
                    .withId(101L)
                    .withTask(mockTask)
                    .withAuthor(mockOwner)
                    .asEdited()  // createdAt != updatedAt
                    .build();

            givenOwnerIsMember();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.findByTaskIdWithAuthor(mockTask.getId()))
                    .willReturn(List.of(editedComment));

            List<CommentResponse> result = commentService.getComments(mockTask.getId(), mockOwner.getId());

            assertTrue(result.getFirst().isEdited());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addComment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addComment")
    class AddCommentTests {

        private CreateCommentRequest validRequest() {
            return new CreateCommentRequest(TestConstants.DEFAULT_COMMENT_CONTENT);
        }

        @Test
        @DisplayName("Should throw TASK_NOT_FOUND when task does not exist")
        void addComment_WhenTaskNotFound_ShouldThrow() {
            given(taskRepository.findByIdWithProject(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.addComment(99L, mockOwner.getId(), validRequest()));

            assertEquals(ErrorCode.TASK_NOT_FOUND, ex.getErrorCode());
            verifyNoInteractions(projectMemberRepository, commentRepository);
        }

        @Test
        @DisplayName("Should throw NOT_PROJECT_MEMBER when caller is not in the project")
        void addComment_WhenCallerNotMember_ShouldThrow() {
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(projectMemberRepository.findByProjectIdAndUserId(mockProject.getId(), 99L))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.addComment(mockTask.getId(), 99L, validRequest()));

            assertEquals(ErrorCode.NOT_PROJECT_MEMBER, ex.getErrorCode());
            verifyNoInteractions(commentRepository);
        }

        @Test
        @DisplayName("Should save comment and return response when request is valid")
        void addComment_WhenValidRequest_ShouldReturnResponse() {
            givenOwnerIsMemberWithUser();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.save(any(Comment.class))).willReturn(mockComment);

            CommentResponse response = commentService.addComment(
                    mockTask.getId(), mockOwner.getId(), validRequest());

            assertNotNull(response);
            assertEquals(mockComment.getId(), response.getId());
            assertEquals(TestConstants.DEFAULT_COMMENT_CONTENT, response.getContent());

            verify(commentRepository).save(any(Comment.class));
        }

        @Test
        @DisplayName("Should use the membership's user as the comment author, not look up by id")
        void addComment_ShouldDeriveAuthorFromMembership_NotUserRepository() {
            givenMemberIsMemberWithUser();
            given(taskRepository.findByIdWithProject(mockTask.getId()))
                    .willReturn(Optional.of(mockTask));
            given(commentRepository.save(any(Comment.class))).willReturn(
                    CommentFixture.aComment()
                            .withId(200L)
                            .withTask(mockTask)
                            .withAuthor(mockMember)
                            .build()
            );

            CommentResponse response = commentService.addComment(
                    mockTask.getId(), mockMember.getId(), validRequest());

            assertNotNull(response);

        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateComment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateComment")
    class UpdateCommentTests {

        private UpdateCommentRequest validRequest() {
            return new UpdateCommentRequest(TestConstants.UPDATED_COMMENT_CONTENT);
        }

        @Test
        @DisplayName("Should throw COMMENT_NOT_FOUND when comment does not exist")
        void updateComment_WhenCommentNotFound_ShouldThrow() {
            given(commentRepository.findByIdWithTask(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.updateComment(99L, mockOwner.getId(), validRequest()));

            assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.getErrorCode());
            verifyNoInteractions(projectMemberRepository);
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when caller is not the comment author")
        void updateComment_WhenCallerNotAuthor_ShouldThrow() {
            // mockComment was authored by mockOwner; mockMember tries to update it
            given(commentRepository.findByIdWithTask(mockComment.getId()))
                    .willReturn(Optional.of(mockComment));

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.updateComment(
                            mockComment.getId(), mockMember.getId(), validRequest()));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            // No membership check — updateComment only checks authorship
            verifyNoInteractions(projectMemberRepository);
        }

        @Test
        @DisplayName("Should update content and return response when caller is the author")
        void updateComment_WhenCallerIsAuthor_ShouldReturnUpdatedResponse() {
            given(commentRepository.findByIdWithTask(mockComment.getId()))
                    .willReturn(Optional.of(mockComment));

            CommentResponse response = commentService.updateComment(
                    mockComment.getId(), mockOwner.getId(), validRequest());

            assertNotNull(response);
            // Real entity mutation — content must have changed
            assertEquals(TestConstants.UPDATED_COMMENT_CONTENT, mockComment.getContent());
            // No save() call — @Transactional dirty checking persists the mutation
            verify(commentRepository, never()).save(any());
            verifyNoInteractions(projectMemberRepository);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteComment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteComment")
    class DeleteCommentTests {

        @Test
        @DisplayName("Should throw COMMENT_NOT_FOUND when comment does not exist")
        void deleteComment_WhenCommentNotFound_ShouldThrow() {
            given(commentRepository.findByIdWithTask(99L)).willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.deleteComment(99L, mockOwner.getId()));

            assertEquals(ErrorCode.COMMENT_NOT_FOUND, ex.getErrorCode());
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw FORBIDDEN when caller is neither author nor project owner")
        void deleteComment_WhenNeitherAuthorNorOwner_ShouldThrow() {
            // mockComment authored by mockOwner; mockMember is just a plain member (Role.MEMBER)
            given(commentRepository.findByIdWithTask(mockComment.getId()))
                    .willReturn(Optional.of(mockComment));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockMember.getId()))
                    .willReturn(Optional.of(mockMemberMembership));
            given(mockMemberMembership.getRole()).willReturn(Role.MEMBER);

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.deleteComment(mockComment.getId(), mockMember.getId()));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            verify(commentRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should delete when caller is the comment author (even if not project owner)")
        void deleteComment_WhenCallerIsAuthor_ShouldDelete() {
            given(commentRepository.findByIdWithTask(mockComment.getId()))
                    .willReturn(Optional.of(mockComment));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.empty()); // not in membership table → isOwner = false

            commentService.deleteComment(mockComment.getId(), mockOwner.getId());

            verify(commentRepository).delete(mockComment);
        }

        @Test
        @DisplayName("Should delete when caller is the project owner (even if not the author)")
        void deleteComment_WhenCallerIsProjectOwner_ShouldDelete() {
            Comment memberComment = CommentFixture.aComment()
                    .withId(200L)
                    .withTask(mockTask)
                    .withAuthor(mockMember)
                    .build();

            given(commentRepository.findByIdWithTask(memberComment.getId()))
                    .willReturn(Optional.of(memberComment));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.of(mockOwnerMembership));
            given(mockOwnerMembership.getRole()).willReturn(Role.OWNER);

            commentService.deleteComment(memberComment.getId(), mockOwner.getId());

            verify(commentRepository).delete(memberComment);
        }

        @Test
        @DisplayName("Should delete when caller is both author and project owner")
        void deleteComment_WhenBothAuthorAndOwner_ShouldDelete() {
            given(commentRepository.findByIdWithTask(mockComment.getId()))
                    .willReturn(Optional.of(mockComment));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.of(mockOwnerMembership));
            given(mockOwnerMembership.getRole()).willReturn(Role.OWNER);

            commentService.deleteComment(mockComment.getId(), mockOwner.getId());

            verify(commentRepository).delete(mockComment);
        }

        @Test
        @DisplayName("Should treat absent membership as isOwner=false without throwing")
        void deleteComment_WhenCallerHasNoMembership_AndNotAuthor_ShouldThrow() {
            Comment memberComment = CommentFixture.aComment()
                    .withId(201L)
                    .withTask(mockTask)
                    .withAuthor(mockMember)
                    .build();

            given(commentRepository.findByIdWithTask(memberComment.getId()))
                    .willReturn(Optional.of(memberComment));
            given(projectMemberRepository.findByProjectIdAndUserId(
                    mockProject.getId(), mockOwner.getId()))
                    .willReturn(Optional.empty());

            AppException ex = assertThrows(AppException.class,
                    () -> commentService.deleteComment(memberComment.getId(), mockOwner.getId()));

            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
            verify(commentRepository, never()).delete(any());
        }
    }
}