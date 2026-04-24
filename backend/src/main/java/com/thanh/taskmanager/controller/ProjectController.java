package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.project.UpdateProjectRequest;
import com.thanh.taskmanager.dto.response.ApiResponse;
import com.thanh.taskmanager.dto.response.MemberResponse;
import com.thanh.taskmanager.dto.response.ProjectResponse;
import com.thanh.taskmanager.service.ProjectService;
import com.thanh.taskmanager.utils.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    private Long getCurrentUserId() {
        return SecurityUtils.getCurrentUserId();
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @RequestBody @Valid CreateProjectRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.createProject(getCurrentUserId(), request))
        );
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<ProjectResponse>>> getMyProjects() {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getMyProjects(getCurrentUserId()))
        );
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getProjectById(projectId, getCurrentUserId()))
        );
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ApiResponse<ProjectResponse>> updateProject(
            @PathVariable Long projectId,
            @RequestBody @Valid UpdateProjectRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.updateProject(projectId, getCurrentUserId(), request))
        );
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        projectService.deleteProject(projectId, getCurrentUserId());
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<MemberResponse>>> getMembers(@PathVariable Long projectId) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.getMembers(projectId, getCurrentUserId()))
        );
    }

    @PostMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<MemberResponse>> addMember(
            @PathVariable Long projectId,
            @RequestBody @Valid AddMemberRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(projectService.addMember(projectId, getCurrentUserId(), request))
        );
    }

    @DeleteMapping("/{projectId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long projectId,
            @PathVariable Long targetUserId) {
        projectService.removeMember(projectId, getCurrentUserId(), targetUserId);
        return ResponseEntity.noContent().build();
    }
}