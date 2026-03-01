package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.feedback.internal.FeedbackRepository
import com.kenneth.datingapp.matching.MeetingStatus
import com.kenneth.datingapp.matching.OkStatus
import com.kenneth.datingapp.matching.internal.MeetingRepository
import com.kenneth.datingapp.matching.internal.OkRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createFeedback
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.UserStatus
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FeedbackRepositoryTest : IntegrationTestBase() {

    @Autowired private lateinit var feedbackRepository: FeedbackRepository
    @Autowired private lateinit var meetingRepository: MeetingRepository
    @Autowired private lateinit var okRepository: OkRepository
    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findByMeetingIdAndSubmittedById returns feedback when exists`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man))
        flushAndClear()

        val found = feedbackRepository.findByMeetingIdAndSubmittedById(meeting.id, woman.id)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(feedback.id)
    }

    @Test
    fun `findByMeetingIdAndSubmittedById returns null when not exists`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        flushAndClear()

        val found = feedbackRepository.findByMeetingIdAndSubmittedById(meeting.id, woman.id)
        assertThat(found).isNull()
    }

    @Test
    fun `existsByMeetingIdAndSubmittedById returns true when feedback exists`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man))
        flushAndClear()

        assertThat(feedbackRepository.existsByMeetingIdAndSubmittedById(meeting.id, woman.id)).isTrue()
    }

    @Test
    fun `existsByMeetingIdAndSubmittedById returns false when no feedback`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        flushAndClear()

        assertThat(feedbackRepository.existsByMeetingIdAndSubmittedById(meeting.id, woman.id)).isFalse()
    }

    @Test
    fun `findBySubmittedById returns all feedback by user`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man2 = userRepository.save(createUser(status = UserStatus.ACTIVE))

        val ok1 = okRepository.save(createOk(sender = woman, recipient = man1, status = OkStatus.ACCEPTED))
        val meeting1 = meetingRepository.save(createMeeting(ok = ok1, woman = woman, man = man1, status = MeetingStatus.COMPLETED))
        feedbackRepository.save(createFeedback(meeting = meeting1, submittedBy = woman, aboutUser = man1))

        val ok2 = okRepository.save(createOk(sender = woman, recipient = man2, status = OkStatus.ACCEPTED))
        val meeting2 = meetingRepository.save(createMeeting(ok = ok2, woman = woman, man = man2, status = MeetingStatus.COMPLETED))
        feedbackRepository.save(createFeedback(meeting = meeting2, submittedBy = woman, aboutUser = man2))
        flushAndClear()

        val results = feedbackRepository.findBySubmittedById(woman.id)
        assertThat(results).hasSize(2)
    }
}
