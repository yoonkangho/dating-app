package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createProfile
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.support.TestFixtures.createUserPhoto
import com.kenneth.datingapp.user.internal.ProfileRepository
import com.kenneth.datingapp.user.internal.UserPhotoRepository
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class UserPhotoRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var userPhotoRepository: UserPhotoRepository

    @Autowired
    private lateinit var profileRepository: ProfileRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByProfileId returns all photos for profile`() {
        val user = userRepository.save(createUser())
        val profile = profileRepository.save(createProfile(user = user))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 1))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 2))
        flushAndClear()

        val photos = userPhotoRepository.findByProfileId(profile.id)

        assertThat(photos).hasSize(2)
    }

    @Test
    fun `findByProfileIdOrderByDisplayOrder returns photos in order`() {
        val user = userRepository.save(createUser())
        val profile = profileRepository.save(createProfile(user = user))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 3))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 1))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 2))
        flushAndClear()

        val photos = userPhotoRepository.findByProfileIdOrderByDisplayOrder(profile.id)

        assertThat(photos).hasSize(3)
        assertThat(photos.map { it.displayOrder }).containsExactly(1, 2, 3)
    }

    @Test
    fun `countByProfileIdAndIsActive counts only active photos`() {
        val user = userRepository.save(createUser())
        val profile = profileRepository.save(createProfile(user = user))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 1, isActive = true))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 2, isActive = true))
        userPhotoRepository.save(createUserPhoto(profile = profile, displayOrder = 3, isActive = false))
        flushAndClear()

        val activeCount = userPhotoRepository.countByProfileIdAndIsActive(profile.id, true)
        val inactiveCount = userPhotoRepository.countByProfileIdAndIsActive(profile.id, false)

        assertThat(activeCount).isEqualTo(2)
        assertThat(inactiveCount).isEqualTo(1)
    }

    @Test
    fun `findByProfileId returns empty list when no photos`() {
        val photos = userPhotoRepository.findByProfileId(java.util.UUID.randomUUID())

        assertThat(photos).isEmpty()
    }
}
