# AuthService Documentation

## Responsibilities and Business Functions

The `AuthService` is responsible for managing user authentication and authorization within the application. Its core functions include:

- **User Registration:** Allowing new users to create accounts by providing email, password, and full name. It validates that the email is unique before saving the user.
- **User Login:** Authenticating existing users using their email and password. Upon successful authentication, it generates an access token and a refresh token.
- **Token Management:** It works with `JwtService` and `RefreshTokenService` to generate and manage access and refresh tokens. The `refreshToken` method allows for the issuance of new access tokens when the current one expires, using a valid refresh token.
- **User Profile Retrieval:** Provides a way for authenticated users to retrieve their own profile information (`getMe` method).
- **Password Management:** Enables users to change their password securely by requiring the current password and providing a new one.

## Transactional Aspects

- The `register` and `changePassword` methods are annotated with `@Transactional`, ensuring that these operations are atomic. If any part of the registration or password change process fails, the entire operation is rolled back.
- The `refreshToken` method is also transactional, ensuring that the generation of a new access token and the rotation of the refresh token (which involves saving a new refresh token and potentially revoking the old one) are atomic.
- The `login` method is *not* explicitly marked with `@Transactional`. In this case, Spring's default `Propagation.REQUIRED` behavior means that if `login` is called by another transactional method, it will participate in that outer transaction. However, if called directly, its database operations might be auto-committed (depending on container configuration) or run without explicit transaction management unless an outer transaction is present.

## Potential Performance Concerns (N+1, Heavy Queries)

- **N+1 Potential:** The `login` method calls `jwtService.generateAccessToken(user)` and `refreshTokenService.createRefreshToken(user.getId())`. If `createRefreshToken` involves fetching related entities that are lazily loaded, an N+1 could occur, although it's less likely here as it seems to be a direct save operation.
- **`refreshToken` Method:** This method involves multiple steps: finding the refresh token, verifying expiration, rotating the token (which might involve fetching user details), generating a new access token, and creating a new refresh token. While each step might be relatively quick, the combination could be considered a multi-step operation. The core concern is ensuring atomicity via the `@Transactional` annotation.

## Security Considerations

- **Password Hashing:** Uses `PasswordEncoder` for securely hashing passwords during registration and password changes, which is a standard security best practice.
- **JWT Usage:** Relies on JWT for access tokens, which is common for stateless authentication in APIs. The use of refresh tokens adds a layer of security for maintaining user sessions without frequent re-authentication.
- **Input Validation:** Basic validation is present (e.g., checking for unique emails, valid current passwords), which is crucial for preventing security vulnerabilities.
- **Error Handling:** Uses `AppException` with `ErrorCode` for consistent error responses, which is good for API design and security (avoids leaking sensitive internal errors).