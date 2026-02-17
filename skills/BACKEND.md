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

### Constructor Injection (Only Way)

```kotlin
// ✅ Good — constructor injection, immutable dependencies
@Service
class MatchingService(
    private val userRepository: UserRepository,
    private val okRepository: OkRepository,
    private val eventPublisher: ApplicationEventPublisher
) {
    // ...
}

// ❌ Avoid — field injection
@Service
class MatchingService {
    @Autowired
    private lateinit var userRepository: UserRepository  // Bad
}
```

### Repository Pattern

```kotlin
// ✅ Good — Spring Data JPA with Kotlin-friendly methods
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByStatus(status: UserStatus): List<User>
    fun existsByEmail(email: String): Boolean
    
    // Custom query when needed
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.waitlistReleasedAt IS NOT NULL")
    fun findReleasedFromWaitlist(@Param("status") status: UserStatus): List<User>
}
```

### Service Layer Structure

```kotlin
@Service
class MatchingService(
    private val userRepository: UserRepository,
    private val okRepository: OkRepository,
    private val clock: Clock  // Inject for testability
) {
    // Public API — orchestrates the use case
    @Transactional
    fun sendOk(senderId: UUID, recipientId: UUID): OkResult {
        val sender = userRepository.findByIdOrNull(senderId)
            ?: return OkResult.SenderNotFound
        val recipient = userRepository.findByIdOrNull(recipientId)
            ?: return OkResult.RecipientNotFound
        
        return when (val validation = validateOk(sender, recipient)) {
            is ValidationResult.Valid -> {
                val ok = createAndSaveOk(sender, recipient)
                OkResult.Success(ok)
            }
            is ValidationResult.Invalid -> OkResult.ValidationFailed(validation.reason)
        }
    }
    
    // Private helpers — single responsibility
    private fun validateOk(sender: User, recipient: User): ValidationResult {
        // validation logic
    }
    
    private fun createAndSaveOk(sender: User, recipient: User): Ok {
        // creation logic
    }
}

// Result type — no exceptions for expected outcomes
sealed class OkResult {
    data class Success(val ok: Ok) : OkResult()
    object SenderNotFound : OkResult()
    object RecipientNotFound : OkResult()
    data class ValidationFailed(val reason: String) : OkResult()
}
```

### Controller Layer

```kotlin
@RestController
@RequestMapping("/api/v1/oks")
class OkController(
    private val matchingService: MatchingService
) {
    @PostMapping
    fun sendOk(
        @AuthenticationPrincipal user: UserPrincipal,
        @Valid @RequestBody request: SendOkRequest
    ): ResponseEntity<Any> {
        return when (val result = matchingService.sendOk(user.id, request.recipientId)) {
            is OkResult.Success -> ResponseEntity
                .status(HttpStatus.CREATED)
                .body(OkResponse.from(result.ok))
            is OkResult.SenderNotFound -> ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .build()
            is OkResult.RecipientNotFound -> ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Recipient not found"))
            is OkResult.ValidationFailed -> ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(result.reason))
        }
    }
}

// Request/Response DTOs
data class SendOkRequest(
    @field:NotNull
    val recipientId: UUID
)

data class OkResponse(
    val id: UUID,
    val recipientId: UUID,
    val sentAt: Instant,
    val expiresAt: Instant
) {
    companion object {
        fun from(ok: Ok) = OkResponse(
            id = ok.id,
            recipientId = ok.recipientId,
            sentAt = ok.sentAt,
            expiresAt = ok.expiresAt
        )
    }
}
```

---

## JPA Entities

### Entity Structure

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
    // Business methods on entity when appropriate
    fun activate() {
        require(status == UserStatus.ReviewPending) { "Can only activate from ReviewPending" }
        status = UserStatus.Active
        updatedAt = Instant.now()
    }
    
    // equals/hashCode based on id only for JPA
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return id == other.id
    }
    
    override fun hashCode(): Int = id.hashCode()
}
```

### Relationships

```kotlin
@Entity
@Table(name = "oks")
class Ok(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    // Many-to-one: use lazy loading
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,
    
    @Enumerated(EnumType.STRING)
    var status: OkStatus = OkStatus.Pending,
    
    val sentAt: Instant = Instant.now(),
    val expiresAt: Instant = sentAt.plus(Duration.ofDays(7))
)

// For one-to-many, prefer querying from repository over entity collections
// Avoids N+1 and large collection loading issues
```

### Avoid Common JPA Pitfalls

```kotlin
// ❌ Avoid — bidirectional relationships unless necessary
@OneToMany(mappedBy = "sender")
val sentOks: MutableList<Ok> = mutableListOf()  // Often causes issues

// ✅ Better — query when needed
interface OkRepository : JpaRepository<Ok, UUID> {
    fun findBySenderId(senderId: UUID): List<Ok>
    fun countBySenderIdAndStatus(senderId: UUID, status: OkStatus): Int
}

// ❌ Avoid — entity in API response (lazy loading, circular refs)
@GetMapping("/{id}")
fun getUser(@PathVariable id: UUID): User  // Bad

// ✅ Good — map to DTO
@GetMapping("/{id}")
fun getUser(@PathVariable id: UUID): UserResponse
```

---

## Spring Modulith

### Module Structure

Each module is a package under the root. Public API is in the package root; internal classes go in subpackages.

```
com.kenneth.datingapp.matching/
├── MatchingService.kt          # Public API
├── OkResponse.kt               # Public DTO
├── internal/                   # Not accessible from other modules
│   ├── OkEntity.kt
│   ├── OkRepository.kt
│   └── MatchingConfig.kt
└── events/                     # Published events (public)
    └── OkSentEvent.kt
```

### Cross-Module Communication via Events

```kotlin
// In matching module — publish event
@Service
class MatchingService(
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun sendOk(senderId: UUID, recipientId: UUID): OkResult {
        // ... create ok ...
        
        eventPublisher.publishEvent(OkSentEvent(
            okId = ok.id,
            senderId = senderId,
            recipientId = recipientId
        ))
        
        return OkResult.Success(ok)
    }
}

// Event class — in matching.events package (public)
data class OkSentEvent(
    val okId: UUID,
    val senderId: UUID,
    val recipientId: UUID
)

// In notification module — listen to event
@Service
class NotificationService {
    @ApplicationModuleListener
    fun onOkSent(event: OkSentEvent) {
        // Send push notification to recipient
    }
}
```

---

## Testing

See `skills/TESTING.md` for all testing patterns, fixtures, and conventions.

---

## Error Handling

### Use Result Types for Expected Failures

```kotlin
// ✅ Good — expected outcomes as sealed class
sealed class CreateUserResult {
    data class Success(val user: User) : CreateUserResult()
    object EmailAlreadyExists : CreateUserResult()
    data class ValidationError(val errors: List<String>) : CreateUserResult()
}

// ❌ Avoid — exceptions for expected cases
fun createUser(email: String): User {
    if (userRepository.existsByEmail(email)) {
        throw EmailAlreadyExistsException(email)  // Don't do this
    }
}
```

### Exceptions for Unexpected Failures

```kotlin
// ✅ Good — exceptions for bugs/infrastructure issues
class UserNotFoundException(id: UUID) : RuntimeException("User not found: $id")

// Use when the caller made a programming error or something unexpected happened
fun getUser(id: UUID): User = 
    userRepository.findByIdOrNull(id) 
        ?: throw UserNotFoundException(id)
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

### Use @ConfigurationProperties

```kotlin
@ConfigurationProperties(prefix = "app.matching")
data class MatchingProperties(
    val okExpirationDays: Long = 7,
    val maxActiveOks: Int = 3,
    val proposalDeadlineHours: Long = 48
)

// Enable in main class or config
@EnableConfigurationProperties(MatchingProperties::class)

// Use in service
@Service
class MatchingService(
    private val properties: MatchingProperties
) {
    fun createOk(): Ok {
        val expiresAt = Instant.now().plus(Duration.ofDays(properties.okExpirationDays))
        // ...
    }
}
```

### application.yml structure

```yaml
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

## Common Pitfalls to Avoid

1. **Don't use `lateinit` for dependencies** — Use constructor injection
2. **Don't return entities from controllers** — Always map to DTOs
3. **Don't use `@Autowired` on fields** — Constructor injection only
4. **Don't catch generic exceptions silently** — Log and handle appropriately
5. **Don't use mutable collections in data classes** — Prefer immutable
6. **Don't forget `@Transactional` on service methods** — When DB operations need atomicity
7. **Don't use `FetchType.EAGER`** — Default to LAZY, fetch explicitly when needed
8. **Don't put business logic in controllers** — Controllers only map requests/responses
