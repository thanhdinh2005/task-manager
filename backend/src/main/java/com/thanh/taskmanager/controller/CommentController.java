package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.response.AppResponse;
import com.thanh.taskmanager.dto.response.CommentResponse;
import com.thanh.taskmanager.security.CustomUserDetails;
import com.thanh.taskmanager.service.CommentService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Comments", description = "APIs for managing comments on tasks")
public class CommentController {

    private final CommentService commentService;

    @Operation(
        summary = "Get comments for a task",
        description = "Retrieves all comments for a specific task"
    )
    @Parameter(name = "taskId", description = "ID of the task", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "403", description = "Access denied to this task",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<AppResponse<List<CommentResponse>>> getComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CommentResponse> comments = commentService.getComments(taskId, userDetails.getUserId());
        return ResponseEntity.ok(AppResponse.success(comments));
    }

    @Operation(
        summary = "Add comment to task",
        description = "Creates a new comment on a specific task"
    )
    @Parameter(name = "taskId", description = "ID of the task", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Comment created successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid comment data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Access denied to this task",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Task not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<AppResponse<CommentResponse>> addComment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse comment = commentService.addComment(taskId, userDetails.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AppResponse.created(comment));
    }

    @Operation(
        summary = "Update comment",
        description = "Updates an existing comment (only if user owns the comment)"
    )
    @Parameter(name = "commentId", description = "ID of the comment to update", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment updated successfully",
                content = @Content(mediaType = "application/json",
                        schema = @Schema(implementation = AppResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid comment data",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to update this comment",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Comment not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<AppResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        CommentResponse comment = commentService.updateComment(commentId, userDetails.getUserId(), request);
        return ResponseEntity.ok(AppResponse.success(comment));
    }

    @Operation(
        summary = "Delete comment",
        description = "Deletes a specific comment (only if user owns the comment)"
    )
    @Parameter(name = "commentId", description = "ID of the comment to delete", required = true, in = ParameterIn.PATH)
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Comment deleted successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete this comment",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Comment not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}