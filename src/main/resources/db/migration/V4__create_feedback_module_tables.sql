-- V4: Creates Feedback module tables: feedbacks, ratings, disputes.
-- Depends on V3 (meetings table).

-- feedbacks: post-meeting feedback from each participant
CREATE TABLE feedbacks (
    id                     UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id             UUID        NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    submitted_by           UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    about_user             UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    meeting_happened       BOOLEAN     NOT NULL,
    no_show_reason         VARCHAR(30)
        CONSTRAINT chk_feedbacks_no_show_reason CHECK (no_show_reason IS NULL OR no_show_reason IN (
            'I_DID_NOT_SHOW', 'THEY_DID_NOT_SHOW', 'MUTUAL_CANCEL', 'OTHER'
        )),

    comment                VARCHAR(2000),

    red_flag_categories    VARCHAR(50)[],
    red_flag_description   VARCHAR(2000),

    wants_contact_exchange BOOLEAN     NOT NULL DEFAULT false,
    contact_shared_at      TIMESTAMPTZ,

    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_feedbacks_different_users CHECK (submitted_by != about_user)
);

CREATE        INDEX idx_feedbacks_meeting      ON feedbacks (meeting_id);
CREATE        INDEX idx_feedbacks_submitted_by ON feedbacks (submitted_by);
-- Unique: one feedback per user per meeting
CREATE UNIQUE INDEX uq_feedbacks_meeting_user  ON feedbacks (meeting_id, submitted_by);


-- ratings: numeric scores given when meeting_happened = true
CREATE TABLE ratings (
    id          UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    feedback_id UUID     NOT NULL REFERENCES feedbacks(id) ON DELETE CASCADE,
    rated_user  UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rated_by    UUID     NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    looks       SMALLINT NOT NULL
        CONSTRAINT chk_ratings_looks CHECK (looks BETWEEN 1 AND 5),
    attitude    SMALLINT NOT NULL
        CONSTRAINT chk_ratings_attitude CHECK (attitude BETWEEN 1 AND 5),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_ratings_feedback   ON ratings (feedback_id);
CREATE        INDEX idx_ratings_rated_user ON ratings (rated_user);


-- disputes: raised when a user reports a no-show or red flag
CREATE TABLE disputes (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    meeting_id       UUID        NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    feedback_id      UUID        NOT NULL REFERENCES feedbacks(id) ON DELETE CASCADE,
    reported_by      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user    UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status           VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CONSTRAINT chk_disputes_status CHECK (status IN (
            'OPEN', 'INVESTIGATING', 'RESOLVED'
        )),
    resolution       VARCHAR(30)
        CONSTRAINT chk_disputes_resolution CHECK (resolution IS NULL OR resolution IN (
            'NO_ACTION', 'WARNING_ISSUED', 'USER_BANNED', 'INSUFFICIENT_EVIDENCE'
        )),
    resolution_notes VARCHAR(2000),
    resolved_by      UUID,       -- FK to admin_users(id) added in later migration
    resolved_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ
);

CREATE INDEX idx_disputes_status        ON disputes (status);
CREATE INDEX idx_disputes_reported_user ON disputes (reported_user);

CREATE TRIGGER trg_disputes_updated_at
    BEFORE UPDATE ON disputes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
