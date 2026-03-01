package com.kenneth.datingapp.feedback

import com.kenneth.datingapp.feedback.internal.FeedbackRepository
import com.kenneth.datingapp.feedback.internal.RatingRepository
import com.kenneth.datingapp.matching.MeetingStatus
import com.kenneth.datingapp.matching.OkStatus
import com.kenneth.datingapp.matching.internal.MeetingRepository
import com.kenneth.datingapp.matching.internal.OkRepository
import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createFeedback
import com.kenneth.datingapp.support.TestFixtures.createMeeting
import com.kenneth.datingapp.support.TestFixtures.createOk
import com.kenneth.datingapp.support.TestFixtures.createRating
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.UserStatus
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RatingRepositoryTest : IntegrationTestBase() {

    @Autowired private lateinit var ratingRepository: RatingRepository
    @Autowired private lateinit var feedbackRepository: FeedbackRepository
    @Autowired private lateinit var meetingRepository: MeetingRepository
    @Autowired private lateinit var okRepository: OkRepository
    @Autowired private lateinit var userRepository: UserRepository

    @Test
    fun `findByFeedbackId returns rating`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man))
        val rating = ratingRepository.save(createRating(feedback = feedback, ratedUser = man, ratedBy = woman, looks = 4, attitude = 5))
        flushAndClear()

        val found = ratingRepository.findByFeedbackId(feedback.id)
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(rating.id)
    }

    @Test
    fun `findByFeedbackId returns null when no rating`() {
        val woman = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val ok = okRepository.save(createOk(sender = woman, recipient = man, status = OkStatus.ACCEPTED))
        val meeting = meetingRepository.save(createMeeting(ok = ok, woman = woman, man = man, status = MeetingStatus.COMPLETED))
        val feedback = feedbackRepository.save(createFeedback(meeting = meeting, submittedBy = woman, aboutUser = man))
        flushAndClear()

        val found = ratingRepository.findByFeedbackId(feedback.id)
        assertThat(found).isNull()
    }

    @Test
    fun `averageLooksByRatedUserId returns correct average`() {
        val woman1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val woman2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))

        val ok1 = okRepository.save(createOk(sender = woman1, recipient = man, status = OkStatus.ACCEPTED))
        val meeting1 = meetingRepository.save(createMeeting(ok = ok1, woman = woman1, man = man, status = MeetingStatus.COMPLETED))
        val feedback1 = feedbackRepository.save(createFeedback(meeting = meeting1, submittedBy = woman1, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback1, ratedUser = man, ratedBy = woman1, looks = 3, attitude = 4))

        val ok2 = okRepository.save(createOk(sender = woman2, recipient = man, status = OkStatus.ACCEPTED))
        val meeting2 = meetingRepository.save(createMeeting(ok = ok2, woman = woman2, man = man, status = MeetingStatus.COMPLETED))
        val feedback2 = feedbackRepository.save(createFeedback(meeting = meeting2, submittedBy = woman2, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback2, ratedUser = man, ratedBy = woman2, looks = 5, attitude = 4))
        flushAndClear()

        val avgLooks = ratingRepository.averageLooksByRatedUserId(man.id)
        assertThat(avgLooks).isEqualTo(4.0)
    }

    @Test
    fun `averageAttitudeByRatedUserId returns correct average`() {
        val woman1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val woman2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))

        val ok1 = okRepository.save(createOk(sender = woman1, recipient = man, status = OkStatus.ACCEPTED))
        val meeting1 = meetingRepository.save(createMeeting(ok = ok1, woman = woman1, man = man, status = MeetingStatus.COMPLETED))
        val feedback1 = feedbackRepository.save(createFeedback(meeting = meeting1, submittedBy = woman1, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback1, ratedUser = man, ratedBy = woman1, looks = 3, attitude = 3))

        val ok2 = okRepository.save(createOk(sender = woman2, recipient = man, status = OkStatus.ACCEPTED))
        val meeting2 = meetingRepository.save(createMeeting(ok = ok2, woman = woman2, man = man, status = MeetingStatus.COMPLETED))
        val feedback2 = feedbackRepository.save(createFeedback(meeting = meeting2, submittedBy = woman2, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback2, ratedUser = man, ratedBy = woman2, looks = 5, attitude = 5))
        flushAndClear()

        val avgAttitude = ratingRepository.averageAttitudeByRatedUserId(man.id)
        assertThat(avgAttitude).isEqualTo(4.0)
    }

    @Test
    fun `averageLooksByRatedUserId returns null when no ratings`() {
        val user = userRepository.save(createUser(status = UserStatus.ACTIVE))
        flushAndClear()

        val avg = ratingRepository.averageLooksByRatedUserId(user.id)
        assertThat(avg).isNull()
    }

    @Test
    fun `findByRatedUserId returns all ratings for user`() {
        val woman1 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val woman2 = userRepository.save(createUser(status = UserStatus.ACTIVE))
        val man = userRepository.save(createUser(status = UserStatus.ACTIVE))

        val ok1 = okRepository.save(createOk(sender = woman1, recipient = man, status = OkStatus.ACCEPTED))
        val meeting1 = meetingRepository.save(createMeeting(ok = ok1, woman = woman1, man = man, status = MeetingStatus.COMPLETED))
        val feedback1 = feedbackRepository.save(createFeedback(meeting = meeting1, submittedBy = woman1, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback1, ratedUser = man, ratedBy = woman1))

        val ok2 = okRepository.save(createOk(sender = woman2, recipient = man, status = OkStatus.ACCEPTED))
        val meeting2 = meetingRepository.save(createMeeting(ok = ok2, woman = woman2, man = man, status = MeetingStatus.COMPLETED))
        val feedback2 = feedbackRepository.save(createFeedback(meeting = meeting2, submittedBy = woman2, aboutUser = man))
        ratingRepository.save(createRating(feedback = feedback2, ratedUser = man, ratedBy = woman2))
        flushAndClear()

        val ratings = ratingRepository.findByRatedUserId(man.id)
        assertThat(ratings).hasSize(2)
    }
}
