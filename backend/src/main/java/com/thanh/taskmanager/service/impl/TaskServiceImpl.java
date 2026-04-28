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
import com.thanh.taskmanager.exception.AppException;
import com.thanh.taskmanager.exception.ErrorCode;
import com.thanh.taskmanager.mapper.UserMapper;
import com.thanh.taskmanager.repository.*;
import com.thanh.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public TaskResponse createTask(Long projectId, Long currentUserId, CreateTaskRequest request) {
        ProjectMember currentMember = getMemberOrThrow(projectId, currentUserId);
        User createdBy = currentMember.getUser();
        Project project = currentMember.getProject();

        User assignee = resolveAssignee(projectId, request.getAssigneeId());

        Task task = taskRepository.save(
                Task.create(
                        project,
                        request.getTitle(),
                        request.getDescription(),
                        request.getPriority(),
                        assignee,
                        createdBy,
                        request.getDueDate()
                )
        );

        return buildTaskResponse(task, 0);
    }

    @Override
    public PageResponse<TaskResponse> getTasks(Long projectId, Long currentUserId, TaskFilterParams filter) {
        getMemberOrThrow(projectId, currentUserId);

        Pageable pageable = PageRequest.of(
                filter.getPage(),
                filter.getSize(),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<Task> page = taskRepository.findByProjectIdAndFilters(
                projectId,
                filter.getStatus(),
                filter.getPriority(),
                filter.getAssigneeId(),
                pageable
        );

        List<TaskResponse> items = page.getContent().stream()
                .map(task -> buildTaskResponse(task, commentRepository.countByTaskId(task.getId())))
                .toList();

        return PageResponse.<TaskResponse>builder()
                .content(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }

    @Override
    public TaskResponse getTaskById(Long taskId, Long currentUserId) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        getMemberOrThrow(task.getProject().getId(), currentUserId);

        return buildTaskResponse(task, commentRepository.countByTaskId(taskId));
    }

    @Override
    @Transactional
    public TaskResponse updateTask(Long taskId, Long currentUserId, UpdateTaskRequest request) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        getMemberOrThrow(task.getProject().getId(), currentUserId);

        User assignee = resolveAssignee(task.getProject().getId(), request.getAssigneeId());

        task.update(
                request.getTitle(),
                request.getDescription(),
                request.getPriority(),
                assignee,
                request.getDueDate()
        );

        return buildTaskResponse(task, commentRepository.countByTaskId(taskId));
    }

    @Override
    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, Long currentUserId, UpdateTaskStatusRequest request) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        getMemberOrThrow(task.getProject().getId(), currentUserId);

        task.updateStatus(request.getStatus());

        return buildTaskResponse(task, commentRepository.countByTaskId(taskId));
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        Task task = taskRepository.findByIdWithProject(taskId)
                .orElseThrow(() -> new AppException(ErrorCode.TASK_NOT_FOUND));

        Long projectId = task.getProject().getId();

        getMemberOrThrow(projectId, currentUserId);

        boolean isOwner = projectRepository.isProjectOwner(projectId, currentUserId);
        boolean isCreator = task.getCreatedBy().getId().equals(currentUserId);

        if (!isOwner && !isCreator)
            throw new AppException(ErrorCode.FORBIDDEN);

        taskRepository.delete(task);
    }
    
    /**
     * Tìm ProjectMember — dùng lại ở mọi method để check membership.
     * getUser() và getProject() từ record này tránh query thêm.
     */
    private ProjectMember getMemberOrThrow(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_MEMBER));
    }

    /**
     * Resolve assignee từ request:
     * - null assigneeId → trả null (unassign)
     * - assigneeId không phải member → throw
     * - assigneeId hợp lệ → load và trả User
     */
    private User resolveAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) return null;

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, assigneeId))
            throw new AppException(ErrorCode.ASSIGNEE_NOT_IN_PROJECT);

        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Build TaskResponse — commentCount truyền vào để tránh query DB ở những
     * chỗ đã biết trước giá trị (createTask = 0).
     */
    private TaskResponse buildTaskResponse(Task task, int commentCount) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .assignee(task.getAssignee() != null
                        ? userMapper.toUserSummary(task.getAssignee())
                        : null)
                .createdBy(userMapper.toUserSummary(task.getCreatedBy()))
                .dueDate(task.getDueDate())
                .commentCount(commentCount)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}