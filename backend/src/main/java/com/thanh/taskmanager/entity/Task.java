package com.thanh.taskmanager.entity;

import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_tasks_project_id", columnList = "project_id"),
        @Index(name = "idx_tasks_assignee_id", columnList = "assignee_id"),
        @Index(name = "idx_tasks_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TodoStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")          // nullable — task có thể chưa gán
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @Column
    private LocalDate dueDate;                 // nullable — không bắt buộc có deadline

    // ─── Static factory ──────────────────────────────────────────────────────

    public static Task create(
            Project project,
            String title,
            String description,
            Priority priority,
            User assignee,
            User createdBy,
            LocalDate dueDate
    ) {
        Task task = new Task();
        task.project = project;
        task.title = title;
        task.description = description;
        task.status = TodoStatus.TODO;                                    // default
        task.priority = priority != null ? priority : Priority.MEDIUM;   // default
        task.assignee = assignee;
        task.createdBy = createdBy;
        task.dueDate = dueDate;
        return task;
    }

    // ─── Business logic ──────────────────────────────────────────────────────

    public void update(
            String title,
            String description,
            Priority priority,
            User assignee,
            LocalDate dueDate
    ) {
        this.title = title;
        this.description = description;
        this.priority = priority != null ? priority : this.priority; // giữ nguyên nếu null
        this.assignee = assignee;                                    // null = unassign
        this.dueDate = dueDate;                                      // null = xóa deadline
    }

    public void updateStatus(TodoStatus newStatus) {
        // Status flow validation — không cho phép nhảy bậc không hợp lệ
        if (!isValidTransition(this.status, newStatus)) {
            throw new com.thanh.taskmanager.exception.AppException(
                    com.thanh.taskmanager.exception.ErrorCode.INVALID_STATUS_TRANSITION
            );
        }
        this.status = newStatus;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    /**
     * Status flow:
     * TODO ↔ IN_PROGRESS ↔ IN_REVIEW ↔ DONE
     * Cho phép chuyển tiến và lùi nhưng không nhảy cách
     * VD: TODO → DONE không hợp lệ
     */
    private boolean isValidTransition(TodoStatus current, TodoStatus next) {
        if (current == next) return false; // không đổi gì cả

        return switch (current) {
            case TODO -> next == TodoStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == TodoStatus.TODO || next == TodoStatus.IN_REVIEW;
            case IN_REVIEW -> next == TodoStatus.IN_PROGRESS || next == TodoStatus.DONE;
            case DONE -> next == TodoStatus.IN_REVIEW;
        };
    }
}