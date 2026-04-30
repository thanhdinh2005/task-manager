package com.thanh.taskmanager.controller;

import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.response.ApiResponse;
import com.thanh.taskmanager.dto.response.CommentResponse;
import com.thanh.taskmanager.security.CustomUserDetails;
import com.thanh.taskmanager.service.CommentService;
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
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CommentResponse> comments = commentService.getComments(taskId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(comments));
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable Long taskId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse comment = commentService.addComment(taskId, userDetails.getUserId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.created(comment));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        CommentResponse comment = commentService.updateComment(commentId, userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(comment));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }
}