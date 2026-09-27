-- ============================================================================
-- PostgreSQL Schema for Mini Hiring Pipeline
-- ============================================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS citext;

-- Pipeline Stage Enum (Drop first if exists for idempotent re-runs)
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'candidate_stage') THEN
        CREATE TYPE candidate_stage AS ENUM (
            'APPLIED',
            'SCREENING',
            'INTERVIEW',
            'OFFER',
            'HIRED',
            'REJECTED'
        );
    END IF;
END $$;

-- ============================================================================
-- 1. CANDIDATES TABLE
-- ============================================================================
CREATE TABLE IF NOT EXISTS candidates (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(255)        NOT NULL,
    email           CITEXT              NOT NULL,
    phone           VARCHAR(32),
    current_stage   candidate_stage     NOT NULL DEFAULT 'APPLIED',
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT clock_timestamp(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT uq_candidates_email UNIQUE (email),
    CONSTRAINT chk_candidates_name_non_empty CHECK (length(trim(name)) > 0)
);

-- ============================================================================
-- 2. CANDIDATE STAGE HISTORY (Append-Only Audit Trail)
-- ============================================================================
CREATE TABLE IF NOT EXISTS candidate_stage_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id    BIGINT              NOT NULL,
    from_stage      candidate_stage,    -- NULL for initial application
    to_stage        candidate_stage     NOT NULL,
    reason          TEXT,               -- Optional notes / rejection reason
    changed_at      TIMESTAMPTZ         NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_history_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_valid_stage_transition CHECK (
        -- Initial registration
        (from_stage IS NULL AND to_stage = 'APPLIED')
        -- Forward progression
        OR (from_stage = 'APPLIED'     AND to_stage = 'SCREENING')
        OR (from_stage = 'SCREENING'   AND to_stage = 'INTERVIEW')
        OR (from_stage = 'INTERVIEW'   AND to_stage = 'OFFER')
        OR (from_stage = 'OFFER'       AND to_stage = 'HIRED')
        -- Rejection from any active non-terminal stage
        OR (from_stage IN ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER') AND to_stage = 'REJECTED')
    )
);

-- ============================================================================
-- 3. TRIGGERS & FUNCTIONS
-- ============================================================================

-- Function: Enforce immutability on candidate_stage_history
CREATE OR REPLACE FUNCTION trg_prevent_history_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit trail violation: candidate_stage_history is immutable (UPDATE and DELETE are forbidden).'
        USING ERRCODE = 'data_exception';
END;
$$ LANGUAGE plpgsql;

-- Trigger: Immutability
DROP TRIGGER IF EXISTS trg_stage_history_immutability ON candidate_stage_history;
CREATE TRIGGER trg_stage_history_immutability
BEFORE UPDATE OR DELETE ON candidate_stage_history
FOR EACH ROW
EXECUTE FUNCTION trg_prevent_history_modification();

-- Function: Automatically update updated_at timestamp on candidate modification
CREATE OR REPLACE FUNCTION trg_update_candidate_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = clock_timestamp();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: Candidate updated_at
DROP TRIGGER IF EXISTS trg_candidate_updated_at ON candidates;
CREATE TRIGGER trg_candidate_updated_at
BEFORE UPDATE ON candidates
FOR EACH ROW
EXECUTE FUNCTION trg_update_candidate_timestamp();

-- ============================================================================
-- 4. INDEXES
-- ============================================================================

-- Trigram GIN index for typo-tolerant fuzzy searching on lowercased candidate name
CREATE INDEX IF NOT EXISTS idx_candidates_name_trgm 
ON candidates USING GIN (lower(name) gin_trgm_ops);

-- Stage filtering index (for pipeline status queries and funnel analytics)
CREATE INDEX IF NOT EXISTS idx_candidates_stage 
ON candidates (current_stage);

-- Candidate-specific temporal index for audit logs
CREATE INDEX IF NOT EXISTS idx_stage_history_candidate_temporal 
ON candidate_stage_history (candidate_id, changed_at DESC);

-- Global temporal index for time-range reporting
CREATE INDEX IF NOT EXISTS idx_stage_history_changed_at 
ON candidate_stage_history (changed_at DESC);
