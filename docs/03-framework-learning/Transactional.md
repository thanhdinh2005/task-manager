# Understanding @Transactional and Transaction Propagation

This document explains the behavior of Spring's `@Transactional` annotation, focusing on transaction propagation and the consequences of managing transactions across service boundaries.

## 1. What is `@Transactional`?

Spring's `@Transactional` annotation manages database transactions, ensuring **Atomicity**, **Consistency**, **Isolation**, and **Durability** (ACID properties) for database operations. When applied to a method or class:

- **Atomicity:** All database operations within the transaction succeed and are committed, or if any fail (via unchecked exceptions), all are rolled back.
- **Consistency:** Ensures the database state is valid before and after the transaction.
- **Isolation:** Manages how concurrent transactions interact.
- **Durability:** Guarantees that committed changes are permanent.

## 2. Transaction Propagation (The Default: `REQUIRED`)

Transaction propagation defines how transactions behave when a transactional method calls another method.

- **`Propagation.REQUIRED` (Default):**
    - If a transaction is already active, the inner method joins the existing transaction.
    - If no transaction is active, a new one is created.

## 3. Scenario: `AuthServiceImpl` calls `JWTService`

Consider `AuthServiceImpl.refreshToken()` annotated with `@Transactional` calling `JWTService.rotateToken()` which lacks its own `@Transactional` annotation.

- **Behavior:** Because `rotateToken()` has no explicit transaction, it **inherits the transaction** from `refreshToken()` due to `Propagation.REQUIRED`.
- **Benefit:** All database operations in *both* `refreshToken` and `rotateToken` are treated as a single atomic unit. If `rotateToken` fails, the entire `refreshToken` operation is rolled back, preventing data inconsistency.

## 4. Consequences of NOT Managing Transactions Together

If `rotateToken` were *not* part of the same transaction (e.g., if `refreshToken` wasn't transactional, or `rotateToken` was called in a way that prevented propagation):

- **Failure Scenario:** If `rotateToken` fails after `refreshToken`'s operations have already been committed (e.g., if they auto-commit), the system ends up in an **inconsistent state**.
- **Example:** `lastLogin` might be updated, but the new refresh token isn't saved. This leads to partial updates and unpredictable application behavior.

## 5. When to Use Separate `@Transactional` Annotations

Only apply `@Transactional` to an inner method if:

- **Independent Unit of Work:** The inner method needs to be executed atomically, regardless of the caller's transaction status.
- **Specific Propagation Behavior:** You require a different propagation type (e.g., `Propagation.REQUIRES_NEW`) for specific, isolated operations. Use with extreme caution.

## Conclusion

In most cases, allowing inner methods to inherit the transaction from the calling outer method (via `Propagation.REQUIRED`) is the correct and safe approach for ensuring data integrity across distributed operations within a single logical unit of work.