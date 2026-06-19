package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.project.UpdateProjectRequest;
import com.thanh.taskmanager.dto.response.AppResponse;
import com.thanh.taskmanager.dto.response.MemberResponse;
import com.thanh.taskmanager.dto.response.ProjectResponse;
import com.thanh.taskmanager.service.ProjectService;
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

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "APIs for managing projects and project membership")
public class ProjectController {

    private final ProjectService projectService;

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @Operation(
        summary = "Create a new project",
        description = "Creates a new project for the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project created successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid project data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PostMapping
    public ResponseEntity<AppResponse<ProjectResponse>> createProject(
            @RequestBody @Valid CreateProjectRequest request) {
        return ResponseEntity.ok(
                AppResponse.success(projectService.createProject(getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Get all projects for current user",
        description = "Retrieves a list of all projects belonging to the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Projects retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<AppResponse<List<ProjectResponse>>> getMyProjects() {
        return ResponseEntity.ok(
                AppResponse.success(projectService.getMyProjects(getCurrentUserId()))
        );
    }

    @Operation(
        summary = "Get project by ID",
        description = "Retrieves a specific project by its ID (only if user has access)"
    )
    @Parameter(name = "projectId", description = "ID of the project to retrieve", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied to this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/{projectId}")
    public ResponseEntity<AppResponse<ProjectResponse>> getById(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                AppResponse.success(projectService.getProjectById(projectId, getCurrentUserId()))
        );
    }

    @Operation(
        summary = "Update project",
        description = "Updates an existing project's details (only if user has permission)"
    )
    @Parameter(name = "projectId", description = "ID of the project to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Project updated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid project data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to update this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PutMapping("/{projectId}")
    public ResponseEntity<AppResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid UpdateProjectRequest request) {
        return ResponseEntity.ok(
                AppResponse.success(projectService.updateProject(projectId, getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Delete project",
        description = "Deletes a project (only if user has permission)"
    )
    @Parameter(name = "projectId", description = "ID of the project to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Project deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }


    @Operation(
        summary = "Get project members",
        description = "Retrieves a list of all members in a specific project"
    )
    @Parameter(name = "projectId", description = "ID of the project", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Members retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied to this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/{projectId}/members")
    public ResponseEntity<AppResponse<List<MemberResponse>>> getMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                AppResponse.success(projectService.getMembers(projectId, getCurrentUserId()))
        );
    }

    @Operation(
        summary = "Add member to project",
        description = "Adds a user as a member to a project (only project owner or admin can do this)"
    )
    @Parameter(name = "projectId", description = "ID of the project", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Member added successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid member data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to add members to this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project or user not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PostMapping("/{projectId}/members")
    public ResponseEntity<AppResponse<MemberResponse>> addMember(
            @PathVariable Long projectId,
            @RequestBody @Valid AddMemberRequest request) {
        return ResponseEntity.ok(
                AppResponse.success(projectService.addMember(projectId, getCurrentUserId(), request))
        );
    }

    @Operation(
        summary = "Remove member from project",
        description = "Removes a user from a project's members (only project owner or admin can do this)"
    )
    @Parameter(name = "projectId", description = "ID of the project", required = true, in = ParameterIn.PATH)
    @Parameter(name = "targetUserId", description = "ID of the user to remove", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Member removed successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to remove members from this project",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Project or user not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @DeleteMapping("/{projectId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId) {
        projectService.removeMember(projectId, getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }
}