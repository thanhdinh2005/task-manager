package com.thanh.taskmanager.repository;

import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.enums.Priority;
import com.thanh.taskmanager.entity.enums.TodoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    int countByProjectId(Long projectId);

    @Modifying
    @Query("DELETE FROM Task t WHERE t.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT t.project.id, COUNT(t) FROM Task t WHERE t.project.id IN :projectIds GROUP BY t.project.id")
    List<Object[]> countTasksByProjectIds(@Param("projectIds") List<Long> projectIds);

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.project
            JOIN FETCH t.createdBy
            LEFT JOIN FETCH t.assignee
            WHERE t.id = :id
            """)
    Optional<Task> findByIdWithProject(@Param("id") Long id);

    // Dynamic filter — JPQL handle null bằng (:x IS NULL OR ...)
    // countQuery riêng để pagination không JOIN thừa
    @Query(
            value = """
                    SELECT t FROM Task t
                    JOIN FETCH t.createdBy
                    LEFT JOIN FETCH t.assignee
                    WHERE t.project.id = :projectId
                    AND (:status IS NULL OR t.status = :status)
                    AND (:priority IS NULL OR t.priority = :priority)
                    AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Task t
                    WHERE t.project.id = :projectId
                    AND (:status IS NULL OR t.status = :status)
                    AND (:priority IS NULL OR t.priority = :priority)
                    AND (:assigneeId IS NULL OR t.assignee.id = :assigneeId)
                    """
    )
    Page<Task> findByProjectIdAndFilters(
            @Param("projectId") Long projectId,
            @Param("status") TodoStatus status,
            @Param("priority") Priority priority,
            @Param("assigneeId") Long assigneeId,
            Pageable pageable
    );

}
