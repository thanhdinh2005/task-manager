package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.auth.ChangePasswordRequest;
import com.thanh.taskmanager.dto.response.AppResponse;
import com.thanh.taskmanager.dto.response.UserResponse;
import com.thanh.taskmanager.service.AuthService;
import com.thanh.taskmanager.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "APIs for managing user profile and settings")
public class ProfileController {
    private final AuthService authService;

    @Operation(
        summary = "Get user profile",
        description = "Retrieves the profile information of the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping
    public ResponseEntity<AppResponse<UserResponse>> getProfile() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(AppResponse.success(authService.getMe(currentUserId)));
    }

    @Operation(
        summary = "Change user password",
        description = "Updates the password for the currently authenticated user"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password changed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid password data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.ok().build();
    }
}