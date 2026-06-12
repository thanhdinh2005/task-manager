# Self-Review: Inversion of Control (IoC) & Dependency Injection (DI)

### 1. Purpose
Reviewing the heart of the Spring Framework: IoC and DI. The goal is to understand why we stopped using the `new` keyword and how handing control over to the Spring Container solves massive architectural problems.

### 2. The Old Way (Tight Coupling)
In older projects, classes had to be instantiated manually using the `new` keyword (e.g., `UserService service = new UserService()`). This made classes **tightly coupled**.
As the project scaled, or when I wanted to write Unit Tests, it became a nightmare because the dependencies were hardcoded and impossible to mock. Furthermore, if both `OrderService` and `UserController` needed `UserService`, they would each create a new instance. This led to terrible **manual instance management**, memory waste, and inconsistent states.

### 3. The Current Approach (Loose Coupling)
Now, we use **Inversion of Control (IoC)**. We don't create objects anymore; we let the Spring Container create and manage them (as Beans). We then use **Dependency Injection (DI)**—specifically Constructor Injection via `@RequiredArgsConstructor`—to simply ask Spring to provide the exact instance we need.

### 4. How it Works
* **The Singleton Magic:** By default, Spring creates only ONE instance of `UserService` (a Singleton) and shares it perfectly across the `OrderService`, `UserController`, and anywhere else it is needed. No more manual instance management!
* **Testability:** Because we inject the dependency through a constructor, writing Unit Tests is incredibly easy. I can just pass a fake, "Mocked" `UserService` into the constructor without starting the whole Spring context.

### 5. Lesson Learned
The `new` keyword is the enemy of testability and scalability. By applying IoC and DI, the code becomes modular, loosely coupled, and extremely easy to test. It allows me to focus on the business logic instead of acting like a memory manager.