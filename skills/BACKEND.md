# Kotlin + Spring Boot Backend Skill

Best practices for developing Kotlin backend applications with Spring Boot.

---

## Kotlin Style

- Prefer `val` over `var`; avoid nullable fields unless truly optional
- Avoid `!!` — use Elvis (`?:`), safe calls (`?.let`), or throw explicitly
- Use sealed classes for result types and domain states (exhaustive `when`)
- Use extension functions for domain-specific readability (e.g., `Instant.isExpired()`)
- Use `require` for argument validation, `check` for state validation
- Use `find` prefix for nullable returns, `get` prefix when throwing if not found

---

## Spring Boot Patterns

- **Constructor injection only** — no `@Autowired`, no `lateinit`, no field injection
- **Repositories** — see `skills/DATABASE.md` for Spring Data JPA patterns

### Service Layer

- Services orchestrate use cases with `@Transactional`
- Inject `Clock` for testable time-dependent logic
- Use sealed result types for expected outcomes (not exceptions)
- Private helpers for validation and creation logic

```kotlin
@Service
class MatchingService(
    private val okRepository: OkRepository,
    private val clock: Clock
) {
    @Transactional
    fun sendOk(senderId: UUID, recipientId: UUID): OkResult {
        // ... validate, create, publish event ...
    }
}

// Result type — no exceptions for expected outcomes
sealed class OkResult {
    data class Success(val ok: Ok) : OkResult()
    data object SenderNotFound : OkResult()
    data object RecipientNotFound : OkResult()
    data class ValidationFailed(val reason: String) : OkResult()
}
```

### Controller Layer

- Controllers only map requests to service calls and results to responses
- Use `@Valid @RequestBody` for input validation
- Map sealed result types to `ResponseEntity` with exhaustive `when`
- Always use DTOs — never return entities

```kotlin
@RestController
@RequestMapping("/api/v1/oks")
class OkController(private val matchingService: MatchingService) {
    @PostMapping
    fun sendOk(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: SendOkRequest
    ): ResponseEntity<Any> = when (val result = matchingService.sendOk(user.id, request.recipientId)) {
        is OkResult.Success -> ResponseEntity.status(CREATED).body(OkResponse.from(result.ok))
        is OkResult.SenderNotFound -> ResponseEntity.status(UNAUTHORIZED).build()
        is OkResult.RecipientNotFound -> ResponseEntity.status(NOT_FOUND).body(ErrorResponse("Recipient not found"))
        is OkResult.ValidationFailed -> ResponseEntity.badRequest().body(ErrorResponse(result.reason))
    }
}
```

**DTOs:** Use data classes with `@field:NotNull` for validation. Add `companion object { fun from(entity) }` factory methods for response mapping.

---

## JPA Entities

```kotlin
@Entity
@Table(name = "users")
class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, unique = true)
    val email: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.VerificationPending,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column
    var updatedAt: Instant? = null
) {
    // Business methods on entity
    fun activate() {
        require(status == UserStatus.ReviewPending) { "Can only activate from ReviewPending" }
        status = UserStatus.Active
        updatedAt = Instant.now()
    }

    // equals/hashCode based on id only (required for JPA)
    override fun equals(other: Any?) = this === other || (other is User && id == other.id)
    override fun hashCode() = id.hashCode()
}
```

**JPA rules:**
- `@ManyToOne(fetch = FetchType.LAZY)` always — never EAGER
- Avoid `@OneToMany` on entities — query from repository instead (prevents N+1)
- Never return entities from controllers — always map to DTOs
- See `skills/DATABASE.md` for relationship mapping and query patterns

---

## Spring Modulith

Public API in package root; internal classes in `internal/` subpackage.

```
com.kenneth.datingapp.matching/
├── MatchingService.kt          # Public API
├── OkResponse.kt               # Public DTO
├── internal/                   # Not accessible from other modules
│   ├── OkRepository.kt
│   └── MatchingConfig.kt
└── events/                     # Published events (public)
    └── OkSentEvent.kt
```

### Cross-Module Communication via Events

```kotlin
// Publisher — in source module
eventPublisher.publishEvent(OkSentEvent(okId = ok.id, senderId = senderId, recipientId = recipientId))

// Event — in source module's events/ package (public)
data class OkSentEvent(val okId: UUID, val senderId: UUID, val recipientId: UUID)

// Listener — in consuming module
@ApplicationModuleListener
fun onOkSent(event: OkSentEvent) { /* handle */ }
```

---

## Error Handling

- **Expected failures** → sealed result types (see Service Layer above)
- **Unexpected failures** → exceptions (bugs, infrastructure issues)

```kotlin
class UserNotFoundException(id: UUID) : RuntimeException("User not found: $id")

fun getUser(id: UUID): User =
    userRepository.findByIdOrNull(id) ?: throw UserNotFoundException(id)
```

### Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity.badRequest().body(ErrorResponse(errors.joinToString(", ")))
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected error", ex)
        return ResponseEntity.internalServerError().body(ErrorResponse("Internal server error"))
    }
}

data class ErrorResponse(val message: String)
```

---

## Configuration

```kotlin
@ConfigurationProperties(prefix = "app.matching")
data class MatchingProperties(
    val okExpirationDays: Long = 7,
    val maxActiveOks: Int = 3,
    val proposalDeadlineHours: Long = 48
)

// Enable with @EnableConfigurationProperties(MatchingProperties::class)
// Inject directly into services via constructor
```

```yaml
# application.yml
app:
  matching:
    ok-expiration-days: 7
    max-active-oks: 3
    proposal-deadline-hours: 48
  meeting:
    max-weeks-from-match: 3
    reschedule-limit: 1
  feedback:
    prompt-delay-hours: 3
```

---

## Testing

See `skills/TESTING.md` for all testing patterns, fixtures, and conventions.

---

## Common Pitfalls

1. **No `lateinit` or `@Autowired`** — constructor injection only
2. **No entities in API responses** — always map to DTOs
3. **No silent exception swallowing** — log and handle appropriately
4. **No mutable collections in data classes** — prefer immutable
5. **No missing `@Transactional`** — required when DB operations need atomicity
6. **No `FetchType.EAGER`** — default to LAZY, fetch explicitly when needed
7. **No business logic in controllers** — controllers only map requests/responses
