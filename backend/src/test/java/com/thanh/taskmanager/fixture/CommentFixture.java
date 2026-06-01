package com.thanh.taskmanager.fixture;

import com.thanh.taskmanager.entity.Comment;
import com.thanh.taskmanager.entity.Task;
import com.thanh.taskmanager.entity.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

public class CommentFixture {

    private Long id = 1L;
    private String content = TestConstants.DEFAULT_COMMENT_CONTENT;
    private User author = null;
    private Task task = null;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = null;

    private CommentFixture() {}

    public static CommentFixture aComment() {
        return new CommentFixture();
    }

    public CommentFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public CommentFixture withContent(String content) {
        this.content = content;
        return this;
    }

    public CommentFixture withAuthor(User author) {
        this.author = author;
        return this;
    }

    public CommentFixture withTask(Task task) {
        this.task = task;
        return this;
    }

    public CommentFixture asEdited() {
        this.createdAt = LocalDateTime.now().minusHours(1);
        this.updatedAt = LocalDateTime.now();
        return this;
    }

    public Comment build() {
        Comment comment = Comment.create(task, author, content);

        setField(comment, "id", id);

        LocalDateTime resolvedUpdatedAt = (updatedAt != null) ? updatedAt : createdAt;
        setField(comment, "createdAt", createdAt);
        setField(comment, "updatedAt", resolvedUpdatedAt);

        return comment;
    }

    private static void setField(Object target, String fieldName, Object value) {
        Class<?> clazz = target.getClass();
        while (clazz != null) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Cannot set field '" + fieldName + "'", e);
            }
        }
        throw new RuntimeException("Field '" + fieldName + "' not found in " + target.getClass().getName());
    }
}