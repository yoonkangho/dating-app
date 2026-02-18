-- V1: Creates users table and reusable updated_at trigger function.
-- The trigger function is defined here once and reused by all subsequent migrations.

-- Reusable trigger function to auto-update updated_at on row modification
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL,
    phone         VARCHAR(50),
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(30)  NOT NULL DEFAULT 'VERIFICATION_PENDING'
        CONSTRAINT chk_users_status CHECK (status IN (
            'VERIFICATION_PENDING',
            'WAITLISTED',
            'REVIEW_PENDING',
            'ACTIVE',
            'REJECTED',
            'BANNED'
        )),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_users_email  ON users (email);
CREATE        INDEX idx_users_status ON users (status);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
