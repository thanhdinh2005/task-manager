package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.dto.request.task.TaskFilterParams;
import com.thanh.taskmanager.dto.request.task.UpdateTaskRequest;
import com.thanh.taskmanager.dto.request.task.UpdateTaskStatusRequest;
import com.thanh.taskmanager.dto.response.ApiResponse;
import com.thanh.taskmanager.dto.response.PageResponse;
import com.thanh.taskmanager.dto.response.TaskResponse;
import com.thanh.taskmanager.service.TaskService;
import com.thanh.taskmanager.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService taskService;
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @PostMapping("/{projectId}")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @RequestBody @Valid CreateTaskRequest request,
            @PathVariable Long projectId
            ){
        return ResponseEntity.ok(
                ApiResponse.success(taskService.createTask(projectId, getCurrentUserId(), request))
        );
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestBody TaskFilterParams params
            ) {
        return ResponseEntity.ok(
                taskService.getTasks(projectId, getCurrentUserId(), params)
        );
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> getTaskById(@PathVariable Long taskId) {
        return ResponseEntity.ok(ApiResponse.success(taskService.getTaskById(taskId, getCurrentUserId())));
    }

    @PutMapping("/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskRequest request
            ){
        return ResponseEntity.ok(
                ApiResponse.success(taskService.updateTask(taskId, getCurrentUserId(), request))
        );
    }

    @PutMapping("/status/{taskId}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateStatus(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskStatusRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(taskService.updateTaskStatus(taskId, getCurrentUserId(), request))
        );
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}
