package com.kenneth.datingapp.user.internal

import com.kenneth.datingapp.user.UserPhoto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserPhotoRepository : JpaRepository<UserPhoto, UUID> {
    fun findByProfileId(profileId: UUID): List<UserPhoto>
    fun findByProfileIdOrderByDisplayOrder(profileId: UUID): List<UserPhoto>
    fun countByProfileIdAndIsActive(profileId: UUID, isActive: Boolean): Long
}
