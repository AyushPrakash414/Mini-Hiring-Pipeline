-- ============================================================================
-- 50 Diverse Candidate Entries & Audit Histories for Testing
-- ============================================================================

DO $$
DECLARE
    rec RECORD;
    new_cid BIGINT;
BEGIN
    -- Temporary table to hold candidate definitions
    CREATE TEMP TABLE tmp_candidates (
        name TEXT,
        email TEXT,
        phone TEXT,
        final_stage candidate_stage
    ) ON COMMIT DROP;

    INSERT INTO tmp_candidates (name, email, phone, final_stage) VALUES
    ('Rohan Verma',          'rohan.verma@example.com',        '+91-9876543220', 'APPLIED'),
    ('Sneha Patel',          'sneha.patel@example.com',        '+91-9876543221', 'SCREENING'),
    ('Vikram Singh',         'vikram.singh@example.com',       '+91-9876543222', 'INTERVIEW'),
    ('Aarav Gupta',          'aarav.gupta@example.com',        '+91-9876543223', 'OFFER'),
    ('Ishita Roy',           'ishita.roy@example.com',          '+91-9876543224', 'HIRED'),
    ('Mohammed Ali',         'mohammed.ali@example.com',       '+91-9876543225', 'REJECTED'),
    ('Neha Reddy',           'neha.reddy@example.com',         '+91-9876543226', 'SCREENING'),
    ('Aditya Mishra',        'aditya.mishra@example.com',      '+91-9876543227', 'INTERVIEW'),
    ('Tanvi Deshmukh',       'tanvi.deshmukh@example.com',     '+91-9876543228', 'OFFER'),
    ('Rahul Nair',           'rahul.nair@example.com',         '+91-9876543229', 'HIRED'),
    ('Pooja Joshi',          'pooja.joshi@example.com',        '+91-9876543230', 'REJECTED'),
    ('Karan Mehta',          'karan.mehta@example.com',        '+91-9876543231', 'APPLIED'),
    ('Ananya Iyer',          'ananya.iyer@example.com',        '+91-9876543232', 'SCREENING'),
    ('Siddharth Rao',        'siddharth.rao@example.com',      '+91-9876543233', 'INTERVIEW'),
    ('Divya Choudhury',      'divya.choudhury@example.com',    '+91-9876543234', 'OFFER'),
    ('Manish Bhatia',        'manish.bhatia@example.com',      '+91-9876543235', 'HIRED'),
    ('Kavita Saxena',        'kavita.saxena@example.com',      '+91-9876543236', 'REJECTED'),
    ('Harshvardhan Jain',    'harsh.jain@example.com',         '+91-9876543237', 'APPLIED'),
    ('Ritu Aggarwal',        'ritu.aggarwal@example.com',      '+91-9876543238', 'SCREENING'),
    ('Gaurav Kulkarni',      'gaurav.kulkarni@example.com',    '+91-9876543239', 'INTERVIEW'),
    ('Meera Pillai',         'meera.pillai@example.com',       '+91-9876543240', 'OFFER'),
    ('Varun Nambiar',        'varun.nambiar@example.com',      '+91-9876543241', 'HIRED'),
    ('Deepika Sen',          'deepika.sen@example.com',        '+91-9876543242', 'REJECTED'),
    ('Abhishek Banerjee',    'abhishek.banerjee@example.com',  '+91-9876543243', 'APPLIED'),
    ('Shreya Bhattacharya',  'shreya.bhatt@example.com',       '+91-9876543244', 'SCREENING'),
    ('Nitin Chauhan',        'nitin.chauhan@example.com',      '+91-9876543245', 'INTERVIEW'),
    ('Swati Mathur',         'swati.mathur@example.com',       '+91-9876543246', 'OFFER'),
    ('Akash Pandey',         'akash.pandey@example.com',       '+91-9876543247', 'HIRED'),
    ('Simran Kaur',          'simran.kaur@example.com',        '+91-9876543248', 'REJECTED'),
    ('Rajeshwari Hegde',     'rajeshwari.hegde@example.com',   '+91-9876543249', 'APPLIED'),
    ('Kunal Kapoor',         'kunal.kapoor@example.com',       '+91-9876543250', 'SCREENING'),
    ('Pallavi Trivedi',      'pallavi.trivedi@example.com',    '+91-9876543251', 'INTERVIEW'),
    ('Arjun Sengupta',       'arjun.sengupta@example.com',     '+91-9876543252', 'OFFER'),
    ('Geeta Menon',          'geeta.menon@example.com',        '+91-9876543253', 'HIRED'),
    ('Naveen Srinivasan',    'naveen.srini@example.com',       '+91-9876543254', 'REJECTED'),
    ('Sanya Malhotra',       'sanya.malhotra@example.com',     '+91-9876543255', 'APPLIED'),
    ('Vishal Bhardwaj',      'vishal.bhardwaj@example.com',    '+91-9876543256', 'SCREENING'),
    ('Sunita Das',           'sunita.das@example.com',         '+91-9876543257', 'INTERVIEW'),
    ('Pranav Mukhopadhyay',  'pranav.mukho@example.com',       '+91-9876543258', 'OFFER'),
    ('Aishwarya Rai',        'aishwarya.rai@example.com',      '+91-9876543259', 'HIRED'),
    ('Devendra Rathore',     'devendra.rathore@example.com',   '+91-9876543260', 'REJECTED'),
    ('Bhavna Goswami',       'bhavna.goswami@example.com',     '+91-9876543261', 'APPLIED'),
    ('Sanjay Singhania',     'sanjay.singhania@example.com',   '+91-9876543262', 'SCREENING'),
    ('Archana Ghosh',        'archana.ghosh@example.com',      '+91-9876543263', 'INTERVIEW'),
    ('Yashwardhan Shukla',   'yash.shukla@example.com',        '+91-9876543264', 'OFFER'),
    ('Payal Somani',         'payal.somani@example.com',       '+91-9876543265', 'HIRED'),
    ('Tarun Grover',         'tarun.grover@example.com',       '+91-9876543266', 'REJECTED'),
    ('Latika Sethi',         'latika.sethi@example.com',       '+91-9876543267', 'APPLIED'),
    ('Chetan Bhagat',        'chetan.bhagat@example.com',      '+91-9876543268', 'SCREENING'),
    ('Rashmi Vohra',         'rashmi.vohra@example.com',       '+91-9876543269', 'INTERVIEW');

    -- Iterate and insert each candidate + valid stage progression
    FOR rec IN SELECT name, email, phone, final_stage FROM tmp_candidates LOOP
        new_cid := NULL;
        
        -- 1. Insert Candidate with current_stage = final_stage
        INSERT INTO candidates (name, email, phone, current_stage)
        VALUES (rec.name, rec.email, rec.phone, rec.final_stage)
        ON CONFLICT (email) DO NOTHING
        RETURNING id INTO new_cid;

        IF new_cid IS NOT NULL THEN
            -- Initial Application History
            INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
            VALUES (new_cid, NULL, 'APPLIED', 'Application submitted via careers portal');

            -- Stage progression based on final stage
            IF rec.final_stage IN ('SCREENING', 'INTERVIEW', 'OFFER', 'HIRED') THEN
                INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
                VALUES (new_cid, 'APPLIED', 'SCREENING', 'Resume shortlisted by recruiter');
            END IF;

            IF rec.final_stage IN ('INTERVIEW', 'OFFER', 'HIRED') THEN
                INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
                VALUES (new_cid, 'SCREENING', 'INTERVIEW', 'Passed initial recruiter screening');
            END IF;

            IF rec.final_stage IN ('OFFER', 'HIRED') THEN
                INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
                VALUES (new_cid, 'INTERVIEW', 'OFFER', 'Cleared technical and leadership interview loops');
            END IF;

            IF rec.final_stage = 'HIRED' THEN
                INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
                VALUES (new_cid, 'OFFER', 'HIRED', 'Offer letter accepted and onboarding initiated');
            END IF;

            IF rec.final_stage = 'REJECTED' THEN
                INSERT INTO candidate_stage_history (candidate_id, from_stage, to_stage, reason)
                VALUES (new_cid, 'APPLIED', 'REJECTED', 'Profile not aligning with current requirements');
            END IF;
        END IF;
    END LOOP;
END $$;
