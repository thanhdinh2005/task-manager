package com.thanh.taskmanager.service;

import com.thanh.taskmanager.dto.request.task.CreateTaskRequest;
import com.thanh.taskmanager.dto.request.task.TaskFilterParams;
import com.thanh.taskmanager.dto.request.task.UpdateTaskRequest;
import com.thanh.taskmanager.dto.request.task.UpdateTaskStatusRequest;
import com.thanh.taskmanager.dto.response.PageResponse;
import com.thanh.taskmanager.dto.response.TaskResponse;

public interface TaskService {

    // Tạo task trong project — currentUser phải là thành viên
    TaskResponse createTask(Long projectId, Long currentUserId, CreateTaskRequest request);

    // Lấy danh sách task có filter + pagination
    PageResponse<TaskResponse> getTasks(Long projectId, Long currentUserId, TaskFilterParams filter);

    // Xem chi tiết task — currentUser phải là thành viên project
    TaskResponse getTaskById(Long taskId, Long currentUserId);

    // Cập nhật task — thành viên project
    TaskResponse updateTask(Long taskId, Long currentUserId, UpdateTaskRequest request);

    // Đổi status theo flow: TODO→IN_PROGRESS→IN_REVIEW→DONE (có thể quay lui)
    TaskResponse updateTaskStatus(Long taskId, Long currentUserId, UpdateTaskStatusRequest request);

    // Xóa task — OWNER project hoặc người tạo task
    void deleteTask(Long taskId, Long currentUserId);
}