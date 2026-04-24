package com.thanh.taskmanager.entity;

import com.thanh.taskmanager.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(name = "project_members")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ProjectMember extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public static ProjectMember initialProject(Project project, User owner) {
        Assert.notNull(project, "Project is required");
        Assert.notNull(owner, "Owner is required");

        ProjectMember projectMember = new ProjectMember();

        projectMember.project = project;
        projectMember.user = owner;
        projectMember.role = Role.OWNER;

        return projectMember;
    }

    public static ProjectMember addMember(Project project, User member) {
        Assert.notNull(project, "Project is required");
        Assert.notNull(member, "Owner is required");

        ProjectMember projectMember = new ProjectMember();

        projectMember.project = project;
        projectMember.user = member;
        projectMember.role = Role.MEMBER;

        return projectMember;
    }
}
