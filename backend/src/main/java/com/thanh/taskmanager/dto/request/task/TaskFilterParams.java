package com.thanh.taskmanager.dto.request.task;

import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskFilterParams {
    private TodoStatus status;
    private Priority priority;
    private Long assigneeId;
    private int page = 0;
    private int size = 20;
}