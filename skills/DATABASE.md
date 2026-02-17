# PostgreSQL + JPA Database Skill

Best practices for database design, migrations, and data access with PostgreSQL, Flyway, and Spring Data JPA in Kotlin.

---

## Schema Design Principles

### Naming Conventions

```sql
-- Tables: snake_case, plural
CREATE TABLE users (...);
CREATE TABLE oks (...);
CREATE TABLE meetings (...);

-- Columns: snake_case
user_id, created_at, expires_at, is_active

-- Primary keys: always 'id'
id UUID PRIMARY KEY

-- Foreign keys: <referenced_table_singular>_id
sender_id, recipient_id, meeting_id

-- Indexes: idx_<table>_<columns>
idx_users_email
idx_oks_sender_id_status
idx_meetings_scheduled_at

-- Unique constraints: uq_<table>_<columns>
uq_users_email
uq_oks_sender_recipient_active

-- Check constraints: chk_<table>_<description>
chk_users_status_valid
chk_meetings_scheduled_future
```

### Use UUIDs for Primary Keys

```sql
-- ✅ Good — UUIDs
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ...
);

-- Why UUIDs over serial/bigint:
-- • No enumeration attacks (can't guess /users/2, /users/3)
-- • Safe to expose in URLs and APIs
-- • Easy to generate in application code
-- • No coordination needed for distributed systems
```

### Timestamps with Time Zones

```sql
-- ✅ Always use TIMESTAMPTZ (timestamp with time zone)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ  -- For soft deletes
);

-- ❌ Avoid — TIMESTAMP without time zone
created_at TIMESTAMP  -- Ambiguous, causes timezone bugs
```

### Enums: Use VARCHAR with CHECK Constraints

```sql
-- ✅ Good — VARCHAR with CHECK (flexible, easy to migrate)
CREATE TABLE users (
    status VARCHAR(50) NOT NULL DEFAULT 'VERIFICATION_PENDING'
        CHECK (status IN (
            'VERIFICATION_PENDING',
            'WAITLISTED', 
            'REVIEW_PENDING',
            'ACTIVE',
            'REJECTED'
        ))
);

-- ❌ Avoid — PostgreSQL ENUM type (hard to modify)
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE');  -- Adding values is painful
```

### Soft Deletes vs Hard Deletes

```sql
-- Soft delete pattern — when you need audit trail
CREATE TABLE users (
    id UUID PRIMARY KEY,
    deleted_at TIMESTAMPTZ,  -- NULL means not deleted
    ...
);

-- Add partial index for active records
CREATE INDEX idx_users_active ON users (email) WHERE deleted_at IS NULL;

-- Hard delete — when data shouldn't persist (GDPR, etc.)
-- Just use DELETE, but consider foreign key implications
```

---

## Table Design Patterns

### Base Columns for All Tables

```sql
-- Standard columns every table should have
CREATE TABLE example (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Your columns here
    
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ
);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_example_updated_at
    BEFORE UPDATE ON example
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

### Foreign Keys with Proper Actions

```sql
-- ✅ Good — explicit ON DELETE behavior
CREATE TABLE oks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ...
);

-- Common ON DELETE options:
-- CASCADE    — Delete child when parent deleted (use for owned relationships)
-- RESTRICT   — Prevent parent deletion if children exist (default, safest)
-- SET NULL   — Set FK to NULL when parent deleted (requires nullable FK)
-- NO ACTION  — Like RESTRICT but checked at transaction end
```

### Junction Tables for Many-to-Many

```sql
-- ✅ Good — explicit junction table with metadata
CREATE TABLE user_blocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    blocker_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    UNIQUE (blocker_id, blocked_id),
    CHECK (blocker_id != blocked_id)  -- Can't block yourself
);

CREATE INDEX idx_user_blocks_blocker ON user_blocks (blocker_id);
CREATE INDEX idx_user_blocks_blocked ON user_blocks (blocked_id);
```

### Composite Unique Constraints

```sql
-- Only one active ok between two users
CREATE TABLE oks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sender_id UUID NOT NULL REFERENCES users(id),
    recipient_id UUID NOT NULL REFERENCES users(id),
    status VARCHAR(50) NOT NULL,
    ...
);

-- Partial unique index — only applies to active oks
CREATE UNIQUE INDEX uq_oks_active_pair 
    ON oks (sender_id, recipient_id) 
    WHERE status IN ('PENDING', 'MUTUAL');
```

---

## Indexing Strategy

### Index What You Query

```sql
-- ✅ Index columns used in WHERE, JOIN, ORDER BY
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_email ON users (email);  -- Also for unique lookups

-- ✅ Composite index for multi-column queries
-- Order matters: most selective or equality columns first
CREATE INDEX idx_oks_recipient_status ON oks (recipient_id, status);

-- Query that benefits: 
-- SELECT * FROM oks WHERE recipient_id = ? AND status = 'PENDING'
```

### Partial Indexes for Filtered Queries

```sql
-- ✅ Index only the rows you care about
CREATE INDEX idx_oks_pending ON oks (recipient_id, sent_at) 
    WHERE status = 'PENDING';

-- Much smaller than full index, faster for common query:
-- SELECT * FROM oks WHERE recipient_id = ? AND status = 'PENDING' ORDER BY sent_at
```

### Covering Indexes for Index-Only Scans

```sql
-- ✅ Include columns needed by SELECT to avoid table lookup
CREATE INDEX idx_users_email_status ON users (email) INCLUDE (status, created_at);

-- Query can be satisfied entirely from index:
-- SELECT status, created_at FROM users WHERE email = ?
```

### Don't Over-Index

```sql
-- ❌ Avoid — indexes on low-cardinality columns alone
CREATE INDEX idx_users_is_active ON users (is_active);  -- Only 2 values

-- ❌ Avoid — indexes on rarely-queried columns
CREATE INDEX idx_users_middle_name ON users (middle_name);

-- ❌ Avoid — duplicate indexes
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_email_v2 ON users (email);  -- Redundant
```

---

## Flyway Migrations

### Migration File Naming

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_oks_table.sql
├── V3__create_meetings_table.sql
├── V4__add_user_reputation_columns.sql
├── V5__create_feedback_table.sql
└── V6__add_index_oks_recipient.sql
```

**Naming rules:**
- `V<version>__<description>.sql` (two underscores)
- Version: sequential integers (V1, V2, V3...)
- Description: snake_case, descriptive
- Never modify a migration after it's been applied

### Migration Structure

```sql
-- V1__create_users_table.sql

-- Always start with a comment describing the migration
-- Creates the users table with core profile fields

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'VERIFICATION_PENDING'
        CHECK (status IN (
            'VERIFICATION_PENDING',
            'WAITLISTED',
            'REVIEW_PENDING', 
            'ACTIVE',
            'REJECTED'
        )),
    gender VARCHAR(20) NOT NULL CHECK (gender IN ('MALE', 'FEMALE')),
    
    -- Profile fields
    display_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    height_cm SMALLINT,
    profession VARCHAR(100),
    education VARCHAR(100),
    income_bracket VARCHAR(50),
    bio TEXT,
    
    -- Reputation flags
    has_undone_ok BOOLEAN NOT NULL DEFAULT false,
    has_let_ok_expire BOOLEAN NOT NULL DEFAULT false,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ,
    waitlist_released_at TIMESTAMPTZ,
    approved_at TIMESTAMPTZ
);

-- Indexes
CREATE UNIQUE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_users_gender_status ON users (gender, status) WHERE status = 'ACTIVE';

-- Updated at trigger
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at();
```

### Additive Migrations

```sql
-- V4__add_user_reputation_columns.sql

-- ✅ Good — additive change, backward compatible
ALTER TABLE users 
    ADD COLUMN ok_undo_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN ok_expire_count INTEGER NOT NULL DEFAULT 0;

-- ❌ Avoid in production — destructive changes
ALTER TABLE users DROP COLUMN middle_name;  -- Data loss
ALTER TABLE users ALTER COLUMN email TYPE TEXT;  -- May lock table
```

### Data Migrations

```sql
-- V7__backfill_reputation_counts.sql

-- For data migrations, handle in batches if table is large
UPDATE users 
SET ok_undo_count = (
    SELECT COUNT(*) FROM oks 
    WHERE oks.sender_id = users.id AND oks.undone = true
)
WHERE ok_undo_count = 0;

-- For very large tables, use batching in application code instead
```

### Repeatable Migrations

```sql
-- R__create_views.sql (starts with R__, re-runs when changed)

-- ✅ Good for views, functions, procedures
CREATE OR REPLACE VIEW active_users AS
SELECT id, email, display_name, gender
FROM users
WHERE status = 'ACTIVE';

CREATE OR REPLACE FUNCTION get_user_match_count(user_uuid UUID)
RETURNS INTEGER AS $$
    SELECT COUNT(*)::INTEGER
    FROM oks
    WHERE (sender_id = user_uuid OR recipient_id = user_uuid)
    AND status = 'MUTUAL';
$$ LANGUAGE SQL STABLE;
```

---

## JPA Entity Mapping

### Entity to Table Mapping

```kotlin
@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_email", columnList = "email"),
        Index(name = "idx_users_status", columnList = "status")
    ]
)
class User(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Column(nullable = false, unique = true, length = 255)
    val email: String,
    
    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    var status: UserStatus = UserStatus.VERIFICATION_PENDING,
    
    @Column(name = "display_name", nullable = false, length = 100)
    val displayName: String,
    
    @Column(name = "birth_date", nullable = false)
    val birthDate: LocalDate,
    
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),
    
    @Column(name = "updated_at")
    var updatedAt: Instant? = null
)
```

### Column Type Mappings

| Kotlin Type | PostgreSQL Type | Notes |
|-------------|-----------------|-------|
| `UUID` | `UUID` | Native support in PostgreSQL |
| `String` | `VARCHAR(n)` | Always specify length |
| `String` (long) | `TEXT` | For unbounded text |
| `Int` | `INTEGER` | |
| `Long` | `BIGINT` | |
| `Boolean` | `BOOLEAN` | |
| `BigDecimal` | `NUMERIC(p,s)` | For money/precise decimals |
| `LocalDate` | `DATE` | Date without time |
| `LocalTime` | `TIME` | Time without date |
| `Instant` | `TIMESTAMPTZ` | Always use for timestamps |
| `Enum` | `VARCHAR` | With `@Enumerated(EnumType.STRING)` |

### Relationship Mapping

```kotlin
// Many-to-One (most common)
@Entity
@Table(name = "oks")
class Ok(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    // ✅ Lazy fetch — load only when accessed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    val sender: User,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    val recipient: User,
    
    // ...
)

// ❌ Avoid One-to-Many on entities — query from repository instead
// This causes N+1 problems and loads unnecessary data
@OneToMany(mappedBy = "sender")
val sentOks: List<Ok>  // Don't do this
```

### Embeddables for Value Objects

```kotlin
// ✅ Good — group related fields
@Embeddable
data class Location(
    @Column(name = "location_name", length = 200)
    val name: String,
    
    @Column(name = "location_address", length = 500)
    val address: String,
    
    @Column(name = "location_lat")
    val latitude: Double?,
    
    @Column(name = "location_lng")  
    val longitude: Double?
)

@Entity
@Table(name = "meetings")
class Meeting(
    @Id
    val id: UUID = UUID.randomUUID(),
    
    @Embedded
    var location: Location,
    
    // ...
)

-- Results in columns: location_name, location_address, location_lat, location_lng
```

---

## Repository Patterns

### Basic Repository

```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    // Derived queries — Spring generates SQL
    fun findByEmail(email: String): User?
    fun findByStatus(status: UserStatus): List<User>
    fun findByStatusAndGender(status: UserStatus, gender: Gender): List<User>
    fun existsByEmail(email: String): Boolean
    fun countByStatus(status: UserStatus): Long
}
```

### Custom Queries with JPQL

```kotlin
interface OkRepository : JpaRepository<Ok, UUID> {
    // JPQL — use entity/field names, not table/column names
    @Query("""
        SELECT o FROM Ok o 
        WHERE o.recipient.id = :recipientId 
        AND o.status = 'PENDING'
        ORDER BY o.sentAt ASC
    """)
    fun findPendingOksForRecipient(@Param("recipientId") recipientId: UUID): List<Ok>
    
    // With eager fetch to avoid N+1
    @Query("""
        SELECT o FROM Ok o
        JOIN FETCH o.sender
        WHERE o.recipient.id = :recipientId
        AND o.status = 'PENDING'
    """)
    fun findPendingOksWithSender(@Param("recipientId") recipientId: UUID): List<Ok>
}
```

### Native Queries When Needed

```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    // Native SQL — for PostgreSQL-specific features
    @Query("""
        SELECT u.* FROM users u
        WHERE u.status = 'ACTIVE'
        AND u.gender = :gender
        AND u.id NOT IN (
            SELECT blocked_id FROM user_blocks WHERE blocker_id = :userId
        )
        ORDER BY u.created_at DESC
        LIMIT :limit
    """, nativeQuery = true)
    fun findMatchCandidates(
        @Param("userId") userId: UUID,
        @Param("gender") gender: String,
        @Param("limit") limit: Int
    ): List<User>
}
```

### Projections for Partial Data

```kotlin
// ✅ Interface projection — only fetches needed columns
interface UserSummary {
    val id: UUID
    val displayName: String
    val birthDate: LocalDate
}

interface UserRepository : JpaRepository<User, UUID> {
    fun findSummaryByStatus(status: UserStatus): List<UserSummary>
}

// ✅ DTO projection with JPQL
data class UserMatchCard(
    val id: UUID,
    val displayName: String,
    val age: Int,
    val profession: String?
)

@Query("""
    SELECT new com.kenneth.datingapp.user.UserMatchCard(
        u.id, u.displayName, 
        YEAR(CURRENT_DATE) - YEAR(u.birthDate),
        u.profession
    )
    FROM User u
    WHERE u.status = 'ACTIVE' AND u.gender = :gender
""")
fun findMatchCards(@Param("gender") gender: Gender): List<UserMatchCard>
```

---

## Query Optimization

### Avoid N+1 Queries

```kotlin
// ❌ Bad — N+1 problem
val oks = okRepository.findByRecipientId(userId)
oks.forEach { ok ->
    println(ok.sender.displayName)  // Each access triggers a query
}

// ✅ Good — fetch join
@Query("SELECT o FROM Ok o JOIN FETCH o.sender WHERE o.recipient.id = :recipientId")
fun findWithSender(@Param("recipientId") recipientId: UUID): List<Ok>

// ✅ Good — entity graph
@EntityGraph(attributePaths = ["sender"])
fun findByRecipientId(recipientId: UUID): List<Ok>
```

### Pagination

```kotlin
interface UserRepository : JpaRepository<User, UUID> {
    // ✅ Paginated queries
    fun findByStatus(status: UserStatus, pageable: Pageable): Page<User>
    
    // ✅ Slice for infinite scroll (doesn't count total)
    fun findByGenderAndStatus(
        gender: Gender, 
        status: UserStatus, 
        pageable: Pageable
    ): Slice<User>
}

// Usage
val pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending())
val users = userRepository.findByStatus(UserStatus.ACTIVE, pageable)
```

### Batch Operations

```kotlin
// ✅ Batch insert with JPA
@Transactional
fun createUsers(users: List<User>) {
    users.chunked(50).forEach { batch ->
        batch.forEach { userRepository.save(it) }
        entityManager.flush()
        entityManager.clear()  // Prevent memory issues
    }
}

// ✅ Bulk update with JPQL (bypasses entity lifecycle)
@Modifying
@Query("UPDATE Ok o SET o.status = 'EXPIRED' WHERE o.expiresAt < :now AND o.status = 'PENDING'")
fun expirePendingOks(@Param("now") now: Instant): Int
```

---

## Transaction Management

### Service-Level Transactions

```kotlin
@Service
class MatchingService(
    private val okRepository: OkRepository,
    private val userRepository: UserRepository
) {
    // ✅ Transaction at service method level
    @Transactional
    fun sendOk(senderId: UUID, recipientId: UUID): OkResult {
        // All operations in single transaction
        val sender = userRepository.findByIdOrNull(senderId) ?: return OkResult.NotFound
        val recipient = userRepository.findByIdOrNull(recipientId) ?: return OkResult.NotFound
        
        val ok = Ok(sender = sender, recipient = recipient)
        okRepository.save(ok)
        
        return OkResult.Success(ok)
    }
    
    // ✅ Read-only transaction for queries
    @Transactional(readOnly = true)
    fun getPendingOks(userId: UUID): List<Ok> {
        return okRepository.findPendingOksForRecipient(userId)
    }
}
```

### Transaction Propagation

```kotlin
// Default: REQUIRED — join existing or create new
@Transactional
fun outerMethod() {
    innerMethod()  // Shares same transaction
}

// REQUIRES_NEW — always create new transaction
@Transactional(propagation = Propagation.REQUIRES_NEW)
fun auditLog(event: AuditEvent) {
    // Commits independently, even if outer transaction rolls back
}

// NOT_SUPPORTED — suspend transaction
@Transactional(propagation = Propagation.NOT_SUPPORTED)
fun sendNotification() {
    // Runs without transaction (external API call)
}
```

---

## Common Pitfalls

### 1. Don't Use @GeneratedValue with UUID

```kotlin
// ❌ Bad — unnecessary, slower
@Id
@GeneratedValue
val id: UUID

// ✅ Good — generate in code or use database default
@Id
val id: UUID = UUID.randomUUID()
```

### 2. Always Specify Column Length for VARCHAR

```kotlin
// ❌ Bad — defaults to VARCHAR(255)
@Column
val profession: String

// ✅ Good — explicit length
@Column(length = 100)
val profession: String
```

### 3. Use Instant, Not LocalDateTime

```kotlin
// ❌ Bad — no timezone information
val createdAt: LocalDateTime

// ✅ Good — unambiguous point in time
val createdAt: Instant
```

### 4. Don't Expose Entity IDs Before Persist

```kotlin
// ❌ Bad — ID might not be set yet
fun createAndNotify(user: User) {
    userRepository.save(user)
    notificationService.notify(user.id)  // What if save failed?
}

// ✅ Good — use returned entity
fun createAndNotify(user: User) {
    val saved = userRepository.save(user)
    notificationService.notify(saved.id)
}
```

### 5. Index Foreign Keys

```sql
-- ❌ Missing — slow JOINs and ON DELETE CASCADE
CREATE TABLE oks (
    sender_id UUID REFERENCES users(id),
    recipient_id UUID REFERENCES users(id)
);

-- ✅ Good — always index FKs
CREATE INDEX idx_oks_sender_id ON oks (sender_id);
CREATE INDEX idx_oks_recipient_id ON oks (recipient_id);
```

### 6. Don't Lock Tables with Long Transactions

```kotlin
// ❌ Bad — holds locks during external call
@Transactional
fun processUser(id: UUID) {
    val user = userRepository.findById(id)
    externalApiClient.verify(user)  // Slow external call
    user.status = VERIFIED
    userRepository.save(user)
}

// ✅ Good — minimize transaction scope
fun processUser(id: UUID) {
    val user = userRepository.findById(id)
    val result = externalApiClient.verify(user)  // Outside transaction
    updateUserStatus(id, result)
}

@Transactional
fun updateUserStatus(id: UUID, status: Status) {
    // Quick DB operation only
}
```
