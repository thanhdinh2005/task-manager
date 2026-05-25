package com.thanh.taskmanager.fixture;

import com.thanh.taskmanager.entity.Project;
import com.thanh.taskmanager.entity.User;

public class ProjectFixture {
    private Long id = 1L;
    private String name = TestConstants.DEFAULT_PROJECT_NAME;
    private String description = TestConstants.DESCRIPTION;
    private User owner = null;

    public static ProjectFixture aProject() {
        return new ProjectFixture();
    }

    public ProjectFixture withId(Long id) {
        this.id = id;
        return this;
    }

    public ProjectFixture withName(String name) {
        this.name = name;
        return this;
    }

    public ProjectFixture withDescription(String description) {
        this.description = description;
        return this;
    }

    public ProjectFixture withOwner(User owner) {
        this.owner = owner;
        return this;
    }

    public Project build() {
        if (this.owner == null) {
            this.owner = UserFixture.aUser().build();
        }

        Project project = Project.create(name, description, owner);

        if (this.id != null) {
            EntityTestUtils.withId(project, this.id);
        }

        return project;
    }
}
