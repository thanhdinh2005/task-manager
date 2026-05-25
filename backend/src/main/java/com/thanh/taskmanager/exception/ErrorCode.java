package com.thanh.taskmanager.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth ──────────────────────────────────────────────────────────────────
    EMAIL_ALREADY_EXISTS(409, "Email already exists"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    REFRESH_TOKEN_EXPIRED(401, "Your refresh token has expired. Please log in again."),
    REFRESH_TOKEN_REVOKED(403, "Refresh token has been revoked."),
    REFRESH_TOKEN_NOT_FOUND(404, "Refresh token not found. Please log in again."),
    INVALID_CURRENT_PASSWORD(400, "Current password is incorrect."),
    SAME_PASSWORD(400, "New password must be different from the current password."),
    WRONG_PASSWORD(400, "Wrong password, please try again"),

    // ── User ─────────────────────────────────────────────────────────────────
    USER_NOT_FOUND(404, "User not found."),

    // ── Project ───────────────────────────────────────────────────────────────
    PROJECT_NOT_FOUND(404, "Project not found."),
    PROJECT_NAME_EXISTS(409, "You already have a project with this name."),
    SAME_PROJECT_NAME(400, "New project name must be different from the current one."),

    NOT_PROJECT_OWNER(403, "You are not the owner of this project."),
    NOT_PROJECT_MEMBER(403, "You are not a member of this project."),
    USER_ALREADY_MEMBER(409, "User is already a member of this project."),
    CANNOT_REMOVE_YOURSELF(400, "You cannot remove yourself from the project."),
    TASK_NOT_FOUND(404, "Task not found."),
    INVALID_STATUS_TRANSITION(400, "Invalid task status transition."),
    ASSIGNEE_NOT_IN_PROJECT(400, "Assignee must be a member of the project."),

    // ── Comment ───────────────────────────────────────────────────────────────
    COMMENT_NOT_FOUND(404, "Comment not found."),          // FIX: was "Task not found" — copy-paste bug
    NOT_COMMENT_AUTHOR(403, "You are not the author of this comment."), // FIX: NOT_AUTHOR_COMMENT → NOT_COMMENT_AUTHOR (adjective order); 400 → 403 (it's an authorization issue)

    // ── General ───────────────────────────────────────────────────────────────
    FORBIDDEN(403, "You are not allowed to access this resource."),

    ;

    private final int httpStatus;
    private final String message;

}
