package com.kenneth.datingapp.user

import com.kenneth.datingapp.support.IntegrationTestBase
import com.kenneth.datingapp.support.TestFixtures.createApprovalRequest
import com.kenneth.datingapp.support.TestFixtures.createUser
import com.kenneth.datingapp.user.internal.ApprovalRequestRepository
import com.kenneth.datingapp.user.internal.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ApprovalRequestRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var approvalRequestRepository: ApprovalRequestRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `findByStatus returns requests with matching status`() {
        val user = userRepository.save(createUser())
        approvalRequestRepository.save(createApprovalRequest(user = user, status = ApprovalRequestStatus.PENDING))
        approvalRequestRepository.save(createApprovalRequest(user = user, status = ApprovalRequestStatus.PENDING))
        approvalRequestRepository.save(createApprovalRequest(user = user, status = ApprovalRequestStatus.APPROVED))
        flushAndClear()

        val pending = approvalRequestRepository.findByStatus(ApprovalRequestStatus.PENDING)

        assertThat(pending).hasSize(2)
        assertThat(pending).allMatch { it.status == ApprovalRequestStatus.PENDING }
    }

    @Test
    fun `findByUserIdAndRequestType returns matching requests`() {
        val user = userRepository.save(createUser())
        approvalRequestRepository.save(createApprovalRequest(user = user, requestType = ApprovalRequestType.SIGN_UP))
        approvalRequestRepository.save(createApprovalRequest(user = user, requestType = ApprovalRequestType.PHOTO_ADD))
        approvalRequestRepository.save(createApprovalRequest(user = user, requestType = ApprovalRequestType.PHOTO_ADD))
        flushAndClear()

        val photoRequests = approvalRequestRepository.findByUserIdAndRequestType(user.id, ApprovalRequestType.PHOTO_ADD)

        assertThat(photoRequests).hasSize(2)
        assertThat(photoRequests).allMatch { it.requestType == ApprovalRequestType.PHOTO_ADD }
    }

    @Test
    fun `findByStatusAndRequestType returns pending sign-ups for admin queue`() {
        val user1 = userRepository.save(createUser())
        val user2 = userRepository.save(createUser())
        approvalRequestRepository.save(createApprovalRequest(user = user1, requestType = ApprovalRequestType.SIGN_UP, status = ApprovalRequestStatus.PENDING))
        approvalRequestRepository.save(createApprovalRequest(user = user2, requestType = ApprovalRequestType.PHOTO_ADD, status = ApprovalRequestStatus.PENDING))
        approvalRequestRepository.save(createApprovalRequest(user = user1, requestType = ApprovalRequestType.SIGN_UP, status = ApprovalRequestStatus.APPROVED))
        flushAndClear()

        val pendingSignups = approvalRequestRepository.findByStatusAndRequestType(
            ApprovalRequestStatus.PENDING, ApprovalRequestType.SIGN_UP
        )

        assertThat(pendingSignups).hasSize(1)
        assertThat(pendingSignups[0].requestType).isEqualTo(ApprovalRequestType.SIGN_UP)
        assertThat(pendingSignups[0].status).isEqualTo(ApprovalRequestStatus.PENDING)
    }

    @Test
    fun `findByStatus returns empty list when no matches`() {
        val results = approvalRequestRepository.findByStatus(ApprovalRequestStatus.REJECTED)

        assertThat(results).isEmpty()
    }
}
