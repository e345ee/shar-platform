


INSERT INTO role(rolename, description) VALUES
 ('ADMIN', 'Администратор системы'),
 ('METHODIST', 'Методист'),
 ('TEACHER', 'Преподаватель'),
 ('STUDENT', 'Студент')
ON CONFLICT (rolename) DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'seed_admin', 'seed_admin@demo.local', '$2b$10$FVpKNvkJNsv32TiDExuewuy2YtOZT1PYRriYIxHZlFhc46lJuKHdG'
FROM role r WHERE r.rolename = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'seed_methodist', 'seed_methodist@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r WHERE r.rolename = 'METHODIST'
ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'seed_teacher', 'seed_teacher@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r WHERE r.rolename = 'TEACHER'
ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'seed_student', 'seed_student@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO'
FROM role r WHERE r.rolename = 'STUDENT'
ON CONFLICT DO NOTHING;


INSERT INTO methodist_teachers(methodist_id, teacher_id)
SELECT m.id, t.id
FROM users m, users t
WHERE m.name='seed_methodist' AND t.name='seed_teacher'
ON CONFLICT DO NOTHING;


INSERT INTO courses(name, description, created_by)
SELECT 'Seed course', 'Small seed course', u.id
FROM users u WHERE u.name='seed_methodist'
ON CONFLICT DO NOTHING;


INSERT INTO classes(name, join_code, course_id, teacher_id, created_by)
SELECT 'Seed class', 'SEED0001', c.id, t.id, m.id
FROM courses c, users t, users m
WHERE c.name='Seed course' AND t.name='seed_teacher' AND m.name='seed_methodist'
ON CONFLICT DO NOTHING;


INSERT INTO class_students(class_id, student_id)
SELECT cl.id, st.id
FROM classes cl, users st
WHERE cl.name='Seed class' AND st.name='seed_student'
ON CONFLICT DO NOTHING;


INSERT INTO achievements(course_id, created_by, title, joke_description, description, photo_url)
SELECT c.id, m.id, 'Seed achievement', 'seed', 'Seed achievement description', 'https://example.com/seed-achievement.png'
FROM courses c, users m
WHERE c.name='Seed course' AND m.name='seed_methodist'
ON CONFLICT DO NOTHING;


INSERT INTO tests(course_id, created_by, title, topic, deadline, status)
SELECT c.id, m.id, 'Seed test', 'Seed topic', NOW() + INTERVAL '7 days', 'READY'
FROM courses c, users m
WHERE c.name='Seed course' AND m.name='seed_methodist'
ON CONFLICT DO NOTHING;


INSERT INTO test_questions(test_id, order_index, question_text, question_type, points,
                           option_1, option_2, correct_option)
SELECT t.id, 1, 'Seed question?', 'SINGLE_CHOICE', 1, 'Yes', 'No', 1
FROM tests t
WHERE t.title='Seed test'
ON CONFLICT DO NOTHING;
