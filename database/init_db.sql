-- ============================================================================
-- Mini Hiring Pipeline - Full Database Setup Script
-- ============================================================================

-- Create database if running from default postgres administrative connection:
-- CREATE DATABASE hiring_pipeline_db;
-- \c hiring_pipeline_db

-- Extensions
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS citext;

-- Pipeline Stage Enum
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

-- 1. Candidates Table
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

-- 2. Stage History Audit Table
CREATE TABLE IF NOT EXISTS candidate_stage_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    candidate_id    BIGINT              NOT NULL,
    from_stage      candidate_stage,
    to_stage        candidate_stage     NOT NULL,
    reason          TEXT,
    changed_at      TIMESTAMPTZ         NOT NULL DEFAULT clock_timestamp(),

    CONSTRAINT fk_history_candidate
        FOREIGN KEY (candidate_id)
        REFERENCES candidates(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_valid_stage_transition CHECK (
        (from_stage IS NULL AND to_stage = 'APPLIED')
        OR (from_stage = 'APPLIED'     AND to_stage = 'SCREENING')
        OR (from_stage = 'SCREENING'   AND to_stage = 'INTERVIEW')
        OR (from_stage = 'INTERVIEW'   AND to_stage = 'OFFER')
        OR (from_stage = 'OFFER'       AND to_stage = 'HIRED')
        OR (from_stage IN ('APPLIED', 'SCREENING', 'INTERVIEW', 'OFFER') AND to_stage = 'REJECTED')
    )
);

-- 3. Immutability Trigger
CREATE OR REPLACE FUNCTION trg_prevent_history_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit trail violation: candidate_stage_history is immutable (UPDATE and DELETE are forbidden).'
        USING ERRCODE = 'data_exception';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_stage_history_immutability ON candidate_stage_history;
CREATE TRIGGER trg_stage_history_immutability
BEFORE UPDATE OR DELETE ON candidate_stage_history
FOR EACH ROW
EXECUTE FUNCTION trg_prevent_history_modification();

-- 4. Candidate updated_at Trigger
CREATE OR REPLACE FUNCTION trg_update_candidate_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = clock_timestamp();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_candidate_updated_at ON candidates;
CREATE TRIGGER trg_candidate_updated_at
BEFORE UPDATE ON candidates
FOR EACH ROW
EXECUTE FUNCTION trg_update_candidate_timestamp();

-- 5. Indexes
CREATE INDEX IF NOT EXISTS idx_candidates_name_trgm 
ON candidates USING GIN (lower(name) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_candidates_stage 
ON candidates (current_stage);

CREATE INDEX IF NOT EXISTS idx_stage_history_candidate_temporal 
ON candidate_stage_history (candidate_id, changed_at DESC);

CREATE INDEX IF NOT EXISTS idx_stage_history_changed_at 
ON candidate_stage_history (changed_at DESC);
