package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.auth.LoginRequest;
import com.thanh.taskmanager.dto.request.auth.RegisterRequest;
import com.thanh.taskmanager.dto.response.AppResponse;
import com.thanh.taskmanager.dto.response.TokenResponse;
import com.thanh.taskmanager.dto.response.UserResponse;
import com.thanh.taskmanager.service.AuthService;
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
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "APIs for user authentication and token management")
public class AuthController {
    private final AuthService authService;

    @Operation(
        summary = "Register a new user",
        description = "Creates a new user account with the provided registration details"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User registered successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data",
                content = @Content),
        @ApiResponse(responseCode = "409", description = "User already exists",
                content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AppResponse<UserResponse>> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(AppResponse.success(authService.register(request)));
    }

    @Operation(
        summary = "Authenticate user and get access token",
        description = "Logs in a user and returns JWT access and refresh tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authentication successful",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "401", description = "Invalid credentials",
                content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<AppResponse<TokenResponse>> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(AppResponse.success(authService.login(request)));
    }

    @Operation(
        summary = "Refresh access token",
        description = "Gets a new access token using a valid refresh token"
    )
    @Parameter(name = "token", description = "Refresh token", required = true, in = ParameterIn.QUERY)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<AppResponse<TokenResponse>> refresh(@RequestParam String token) {
        return  ResponseEntity.ok(AppResponse.success(authService.refreshToken(token)));
    }
}