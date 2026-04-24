package com.thanh.taskmanager.service;

import com.thanh.taskmanager.dto.request.project.AddMemberRequest;
import com.thanh.taskmanager.dto.request.project.CreateProjectRequest;
import com.thanh.taskmanager.dto.request.project.UpdateProjectRequest;
import com.thanh.taskmanager.dto.response.MemberResponse;
import com.thanh.taskmanager.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {

    // Tạo project mới — currentUserId tự động trở thành OWNER
    ProjectResponse createProject(Long currentUserId, CreateProjectRequest request);

    // Lấy danh sách project mà currentUser là thành viên
    List<ProjectResponse> getMyProjects(Long currentUserId);

    // Xem chi tiết project — kiểm tra currentUser là thành viên
    ProjectResponse getProjectById(Long projectId, Long currentUserId);

    // Cập nhật project — chỉ OWNER
    ProjectResponse updateProject(Long projectId, Long currentUserId, UpdateProjectRequest request);

    // Xóa project — chỉ OWNER, cascade xóa task và comment
    void deleteProject(Long projectId, Long currentUserId);

    // Lấy danh sách thành viên — chỉ thành viên project mới xem được
    List<MemberResponse> getMembers(Long projectId, Long currentUserId);

    // Thêm thành viên — chỉ OWNER, role mặc định MEMBER
    MemberResponse addMember(Long projectId, Long currentUserId, AddMemberRequest request);

    // Xóa thành viên — chỉ OWNER, không tự xóa chính mình
    void removeMember(Long projectId, Long currentUserId, Long targetUserId);
}
