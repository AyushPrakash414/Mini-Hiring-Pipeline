-- ============================================================================
-- Sample Candidates & Progression Data for Testing
-- ============================================================================

-- 1. Insert Initial Candidates (Stage defaults to APPLIED)
INSERT INTO candidates (name, email, phone, current_stage) VALUES
('Ayush Prakash Tiwari', 'ayush.tiwari@example.com', '+91-9876543210', 'APPLIED'),
('Ayush Kumar',          'ayush.kumar@example.com',   '+91-9876543211', 'APPLIED'),
('Ankit Sharma',         'ankit.sharma@example.com',  '+91-9876543212', 'APPLIED'),
('Priya Sharma',         'priya.sharma@example.com',  '+91-9876543213', 'OFFER');

-- 2. Insert Initial Audit Records for Creation
INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
SELECT id, NULL, 'APPLIED', 'Candidate applied online'
FROM candidates;

-- 3. Simulate Progression for Priya Sharma (ID 4)
INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason) VALUES
(4, 'APPLIED',   'SCREENING', 'Resume shortlisted by recruiter'),
(4, 'SCREENING', 'INTERVIEW', 'Cleared screening call with HR'),
(4, 'INTERVIEW', 'OFFER',     'Passed technical & system design rounds');
