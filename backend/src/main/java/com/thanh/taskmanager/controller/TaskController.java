package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.dto.request.task.TaskFilterParams;
import com.thanh.taskmanager.dto.request.task.UpdateTaskRequest;
import com.thanh.taskmanager.dto.request.task.UpdateTaskStatusRequest;
import com.thanh.taskmanager.dto.response.AppResponse;
import com.thanh.taskmanager.dto.response.PageResponse;
import com.thanh.taskmanager.dto.response.TaskResponse;
import com.thanh.taskmanager.service.TaskService;
import com.thanh.taskmanager.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "APIs for managing tasks within projects")
public class TaskController {
    private final TaskService taskService;
    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Operation(
        summary = "Create a new task",
        description = "Creates a new task in the specified project"
    )
    @Parameter(name = "projectId", description = "ID of the project where task will be created", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task created successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid task data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied to this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PostMapping("/{projectId}")
    public ResponseEntity<AppResponse<TaskResponse>> createTask(
            @RequestBody @Valid CreateTaskRequest request,
            @PathVariable Long projectId
            ){
        return ResponseEntity.ok(
                AppResponse.success(taskService.createTask(projectId, getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Get tasks for a project with filtering",
        description = "Retrieves a paginated list of tasks for a specific project with optional filtering"
    )
    @Parameter(name = "projectId", description = "ID of the project", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tasks retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = PageResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied to this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/project/{projectId}")
    public ResponseEntity<PageResponse<TaskResponse>> getTasksByProject(
            @PathVariable Long projectId,
            @RequestBody TaskFilterParams params
            ) {
        return ResponseEntity.ok(
                taskService.getTasks(projectId, getCurrentUserId(), params)
        );
    }

    @Operation(
        summary = "Get task by ID",
        description = "Retrieves a specific task by its ID (only if user has access to the task's project)"
    )
    @Parameter(name = "taskId", description = "ID of the task to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied to this task",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/{taskId}")
    public ResponseEntity<AppResponse<TaskResponse>> getTaskById(@PathVariable Long taskId) {
        return ResponseEntity.ok(AppResponse.success(taskService.getTaskById(taskId, getCurrentUserId())));
    }

    @Operation(
        summary = "Update task",
        description = "Updates an existing task's details (only if user has permission)"
    )
    @Parameter(name = "taskId", description = "ID of the task to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task updated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid task data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to update this task",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PutMapping("/{taskId}")
    public ResponseEntity<AppResponse<TaskResponse>> updateTask(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskRequest request
            ){
        return ResponseEntity.ok(
                AppResponse.success(taskService.updateTask(taskId, getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Update task status",
        description = "Updates the status of a specific task (e.g., to-do, in-progress, done)"
    )
    @Parameter(name = "taskId", description = "ID of the task to update status", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Task status updated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid status data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to update this task's status",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PutMapping("/status/{taskId}")
    public ResponseEntity<AppResponse<TaskResponse>> updateStatus(
            @PathVariable Long taskId,
            @RequestBody @Valid UpdateTaskStatusRequest request
    ) {
        return ResponseEntity.ok(
                AppResponse.success(taskService.updateTaskStatus(taskId, getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Delete task",
        description = "Deletes a specific task (only if user has permission)"
    )
    @Parameter(name = "taskId", description = "ID of the task to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Task deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete this task",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }
}