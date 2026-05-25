package com.thanh.taskmanager.fixture;

import org.springframework.test.util.ReflectionTestUtils;

public class EntityTestUtils {

    public static <T> void withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
    }

    public static <T> void withFileld(T entity, String fieldName, Object value) {
        ReflectionTestUtils.setField(entity, fieldName, value);
    }
}