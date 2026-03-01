-- V3: Creates Matching module tables: oks, meetings, meeting_proposals, meeting_events.
-- Depends on V1 (users table and update_updated_at() function).

-- oks: represents a woman expressing interest in a man
CREATE TABLE oks (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_oks_status CHECK (status IN (
            'PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'UNDONE'
        )),
    sent_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at    TIMESTAMPTZ NOT NULL,
    responded_at  TIMESTAMPTZ,
    undone_by     UUID        REFERENCES users(id) ON DELETE SET NULL,
    undone_at     TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ,

    CONSTRAINT chk_oks_different_users CHECK (sender_id != recipient_id)
);

-- One ok per pair (woman can only send one ok to a specific man ever)
CREATE UNIQUE INDEX uq_oks_sender_recipient ON oks (sender_id, recipient_id);
CREATE        INDEX idx_oks_sender_status    ON oks (sender_id, status);
CREATE        INDEX idx_oks_recipient_status ON oks (recipient_id, status);
CREATE        INDEX idx_oks_expires_at       ON oks (expires_at);

CREATE TRIGGER trg_oks_updated_at
    BEFORE UPDATE ON oks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();


-- meetings: created when an ok is accepted; tracks the meeting lifecycle
CREATE TABLE meetings (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    ok_id                  UUID        NOT NULL REFERENCES oks(id) ON DELETE CASCADE,
    woman_id               UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    man_id                 UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status                 VARCHAR(30) NOT NULL DEFAULT 'PENDING_PROPOSAL'
        CONSTRAINT chk_meetings_status CHECK (status IN (
            'PENDING_PROPOSAL', 'PENDING_FINALIZATION', 'SCHEDULED', 'COMPLETED', 'CANCELLED'
        )),
    proposal_deadline      TIMESTAMPTZ NOT NULL,
    finalization_deadline  TIMESTAMPTZ,
    meeting_deadline       TIMESTAMPTZ NOT NULL,
    scheduled_at           TIMESTAMPTZ,
    location_name          VARCHAR(200),
    location_address       VARCHAR(500),
    is_rescheduled         BOOLEAN     NOT NULL DEFAULT false,
    cancelled_by           UUID        REFERENCES users(id) ON DELETE SET NULL,
    cancelled_at           TIMESTAMPTZ,
    cancellation_reason    VARCHAR(50)
        CONSTRAINT chk_meetings_cancellation_reason CHECK (cancellation_reason IS NULL OR cancellation_reason IN (
            'PROPOSAL_DEADLINE_MISSED', 'FINALIZATION_DEADLINE_MISSED',
            'USER_CANCELLED', 'UNDO_BY_WOMAN', 'UNDO_BY_MAN'
        )),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_meetings_ok_id     ON meetings (ok_id);
CREATE        INDEX idx_meetings_status    ON meetings (status);
CREATE        INDEX idx_meetings_woman_id  ON meetings (woman_id);
CREATE        INDEX idx_meetings_man_id    ON meetings (man_id);
CREATE        INDEX idx_meetings_deadlines ON meetings (proposal_deadline, finalization_deadline);

CREATE TRIGGER trg_meetings_updated_at
    BEFORE UPDATE ON meetings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();


-- meeting_proposals: proposed dates/locations for a meeting
CREATE TABLE meeting_proposals (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id       UUID        NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    proposed_date    TIMESTAMPTZ NOT NULL,
    proposed_location VARCHAR(500),
    is_selected      BOOLEAN     NOT NULL DEFAULT false,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_proposals_meeting ON meeting_proposals (meeting_id);


-- meeting_events: audit log of all events in a meeting lifecycle
CREATE TABLE meeting_events (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id  UUID        NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    event_type  VARCHAR(30) NOT NULL
        CONSTRAINT chk_meeting_events_event_type CHECK (event_type IN (
            'PROPOSAL_SUBMITTED', 'MEETING_FINALIZED', 'RESCHEDULE_REQUESTED',
            'MEETING_CANCELLED', 'MEETING_COMPLETED'
        )),
    actor_id    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_meeting_events_meeting ON meeting_events (meeting_id);
