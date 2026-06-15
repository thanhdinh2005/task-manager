# Application Layer Optimization Notes

This document summarizes key considerations for optimizing the application layer, particularly when interacting with the database through an ORM (Object-Relational Mapper) like JPA/Hibernate.

## Mindset: Anticipate Lazy Loading, Optimize for Batching

When working with ORMs, proactively anticipate where lazy loading might lead to excessive database calls, especially when dealing with collections or related entities. The goal is to fetch all necessary data for a given operation in the fewest possible database round trips, even if it means writing slightly more complex queries.

## 1. N+1 Query Problem

### Concept
The N+1 query problem occurs when an ORM executes N additional queries to fetch "child" entities for N "parent" entities, after an initial query for the parents. This typically happens with `FetchType.LAZY` associations when child entities are accessed individually in a loop outside the initial fetching query.

**Example (Imaginary Bad Code):**
If `Project` has `FetchType.LAZY` `members` and `tasks` associations:
```java
// 1. Fetch N projects (1 query)
List<Project> projects = projectRepository.findAll();

// 2. Loop through each project, accessing lazy-loaded data (N*2 queries)
for (Project project : projects) {
    int memberCount = projectMemberRepository.countByProjectId(project.getId()); // Query for each project
    int taskCount = taskRepository.countByProjectId(project.getId()); // Query for each project
    // ... then map to DTO
}
// Total: 1 + 2N queries
```

### Avoidance Strategies
*   **Default to `FetchType.LAZY` for Associations:** Always configure `@ManyToOne`, `@OneToOne` (unless truly always needed), and `@OneToMany`/`@ManyToMany` relationships with `FetchType.LAZY` by default.
*   **Identify Collection Access Patterns:** Be vigilant when iterating over a collection of parent entities and needing to access lazily loaded children or derived values for each.
*   **Prioritize Batch Fetching for Collections:** Instead of fetching related data/counts one-by-one in a loop, aim to fetch all of them in a single query (or a very small number of batch queries).
    *   **For counts/aggregations:** Use custom repository methods with `@Query` annotations that leverage SQL `GROUP BY` and aggregate functions (`COUNT`, `SUM`, `AVG`) over a list of parent IDs.
    *   **For fetching associated entities:** Use JPQL `JOIN FETCH` (e.g., `SELECT p FROM Project p JOIN FETCH p.members`) in your repository methods to eagerly fetch related entities in the initial query.
*   **Use DTO Projections for Read-Only Views:** If your service method primarily returns data for display, project your query results directly into a DTO using `SELECT new com.yourpackage.YourDto(...)` in your `@Query` annotations.

## 2. Heavy Queries

### Concept
A "heavy query" is a database query that consumes a significant amount of database resources (CPU, memory, disk I/O, network bandwidth) and/or takes a long time to execute, regardless of the number of queries.

### Factors Contributing to Heavy Queries
*   **Large Data Scans:** Reading through a very large portion of a table that isn't efficiently indexed (full table scans).
*   **Complex Joins:** Combining many large tables, especially without proper indexing or efficient join strategies.
*   **Aggregations on Large Datasets:** `COUNT`, `SUM`, `AVG`, `GROUP BY` operations over millions of rows.
*   **Sorting (`ORDER BY`):** Sorting large result sets on unindexed columns.
*   **Inefficient Subqueries:** Correlated subqueries that re-execute for each outer row.

### Optimization Mindset for Heavy Queries
*   **Indexes are Your Friends:** Ensure all columns used in `WHERE` clauses, `JOIN` conditions, and `ORDER BY` clauses are properly indexed, especially on large tables.
*   **Selective Filtering:** Narrow down the dataset as early as possible in your queries (e.g., strong `WHERE` clauses).
*   **Profiler is Your Spyglass:** Use database query profilers (e.g., `EXPLAIN ANALYZE` in PostgreSQL) to understand the execution plan and identify bottlenecks.
*   **Balance Data Needs:** Don't fetch more data than you need. Use `SELECT` clauses to specify only required columns, and DTO projections to shape the output.
