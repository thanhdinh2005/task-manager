package com.thanh.taskmanager.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

@Table(name = "projects", uniqueConstraints = {
        @UniqueConstraint(name = "uk_project_name_created_by", columnNames = {"name", "created_by"})
})
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Project extends BaseEntity{
    @Column(nullable = false, length = 100)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    public static Project create(
            String name,
            String description,
            User owner
    ) {
        Assert.hasText(name, "Project's name is required");
        Assert.notNull(owner, "Owner is required");

        Project project = new Project();
        project.name = name;
        project.createdBy = owner;
        if (description != null && !description.isBlank()) {
            project.description = description;
        }
        return project;
    }

    public void update(
            String name,
            String description
    ) {
        Assert.hasText(name, "Project's name is required");
        this.name = name;
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
    }
}
