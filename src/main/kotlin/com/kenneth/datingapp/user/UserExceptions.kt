package com.kenneth.datingapp.user

/**
 * Module-local exceptions for the `user` module.
 *
 * These deliberately do NOT extend the root `com.kenneth.datingapp.BusinessException`
 * hierarchy: subclassing a root-package type from inside a module creates a
 * `user -> root` dependency edge. Since `DatingAppApplication` (root) already
 * depends on `matching`/`feedback` for `@EnableConfigurationProperties`, and
 * `matching`/`feedback` both depend on `user`, that edge closes a cycle
 * (`user -> root -> matching/feedback -> user`). `GlobalExceptionHandler`
 * (root) maps these to HTTP responses individually instead.
 */
class UserNotFoundException(message: String) : RuntimeException(message)
class UserValidationException(message: String) : RuntimeException(message)
