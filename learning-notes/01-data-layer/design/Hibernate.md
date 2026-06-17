# Self-Review: Under the Hood of Hibernate

### 1. Purpose
Digging into the Data Layer to understand how Hibernate actually works under the hood, 
moving beyond just knowing how to use Spring Data JPA interfaces.

### 2. The Old Way
Before ORM frameworks, we had to use raw JDBC. This meant opening a database connection,
writing raw SQL strings like `"SELECT * FROM users"`, and manually mapping the `ResultSet` into Java objects row by row. It was a nightmare to maintain. If we added a new column to the table, we had to update every single SQL string and mapping method.

### 3. The Current Approach
Now, we use Hibernate (an ORM). We just map our Java classes to DB tables using annotations (like `@Entity`). 
When we want to save or fetch data, we work purely with Java objects, and Hibernate generates the SQL for us.

### 4. How it Works
Hibernate isn't just a SQL generator; it's a **state management machine**.
* **The Dialect Translator:** Hibernate knows different "accents" of SQL. If I switch my DB from MySQL to PostgreSQL, I just change the Dialect config. Hibernate automatically translates my Java code into the exact SQL syntax for PostgreSQL.
* **First-Level Cache:** When I fetch a `User` by ID, Hibernate stores it in a short-term memory (the Session). If I ask for that same `User` again in the same transaction, it just gives me the cached object without hitting the database again.
* **Dirty Checking:** This is the real magic. When I load a `User` from the DB, Hibernate keeps a hidden "snapshot" of it. If I do `user.changePassword()`, Hibernate compares the object to the snapshot before the transaction ends. It notices the difference and automatically fires an `UPDATE` statement. I don't even have to call a `save()` method!

### 5. Lesson Learned
Hibernate is powerful because it bridges the gap between Object-Oriented Programming and Relational Databases. However, it taught me that "magic has a cost". Since it hides the SQL, I need to be careful and always monitor the generated queries (via `show-sql=true`) to avoid performance traps like fetching too much data or making too many DB calls.