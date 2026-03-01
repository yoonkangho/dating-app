package com.kenneth.datingapp.matching.internal

import com.kenneth.datingapp.matching.MeetingProposal
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MeetingProposalRepository : JpaRepository<MeetingProposal, UUID> {
    fun findByMeetingId(meetingId: UUID): List<MeetingProposal>
    fun findByMeetingIdAndIsSelected(meetingId: UUID, isSelected: Boolean): MeetingProposal?
}
