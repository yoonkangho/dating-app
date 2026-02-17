# Testing Skill

Best practices for testing Kotlin Spring Boot applications with JUnit 5, MockK, Testcontainers, and Spring REST Docs.

---

## Testing Philosophy

### Test Pyramid

```
        /\
       /  \      E2E Tests (few)
      /----\     - Full system tests
     /      \    - Slow, brittle
    /--------\   Integration Tests (some)
   /          \  - Spring context, real DB
  /------------\ - Testcontainers
 /              \Unit Tests (many)
/----------------\- Fast, isolated
                  - No Spring context
```

**Guidelines:**
- Unit tests: Business logic, domain rules, utilities
- Integration tests: Repository queries, service orchestration, API endpoints
- E2E tests: Critical user flows only (keep minimal)

### What to Test

| Layer | Test Type | What to Verify |
|-------|-----------|----------------|
| Entities/Domain | Unit | Business rules, state transitions, validation |
| Services | Unit + Integration | Business logic, orchestration, transactions |
| Repositories | Integration | Custom queries, projections, edge cases |
| Controllers | Integration | Request mapping, validation, response format |
| Module boundaries | Integration | Spring Modulith boundary enforcement |

---

## Project Test Structure

```
src/test/kotlin/com/kenneth/datingapp/
├── TestDatingAppApplication.kt      # Test app with Testcontainers
├── TestcontainersConfiguration.kt   # Shared container config
├── support/                         # Test utilities
│   ├── TestFixtures.kt              # Factory methods for test data
│   ├── IntegrationTestBase.kt       # Base class for integration tests
│   └── MockkExtensions.kt           # Custom MockK helpers
├── user/
│   ├── UserTest.kt                  # Unit tests for User entity
│   ├── UserServiceTest.kt           # Unit tests with mocks
│   ├── UserServiceIntegrationTest.kt
│   ├── UserRepositoryTest.kt        # Integration tests
│   └── UserControllerTest.kt        # API tests
├── matching/
│   ├── OkTest.kt
│   ├── MatchingServiceTest.kt
│   └── ...
└── ModulithArchitectureTest.kt      # Module boundary tests
```

---

## Unit Testing

### Test Naming Convention

```kotlin
// Use backticks with descriptive behavior-focused names
class UserTest {
    @Test
    fun `new user starts in verification pending status`() { }
    
    @Test
    fun `user cannot be activated from waitlisted status`() { }
    
    @Test
    fun `user with 3 active oks cannot send more`() { }
    
    @Test
    fun `undoing ok marks profile and increments counter`() { }
}
```

### Testing Domain Entities

```kotlin
class UserTest {
    @Test
    fun `new user starts in verification pending status`() {
        val user = User(
            email = "test@example.com",
            displayName = "Test User",
            birthDate = LocalDate.of(1990, 1, 1),
            gender = Gender.FEMALE
        )
        
        assertThat(user.status).isEqualTo(UserStatus.VERIFICATION_PENDING)
    }
    
    @Test
    fun `activate transitions from review pending to active`() {
        val user = createUser(status = UserStatus.REVIEW_PENDING)
        
        user.activate()
        
        assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
        assertThat(user.approvedAt).isNotNull()
    }
    
    @Test
    fun `activate throws when not in review pending`() {
        val user = createUser(status = UserStatus.WAITLISTED)
        
        assertThatThrownBy { user.activate() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("ReviewPending")
    }
    
    @Test
    fun `canSendOk returns false when at max active oks`() {
        val user = createUser(activeOkCount = 3)
        
        assertThat(user.canSendOk()).isFalse()
    }
    
    @Test
    fun `canSendOk returns true when below max`() {
        val user = createUser(activeOkCount = 2)
        
        assertThat(user.canSendOk()).isTrue()
    }
}
```

### Testing Services with MockK

```kotlin
class MatchingServiceTest {
    // Create mocks
    private val userRepository = mockk<UserRepository>()
    private val okRepository = mockk<OkRepository>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val clock = Clock.fixed(
        Instant.parse("2025-01-15T10:00:00Z"), 
        ZoneOffset.UTC
    )
    
    // System under test
    private val service = MatchingService(
        userRepository = userRepository,
        okRepository = okRepository,
        eventPublisher = eventPublisher,
        clock = clock
    )
    
    @Test
    fun `sendOk creates ok and publishes event`() {
        // Given
        val sender = createUser(id = uuid("sender"), activeOkCount = 0)
        val recipient = createUser(id = uuid("recipient"))
        
        every { userRepository.findByIdOrNull(sender.id) } returns sender
        every { userRepository.findByIdOrNull(recipient.id) } returns recipient
        every { okRepository.existsActiveOkBetween(sender.id, recipient.id) } returns false
        every { okRepository.save(any()) } answers { firstArg() }
        
        // When
        val result = service.sendOk(sender.id, recipient.id)
        
        // Then
        assertThat(result).isInstanceOf(OkResult.Success::class.java)
        val ok = (result as OkResult.Success).ok
        assertThat(ok.sender.id).isEqualTo(sender.id)
        assertThat(ok.recipient.id).isEqualTo(recipient.id)
        assertThat(ok.expiresAt).isEqualTo(clock.instant().plus(Duration.ofDays(7)))
        
        verify { eventPublisher.publishEvent(match<OkSentEvent> { 
            it.senderId == sender.id && it.recipientId == recipient.id 
        }) }
    }
    
    @Test
    fun `sendOk fails when sender at max active oks`() {
        // Given
        val sender = createUser(activeOkCount = 3)  // At max
        val recipient = createUser()
        
        every { userRepository.findByIdOrNull(sender.id) } returns sender
        every { userRepository.findByIdOrNull(recipient.id) } returns recipient
        
        // When
        val result = service.sendOk(sender.id, recipient.id)
        
        // Then
        assertThat(result).isInstanceOf(OkResult.ValidationFailed::class.java)
        assertThat((result as OkResult.ValidationFailed).reason)
            .contains("maximum")
        
        verify(exactly = 0) { okRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publishEvent(any()) }
    }
    
    @Test
    fun `sendOk fails when ok already exists between users`() {
        // Given
        val sender = createUser()
        val recipient = createUser()
        
        every { userRepository.findByIdOrNull(sender.id) } returns sender
        every { userRepository.findByIdOrNull(recipient.id) } returns recipient
        every { okRepository.existsActiveOkBetween(sender.id, recipient.id) } returns true
        
        // When
        val result = service.sendOk(sender.id, recipient.id)
        
        // Then
        assertThat(result).isInstanceOf(OkResult.AlreadyExists::class.java)
    }
}
```

### MockK Patterns

```kotlin
// Relaxed mock — returns default values, doesn't fail on unexpected calls
private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

// Strict mock — fails on unexpected calls (default)
private val userRepository = mockk<UserRepository>()

// Stub return value
every { userRepository.findByIdOrNull(any()) } returns null
every { userRepository.findByIdOrNull(userId) } returns user  // More specific wins

// Stub with argument capture
val savedOk = slot<Ok>()
every { okRepository.save(capture(savedOk)) } answers { savedOk.captured }

// Verify call happened
verify { okRepository.save(any()) }
verify(exactly = 1) { okRepository.save(any()) }
verify(exactly = 0) { eventPublisher.publishEvent(any()) }

// Verify with argument matching
verify { eventPublisher.publishEvent(match<OkSentEvent> { it.senderId == userId }) }

// Verify order
verifyOrder {
    okRepository.save(any())
    eventPublisher.publishEvent(any())
}
```

---

## Integration Testing

### Testcontainers Configuration

```kotlin
// TestcontainersConfiguration.kt
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {
    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:16"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
    }
    
    @Bean
    @ServiceConnection
    fun redisContainer(): GenericContainer<*> {
        return GenericContainer(DockerImageName.parse("redis:7"))
            .withExposedPorts(6379)
    }
}
```

### Integration Test Base Class

```kotlin
// IntegrationTestBase.kt
@SpringBootTest
@Transactional  // Rollback after each test
@Import(TestcontainersConfiguration::class)
abstract class IntegrationTestBase {
    @Autowired
    protected lateinit var entityManager: EntityManager
    
    // Flush and clear to ensure queries hit the database
    protected fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }
}
```

### Repository Integration Tests

```kotlin
class UserRepositoryTest : IntegrationTestBase() {
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Test
    fun `findByEmail returns user when exists`() {
        // Given
        val user = userRepository.save(createUser(email = "test@example.com"))
        flushAndClear()
        
        // When
        val found = userRepository.findByEmail("test@example.com")
        
        // Then
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(user.id)
    }
    
    @Test
    fun `findByEmail returns null when not exists`() {
        val found = userRepository.findByEmail("nonexistent@example.com")
        
        assertThat(found).isNull()
    }
    
    @Test
    fun `findActiveByGender returns only active users of specified gender`() {
        // Given
        userRepository.save(createUser(gender = Gender.FEMALE, status = UserStatus.ACTIVE))
        userRepository.save(createUser(gender = Gender.FEMALE, status = UserStatus.ACTIVE))
        userRepository.save(createUser(gender = Gender.FEMALE, status = UserStatus.WAITLISTED))
        userRepository.save(createUser(gender = Gender.MALE, status = UserStatus.ACTIVE))
        flushAndClear()
        
        // When
        val results = userRepository.findActiveByGender(Gender.FEMALE)
        
        // Then
        assertThat(results).hasSize(2)
        assertThat(results).allMatch { it.gender == Gender.FEMALE }
        assertThat(results).allMatch { it.status == UserStatus.ACTIVE }
    }
    
    @Test
    fun `custom query handles pagination correctly`() {
        // Given
        repeat(25) { i ->
            userRepository.save(createUser(
                email = "user$i@example.com",
                status = UserStatus.ACTIVE
            ))
        }
        flushAndClear()
        
        // When
        val page = userRepository.findByStatus(
            UserStatus.ACTIVE,
            PageRequest.of(0, 10, Sort.by("email"))
        )
        
        // Then
        assertThat(page.content).hasSize(10)
        assertThat(page.totalElements).isEqualTo(25)
        assertThat(page.totalPages).isEqualTo(3)
    }
}
```

### Service Integration Tests

```kotlin
class MatchingServiceIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var matchingService: MatchingService
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @Autowired
    private lateinit var okRepository: OkRepository
    
    @Test
    fun `sendOk persists ok and updates sender count`() {
        // Given
        val sender = userRepository.save(createUser(gender = Gender.FEMALE, status = UserStatus.ACTIVE))
        val recipient = userRepository.save(createUser(gender = Gender.MALE, status = UserStatus.ACTIVE))
        flushAndClear()
        
        // When
        val result = matchingService.sendOk(sender.id, recipient.id)
        flushAndClear()
        
        // Then
        assertThat(result).isInstanceOf(OkResult.Success::class.java)
        
        val savedOk = okRepository.findAll().single()
        assertThat(savedOk.sender.id).isEqualTo(sender.id)
        assertThat(savedOk.recipient.id).isEqualTo(recipient.id)
        assertThat(savedOk.status).isEqualTo(OkStatus.PENDING)
    }
    
    @Test
    fun `full matching flow from ok to mutual ok`() {
        // Given
        val woman = userRepository.save(createUser(gender = Gender.FEMALE, status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(gender = Gender.MALE, status = UserStatus.ACTIVE))
        flushAndClear()
        
        // When — woman sends ok
        val sendResult = matchingService.sendOk(woman.id, man.id)
        assertThat(sendResult).isInstanceOf(OkResult.Success::class.java)
        val okId = (sendResult as OkResult.Success).ok.id
        
        // When — man responds with ok
        val respondResult = matchingService.respondToOk(okId, man.id, accepted = true)
        flushAndClear()
        
        // Then
        assertThat(respondResult).isInstanceOf(RespondResult.MutualMatch::class.java)
        
        val ok = okRepository.findByIdOrNull(okId)!!
        assertThat(ok.status).isEqualTo(OkStatus.MUTUAL)
    }
}
```

---

## Controller Testing

### MockMvc with Spring REST Docs

```kotlin
@WebMvcTest(OkController::class)
@AutoConfigureRestDocs
@Import(SecurityTestConfig::class)  // Disable security for tests
class OkControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc
    
    @MockkBean
    private lateinit var matchingService: MatchingService
    
    @Test
    fun `POST ok returns 201 with ok response`() {
        // Given
        val senderId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        val ok = createOk(senderId = senderId, recipientId = recipientId)
        
        every { matchingService.sendOk(senderId, recipientId) } returns OkResult.Success(ok)
        
        // When/Then
        mockMvc.perform(
            post("/api/v1/oks")
                .with(user(senderId))  // Mock authenticated user
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"recipientId": "$recipientId"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(ok.id.toString()))
            .andExpect(jsonPath("$.recipientId").value(recipientId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andDo(document("ok-create",
                requestFields(
                    fieldWithPath("recipientId").description("ID of the user to send ok to")
                ),
                responseFields(
                    fieldWithPath("id").description("Unique identifier of the ok"),
                    fieldWithPath("recipientId").description("ID of the recipient"),
                    fieldWithPath("status").description("Current status of the ok"),
                    fieldWithPath("sentAt").description("Timestamp when ok was sent"),
                    fieldWithPath("expiresAt").description("Timestamp when ok expires")
                )
            ))
    }
    
    @Test
    fun `POST ok returns 400 when validation fails`() {
        val senderId = UUID.randomUUID()
        val recipientId = UUID.randomUUID()
        
        every { matchingService.sendOk(senderId, recipientId) } returns 
            OkResult.ValidationFailed("Maximum active oks reached")
        
        mockMvc.perform(
            post("/api/v1/oks")
                .with(user(senderId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"recipientId": "$recipientId"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Maximum active oks reached"))
    }
    
    @Test
    fun `POST ok returns 400 when recipientId missing`() {
        mockMvc.perform(
            post("/api/v1/oks")
                .with(user(UUID.randomUUID()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").exists())
    }
    
    @Test
    fun `GET pending oks returns list for recipient`() {
        val userId = UUID.randomUUID()
        val oks = listOf(
            createOkResponse(recipientId = userId),
            createOkResponse(recipientId = userId)
        )
        
        every { matchingService.getPendingOksForUser(userId) } returns oks
        
        mockMvc.perform(
            get("/api/v1/oks/pending")
                .with(user(userId))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }
}
```

### Security Test Configuration

```kotlin
@TestConfiguration
class SecurityTestConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
        return http.build()
    }
}

// Helper to mock authenticated user
fun user(userId: UUID): RequestPostProcessor = RequestPostProcessor { request ->
    request.setAttribute("userId", userId)
    // Or set up proper security context
    request
}
```

---

## Spring Modulith Tests

### Architecture Tests

```kotlin
@AnalyzeClasses(packagesOf = [DatingAppApplication::class])
class ModulithArchitectureTest {
    @Autowired
    private lateinit var modules: ApplicationModules
    
    @Test
    fun `modules have valid structure`() {
        modules.verify()
    }
    
    @Test
    fun `module dependencies are cycle-free`() {
        modules.detectCycles()
            .throwIfPresent()
    }
    
    @Test
    fun `documents module structure`() {
        Documenter(modules)
            .writeDocumentation()
    }
}
```

### Module Integration Tests

```kotlin
@ApplicationModuleTest
class MatchingModuleIntegrationTest {
    @Autowired
    private lateinit var matchingService: MatchingService
    
    @Autowired
    private lateinit var events: PublishedEvents
    
    @Test
    fun `sendOk publishes OkSentEvent`() {
        // Given
        val senderId = setupUser(Gender.FEMALE)
        val recipientId = setupUser(Gender.MALE)
        
        // When
        matchingService.sendOk(senderId, recipientId)
        
        // Then
        assertThat(events.ofType(OkSentEvent::class.java))
            .hasSize(1)
            .element(0)
            .satisfies({ event ->
                assertThat(event.senderId).isEqualTo(senderId)
                assertThat(event.recipientId).isEqualTo(recipientId)
            })
    }
}
```

---

## Test Fixtures

### Factory Methods

```kotlin
// TestFixtures.kt
object TestFixtures {
    private var counter = AtomicLong(0)
    
    fun createUser(
        id: UUID = UUID.randomUUID(),
        email: String = "user${counter.incrementAndGet()}@example.com",
        displayName: String = "Test User",
        gender: Gender = Gender.FEMALE,
        birthDate: LocalDate = LocalDate.of(1990, 1, 1),
        status: UserStatus = UserStatus.ACTIVE,
        activeOkCount: Int = 0,
        hasUndoneOk: Boolean = false,
        hasLetOkExpire: Boolean = false
    ) = User(
        id = id,
        email = email,
        displayName = displayName,
        gender = gender,
        birthDate = birthDate,
        status = status,
        activeOkCount = activeOkCount,
        hasUndoneOk = hasUndoneOk,
        hasLetOkExpire = hasLetOkExpire
    )
    
    fun createOk(
        id: UUID = UUID.randomUUID(),
        sender: User = createUser(gender = Gender.FEMALE),
        recipient: User = createUser(gender = Gender.MALE),
        status: OkStatus = OkStatus.PENDING,
        sentAt: Instant = Instant.now(),
        expiresAt: Instant = sentAt.plus(Duration.ofDays(7))
    ) = Ok(
        id = id,
        sender = sender,
        recipient = recipient,
        status = status,
        sentAt = sentAt,
        expiresAt = expiresAt
    )
    
    fun createMeeting(
        id: UUID = UUID.randomUUID(),
        ok: Ok = createOk(status = OkStatus.MUTUAL),
        scheduledAt: Instant = Instant.now().plus(Duration.ofDays(7)),
        location: Location = Location(
            name = "Coffee Shop",
            address = "123 Main St",
            latitude = 37.5665,
            longitude = 126.9780
        )
    ) = Meeting(
        id = id,
        ok = ok,
        scheduledAt = scheduledAt,
        location = location
    )
    
    // Helper for deterministic UUIDs in tests
    fun uuid(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())
}
```

### Import in Tests

```kotlin
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.uuid

class MatchingServiceTest {
    @Test
    fun `example test`() {
        val sender = createUser(gender = Gender.FEMALE, activeOkCount = 2)
        val recipient = createUser(id = uuid("recipient"), gender = Gender.MALE)
        // ...
    }
}
```

---

## Testing Async & Events

### Testing Event Listeners

```kotlin
class NotificationServiceTest {
    private val pushService = mockk<PushService>(relaxed = true)
    private val service = NotificationService(pushService)
    
    @Test
    fun `onOkSent sends push notification to recipient`() {
        // Given
        val event = OkSentEvent(
            okId = UUID.randomUUID(),
            senderId = UUID.randomUUID(),
            recipientId = UUID.randomUUID()
        )
        
        // When
        service.onOkSent(event)
        
        // Then
        verify { pushService.send(
            userId = event.recipientId,
            title = "New Interest!",
            body = any()
        )}
    }
}
```

### Testing Scheduled Tasks

```kotlin
class OkExpirationJobTest : IntegrationTestBase() {
    @Autowired
    private lateinit var expirationJob: OkExpirationJob
    
    @Autowired
    private lateinit var okRepository: OkRepository
    
    @Test
    fun `expireOks marks expired oks and updates user profiles`() {
        // Given — create oks with past expiration
        val expiredOk = okRepository.save(createOk(
            status = OkStatus.PENDING,
            expiresAt = Instant.now().minus(Duration.ofHours(1))
        ))
        val validOk = okRepository.save(createOk(
            status = OkStatus.PENDING,
            expiresAt = Instant.now().plus(Duration.ofDays(1))
        ))
        flushAndClear()
        
        // When
        expirationJob.expireOks()
        flushAndClear()
        
        // Then
        assertThat(okRepository.findByIdOrNull(expiredOk.id)!!.status)
            .isEqualTo(OkStatus.EXPIRED)
        assertThat(okRepository.findByIdOrNull(validOk.id)!!.status)
            .isEqualTo(OkStatus.PENDING)
    }
}
```

---

## Assertions Best Practices

### Use AssertJ

```kotlin
// ✅ Good — fluent, readable assertions
assertThat(user.status).isEqualTo(UserStatus.ACTIVE)
assertThat(users).hasSize(3)
assertThat(users).extracting("email").contains("a@test.com", "b@test.com")
assertThat(result).isInstanceOf(OkResult.Success::class.java)
assertThat(user.createdAt).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))

// ✅ Good — collection assertions
assertThat(oks)
    .hasSize(2)
    .allMatch { it.status == OkStatus.PENDING }
    .extracting(Ok::recipientId)
    .containsExactlyInAnyOrder(user1.id, user2.id)

// ✅ Good — exception assertions
assertThatThrownBy { service.activate(user) }
    .isInstanceOf(IllegalStateException::class.java)
    .hasMessageContaining("invalid status")

// ❌ Avoid — JUnit assertions (less readable)
assertEquals(UserStatus.ACTIVE, user.status)
assertTrue(users.size == 3)
```

### Custom Assertions

```kotlin
// Create domain-specific assertions
class OkAssert(ok: Ok) : AbstractAssert<OkAssert, Ok>(ok, OkAssert::class.java) {
    fun isPending(): OkAssert {
        assertThat(actual.status).isEqualTo(OkStatus.PENDING)
        return this
    }
    
    fun isMutual(): OkAssert {
        assertThat(actual.status).isEqualTo(OkStatus.MUTUAL)
        return this
    }
    
    fun hasSender(userId: UUID): OkAssert {
        assertThat(actual.sender.id).isEqualTo(userId)
        return this
    }
    
    companion object {
        fun assertThat(ok: Ok) = OkAssert(ok)
    }
}

// Usage
OkAssert.assertThat(ok)
    .isPending()
    .hasSender(senderId)
```

---

## Common Pitfalls

### 1. Don't Test Implementation Details

```kotlin
// ❌ Bad — testing internal method calls
verify(exactly = 1) { userRepository.save(any()) }
verify { eventPublisher.publishEvent(any()) }
// This makes refactoring hard

// ✅ Good — test observable behavior
assertThat(userRepository.findByIdOrNull(userId)).isNotNull
assertThat(events.ofType(UserCreatedEvent::class.java)).hasSize(1)
```

### 2. Don't Share Mutable State Between Tests

```kotlin
// ❌ Bad — shared mutable state
class UserServiceTest {
    private val users = mutableListOf<User>()  // Shared across tests!
    
    @Test
    fun test1() {
        users.add(createUser())  // Pollutes other tests
    }
}

// ✅ Good — fresh state per test
class UserServiceTest {
    private lateinit var users: MutableList<User>
    
    @BeforeEach
    fun setup() {
        users = mutableListOf()
    }
}
```

### 3. Don't Forget to Flush in Integration Tests

```kotlin
// ❌ Bad — might pass due to first-level cache
val user = userRepository.save(createUser())
val found = userRepository.findByIdOrNull(user.id)  // Returns cached entity

// ✅ Good — ensure query hits database
val user = userRepository.save(createUser())
flushAndClear()
val found = userRepository.findByIdOrNull(user.id)  // Actually queries DB
```

### 4. Don't Use Fixed Timestamps

```kotlin
// ❌ Bad — flaky, depends on execution speed
val user = createUser()
assertThat(user.createdAt).isEqualTo(Instant.now())  // Might fail

// ✅ Good — inject clock or use range
val clock = Clock.fixed(Instant.parse("2025-01-15T10:00:00Z"), ZoneOffset.UTC)
val service = UserService(clock = clock)

val user = service.createUser(...)
assertThat(user.createdAt).isEqualTo(clock.instant())

// Or use range assertion
assertThat(user.createdAt).isCloseTo(Instant.now(), within(1, ChronoUnit.SECONDS))
```

### 5. Don't Write Tests That Always Pass

```kotlin
// ❌ Bad — test doesn't verify anything meaningful
@Test
fun `user is created`() {
    val user = createUser()
    assertThat(user).isNotNull()  // Always passes
}

// ✅ Good — verify specific behavior
@Test
fun `new user has verification pending status and zero ok count`() {
    val user = createUser()
    assertThat(user.status).isEqualTo(UserStatus.VERIFICATION_PENDING)
    assertThat(user.activeOkCount).isEqualTo(0)
    assertThat(user.hasUndoneOk).isFalse()
}
```

### 6. Make Test Failures Informative

```kotlin
// ❌ Bad — unhelpful failure message
assertThat(result is OkResult.Success).isTrue()
// Failure: expected true but was false

// ✅ Good — clear failure message
assertThat(result)
    .isInstanceOf(OkResult.Success::class.java)
// Failure: expected instance of OkResult.Success but was OkResult.ValidationFailed(reason=...)

// ✅ Even better — custom message
assertThat(result)
    .withFailMessage("Expected successful ok creation but got: $result")
    .isInstanceOf(OkResult.Success::class.java)
```
