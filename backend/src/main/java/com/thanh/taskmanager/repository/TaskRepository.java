package com.thanh.taskmanager.repository;

import com.thanh.taskmanager.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    int countByProjectId(Long projectId);

    @Modifying
    @Query("DELETE FROM Task t WHERE t.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT t.project.id, COUNT(t) FROM Task t WHERE t.project.id IN :projectIds GROUP BY t.project.id")
    List<Object[]> countTasksByProjectIds(@Param("projectIds") List<Long> projectIds);
}
