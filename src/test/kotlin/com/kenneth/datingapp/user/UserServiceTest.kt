package com.kenneth.datingapp.user

import com.kenneth.datingapp.NotFoundException
import com.kenneth.datingapp.ValidationException
import com.kenneth.datingapp.support.TestFixtures.FIXED_NOW
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.events.UserRegisteredEvent
import com.kenneth.datingapp.user.events.UserVerifiedEvent
import com.kenneth.datingapp.user.internal.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Clock
import java.time.ZoneOffset
import java.util.UUID

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
}
