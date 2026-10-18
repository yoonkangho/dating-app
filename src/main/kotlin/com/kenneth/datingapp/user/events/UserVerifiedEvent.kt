package com.kenneth.datingapp.user.events

import java.util.UUID

data class UserVerifiedEvent(
    val userId: UUID
)
