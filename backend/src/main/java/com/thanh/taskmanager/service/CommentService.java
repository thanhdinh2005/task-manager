package com.thanh.taskmanager.service;

import com.thanh.taskmanager.dto.request.comment.CreateCommentRequest;
import com.thanh.taskmanager.dto.request.comment.UpdateCommentRequest;
import com.thanh.taskmanager.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    // Lấy toàn bộ comment của task — currentUser phải là thành viên project
    List<CommentResponse> getComments(Long taskId, Long currentUserId);

    // Thêm comment — currentUser phải là thành viên project
    CommentResponse addComment(Long taskId, Long currentUserId, CreateCommentRequest request);

    // Sửa comment — chỉ người viết
    CommentResponse updateComment(Long commentId, Long currentUserId, UpdateCommentRequest request);

    // Xóa comment — người viết hoặc OWNER project
    void deleteComment(Long commentId, Long currentUserId);
}
