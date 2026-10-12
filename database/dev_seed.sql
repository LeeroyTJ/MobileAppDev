-- ============================================================
-- CohortHub dev seed data — populated with real team members from TEAM.md
-- Run AFTER database/schema.sql:
--     mysql -u root -p < database/schema.sql
--     mysql -u root -p cohorthub < database/dev_seed.sql
-- Then create the lecturer account with server/scripts/create-lecturer.js
-- (schema.sql drops all tables, so repeat all three steps for a clean reset)
-- ============================================================

USE cohorthub;

-- ------------------------------------------------------------
-- G01: exactly 14 team members from TEAM.md -> 14/15 places taken for Challenge 1 race test
-- G02: 5 students | G03: 3 students | G04: empty
-- 4 Unassigned students (race test candidates + claim code student)
-- ------------------------------------------------------------
INSERT INTO students (programme_id, group_id, student_number, student_name)
SELECT p.programme_id, g.group_id, v.num, v.name
FROM (
    -- Group G01: 14 Team Members from TEAM.md
    SELECT '202406168' AS num, 'Thabo Jumbe'          AS name, 'CS' AS prog, 'G01' AS grp
    UNION ALL SELECT '202403000', 'Josephat Lungu',     'IT', 'G01'
    UNION ALL SELECT '202410125', 'Ezekiel Judge',      'DS', 'G01'
    UNION ALL SELECT '202402976', 'Henry Mapulanga',    'CS', 'G01'
    UNION ALL SELECT '202401120', 'Antonette Kapinga',  'IT', 'G01'
    UNION ALL SELECT '202406183', 'Sylvester Chansa',   'DS', 'G01'
    UNION ALL SELECT '202407636', 'Foster Namukanzye',  'CS', 'G01'
    UNION ALL SELECT '202408068', 'Joel Nduba',         'IT', 'G01'
    UNION ALL SELECT '202301847', 'Emmanuel Njunga',    'DS', 'G01'
    UNION ALL SELECT '202308118', 'Emmanuel Sikubeka',  'CS', 'G01'
    UNION ALL SELECT '202406795', 'Andrew Kalengo',     'IT', 'G01'
    UNION ALL SELECT '202407198', 'Neo Maseba',         'DS', 'G01'
    UNION ALL SELECT '202408447', 'Nathan Kamfwa',      'CS', 'G01'
    UNION ALL SELECT '202410082', 'Joyce Gondwe',       'IT', 'G01'

    -- Group G02
    UNION ALL SELECT '202401015', 'Mwansa Chibale',      'CS', 'G02'
    UNION ALL SELECT '202401016', 'Precious Daka',       'IT', 'G02'
    UNION ALL SELECT '202401017', 'Isaac Njobvu',        'DS', 'G02'
    UNION ALL SELECT '202401018', 'Beauty Sinkala',      'CS', 'G02'
    UNION ALL SELECT '202401019', 'Moses Kaunda',        'IT', 'G02'

    -- Group G03
    UNION ALL SELECT '202401020', 'Dennis Mwape',        'CS', 'G03'
    UNION ALL SELECT '202401021', 'Chileshe Nkonde',     'IT', 'G03'
    UNION ALL SELECT '202401022', 'Loveness Musonda',    'DS', 'G03'

    -- Unassigned Students (Race test candidates + claim code student)
    UNION ALL SELECT '202401023', 'Faith Sikaonga',      'IT', NULL
    UNION ALL SELECT '202401024', 'Brian Tembo',         'DS', NULL
    UNION ALL SELECT '202401025', 'Agnes Phiri',         'CS', NULL
    UNION ALL SELECT '202401099', 'Pretest Student',     'CS', NULL
) v
JOIN programmes p ON p.code = v.prog
LEFT JOIN lab_groups g ON g.group_code = v.grp;

-- ------------------------------------------------------------
-- Claim code for 202401099. Raw code to type in the app: TESTCODE123
-- ------------------------------------------------------------
INSERT INTO claim_codes (student_id, code_hash, expires_at)
SELECT student_id, SHA2('TESTCODE123', 256), DATE_ADD(NOW(), INTERVAL 30 DAY)
FROM students
WHERE student_number = '202401099';
