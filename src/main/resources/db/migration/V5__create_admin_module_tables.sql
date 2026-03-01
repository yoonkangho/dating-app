-- V5: Creates Admin module tables: admin_users, admin_actions, user_warnings.
-- Also adds deferred FK from disputes.resolved_by → admin_users.
-- Depends on V1 (users table, update_updated_at()), V4 (disputes, feedbacks tables).

-- admin_users: admin accounts with role-based access
CREATE TABLE admin_users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    name          VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(20)  NOT NULL
        CONSTRAINT chk_admin_users_role CHECK (role IN (
            'REVIEWER', 'MODERATOR', 'SUPER_ADMIN'
        )),
    is_active     BOOLEAN     NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX uq_admin_users_email ON admin_users (email);

CREATE TRIGGER trg_admin_users_updated_at
    BEFORE UPDATE ON admin_users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();


-- admin_actions: audit log of all admin operations
CREATE TABLE admin_actions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id    UUID        NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    action_type VARCHAR(40) NOT NULL
        CONSTRAINT chk_admin_actions_action_type CHECK (action_type IN (
            'APPROVAL_REQUEST_APPROVED', 'APPROVAL_REQUEST_REJECTED',
            'USER_RELEASED_FROM_WAITLIST', 'USER_WARNED', 'USER_BANNED',
            'USER_UNBANNED', 'USER_REACTIVATED', 'DISPUTE_RESOLVED', 'DISPUTE_REOPENED'
        )),
    target_type VARCHAR(20) NOT NULL
        CONSTRAINT chk_admin_actions_target_type CHECK (target_type IN (
            'USER', 'APPROVAL_REQUEST', 'DISPUTE'
        )),
    target_id   UUID        NOT NULL,
    details     JSONB,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_actions_admin  ON admin_actions (admin_id);
CREATE INDEX idx_admin_actions_target ON admin_actions (target_type, target_id);


-- user_warnings: warnings issued to users by admins
CREATE TABLE user_warnings (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    issued_by           UUID         NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
    reason              VARCHAR(2000) NOT NULL,
    related_dispute_id  UUID         REFERENCES disputes(id) ON DELETE SET NULL,
    related_feedback_id UUID         REFERENCES feedbacks(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_warnings_user      ON user_warnings (user_id);
CREATE INDEX idx_user_warnings_issued_by ON user_warnings (issued_by);


-- Add deferred FK: disputes.resolved_by → admin_users
-- (V4 left this as a bare UUID column since admin_users didn't exist yet)
ALTER TABLE disputes
    ADD CONSTRAINT fk_disputes_resolved_by FOREIGN KEY (resolved_by) REFERENCES admin_users(id) ON DELETE SET NULL;

-- Add deferred FK: approval_requests.reviewed_by → admin_users
ALTER TABLE approval_requests
    ADD CONSTRAINT fk_approval_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES admin_users(id) ON DELETE SET NULL;
