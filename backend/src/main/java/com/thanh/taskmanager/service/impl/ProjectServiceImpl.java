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
import com.thanh.taskmanager.mapper.MemberMapper;
import com.thanh.taskmanager.mapper.ProjectMapper;
import com.thanh.taskmanager.repository.*;
import com.thanh.taskmanager.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskRepository taskRepository;
    private final CommentRepository commentRepository;
    private final MemberMapper memberMapper;

    @Override
    @Transactional
    public ProjectResponse createProject(Long currentUserId, CreateProjectRequest request) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (projectRepository.existsByNameAndCreatedById(request.getName(), currentUserId)) {
            throw new AppException(ErrorCode.PROJECT_NAME_EXISTS);
        }

        Project project = projectRepository.save(
                Project.create(request.getName(), request.getDescription(), currentUser)
        );
        projectMemberRepository.save(ProjectMember.initialProject(project, currentUser));

        return projectMapper.toResponse(project, 1, 0);
    }

    @Override
    public List<ProjectResponse> getMyProjects(Long currentUserId) {
        List<Project> projects = projectRepository.findProjectsByUserId(currentUserId);
        if (projects.isEmpty()) return Collections.emptyList();

        List<Long> projectIds = projects.stream().map(Project::getId).toList();

        Map<Long, Long> memberCountMap = projectMemberRepository.countMembersByProjectIds(projectIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Map<Long, Long> taskCountMap = taskRepository.countTasksByProjectIds(projectIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return projects.stream()
                .map(project -> projectMapper.toResponse(
                        project,
                        memberCountMap.getOrDefault(project.getId(), 0L).intValue(),
                        taskCountMap.getOrDefault(project.getId(), 0L).intValue()
                ))
                .toList();
    }

    @Override
    public ProjectResponse getProjectById(Long projectId, Long currentUserId) {
        ProjectMember projectMember = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_MEMBER));

        Project project = projectMember.getProject();

        return buildProjectResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateProject(Long projectId, Long currentUserId, UpdateProjectRequest request) {
        Project project = projectRepository.findByProjectIdAndOwnerId(projectId, currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_PROJECT_OWNER));

        if (!project.getName().equals(request.getName())) {
            if (projectRepository.existsByNameAndCreatedById(request.getName(), currentUserId)) {
                throw new AppException(ErrorCode.PROJECT_NAME_EXISTS);
            }
        }
        project.update(request.getName(), request.getDescription());

        return buildProjectResponse(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long projectId, Long currentUserId) {
        if (!projectRepository.isProjectOwner(projectId, currentUserId)) throw new AppException(ErrorCode.NOT_PROJECT_OWNER);

        commentRepository.deleteAllByProjectId(projectId);
        taskRepository.deleteAllByProjectId(projectId);
        projectMemberRepository.deleteAllByProjectId(projectId);
        projectRepository.deleteById(projectId);
    }

    @Override
    public List<MemberResponse> getMembers(Long projectId, Long currentUserId) {
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, currentUserId))
            throw new AppException(ErrorCode.NOT_PROJECT_MEMBER);

        List<ProjectMember> members = projectMemberRepository.findMembersByProjectId(projectId);

        return members.stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse addMember(Long projectId, Long currentUserId, AddMemberRequest request) {
        if (!projectRepository.isProjectOwner(projectId, currentUserId)) {
            throw new AppException(ErrorCode.NOT_PROJECT_OWNER);
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new AppException(ErrorCode.PROJECT_NOT_FOUND));

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (projectMemberRepository.existsByProjectIdAndUserId(projectId, request.getUserId()))
            throw new AppException(ErrorCode.USER_ALREADY_MEMBER);

        ProjectMember projectMember = ProjectMember.addMember(project, targetUser);
        return memberMapper.toResponse(projectMemberRepository.save(projectMember));
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Long currentUserId, Long targetUserId) {

        if (!projectRepository.isProjectOwner(projectId, currentUserId))
            throw new AppException(ErrorCode.NOT_PROJECT_OWNER);

        if (currentUserId.equals(targetUserId))
            throw new AppException(ErrorCode.CANNOT_REMOVE_YOURSELF);

        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, targetUserId))
            throw new AppException(ErrorCode.NOT_PROJECT_MEMBER);

        projectMemberRepository.deleteByProjectIdAndUserId(projectId, targetUserId);
    }

    private ProjectResponse buildProjectResponse(Project project) {
        int memberCount = projectMemberRepository.countByProjectId(project.getId());
        int taskCount = taskRepository.countByProjectId(project.getId());
        return projectMapper.toResponse(project, memberCount, taskCount);
    }
}