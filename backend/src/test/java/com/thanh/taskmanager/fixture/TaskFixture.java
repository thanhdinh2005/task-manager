package com.thanh.taskmanager.fixture;

import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.User;
import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;

import java.lang.reflect.Field;
import java.time.LocalDate;

public class TaskFixture {

    private Long id = 1L;
    private String title = TestConstants.DEFAULT_TASK_TITLE;
    private String description = TestConstants.DESCRIPTION;
    private TodoStatus status = TodoStatus.TODO;
    private Priority priority = Priority.MEDIUM;
    private User assignee = null;
    private User createdBy = null;
    private Project project = null;
    private LocalDate dueDate = null;

    private TaskFixture() {}

    public static TaskFixture aTask() {
        return new TaskFixture();
    }

    public TaskFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public TaskFixture withTitle(String title) {
        this.title = title;
        return this;
    }

    public TaskFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public TaskFixture withStatus(TodoStatus status) {
        this.status = status;
        return this;
    }

    public TaskFixture withPriority(Priority priority) {
        this.priority = priority;
        return this;
    }

    public TaskFixture withAssignee(User assignee) {
        this.assignee = assignee;
        return this;
    }

    public TaskFixture withCreatedBy(User createdBy) {
        this.createdBy = createdBy;
        return this;
    }

    public TaskFixture withProject(Project project) {
        this.project = project;
        return this;
    }

    public TaskFixture withDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    public Task build() {
        Task task = Task.create(project, title, description, priority, assignee, createdBy, dueDate);

        if (status != TodoStatus.TODO) {
            setField(task, "status", status);
        }

        setField(task, "id", id);

        return task;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Class<?> clazz = target.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
            throw new RuntimeException("Field '" + fieldName + "' not found in " + target.getClass().getName());
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot set field '" + fieldName + "'", e);
        }
    }
}