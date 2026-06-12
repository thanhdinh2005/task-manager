# Self-Review: Authentication Flow & Token Rotation Strategies

### 1. Purpose
Looking into the complex methods of `AuthService` (`login` and `refreshToken`). The goal is to understand how we delegate authentication to Spring Security and how business rules dictate our token management strategy.

### 2. The Old Way (The Naive Approach)
In the past, to log a user in, I would manually fetch the user by email from the database, hash the incoming password, compare the two strings, and write custom `if-else` logic to throw "Wrong password" errors. It was tedious and risky to write security cryptography by hand. Also, hardcoding JWT secret keys directly into the source code was a massive security vulnerability.

### 3. The Current Approach
Now, we let Spring Security do the heavy lifting using the `AuthenticationManager`. We also manage state via stateless JWTs (Access & Refresh tokens), with all sensitive configurations (secrets, expiration times) strictly injected via Environment Variables (ENV).

### 4. How it Works
* **The Bouncer (`AuthenticationManager`):** I just pass the raw username and password to this manager. It acts like a bouncer at a club. If the credentials don't match, it automatically throws an exception (which our Global `RestControllerAdvice` catches nicely). If it succeeds, it lets me cast the `Principal` into my `CustomUserDetails` to grab the specific `User` entity.
* **Environment Configuration (ENV):** By keeping token configurations in ENV instead of hardcoding them in `application.yml`, the application becomes "Cloud-Ready". DevOps can easily rotate keys or change expiration times without touching or recompiling the code.
* **Token Rotation & Security Posture:** Inside `refreshToken`, we rotate the old token for a new one. The exact logic heavily depends on the business's security posture:
    * *Standard Security:* We just delete/invalidate the exact refresh token that was just used.
    * *High Security (Strict Mode):* We call `deleteAllByUser()`. If a token expires or is suspected to be compromised, we wipe out ALL tokens for that user. This forcefully logs them out of everywhere (Mobile, Web, iPad) simultaneously, requiring a fresh login across all devices.

### 5. Lesson Learned
Security is not just a technical implementation; it's a business decision. Designing a high-security app means understanding the trade-offs between User Experience (making them re-login) and System Safety. Furthermore, delegating core security checks to the framework makes the Service layer much cleaner.