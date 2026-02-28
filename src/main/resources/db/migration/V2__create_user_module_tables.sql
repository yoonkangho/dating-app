-- V2: Creates User module tables: profiles, user_photos, approval_requests.
-- Depends on V1 (users table and update_updated_at() function).

-- profiles: one-to-one with users; contains all profile information visible to others
CREATE TABLE profiles (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Immutable fields (set once at sign-up)
    display_name   VARCHAR(100) NOT NULL,
    birth_date     DATE         NOT NULL,
    gender         VARCHAR(10)  NOT NULL
        CONSTRAINT chk_profiles_gender CHECK (gender IN ('MALE', 'FEMALE')),
    height_cm      SMALLINT,
    profession     VARCHAR(100),
    education      VARCHAR(20)
        CONSTRAINT chk_profiles_education CHECK (education IS NULL OR education IN (
            'HIGH_SCHOOL', 'BACHELORS', 'MASTERS', 'DOCTORATE'
        )),
    income_bracket VARCHAR(20)
        CONSTRAINT chk_profiles_income_bracket CHECK (income_bracket IS NULL OR income_bracket IN (
            'UNDER_50K', '50K_100K', '100K_150K', '150K_200K', 'OVER_200K'
        )),
    asset_bracket  VARCHAR(20)
        CONSTRAINT chk_profiles_asset_bracket CHECK (asset_bracket IS NULL OR asset_bracket IN (
            'UNDER_100K', '100K_500K', '500K_1M', '1M_5M', 'OVER_5M'
        )),

    -- Mutable fields (require ApprovalRequest)
    bio            TEXT,

    -- Reputation flags (set by system)
    has_undone_ok      BOOLEAN NOT NULL DEFAULT false,
    has_let_ok_expire  BOOLEAN NOT NULL DEFAULT false,

    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ
);

CREATE UNIQUE INDEX idx_profiles_user_id       ON profiles (user_id);
-- Note: active-user filtering is done via JOIN at query time;
-- PostgreSQL does not allow subqueries in partial index predicates.
CREATE        INDEX idx_profiles_gender        ON profiles (gender);

CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();


-- user_photos: photos belonging to a profile; multiple per profile
CREATE TABLE user_photos (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    profile_id    UUID        NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    url           VARCHAR(500) NOT NULL,
    display_order INTEGER      NOT NULL,
    is_primary    BOOLEAN      NOT NULL DEFAULT false,
    is_active     BOOLEAN      NOT NULL DEFAULT false,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE INDEX idx_user_photos_profile_active ON user_photos (profile_id, is_active);

CREATE TRIGGER trg_user_photos_updated_at
    BEFORE UPDATE ON user_photos
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();


-- approval_requests: tracks all user-initiated changes requiring admin review
CREATE TABLE approval_requests (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    request_type  VARCHAR(30) NOT NULL
        CONSTRAINT chk_approval_requests_request_type CHECK (request_type IN (
            'SIGN_UP', 'PHOTO_ADD', 'BIO_CHANGE'
        )),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CONSTRAINT chk_approval_requests_status CHECK (status IN (
            'PENDING', 'APPROVED', 'REJECTED'
        )),
    target_type   VARCHAR(20)
        CONSTRAINT chk_approval_requests_target_type CHECK (target_type IS NULL OR target_type IN (
            'PROFILE', 'PHOTO'
        )),
    target_id     UUID,
    payload       JSONB       NOT NULL,
    reviewed_by   UUID,       -- FK to admin_users(id) added in later migration
    reviewed_at   TIMESTAMPTZ,
    review_notes  VARCHAR(1000),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ
);

CREATE INDEX idx_approval_requests_status ON approval_requests (status, request_type);
CREATE INDEX idx_approval_requests_user   ON approval_requests (user_id);

CREATE TRIGGER trg_approval_requests_updated_at
    BEFORE UPDATE ON approval_requests
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
