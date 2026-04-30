package com.thanh.taskmanager.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name = "comments")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Comment extends BaseEntity{
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private Task task;

    @ManyToOne(targetEntity = User.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User author;

    @Column(nullable = false)
    private String content;

    public static Comment create(
            Task task,
            User author,
            String content
    ) {
        Comment comment = new Comment();
        comment.author = author;
        comment.task = task;
        comment.content = content;
        return comment;
    }

    public void update(String content) {
        this.content = content;
    }

    public boolean isAuthor(Long userId) {
        return this.author.getId().equals(userId);
    }
}
