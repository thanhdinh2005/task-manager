package com.thanh.taskmanager.mapper;

import com.thanh.taskmanager.dto.response.ProjectResponse;
import com.thanh.taskmanager.dto.response.UserSummary;
import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {
    public ProjectResponse toResponse(Project project, int memberCount, int taskCount) {
        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .createdBy(toUserSummary(project.getCreatedBy()))
                .memberCount(memberCount)
                .taskCount(taskCount)
                .createdAt(project.getCreatedAt())
                .build();
    }

    public UserSummary toUserSummary(User user) {
        if (user == null) return null;
        return UserSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .build();
    }
}
