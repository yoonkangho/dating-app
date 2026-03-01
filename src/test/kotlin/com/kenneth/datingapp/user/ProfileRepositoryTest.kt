package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createProfile
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.internal.ProfileRepository
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import java.time.LocalDate

class ProfileRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var profileRepository: ProfileRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByUserId returns profile when exists`() {
        val user = userRepository.save(createUser())
        profileRepository.save(createProfile(user = user, displayName = "Jane"))
        flushAndClear()

        val found = profileRepository.findByUserId(user.id)

        assertThat(found).isNotNull
        assertThat(found!!.displayName).isEqualTo("Jane")
    }

    @Test
    fun `findByUserId returns null when not exists`() {
        val found = profileRepository.findByUserId(java.util.UUID.randomUUID())

        assertThat(found).isNull()
    }

    @Test
    fun `browse filters by gender and active status`() {
        val activeWoman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val activeMan = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val inactiveWoman = userRepository.save(createUser(status = UserStatus.WAITLISTED))
        profileRepository.save(createProfile(user = activeWoman, gender = Gender.FEMALE))
        profileRepository.save(createProfile(user = activeMan, gender = Gender.MALE))
        profileRepository.save(createProfile(user = inactiveWoman, gender = Gender.FEMALE))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.FEMALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = null, maxHeight = null,
            education = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).hasSize(1)
        assertThat(results.content[0].gender).isEqualTo(Gender.FEMALE)
    }

    @Test
    fun `browse filters by age range via birth date`() {
        val user25 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user35 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user45 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        profileRepository.save(createProfile(user = user25, gender = Gender.FEMALE, birthDate = LocalDate.of(2001, 1, 1)))
        profileRepository.save(createProfile(user = user35, gender = Gender.FEMALE, birthDate = LocalDate.of(1991, 1, 1)))
        profileRepository.save(createProfile(user = user45, gender = Gender.FEMALE, birthDate = LocalDate.of(1981, 1, 1)))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.FEMALE,
            minBirthDate = LocalDate.of(1996, 3, 1),
            maxBirthDate = LocalDate.of(1986, 3, 1),
            minHeight = null, maxHeight = null,
            education = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).hasSize(1)
        assertThat(results.content[0].birthDate).isEqualTo(LocalDate.of(1991, 1, 1))
    }

    @Test
    fun `browse filters by height range`() {
        val user160 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user170 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user180 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        profileRepository.save(createProfile(user = user160, gender = Gender.MALE, heightCm = 160))
        profileRepository.save(createProfile(user = user170, gender = Gender.MALE, heightCm = 170))
        profileRepository.save(createProfile(user = user180, gender = Gender.MALE, heightCm = 180))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.MALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = 165, maxHeight = 175,
            education = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).hasSize(1)
        assertThat(results.content[0].heightCm).isEqualTo(170)
    }

    @Test
    fun `browse filters by education`() {
        val userBa = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val userMa = userRepository.save(createUser(status = UserStatus.ACTIVE))
        profileRepository.save(createProfile(user = userBa, gender = Gender.FEMALE, education = Education.BACHELORS))
        profileRepository.save(createProfile(user = userMa, gender = Gender.FEMALE, education = Education.MASTERS))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.FEMALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = null, maxHeight = null,
            education = Education.MASTERS,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).hasSize(1)
        assertThat(results.content[0].education).isEqualTo(Education.MASTERS)
    }

    @Test
    fun `browse applies multiple filters simultaneously`() {
        // 4 active women with varying attributes
        val user1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user3 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val user4 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        // Matches: age 30, height 170, MASTERS
        profileRepository.save(createProfile(user = user1, gender = Gender.FEMALE, birthDate = LocalDate.of(1996, 1, 1), heightCm = 170, education = Education.MASTERS))
        // Wrong height
        profileRepository.save(createProfile(user = user2, gender = Gender.FEMALE, birthDate = LocalDate.of(1996, 1, 1), heightCm = 155, education = Education.MASTERS))
        // Wrong education
        profileRepository.save(createProfile(user = user3, gender = Gender.FEMALE, birthDate = LocalDate.of(1996, 1, 1), heightCm = 170, education = Education.BACHELORS))
        // Wrong age (too young)
        profileRepository.save(createProfile(user = user4, gender = Gender.FEMALE, birthDate = LocalDate.of(2001, 1, 1), heightCm = 170, education = Education.MASTERS))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.FEMALE,
            minBirthDate = LocalDate.of(1998, 3, 1),   // age >= 28
            maxBirthDate = LocalDate.of(1990, 3, 1),   // age <= 36
            minHeight = 165, maxHeight = 175,
            education = Education.MASTERS,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).hasSize(1)
        assertThat(results.content[0].heightCm).isEqualTo(170)
        assertThat(results.content[0].education).isEqualTo(Education.MASTERS)
    }

    @Test
    fun `browse returns empty page when no profiles match`() {
        val user = userRepository.save(createUser(status = UserStatus.ACTIVE))
        profileRepository.save(createProfile(user = user, gender = Gender.FEMALE, heightCm = 160))
        flushAndClear()

        val results = profileRepository.browse(
            status = UserStatus.ACTIVE,
            gender = Gender.FEMALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = 180, maxHeight = 190,
            education = null,
            pageable = PageRequest.of(0, 10)
        )

        assertThat(results.content).isEmpty()
        assertThat(results.totalElements).isEqualTo(0)
    }

    @Test
    fun `browse paginates correctly`() {
        repeat(15) { i ->
            val user = userRepository.save(createUser(status = UserStatus.ACTIVE))
            profileRepository.save(createProfile(user = user, gender = Gender.FEMALE, displayName = "User $i"))
        }
        flushAndClear()

        val page0 = profileRepository.browse(
            status = UserStatus.ACTIVE, gender = Gender.FEMALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = null, maxHeight = null, education = null,
            pageable = PageRequest.of(0, 10)
        )
        val page1 = profileRepository.browse(
            status = UserStatus.ACTIVE, gender = Gender.FEMALE,
            minBirthDate = null, maxBirthDate = null,
            minHeight = null, maxHeight = null, education = null,
            pageable = PageRequest.of(1, 10)
        )

        assertThat(page0.content).hasSize(10)
        assertThat(page0.totalElements).isEqualTo(15)
        assertThat(page1.content).hasSize(5)
    }
}
