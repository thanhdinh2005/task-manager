package com.thanh.taskmanager.service.impl;

import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.response.CommentResponse;
import com.thanh.taskmanager.entity.Comment;
import com.thanh.taskmanager.entity.ProjectMember;
import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.entity.enums.Role;
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.CommentRepository;
import com.thanh.taskmanager.repository.ProjectMemberRepository;
import com.thanh.taskmanager.repository.TaskRepository;
import com.thanh.taskmanager.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserMapper userMapper;

    @Override
    public List<CommentResponse> getComments(Long taskId, Long currentUserId) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        projectMemberRepository.findByProjectIdAndUserId(task.getProject().getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_MEMBER));

        return commentRepository.findByTaskIdWithAuthor(taskId).stream()
                .map(this::buildCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse addComment(Long taskId, Long currentUserId, CreateCommentRequest request) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserId(task.getProject().getId(), currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_MEMBER));

        Comment comment = Comment.create(task, member.getUser(), request.getContent());

        return buildCommentResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long commentId, Long currentUserId, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdWithTask(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.isAuthor(currentUserId))
            throw new AppException(ErrorCode.FORBIDDEN);

        comment.update(request.getContent());

        return buildCommentResponse(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = commentRepository.findByIdWithTask(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        Long projectId = comment.getTask().getProject().getId();

        boolean isAuthor = comment.isAuthor(currentUserId);
        boolean isOwner = projectMemberRepository
                .findByProjectIdAndUserId(projectId, currentUserId)
                .map(member -> member.getRole() == Role.OWNER)
                .orElse(false);

        if (!isAuthor && !isOwner)
            throw new AppException(ErrorCode.FORBIDDEN);

        commentRepository.delete(comment);
    }

    private CommentResponse buildCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .author(userMapper.toUserSummary(comment.getAuthor()))
                .edited(!comment.getCreatedAt().equals(comment.getUpdatedAt()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
