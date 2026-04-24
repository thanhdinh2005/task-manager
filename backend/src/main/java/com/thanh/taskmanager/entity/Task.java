package com.thanh.taskmanager.entity;

import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.Objects;

@Table(name = "tasks")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Task extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private TodoStatus status;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDate dueDate;

    //
    public static Task create(
            Project project,
            String title,
            String description,
            Priority priority,
            User assignee,
            User createdBy,
            LocalDate dueDate
    ) {
        Assert.hasText(title, "Title is required");
        Assert.notNull(project, "Project is required");
        Assert.notNull(createdBy, "Created User is required");

        Task task = new Task();
        task.createdBy = createdBy;
        task.title = title;

        if (description != null && !description.isBlank()) {
            task.description = description;
        }

        task.assignee = assignee;

        task.priority = Objects.requireNonNullElse(priority, Priority.MEDIUM);

        if (dueDate != null && dueDate.isAfter(LocalDate.now())) {
            task.dueDate = dueDate;
        } else task.dueDate = null;

        task.status = TodoStatus.TODO;

        return task;
    }
}
