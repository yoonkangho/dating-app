package com.kenneth.datingapp.user

import com.kenneth.datingapp.NotFoundException
import com.kenneth.datingapp.ValidationException
import com.kenneth.datingapp.user.events.UserRegisteredEvent
import com.kenneth.datingapp.user.events.UserVerifiedEvent
import com.kenneth.datingapp.user.internal.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.UUID

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
                passwordHash = passwordEncoder.encode(rawPassword)
                    ?: error("PasswordEncoder returned null"),
                createdAt = clock.instant()
            )
        )
        eventPublisher.publishEvent(UserRegisteredEvent(userId = user.id, email = user.email))
        return RegistrationResult.Success(user)
    }

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
}

sealed class RegistrationResult {
    data class Success(val user: User) : RegistrationResult()
    data class EmailAlreadyExists(val email: String) : RegistrationResult()
    data class PhoneAlreadyExists(val phone: String) : RegistrationResult()
    data class ValidationFailed(val reason: String) : RegistrationResult()
}
