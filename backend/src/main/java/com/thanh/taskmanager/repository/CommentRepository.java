package com.thanh.taskmanager.repository;

import com.thanh.taskmanager.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.task.project.id = :projectId")
    void deleteAllByProjectId(@Param("projectId") Long projectId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        WHERE c.task.id = :taskId
        ORDER BY c.createdAt ASC
    """)
    List<Comment> findByTaskIdWithAuthor(@Param("taskId") Long taskId);

    @Query("""
        SELECT c FROM Comment c
        JOIN FETCH c.author
        JOIN FETCH c.task t
        JOIN FETCH t.project
        WHERE c.id = :id
    """)
    Optional<Comment> findByIdWithTask(@Param("id") Long id);

    int countByTaskId(Long taskId);
}
