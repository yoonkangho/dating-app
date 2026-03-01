package com.kenneth.datingapp.matching

import com.kenneth.datingapp.matching.internal.MeetingRepository
import com.kenneth.datingapp.matching.internal.OkRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.UserStatus
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class MeetingRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var okRepository: OkRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByOkId returns meeting for ok`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man))
        flushAndClear()

        val found = meetingRepository.findByOkId(ok.id)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(meeting.id)
    }

    @Test
    fun `findByStatus returns meetings with given status`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok1 = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val man2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok2 = okRepository.save(createOk(sender = woman, recipient = man2, status = OkStatus.ACCEPTED))

        meetingRepository.save(createMeeting(ok = ok1, woman = woman, man = man, status = MeetingStatus.PENDING_PROPOSAL))
        meetingRepository.save(createMeeting(ok = ok2, woman = woman, man = man2, status = MeetingStatus.SCHEDULED))
        flushAndClear()

        val pending = meetingRepository.findByStatus(MeetingStatus.PENDING_PROPOSAL)
        assertThat(pending).hasSize(1)
    }

    @Test
    fun `findByParticipantAndStatusIn returns meetings for user`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.SCHEDULED))
        flushAndClear()

        val meetings = meetingRepository.findByParticipantAndStatusIn(woman.id, listOf(MeetingStatus.SCHEDULED, MeetingStatus.PENDING_PROPOSAL))
        assertThat(meetings).hasSize(1)

        val manMeetings = meetingRepository.findByParticipantAndStatusIn(man.id, listOf(MeetingStatus.SCHEDULED))
        assertThat(manMeetings).hasSize(1)
    }
}
