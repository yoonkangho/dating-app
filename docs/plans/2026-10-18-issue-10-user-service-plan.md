# Issue #10 — UserService: Registration and Verification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement `UserService.register()` and `UserService.markVerified()` — the first two use-cases in the User module's service layer — closing out GitHub issue [#10](https://github.com/yoonkangho/dating-app/issues/10) (Milestone 2).

**Architecture:** `UserService` is the module's public API (package root `com.kenneth.datingapp.user`), injecting the already-wired `UserRepository` (internal), `PasswordEncoder` (`SecurityConfig`), `ApplicationEventPublisher`, and `Clock` (`AppConfig`) beans — no new infrastructure beans needed. `register()` returns a sealed `RegistrationResult` for its *expected* failure modes (blank fields, duplicate email/phone), matching the `OkResult` pattern in `skills/BACKEND.md`. `markVerified()` instead throws the project's existing `NotFoundException` / `ValidationException` (from `Exceptions.kt`, already wired to `GlobalExceptionHandler`) — this split is deliberate: the issue's acceptance criteria name a sealed result only for registration, but explicitly say "Invalid state transitions throw `BusinessException`" for verification. `markVerified()` delegates the actual state-machine rule to the existing `User.markVerified(now)` entity method (already implemented and unit-tested in `UserTest.kt`) and only adapts the `IllegalArgumentException` it throws into a `ValidationException` — the business rule itself stays defined once, on the entity.

**Tech Stack:** Kotlin 2.3.0, Spring Boot 4.0.1, Spring Data JPA, Spring Security (`BCryptPasswordEncoder`), MockK (new dependency — see Global Constraints), AssertJ, JUnit 5.

**Branch:** `issue-10-user-service` — create via `superpowers:using-git-worktrees` at execution time, following the same `.worktrees/issue-<n>-<slug>` convention as issues #9 and #56.

## Global Constraints

- Public API in package root, internal implementation in `internal/` — `UserService` goes in `com.kenneth.datingapp.user`, not `.internal` (`CLAUDE.md` module structure rule).
- Reuse the existing generic exceptions in `src/main/kotlin/com/kenneth/datingapp/Exceptions.kt` (`NotFoundException`, `ValidationException`) — do not invent module-specific exception classes. They're already wired to `GlobalExceptionHandler` (404 / 400 respectively).
- Reuse the existing `PasswordEncoder` bean (`BCryptPasswordEncoder`, defined in `SecurityConfig.kt`) and `Clock` bean (`Clock.systemDefaultZone()`, defined in `AppConfig.kt`) via constructor injection — do not call `Instant.now()` or hash passwords manually inside the service.
- Event naming: `<Entity><Action>Event` (`CLAUDE.md` naming convention) — `UserRegisteredEvent`, `UserVerifiedEvent`, placed in a new `com.kenneth.datingapp.user.events` package (public), per the Modulith example in `skills/BACKEND.md`.
- This is the first service in the codebase, so there is no existing precedent to clash with on sealed-result or MockK usage beyond what's documented in `skills/BACKEND.md` / `skills/TESTING.md` — follow those exactly.
- New test dependency required: **`io.mockk:mockk:1.14.11`** (verified as the current `<release>` on Maven Central at plan-writing time — confirm it still resolves when you run the build; if not, use whatever version `https://repo1.maven.org/maven2/io/mockk/mockk/maven-metadata.xml` reports as `<release>`). No other MockK module (e.g. `springmockk`) is needed — these are plain unit tests with manual constructor injection, no `@MockkBean`/Spring context.
- Constructor injection only, `@Transactional` on both service methods (`skills/BACKEND.md` Service Layer convention).
- Docker must be running locally before executing Task 1 (Testcontainers starts Postgres automatically for `IntegrationTestBase`-derived tests).

---

## Task 1: `UserRepository.existsByPhone`

Registration must reject a duplicate phone number, but `UserRepository` currently only exposes `existsByEmail`. Add the matching derived query for phone.

**Files:**
- Modify: `src/main/kotlin/com/kenneth/datingapp/user/internal/UserRepository.kt`
- Test: `src/test/kotlin/com/kenneth/datingapp/user/UserRepositoryTest.kt`

**Interfaces:**
- Produces: `UserRepository.existsByPhone(phone: String): Boolean` — consumed by `UserService.register()` in Task 3.

- [ ] **Step 1: Write the failing tests**

Append to `src/test/kotlin/com/kenneth/datingapp/user/UserRepositoryTest.kt`, right after the existing `existsByEmail returns false when email does not exist` test:

```kotlin
    @Test
    fun `existsByPhone returns true when phone exists`() {
        userRepository.save(createUser(phone = "010-1234-5678"))
        flushAndClear()

        assertThat(userRepository.existsByPhone("010-1234-5678")).isTrue()
    }

    @Test
    fun `existsByPhone returns false when phone does not exist`() {
        assertThat(userRepository.existsByPhone("000-0000-0000")).isFalse()
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew compileTestKotlin`
Expected: `FAILED` — compile error, `Unresolved reference: existsByPhone`

- [ ] **Step 3: Add the method**

In `src/main/kotlin/com/kenneth/datingapp/user/internal/UserRepository.kt`, change:

```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByPhone(phone: String): User?
    fun existsByEmail(email: String): Boolean
    fun findByStatus(status: UserStatus): List<User>
}
```

to:

```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmail(email: String): User?
    fun findByPhone(phone: String): User?
    fun existsByEmail(email: String): Boolean
    fun existsByPhone(phone: String): Boolean
    fun findByStatus(status: UserStatus): List<User>
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests "com.kenneth.datingapp.user.UserRepositoryTest"`
Expected: `BUILD SUCCESSFUL`, all tests in the class pass (requires Docker running for Testcontainers)

- [ ] **Step 5: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/user/internal/UserRepository.kt src/test/kotlin/com/kenneth/datingapp/user/UserRepositoryTest.kt
git commit -m "feat(user): add UserRepository.existsByPhone"
```

---

## Task 2: Add MockK test dependency

`UserService` is the first service in the codebase, so no unit-test MockK infrastructure exists yet. Add it before writing `UserServiceTest`.

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Add the dependency**

In `build.gradle.kts`, in the `dependencies { ... }` block, add this line immediately before `testImplementation("org.springframework.boot:spring-boot-restdocs")`:

```kotlin
	testImplementation("io.mockk:mockk:1.14.11")
```

- [ ] **Step 2: Verify it resolves**

Run: `./gradlew dependencies --configuration testCompileClasspath | grep mockk`
Expected: output includes `io.mockk:mockk:1.14.11` with no resolution failure. If the version doesn't resolve, check `https://repo1.maven.org/maven2/io/mockk/mockk/maven-metadata.xml` for the current `<release>` value and use that instead.

- [ ] **Step 3: Commit**

```bash
git add build.gradle.kts
git commit -m "chore: add mockk test dependency for service unit tests"
```

---

## Task 3: `UserService.register()`

**Files:**
- Create: `src/main/kotlin/com/kenneth/datingapp/user/events/UserRegisteredEvent.kt`
- Create: `src/main/kotlin/com/kenneth/datingapp/user/UserService.kt`
- Test: `src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt`

**Interfaces:**
- Consumes: `UserRepository.existsByEmail(email: String): Boolean`, `UserRepository.existsByPhone(phone: String): Boolean` (Task 1), `UserRepository.save(user: User): User` (existing `JpaRepository` member), `PasswordEncoder.encode(rawPassword: String): String` (existing `SecurityConfig` bean), `Clock` (existing `AppConfig` bean), `User(email, phone, passwordHash, createdAt)` constructor (existing, all other params default).
- Produces: `UserService.register(email: String, phone: String?, rawPassword: String): RegistrationResult`, sealed `RegistrationResult` with variants `Success(user: User)`, `EmailAlreadyExists(email: String)`, `PhoneAlreadyExists(phone: String)`, `ValidationFailed(reason: String)`. `UserRegisteredEvent(userId: UUID, email: String)`. Consumed by Task 4 (shares the `UserService` class and constructor) and by issue #11 (controller).

- [ ] **Step 1: Create the event**

Create `src/main/kotlin/com/kenneth/datingapp/user/events/UserRegisteredEvent.kt`:

```kotlin
package com.kenneth.datingapp.user.events

import java.util.UUID

data class UserRegisteredEvent(
    val userId: UUID,
    val email: String
)
```

- [ ] **Step 2: Write the failing tests**

Create `src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt`:

```kotlin
package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.user.events.UserRegisteredEvent
import com.kenneth.datingapp.user.internal.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.ZoneOffset

class UserServiceTest {

    private val userRepository = mockk<UserRepository>()
    private val passwordEncoder = mockk<PasswordEncoder>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC)

    private val service = UserService(
        userRepository = userRepository,
        passwordEncoder = passwordEncoder,
        eventPublisher = eventPublisher,
        clock = clock
    )

    @Nested
    inner class Register {

        @Test
        fun `creates user in VERIFICATION_PENDING status with hashed password`() {
            every { userRepository.existsByEmail("jane@example.com") } returns false
            every { passwordEncoder.encode("plaintext123") } returns "hashed_password"
            every { userRepository.save(any()) } answers { firstArg() }

            val result = service.register(
                email = "jane@example.com",
                phone = null,
                rawPassword = "plaintext123"
            )

            assertThat(result).isInstanceOf(RegistrationResult.Success::class.java)
            val user = (result as RegistrationResult.Success).user
            assertThat(user.email).isEqualTo("jane@example.com")
            assertThat(user.phone).isNull()
            assertThat(user.passwordHash).isEqualTo("hashed_password")
            assertThat(user.status).isEqualTo(UserStatus.VERIFICATION_PENDING)
            assertThat(user.createdAt).isEqualTo(FIXED_NOW)
        }

        @Test
        fun `creates user with provided phone when phone is unique`() {
            every { userRepository.existsByEmail(any()) } returns false
            every { userRepository.existsByPhone("010-1234-5678") } returns false
            every { passwordEncoder.encode(any()) } returns "hashed_password"
            every { userRepository.save(any()) } answers { firstArg() }

            val result = service.register(
                email = "jane@example.com",
                phone = "010-1234-5678",
                rawPassword = "plaintext123"
            )

            assertThat(result).isInstanceOf(RegistrationResult.Success::class.java)
            assertThat((result as RegistrationResult.Success).user.phone).isEqualTo("010-1234-5678")
        }

        @Test
        fun `does not check phone uniqueness when phone is null`() {
            every { userRepository.existsByEmail(any()) } returns false
            every { passwordEncoder.encode(any()) } returns "hashed_password"
            every { userRepository.save(any()) } answers { firstArg() }

            service.register(email = "jane@example.com", phone = null, rawPassword = "plaintext123")

            verify(exactly = 0) { userRepository.existsByPhone(any()) }
        }

        @Test
        fun `publishes UserRegisteredEvent with new user id and email`() {
            every { userRepository.existsByEmail(any()) } returns false
            every { passwordEncoder.encode(any()) } returns "hashed_password"
            every { userRepository.save(any()) } answers { firstArg() }

            val result = service.register(email = "jane@example.com", phone = null, rawPassword = "plaintext123")
            val user = (result as RegistrationResult.Success).user

            verify {
                eventPublisher.publishEvent(match<UserRegisteredEvent> {
                    it.userId == user.id && it.email == "jane@example.com"
                })
            }
        }

        @Test
        fun `returns ValidationFailed when email is blank`() {
            val result = service.register(email = "   ", phone = null, rawPassword = "plaintext123")

            assertThat(result).isInstanceOf(RegistrationResult.ValidationFailed::class.java)
            assertThat((result as RegistrationResult.ValidationFailed).reason).contains("Email")
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `returns ValidationFailed when password is blank`() {
            val result = service.register(email = "jane@example.com", phone = null, rawPassword = "  ")

            assertThat(result).isInstanceOf(RegistrationResult.ValidationFailed::class.java)
            assertThat((result as RegistrationResult.ValidationFailed).reason).contains("Password")
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `returns EmailAlreadyExists when email is already taken`() {
            every { userRepository.existsByEmail("jane@example.com") } returns true

            val result = service.register(email = "jane@example.com", phone = null, rawPassword = "plaintext123")

            assertThat(result).isInstanceOf(RegistrationResult.EmailAlreadyExists::class.java)
            verify(exactly = 0) { userRepository.save(any()) }
        }

        @Test
        fun `returns PhoneAlreadyExists when phone is already taken`() {
            every { userRepository.existsByEmail(any()) } returns false
            every { userRepository.existsByPhone("010-1234-5678") } returns true

            val result = service.register(
                email = "jane@example.com",
                phone = "010-1234-5678",
                rawPassword = "plaintext123"
            )

            assertThat(result).isInstanceOf(RegistrationResult.PhoneAlreadyExists::class.java)
            verify(exactly = 0) { userRepository.save(any()) }
        }
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew compileTestKotlin`
Expected: `FAILED` — compile error, `Unresolved reference: UserService` (and `RegistrationResult`)

- [ ] **Step 4: Create `UserService` with `register()`**

Create `src/main/kotlin/com/kenneth/datingapp/user/UserService.kt`:

```kotlin
package com.kenneth.datingapp.user

import com.kenneth.datingapp.user.events.UserRegisteredEvent
import com.kenneth.datingapp.user.internal.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: ApplicationEventPublisher,
    private val clock: Clock
) {

    @Transactional
    fun register(email: String, phone: String?, rawPassword: String): RegistrationResult {
        if (email.isBlank()) {
            return RegistrationResult.ValidationFailed("Email is required")
        }
        if (rawPassword.isBlank()) {
            return RegistrationResult.ValidationFailed("Password is required")
        }
        if (userRepository.existsByEmail(email)) {
            return RegistrationResult.EmailAlreadyExists(email)
        }
        if (!phone.isNullOrBlank() && userRepository.existsByPhone(phone)) {
            return RegistrationResult.PhoneAlreadyExists(phone)
        }

        val user = userRepository.save(
            User(
                email = email,
                phone = phone,
                passwordHash = passwordEncoder.encode(rawPassword),
                createdAt = clock.instant()
            )
        )
        eventPublisher.publishEvent(UserRegisteredEvent(userId = user.id, email = user.email))
        return RegistrationResult.Success(user)
    }
}

sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class EmailAlreadyExists(val email: String) : RegistrationResult()
    data class PhoneAlreadyExists(val phone: String) : RegistrationResult()
    data class ValidationFailed(val reason: String) : RegistrationResult()
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.kenneth.datingapp.user.UserServiceTest"`
Expected: `BUILD SUCCESSFUL`, all 8 tests pass

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/user/events/UserRegisteredEvent.kt src/main/kotlin/com/kenneth/datingapp/user/UserService.kt src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt
git commit -m "feat(user): add UserService.register with RegistrationResult"
```

---

## Task 4: `UserService.markVerified()`

**Files:**
- Create: `src/main/kotlin/com/kenneth/datingapp/user/events/UserVerifiedEvent.kt`
- Modify: `src/main/kotlin/com/kenneth/datingapp/user/UserService.kt`
- Test: `src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt`

**Interfaces:**
- Consumes: `UserRepository.findByIdOrNull(id: UUID): User?` (Spring Data Kotlin extension, `org.springframework.data.repository.findByIdOrNull`), `User.markVerified(now: Instant)` (existing entity method, throws `IllegalArgumentException` if not `VERIFICATION_PENDING`), `NotFoundException`/`ValidationException` (existing, `com.kenneth.datingapp.Exceptions.kt`).
- Produces: `UserService.markVerified(userId: UUID): User`. `UserVerifiedEvent(userId: UUID)`.

- [ ] **Step 1: Create the event**

Create `src/main/kotlin/com/kenneth/datingapp/user/events/UserVerifiedEvent.kt`:

```kotlin
package com.kenneth.datingapp.user.events

import java.util.UUID

data class UserVerifiedEvent(
    val userId: UUID
)
```

- [ ] **Step 2: Write the failing tests**

Add this `@Nested` class to `src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt`, as a sibling of `Register` (inside the outer `UserServiceTest` class, after the `Register` class's closing brace):

```kotlin
    @Nested
    inner class MarkVerified {

        @Test
        fun `transitions user to WAITLISTED and publishes UserVerifiedEvent`() {
            val user = createUser(status = UserStatus.VERIFICATION_PENDING)
            every { userRepository.findByIdOrNull(user.id) } returns user
            every { userRepository.save(any()) } answers { firstArg() }

            val result = service.markVerified(user.id)

            assertThat(result.status).isEqualTo(UserStatus.WAITLISTED)
            assertThat(result.updatedAt).isEqualTo(FIXED_NOW)
            verify {
                eventPublisher.publishEvent(match<UserVerifiedEvent> { it.userId == user.id })
            }
        }

        @Test
        fun `throws NotFoundException when user does not exist`() {
            val userId = UUID.randomUUID()
            every { userRepository.findByIdOrNull(userId) } returns null

            assertThatThrownBy { service.markVerified(userId) }
                .isInstanceOf(NotFoundException::class.java)
        }

        @Test
        fun `throws ValidationException when user is not in VERIFICATION_PENDING status`() {
            val user = createUser(status = UserStatus.ACTIVE)
            every { userRepository.findByIdOrNull(user.id) } returns user

            assertThatThrownBy { service.markVerified(user.id) }
                .isInstanceOf(ValidationException::class.java)
                .hasMessageContaining("VERIFICATION_PENDING")

            verify(exactly = 0) { userRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any()) }
        }
    }
```

Add these imports to the top of `UserServiceTest.kt` (alongside the existing ones from Task 3):

```kotlin
import com.kenneth.datingapp.NotFoundException
import com.kenneth.datingapp.ValidationException
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.events.UserVerifiedEvent
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
```

- [ ] **Step 3: Run tests to verify they fail**

Run: `./gradlew compileTestKotlin`
Expected: `FAILED` — compile error, `Unresolved reference: markVerified`

- [ ] **Step 4: Add `markVerified` to `UserService`**

In `src/main/kotlin/com/kenneth/datingapp/user/UserService.kt`, add these imports:

```kotlin
import com.kenneth.datingapp.NotFoundException
import com.kenneth.datingapp.ValidationException
import com.kenneth.datingapp.user.events.UserVerifiedEvent
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID
```

Then add this method to the `UserService` class, after `register()`:

```kotlin
    @Transactional
    fun markVerified(userId: UUID): User {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw NotFoundException("User not found: $userId")

        try {
            user.markVerified(clock.instant())
        } catch (ex: IllegalArgumentException) {
            throw ValidationException(ex.message ?: "Invalid state transition")
        }

        val saved = userRepository.save(user)
        eventPublisher.publishEvent(UserVerifiedEvent(userId = saved.id))
        return saved
    }
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew test --tests "com.kenneth.datingapp.user.UserServiceTest"`
Expected: `BUILD SUCCESSFUL`, all 11 tests pass (8 from `Register` + 3 from `MarkVerified`)

- [ ] **Step 6: Commit**

```bash
git add src/main/kotlin/com/kenneth/datingapp/user/events/UserVerifiedEvent.kt src/main/kotlin/com/kenneth/datingapp/user/UserService.kt src/test/kotlin/com/kenneth/datingapp/user/UserServiceTest.kt
git commit -m "feat(user): add UserService.markVerified with BusinessException translation"
```

---

## Task 5: Full verification

Confirm the new module code doesn't break the Modulith boundary rules or anything else in the build.

**Files:** none (verification only)

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew test`
Expected: `BUILD SUCCESSFUL`, no regressions in any existing test class

- [ ] **Step 2: Run the Modulith architecture test explicitly**

Run: `./gradlew test --tests "com.kenneth.datingapp.ModulithArchitectureTest"`
Expected: `BUILD SUCCESSFUL` — `modules.verify()` and `modules.detectCycles()` still pass (the new `UserService`/events/`UserRepository` change are entirely inside the `user` module, so no new cross-module edges are introduced)

- [ ] **Step 3: Full build**

Run: `./gradlew build`
Expected: `BUILD SUCCESSFUL`

No commit needed for this task — it's verification only. If any step fails, fix the root cause and re-run before moving on (do not skip or weaken an assertion to make it pass).

---

## Self-Review Notes

- **Spec coverage:** All 5 acceptance criteria from issue #10 are covered — new user VERIFICATION_PENDING (Task 3 test 1), verified user → WAITLISTED (Task 4 test 1), duplicate email/phone errors (Task 3 tests 7–8), events published on both (Task 3 test 4, Task 4 test 1), invalid state transitions throw `BusinessException` (Task 4 test 3, via `ValidationException : BusinessException`).
- **Dependencies #6 and #9** are both closed/merged into `main` already (verified via `git merge-base --is-ancestor`), so this plan has no blockers.
- **Scope boundary:** Integration tests spanning the full user module (issue #15) and the registration DTO/controller (issue #11) are deliberately out of scope here — this plan only covers the service layer named in #10.
