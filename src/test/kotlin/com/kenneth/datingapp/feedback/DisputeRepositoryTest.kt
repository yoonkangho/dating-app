package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.feedback.internal.DisputeRepository
import com.kenneth.datingapp.feedback.internal.FeedbackRepository
import com.kenneth.datingapp.matching.MeetingStatus
import com.kenneth.datingapp.matching.OkStatus
import com.kenneth.datingapp.matching.internal.MeetingRepository
import com.kenneth.datingapp.matching.internal.OkRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createDispute
import com.kenneth.datingapp.support.TestFixtures.createFeedback
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.UserStatus
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class DisputeRepositoryTest : IntegrationTestBase() {

    @Autowired private lateinit var disputeRepository: DisputeRepository
    @Autowired private lateinit var feedbackRepository: FeedbackRepository
    @Autowired private lateinit var meetingRepository: MeetingRepository
    @Autowired private lateinit var okRepository: OkRepository
    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findByStatus returns disputes with given status`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man, meetingHappened = false, noShowReason = NoShowReason.THEY_DID_NOT_SHOW))
        disputeRepository.save(createDispute(meeting = meeting, feedback = feedback, reportedBy = woman, reportedUser = man))
        flushAndClear()

        val open = disputeRepository.findByStatus(DisputeStatus.OPEN)
        assertThat(open).hasSize(1)

        val resolved = disputeRepository.findByStatus(DisputeStatus.RESOLVED)
        assertThat(resolved).isEmpty()
    }

    @Test
    fun `findByMeetingId returns disputes for meeting`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man, meetingHappened = false, noShowReason = NoShowReason.THEY_DID_NOT_SHOW))
        disputeRepository.save(createDispute(meeting = meeting, feedback = feedback, reportedBy = woman, reportedUser = man))
        flushAndClear()

        assertThat(disputeRepository.findByMeetingId(meeting.id)).hasSize(1)
    }

    @Test
    fun `findByReportedUserId returns disputes for user`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man, meetingHappened = false, noShowReason = NoShowReason.THEY_DID_NOT_SHOW))
        disputeRepository.save(createDispute(meeting = meeting, feedback = feedback, reportedBy = woman, reportedUser = man))
        flushAndClear()

        assertThat(disputeRepository.findByReportedUserId(man.id)).hasSize(1)
        assertThat(disputeRepository.findByReportedUserId(woman.id)).isEmpty()
    }

    @Test
    fun `findByReportedById returns disputes filed by user`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man, meetingHappened = false, noShowReason = NoShowReason.THEY_DID_NOT_SHOW))
        disputeRepository.save(createDispute(meeting = meeting, feedback = feedback, reportedBy = woman, reportedUser = man))
        flushAndClear()

        assertThat(disputeRepository.findByReportedById(woman.id)).hasSize(1)
        assertThat(disputeRepository.findByReportedById(man.id)).isEmpty()
    }
}
