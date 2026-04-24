package com.thanh.taskmanager.repository;

import com.thanh.taskmanager.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    // SELECT count(id) > 0 from projects where name = ? and created_by = ?
    boolean existsByNameAndCreatedById(String name, Long userId);

    @Query("""
        SELECT pm.project FROM ProjectMember pm
        JOIN FETCH pm.project.createdBy
        WHERE pm.user.id = :userId
    """)
    List<Project> findProjectsByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(p) > 0 FROM Project p WHERE p.id = :projectId AND p.createdBy.id = :userId")
    boolean isProjectOwner(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT p FROM Project p WHERE p.id = :projectId AND p.createdBy.id = :userId")
    Optional<Project> findByProjectIdAndOwnerId(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
