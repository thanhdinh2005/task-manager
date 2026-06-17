# Self-Review: Rich Domain Model vs. Anemic Domain Model

### 1. Purpose
Looking at entities (like `User` or `Project`). The goal is to move towards a Rich Domain Model where the entity manages its own internal state using static factory methods and behavior-driven functions, rather than exposing a public constructor and setters.

### 2. The Old Way
In older projects, I used to just slap on `@AllArgsConstructor`, `@Builder`, or `@Data` (the magic of Lombok). Or even worse, letting the IDE generate a massive wall of getters and setters. This created an "Anemic Domain Model" where entities are just dumb data bags manipulated by bloated Service classes.

### 3. The Current Approach
Now, with a Rich Domain approach, a `User` entity uses a static factory method like `User.register()`. To modify state, we use specific methods like `changePassword()` or `updateProfile()`.
The coolest part: **no public setters**. Outside classes tell the entity *what* to do, but they don't micromanage *how* the entity changes its fields. This makes Unit Testing incredibly straightforward and improves readability.

### 4. How it Works
* **Business-Driven Naming:** The design maps directly to real business logic. Instead of doing `user.setStatus("ACTIVE")`, we simply call `user.activate()`. It speaks the language of the business.
* **Encapsulation:** By hiding the setters, we protect the data integrity. A Service class can't accidentally set a user's password to `null`.
* **The JPA Gotcha:** Even though we don't want a public constructor, Spring Data JPA (Hibernate) still needs an empty constructor to fetch data from the DB. So, we sneakily add a `protected` or `private` no-args constructor just to keep the framework happy, while strictly keeping it hidden from other developers.

### 5. Lesson Learned
Entities shouldn't just map database tables; they should contain core business rules. Letting the entity take care of its own state prevents logic from leaking into the Service layer. This makes the codebase much safer, cleaner, and strictly object-oriented.