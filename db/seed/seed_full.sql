


INSERT INTO role(rolename, description) VALUES
 ('ADMIN', 'Администратор системы'),
 ('METHODIST', 'Методист'),
 ('TEACHER', 'Преподаватель'),
 ('STUDENT', 'Студент')
ON CONFLICT (rolename) DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'seed_full_admin', 'seed_full_admin@demo.local', '$2b$10$FVpKNvkJNsv32TiDExuewuy2YtOZT1PYRriYIxHZlFhc46lJuKHdG'
FROM role r WHERE r.rolename = 'ADMIN'
ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id,
       'seed_full_methodist_' || lpad(gs::text, 3, '0'),
       'seed_full_methodist_' || lpad(gs::text, 3, '0') || '@demo.local',
       '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r
CROSS JOIN generate_series(1, 20) gs
WHERE r.rolename = 'METHODIST'
ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id,
       'seed_full_teacher_' || lpad(gs::text, 3, '0'),
       'seed_full_teacher_' || lpad(gs::text, 3, '0') || '@demo.local',
       '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r
CROSS JOIN generate_series(1, 60) gs
WHERE r.rolename = 'TEACHER'
ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id,
       'seed_full_student_' || lpad(gs::text, 4, '0'),
       'seed_full_student_' || lpad(gs::text, 4, '0') || '@demo.local',
       '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r
CROSS JOIN generate_series(1, 600) gs
WHERE r.rolename = 'STUDENT'
ON CONFLICT DO NOTHING;


INSERT INTO methodist_teachers(methodist_id, teacher_id)
SELECT m.id, t.id
FROM users t
JOIN LATERAL (
    SELECT id
    FROM users
    WHERE name LIKE 'seed_full_methodist_%'
    ORDER BY random()
    LIMIT 1
) m ON true
WHERE t.name LIKE 'seed_full_teacher_%'
ON CONFLICT DO NOTHING;


INSERT INTO courses(name, description, created_by)
SELECT
    'Seed full course ' || lpad(gs::text, 3, '0'),
    'Demo course generated for UI/API testing',
    (SELECT id FROM users WHERE name LIKE 'seed_full_methodist_%' ORDER BY random() LIMIT 1)
FROM generate_series(1, 80) gs
ON CONFLICT DO NOTHING;


INSERT INTO classes(name, join_code, course_id, teacher_id, created_by)
SELECT
    'Seed full class ' || lpad(gs::text, 3, '0'),
    'SF' || lpad(gs::text, 6, '0'),
    (SELECT id FROM courses WHERE name LIKE 'Seed full course %' ORDER BY random() LIMIT 1),
    (SELECT id FROM users WHERE name LIKE 'seed_full_teacher_%' ORDER BY random() LIMIT 1),
    (SELECT id FROM users WHERE name LIKE 'seed_full_methodist_%' ORDER BY random() LIMIT 1)
FROM generate_series(1, 40) gs
ON CONFLICT DO NOTHING;


INSERT INTO class_students(class_id, student_id)
SELECT c.id, s.id
FROM classes c
JOIN LATERAL (
    SELECT id
    FROM users
    WHERE name LIKE 'seed_full_student_%'
    ORDER BY random()
    LIMIT 20
) s ON true
WHERE c.name LIKE 'Seed full class %'
ON CONFLICT DO NOTHING;


INSERT INTO achievements(course_id, created_by, title, joke_description, description, photo_url)
SELECT
    c.id,
    (SELECT id FROM users WHERE name LIKE 'seed_full_methodist_%' ORDER BY random() LIMIT 1),
    'Seed full achievement ' || lpad(gs::text, 3, '0'),
    'Generated',
    'Achievement created for demo/testing',
    'https://example.com/seed-full-achievement-' || lpad(gs::text, 3, '0') || '.png'
FROM generate_series(1, 60) gs
JOIN LATERAL (
    SELECT id FROM courses WHERE name LIKE 'Seed full course %' ORDER BY random() LIMIT 1
) c ON true
ON CONFLICT DO NOTHING;


INSERT INTO tests(course_id, created_by, title, topic, deadline, status)
SELECT
    (SELECT id FROM courses WHERE name LIKE 'Seed full course %' ORDER BY random() LIMIT 1),
    (SELECT id FROM users WHERE name LIKE 'seed_full_methodist_%' ORDER BY random() LIMIT 1),
    'Seed full test ' || lpad(gs::text, 4, '0'),
    'Topic ' || ((gs % 20) + 1),
    NOW() + (gs % 30) * INTERVAL '1 day',
    'READY'
FROM generate_series(1, 160) gs
ON CONFLICT DO NOTHING;


INSERT INTO test_questions(test_id, order_index, question_text, question_type, points,
                           option_1, option_2, option_3, option_4, correct_option)
SELECT t.id,
       q.order_index,
       'Question ' || q.order_index || ' for ' || t.title,
       'SINGLE_CHOICE',
       1,
       'Option A',
       'Option B',
       'Option C',
       'Option D',
       ((q.order_index % 4) + 1)
FROM tests t
JOIN LATERAL (
    SELECT 1 AS order_index UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) q ON true
WHERE t.title LIKE 'Seed full test %'
ON CONFLICT DO NOTHING;


INSERT INTO class_opened_tests(class_id, test_id)
SELECT c.id, t.id
FROM classes c
JOIN LATERAL (
    SELECT id FROM tests WHERE title LIKE 'Seed full test %' ORDER BY random() LIMIT 5
) t ON true
WHERE c.name LIKE 'Seed full class %'
ON CONFLICT DO NOTHING;
