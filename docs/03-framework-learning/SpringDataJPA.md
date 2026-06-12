# Self-Review: The Data Layer Evolution (JDBC -> Hibernate -> Spring Data JPA)

### 1. Purpose
Reflecting on the evolution of the Data Layer. The goal is to clearly understand the roles of JDBC, Hibernate, JPA, and Spring Data JPA, so I know exactly what is happening under the hood when I interact with the database.

### 2. The Old Way
In the past, I used to manually write native SQL queries using raw JDBC. I had to open connections, map `ResultSet` columns to Java objects row by row, and write the exact same CRUD (Create, Read, Update, Delete) methods for every single entity. It was pure boilerplate and highly error-prone.

### 3. The Current Approach
Now, the Data Layer is incredibly streamlined. We use **Hibernate** (an ORM) to automatically map tables to Objects, and **Spring Data JPA** to completely eliminate the repository boilerplate.

### 4. How it Works 
The biggest breakthrough is understanding the difference between the three key layers:
* **JPA (The Rulebook):** It’s just an interface/specification. It defines the rules (like `@Entity` or `@Id`) but doesn't actually execute any logic.
* **Hibernate (The Engine):** It is the actual ORM implementation. It follows the JPA rules to generate the real SQL behind the scenes.
* **Spring Data JPA (The Magic Wrapper):** This is the framework module that simplifies the Repository layer. By simply extending `JpaRepository<Entity, ID>`, Spring automatically generates all the basic CRUD methods (`findAll`, `findById`) on the fly.

### 5. Lesson Learned
Let the framework handle the boring, repetitive CRUD operations. However, if the built-in methods are not enough (like needing a complex filter or a specific `JOIN FETCH`), I can easily drop down a level and write a custom JPQL query using the `@Query` annotation.