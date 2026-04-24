package com.thanh.taskmanager.dto.response;

import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TodoStatus status;
    private Priority priority;
    private UserSummary assignee;
    private UserSummary createdBy;
    private LocalDate dueDate;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}