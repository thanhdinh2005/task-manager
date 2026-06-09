# Self-Review: Entity Relationships (Manual ID Mapping vs. Hibernate ORM)

### 1. Purpose
Looking at how we handle relationships between entities (like a `User` and their `Order`). The goal is to understand the shift from manual ID mapping to leveraging Hibernate's relational mapping.

### 2. The Old Way
In older Java/OOP projects, we used to manually link tables by storing raw IDs. An `Order` class would just have a `Long userId`. When saving an order, the Service layer had to manually check for nulls and verify if the user actually existed by calling methods like `userDAO.existsById(userId)`. It was such a hassle and created a lot of repetitive validation code.

### 3. The Current Approach
Now, Hibernate changes everything automatically. Instead of holding a raw `userId`, the `Order` entity holds the actual `User` object. We just add annotations like `@ManyToOne` or `@OneToMany` to tell Hibernate that these entities have a relationship.

### 4. How it Works
* **Object Graph Navigation:** I can directly call `order.getUser().getName()` without writing a separate SQL `JOIN` or hitting the database twice. Hibernate translates my object calls into the correct SQL joins behind the scenes.
* **Automatic Integrity:** Because we use `@ManyToOne`, Hibernate and the database's Foreign Key constraints handle the validation. If I try to save an `Order` with a non-existent `User`, the framework catches it instantly. We no longer need to write boilerplate `existsById()` checks in the Service layer.

### 5. Lesson Learned
Hibernate allows us to think in terms of "Objects" rather than "Tables and Foreign Keys". By letting the ORM handle relationship mapping and data integrity, the Service layer stays clean and focused entirely on the actual business flow.