# Self-Assumption: Use abstract class `BaseEntity` to avoid boilerplate fields

### 1. Purpose
Looking at the `BaseEntity` class. Its main job is to hold common fields to avoid writing 
redundant code in other entities.

### 2. The old way
In older projects, we used to manually add these fields into every single entity class (like `User`, `Project`). 
And every time we saved or updated a record, we had to write `user.setCreatedAt(now())` by hand.
It was such a hassle.

### 3. The current approach
Now, by using the magic of Spring Boot, we can use `@MappedSuperclass` and `@EntityListeners` to handle this logic centrally.

### 4. How it works
* `@MappedSuperclass` shares the fields with child classes without creating a weird separate table
in the database.
* `@EntityListeners` acts like a background watcher. When I call `repository.save()`,
Spring automatically catches the action and injects the exact time. I don't have to write a single
`set()` method.

### 5. Lesson learned
Let the framework handle the boring, repetitive stuff. 
This saves time so we can focus on the actual business logic.