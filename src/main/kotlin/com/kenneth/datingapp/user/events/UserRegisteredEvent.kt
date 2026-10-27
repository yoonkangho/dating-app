package com.kenneth.datingapp.user.events

import java.util.UUID

data class UserRegisteredEvent(
    val userId: UUID,
    val email: String
)
