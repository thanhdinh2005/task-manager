package com.thanh.taskmanager.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth
    EMAIL_ALREADY_EXISTS(409, "Email already exists"),
    INVALID_CREDENTIALS(401, "Invalid email or password"),
    TOKEN_EXPIRED(403, "Your refresh token has expired. Please log in again."),
    TOKEN_REVOKED(403, "Token is already revoked."),
    REFRESH_TOKEN_NOT_FOUND(404, "Refresh token not found, please log in again."),
    INVALID_CURRENT_PASSWORD(400, "Invalid current password."),
    SAME_AS_OLD_PASSWORD(400, "The password has been used, try another password"),

    // Project
    PROJECT_NOT_FOUND(404, "Project not found"),
    USER_ALREADY_MEMBER(409, "User is already a member of this project"),
    NOT_PROJECT_MEMBER(403, "You are not a member of this project"),
    PROJECT_NAME_EXISTS(400, "You already have a project with this name"),
    NOT_PROJECT_OWNER(403, "You are not owner of this project"),
    SAME_AS_OLD_PROJECT_NAME(400, "The project name has been used, try another name"),
    CANNOT_REMOVE_YOURSELF(400, "You cannot remove yourself"),
    FORBIDDEN(403, "You are not allow for this resourse"),

    // Task
    TASK_NOT_FOUND(404, "Task not found"),
    INVALID_STATUS_TRANSITION(400, "Invalid task status transition"),
    ASSIGNEE_NOT_IN_PROJECT(400, "Assignee must be a member of the project"),

    // User
    USER_NOT_FOUND(404, "User not found"),

    // Comment
    COMMENT_NOT_FOUND(404, "Task not found"),
    NOT_AUTHOR_COMMENT(400, "You are not author to update this comment"),

    ;

    private final int httpStatus;
    private final String message;
}
