package com.kenneth.datingapp.user.internal

import com.kenneth.datingapp.user.Education
import com.kenneth.datingapp.user.Gender
import com.kenneth.datingapp.user.Profile
import com.kenneth.datingapp.user.UserStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate
import java.util.UUID

interface ProfileRepository : JpaRepository<Profile, UUID> {
    fun findByUserId(userId: UUID): Profile?

    @Query("""
        SELECT p FROM Profile p
        JOIN p.user u
        WHERE u.status = :status
        AND p.gender = :gender
        AND (CAST(:minBirthDate AS LocalDate) IS NULL OR p.birthDate <= :minBirthDate)
        AND (CAST(:maxBirthDate AS LocalDate) IS NULL OR p.birthDate >= :maxBirthDate)
        AND (CAST(:minHeight AS int) IS NULL OR p.heightCm >= :minHeight)
        AND (CAST(:maxHeight AS int) IS NULL OR p.heightCm <= :maxHeight)
        AND (CAST(:education AS string) IS NULL OR p.education = :education)
        ORDER BY p.createdAt DESC
    """)
    fun browse(
        @Param("status") status: UserStatus,
        @Param("gender") gender: Gender,
        @Param("minBirthDate") minBirthDate: LocalDate?,
        @Param("maxBirthDate") maxBirthDate: LocalDate?,
        @Param("minHeight") minHeight: Int?,
        @Param("maxHeight") maxHeight: Int?,
        @Param("education") education: Education?,
        pageable: Pageable
    ): Page<Profile>
}
