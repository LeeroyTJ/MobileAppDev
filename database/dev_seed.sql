-- ============================================================
-- CohortHub dev seed — real class list (14 groups, G14 excluded)
-- Group 14 (our team) is intentionally missing — team members
-- register through the app and are assigned by a lecturer.
--
-- Over-capacity resolution: groups with >15 students have the
-- excess moved to Unassigned, so capacity rule is preserved.
--
-- Duplicate student numbers in Group 11: only the first
-- occurrence is inserted; the rest are commented out pending
-- lecturer confirmation.
-- ============================================================

USE cohorthub;

-- Programme codes are CS, IT, DS (from schema.sql).
-- No DS students in the class list.

INSERT INTO students (programme_id, group_id, student_number, student_name)
SELECT p.programme_id, g.group_id, v.num, v.name
FROM (
    -- ---- G01: 15 CS, 1 CS moved to Unassigned ----
    SELECT '202402387' AS num, 'Mulemwa Muchima'           AS name, 'CS' AS prog, 'G01' AS grp
    UNION ALL SELECT '202406643', 'Edwin Kampamba',         'CS', 'G01'
    UNION ALL SELECT '202400442', 'Ivwananji Chipasha',     'CS', 'G01'
    UNION ALL SELECT '202305208', 'Alinani Sinyangwe',      'CS', 'G01'
    UNION ALL SELECT '202408399', 'Grace Chilufya',         'CS', 'G01'
    UNION ALL SELECT '202402615', 'Charity Namangolwa',     'CS', 'G01'
    UNION ALL SELECT '202404528', 'Memory Mutemwa',         'CS', 'G01'
    UNION ALL SELECT '202409048', 'Mwila Siaciti',          'CS', 'G01'
    UNION ALL SELECT '202402859', 'Hope Nsana',             'CS', 'G01'
    UNION ALL SELECT '202403063', 'Judith Mushabati',       'CS', 'G01'
    UNION ALL SELECT '202406887', 'Mcnaldo Chilanga',       'CS', 'G01'
    UNION ALL SELECT '202403266', 'Collins Kalela',         'CS', 'G01'
    UNION ALL SELECT '202409163', 'Nancy Mailoni',          'CS', 'G01'
    UNION ALL SELECT '202408720', 'Onester Kalunga',        'CS', 'G01'
    UNION ALL SELECT '202409779', 'Mayamiko Phiri',         'CS', 'G01'
    -- 16th student moved to Unassigned (capacity is 15)
    UNION ALL SELECT '202408882', 'Atupele Andende',        'CS', NULL

    -- ---- G02: 15 CS ----
    UNION ALL SELECT '202400549', 'Joseph Chiwuswa',        'CS', 'G02'
    UNION ALL SELECT '202400526', 'Maluba Bakakandelwa',    'CS', 'G02'
    UNION ALL SELECT '202403134', 'Emmanuel Viazi',         'CS', 'G02'
    UNION ALL SELECT '202408701', 'Memory Tembo',           'CS', 'G02'
    UNION ALL SELECT '202401737', 'Salome Sitwala',         'CS', 'G02'
    UNION ALL SELECT '202405690', 'Mwansa Mubanga',         'CS', 'G02'
    UNION ALL SELECT '202404540', 'Paul Munene',            'CS', 'G02'
    UNION ALL SELECT '202408808', 'Blessings Sinkamba',     'CS', 'G02'
    UNION ALL SELECT '202308808', 'Joshua Chibale',         'CS', 'G02'
    UNION ALL SELECT '202408439', 'Blessings Tembo',        'CS', 'G02'
    UNION ALL SELECT '202402633', 'Mwaka Hamilemba',        'CS', 'G02'
    UNION ALL SELECT '202408554', 'Luumbi Malambo',         'CS', 'G02'
    UNION ALL SELECT '202406948', 'Liseli Faustina Kalowa', 'CS', 'G02'
    UNION ALL SELECT '202407170', 'Lute Walusungu Gondwe',  'CS', 'G02'
    UNION ALL SELECT '202405590', 'Matildah Yusiku',        'CS', 'G02'

    -- ---- G03: 14 mixed programmes ----
    UNION ALL SELECT '202404194', 'Gift Chabuka',           'CS', 'G03'
    UNION ALL SELECT '202307232', 'Ruth Kangwa',            'CS', 'G03'
    UNION ALL SELECT '202406919', 'Jason Mbao',             'CS', 'G03'
    UNION ALL SELECT '202404164', 'Chiko Mutale',           'CS', 'G03'
    UNION ALL SELECT '202407862', 'Fabian Mulongo',         'CS', 'G03'
    UNION ALL SELECT '202402069', 'Richard Mwansa',         'CS', 'G03'
    UNION ALL SELECT '202410371', 'Memory Mwela',           'CS', 'G03'
    UNION ALL SELECT '202406543', 'Gillian Mpola',          'IT', 'G03'
    UNION ALL SELECT '201901101', 'Christabel Chikomona',   'IT', 'G03'
    UNION ALL SELECT '202301795', 'Steven Zulu',            'CS', 'G03'
    UNION ALL SELECT '202409282', 'Kapunza Gwenani',        'CS', 'G03'
    UNION ALL SELECT '202408164', 'Lenganji Simwanza',      'CS', 'G03'
    UNION ALL SELECT '202403669', 'Eustane Chulu',          'CS', 'G03'
    UNION ALL SELECT '202401965', 'Erastas Michelo',        'CS', 'G03'

    -- ---- G04: 15 mixed, 1 moved to Unassigned ----
    UNION ALL SELECT '202405342', 'Manda Dickson',          'CS', 'G04'
    UNION ALL SELECT '202405228', 'Nemar Kanyondwi',        'CS', 'G04'
    UNION ALL SELECT '202402427', 'Michael Mutale',         'IT', 'G04'
    UNION ALL SELECT '202402839', 'Sikumbuzo Monde',        'CS', 'G04'
    UNION ALL SELECT '202107598', 'Cleopatra Ngulube',      'CS', 'G04'
    UNION ALL SELECT '202404130', 'Hellen Mwanza',          'CS', 'G04'
    UNION ALL SELECT '202306274', 'Lloyd Bulaya',           'CS', 'G04'
    UNION ALL SELECT '202407149', 'Gedan Simbeye',          'CS', 'G04'
    UNION ALL SELECT '202408185', 'Muleba Mwashi',          'CS', 'G04'
    UNION ALL SELECT '202402132', 'Nkonde Zimba',           'IT', 'G04'
    UNION ALL SELECT '202105977', 'Memory Sokontwe',        'IT', 'G04'
    UNION ALL SELECT '202408941', 'David Kanyama',          'CS', 'G04'
    UNION ALL SELECT '202302576', 'Frackson Mkandawire',    'IT', 'G04'
    UNION ALL SELECT '202203671', 'Andrew Moyo',            'CS', 'G04'
    UNION ALL SELECT '202404135', 'Thabo Muliwana',         'CS', 'G04'
    -- 16th student moved to Unassigned
    UNION ALL SELECT '202402261', 'Mwila Stephen',          'CS', NULL

    -- ---- G05: 15 CS ----
    UNION ALL SELECT '202406213', 'Simazuwo Brian',         'CS', 'G05'
    UNION ALL SELECT '202406222', 'Sanalumba Wakun''oli',   'CS', 'G05'
    UNION ALL SELECT '202406591', 'David Mbilima',          'CS', 'G05'
    UNION ALL SELECT '202410059', 'Tembo Kunda',            'CS', 'G05'
    UNION ALL SELECT '202407895', 'Chabu Chileleko',        'CS', 'G05'
    UNION ALL SELECT '202400756', 'Nachinga Thelma',        'CS', 'G05'
    UNION ALL SELECT '202406797', 'Kamwana Miriam',         'CS', 'G05'
    UNION ALL SELECT '202409524', 'Mwelwa Maggie',          'CS', 'G05'
    UNION ALL SELECT '202200670', 'Mshisha Dada',           'CS', 'G05'
    UNION ALL SELECT '202409126', 'Mukangwa Brian',         'CS', 'G05'
    UNION ALL SELECT '202408328', 'Shikupa Charles',        'CS', 'G05'
    UNION ALL SELECT '202307112', 'Mwape Prisca',           'CS', 'G05'
    UNION ALL SELECT '202400260', 'Shikabanga Cecilia',     'CS', 'G05'
    UNION ALL SELECT '202403973', 'Nevers Mwaba',           'CS', 'G05'
    UNION ALL SELECT '202403184', 'Febby Nganwe',           'CS', 'G05'

    -- ---- G06: 15 CS ----
    UNION ALL SELECT '202407184', 'Muyoba Mulako',          'CS', 'G06'
    UNION ALL SELECT '202400975', 'Owen Mukela',            'CS', 'G06'
    UNION ALL SELECT '202407403', 'Previous Mukosha',       'CS', 'G06'
    UNION ALL SELECT '202400421', 'Immaculate Mwanangombe', 'CS', 'G06'
    UNION ALL SELECT '202301165', 'Houghton Chibozyi',      'CS', 'G06'
    UNION ALL SELECT '202402071', 'Winnia Mwanza',          'CS', 'G06'
    UNION ALL SELECT '202400957', 'Bosswell Habenzu',       'CS', 'G06'
    UNION ALL SELECT '202402043', 'Kamwemba Kulumuku',      'CS', 'G06'
    UNION ALL SELECT '202409550', 'Wilfred Lungo',          'CS', 'G06'
    UNION ALL SELECT '202405691', 'Chakuletela Musukwa',    'CS', 'G06'
    UNION ALL SELECT '202403830', 'Charles Chisala',        'CS', 'G06'
    UNION ALL SELECT '202407202', 'Namasiku Beauty Siemisa','CS', 'G06'
    UNION ALL SELECT '202401889', 'Fridah Muzungu',         'CS', 'G06'
    UNION ALL SELECT '202406520', 'Mundia Mundia',          'CS', 'G06'
    UNION ALL SELECT '202408897', 'Marcus Malulu',          'CS', 'G06'

    -- ---- G07: 15 CS ----
    UNION ALL SELECT '202303351', 'Mwendalubi Mike Manyika','CS', 'G07'
    UNION ALL SELECT '202406767', 'Bwalya Manyando',        'CS', 'G07'
    UNION ALL SELECT '202404746', 'Rachael Shabolyo',       'CS', 'G07'
    UNION ALL SELECT '202408031', 'Edwin Makuya',           'CS', 'G07'
    UNION ALL SELECT '202408025', 'Cryvet Simfukwe',        'CS', 'G07'
    UNION ALL SELECT '202404317', 'Yeshua Owen',            'CS', 'G07'
    UNION ALL SELECT '202407565', 'Reagan Chisanga',        'CS', 'G07'
    UNION ALL SELECT '202408138', 'Chanda Mubanga',         'CS', 'G07'
    UNION ALL SELECT '202405883', 'Isaiah Mbangweta',       'CS', 'G07'
    UNION ALL SELECT '202409469', 'Mapalo Mwape',           'CS', 'G07'
    UNION ALL SELECT '202402434', 'Ngosa Florence',         'CS', 'G07'
    UNION ALL SELECT '202403931', 'M''manga Modester',      'CS', 'G07'
    UNION ALL SELECT '202400114', 'Kazembe Sing''ambwa',    'CS', 'G07'
    UNION ALL SELECT '202404096', 'Temwini Phiri',          'CS', 'G07'
    UNION ALL SELECT '202401761', 'Caleb Mukelabai Kalimina','CS','G07'

    -- ---- G08: 12 mixed ----
    UNION ALL SELECT '202406501', 'Bwalya Marvelous',       'CS', 'G08'
    UNION ALL SELECT '202408157', 'Soko Zilombo Choolwe',   'CS', 'G08'
    UNION ALL SELECT '202408295', 'Mbewe Mathews',          'CS', 'G08'
    UNION ALL SELECT '202404634', 'Sakala Blessings',       'CS', 'G08'
    UNION ALL SELECT '202409794', 'Moyo Sibongile',         'CS', 'G08'
    UNION ALL SELECT '202406011', 'Gumbo Dailess',          'CS', 'G08'
    UNION ALL SELECT '202409214', 'Shaba Patience',         'CS', 'G08'
    UNION ALL SELECT '202400397', 'Chishala Juliet',        'CS', 'G08'
    UNION ALL SELECT '202407247', 'Chileshe Sampa',         'CS', 'G08'
    UNION ALL SELECT '201802185', 'Kabengele Remmy',        'CS', 'G08'
    UNION ALL SELECT '202402295', 'Miyambo Emmanuel',       'IT', 'G08'
    UNION ALL SELECT '202106741', 'Kanjela Musenge',        'CS', 'G08'

    -- ---- G09: 15 mixed, 1 moved to Unassigned ----
    UNION ALL SELECT '202406031', 'Florence Shimpa Musonda','CS', 'G09'
    UNION ALL SELECT '202407879', 'Gerald Gondwe',          'CS', 'G09'
    UNION ALL SELECT '202400653', 'Bernadette Chanda',      'IT', 'G09'
    UNION ALL SELECT '202403271', 'William Simbeye',        'CS', 'G09'
    UNION ALL SELECT '202402510', 'Derrick Kabonso',        'CS', 'G09'
    UNION ALL SELECT '202405283', 'Miti Steven',            'CS', 'G09'
    UNION ALL SELECT '202409051', 'Kampamba Charity',       'CS', 'G09'
    UNION ALL SELECT '202403132', 'Benzu Himoonde',         'CS', 'G09'
    UNION ALL SELECT '202405388', 'Frazer Macha',           'CS', 'G09'
    UNION ALL SELECT '202408282', 'Timothy Malasha',        'CS', 'G09'
    UNION ALL SELECT '202410672', 'Kalunga Mervis',         'IT', 'G09'
    UNION ALL SELECT '202407064', 'Violet Ngoma',           'CS', 'G09'
    UNION ALL SELECT '201701343', 'Mweene Simon',           'CS', 'G09'
    UNION ALL SELECT '202406408', 'Chimbala Christopher',   'CS', 'G09'
    UNION ALL SELECT '202406593', 'Enock Simumba',          'CS', 'G09'
    -- 16th student moved to Unassigned
    UNION ALL SELECT '202406805', 'Eustuce Kasongo',        'CS', NULL

    -- ---- G10: 15 mixed, 1 moved to Unassigned ----
    UNION ALL SELECT '202404114', 'Beatrice Chibesa',       'CS', 'G10'
    UNION ALL SELECT '202400350', 'Angel Bwalya',           'CS', 'G10'
    UNION ALL SELECT '202400550', 'Evans Saikolo',          'CS', 'G10'
    UNION ALL SELECT '202402843', 'Titus Bwalya',           'CS', 'G10'
    UNION ALL SELECT '202404621', 'Ezron Chiluba',          'CS', 'G10'
    UNION ALL SELECT '202410470', 'Juliet Mulenga',         'CS', 'G10'
    UNION ALL SELECT '202402030', 'Ilukena Mundia',         'CS', 'G10'
    UNION ALL SELECT '202409245', 'Cascious Nyambe',        'CS', 'G10'
    UNION ALL SELECT '202410007', 'Aquirina Mulauzi',       'CS', 'G10'
    UNION ALL SELECT '202402824', 'Tobias Phiri',           'IT', 'G10'
    UNION ALL SELECT '202410384', 'Mary Mula',              'CS', 'G10'
    UNION ALL SELECT '202407468', 'Weston Mulenga',         'IT', 'G10'
    UNION ALL SELECT '202404891', 'Christopher Chimfwembe', 'CS', 'G10'
    UNION ALL SELECT '202405964', 'Choolwe Mweembe',        'CS', 'G10'
    UNION ALL SELECT '202410253', 'Oscar Chipango',         'CS', 'G10'
    -- 16th student moved to Unassigned
    UNION ALL SELECT '202109428', 'Gaidon Sichone',         'CS', NULL

    -- ---- G11: 15 CS, 1 moved to Unassigned, 2 duplicates skipped ----
    -- NOTE: student #7 (SIMASIKU ABEL, 202407015) duplicates #1's number
    --       student #9 (TRAORE MORDECAI SALIM, 202403846) duplicates #8's number
    -- These are skipped pending lecturer confirmation.
    UNION ALL SELECT '202407015', 'Miti Katanga',           'CS', 'G11'
    UNION ALL SELECT '202404688', 'Mbasela Fabian',         'CS', 'G11'
    UNION ALL SELECT '202404172', 'Chola Ben',              'CS', 'G11'
    UNION ALL SELECT '202403019', 'Hamasukwa Agrippa C.',   'CS', 'G11'
    UNION ALL SELECT '202401150', 'Muunda Mainza',          'CS', 'G11'
    UNION ALL SELECT '202001699', 'Mumbi Chibesa',          'CS', 'G11'
    -- SKIP: SIMASIKU ABEL 202407015 (duplicate of #1)
    UNION ALL SELECT '202403846', 'Chanda Collins',         'CS', 'G11'
    -- SKIP: TRAORE MORDECAI SALIM 202403846 (duplicate of #8)
    UNION ALL SELECT '202405943', 'Chilufya Mapalo',        'CS', 'G11'
    UNION ALL SELECT '202203897', 'Mwape Kelvin',           'CS', 'G11'
    UNION ALL SELECT '202206607', 'Kansamba Auxiria',       'CS', 'G11'
    UNION ALL SELECT '202305732', 'Banda Salima',           'CS', 'G11'
    UNION ALL SELECT '202403844', 'Traore Lamin',           'CS', 'G11'
    UNION ALL SELECT '202303375', 'Daka Racheal',           'CS', 'G11'

    -- ---- G12: 14 mixed ----
    UNION ALL SELECT '202401321', 'Moon Clive',             'IT', 'G12'
    UNION ALL SELECT '202404407', 'Zimba Emmanuel',         'IT', 'G12'
    UNION ALL SELECT '202405095', 'Kaonga Memory',          'CS', 'G12'
    UNION ALL SELECT '202405321', 'Mukupa Franscine',       'CS', 'G12'
    UNION ALL SELECT '202409728', 'Nakanyika Nsungani',     'CS', 'G12'
    UNION ALL SELECT '202406233', 'Chipampe Stanslous',     'CS', 'G12'
    UNION ALL SELECT '202401475', 'Mukonta Davis',          'CS', 'G12'
    UNION ALL SELECT '202406877', 'Chishala Alfred',        'CS', 'G12'
    UNION ALL SELECT '202301116', 'Phiri Brian',            'CS', 'G12'
    UNION ALL SELECT '202301173', 'Chola Simon',            'CS', 'G12'
    UNION ALL SELECT '202300654', 'Ng''omba Kangwa',        'CS', 'G12'
    UNION ALL SELECT '202404608', 'Kabutu Ethel',           'IT', 'G12'
    UNION ALL SELECT '202409694', 'Mwenda Hamulambo Sostern','CS','G12'
    UNION ALL SELECT '202404703', 'Masambu Kalenga',        'CS', 'G12'

    -- ---- G13: 15 CS ----
    UNION ALL SELECT '202408913', 'Silunji Victor',         'CS', 'G13'
    UNION ALL SELECT '202404454', 'Sakala Daniel',          'CS', 'G13'
    UNION ALL SELECT '202410021', 'Mawere Saidi',           'CS', 'G13'
    UNION ALL SELECT '202408632', 'Mwanza Taizya',          'CS', 'G13'
    UNION ALL SELECT '202406171', 'Mwitwa Clement',         'CS', 'G13'
    UNION ALL SELECT '202408752', 'Banda Agness',           'CS', 'G13'
    UNION ALL SELECT '202400961', 'N''Gandwe Charles',      'CS', 'G13'
    UNION ALL SELECT '202409714', 'M''Soni Deborah',        'CS', 'G13'
    UNION ALL SELECT '202402226', 'Ng''andu Lwendo',        'CS', 'G13'
    UNION ALL SELECT '202400014', 'Chile Bwalya',           'CS', 'G13'
    UNION ALL SELECT '202404480', 'Banda Mphaso',           'CS', 'G13'
    UNION ALL SELECT '202406515', 'Tembo Patience',         'CS', 'G13'
    UNION ALL SELECT '202407984', 'Mwambazi Grace',         'CS', 'G13'
    UNION ALL SELECT '202401433', 'Ndhlovu Godfrey',        'CS', 'G13'
    UNION ALL SELECT '202406496', 'Lungu Rebecca',          'CS', 'G13'

    -- ---- G14 EXCLUDED: team members register through the app ----
) v
JOIN programmes p ON p.code = v.prog
LEFT JOIN lab_groups g ON g.group_code = v.grp;

-- Claim code for a specific student to demo the "already pre-entered" flow.
-- Replace '202400549' with any student from the seed above.
-- Raw code to type in the app: TESTCODE123
INSERT INTO claim_codes (student_id, code_hash, expires_at)
SELECT student_id, SHA2('TESTCODE123', 256), DATE_ADD(NOW(), INTERVAL 30 DAY)
FROM students
WHERE student_number = '202400549';