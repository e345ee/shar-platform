INSERT INTO role(rolename, description) VALUES
                                            ('ADMIN', 'Администратор системы'),
                                            ('METHODIST', 'Методист'),
                                            ('TEACHER', 'Преподаватель'),
                                            ('STUDENT', 'Студент')
    ON CONFLICT (rolename) DO NOTHING;



INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'admin', 'admin@demo.local', '$2b$10$FVpKNvkJNsv32TiDExuewuy2YtOZT1PYRriYIxHZlFhc46lJuKHdG' -- пароль до хеширования: admin
FROM role r WHERE r.rolename = 'ADMIN'
    ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'methodist_anna', 'anna@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'METHODIST'
    ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'methodist_boris', 'boris@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'METHODIST'
    ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'teacher_alex', 'alex@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'TEACHER'
    ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'teacher_irina', 'irina@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'TEACHER'
    ON CONFLICT DO NOTHING;


INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'student_ivan', 'ivan@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'STUDENT'
    ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'student_maria', 'maria@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'STUDENT'
    ON CONFLICT DO NOTHING;

INSERT INTO users(role_id, name, email, password)
SELECT r.id, 'student_oleg', 'oleg@demo.local', '$2b$10$bKmYqnW7WQ6Oo94kiRCeC.XRoVt5CQ2p15VANIsYmna/AKRo17.EO' -- пароль до хеширования: pass
FROM role r WHERE r.rolename = 'STUDENT'
    ON CONFLICT DO NOTHING;





INSERT INTO courses(name, description, created_by)
VALUES
    ('Основы программирования', 'Базовый курс', (SELECT id FROM users WHERE name='methodist_anna')),
    ('Алгоритмы', 'Алгоритмы и структуры данных', (SELECT id FROM users WHERE name='methodist_boris'));





INSERT INTO classes(name, join_code, course_id, teacher_id, created_by)
VALUES
    ('Группа A', 'ABC12345',
     (SELECT id FROM courses WHERE name='Основы программирования'),
     (SELECT id FROM users WHERE name='teacher_alex'),
     (SELECT id FROM users WHERE name='methodist_anna')),

    ('Группа B', 'XYZ67890',
     (SELECT id FROM courses WHERE name='Алгоритмы'),
     (SELECT id FROM users WHERE name='teacher_irina'),
     (SELECT id FROM users WHERE name='methodist_boris'));






INSERT INTO class_students(class_id, student_id)
SELECT c.id, u.id
FROM classes c, users u
WHERE c.name='Группа A' AND u.name IN ('student_ivan','student_maria')
    ON CONFLICT DO NOTHING;


INSERT INTO class_students(class_id, student_id)
SELECT c.id, u.id
FROM classes c, users u
WHERE c.name='Группа B' AND u.name IN ('student_maria','student_oleg')
    ON CONFLICT DO NOTHING;





INSERT INTO achievements(course_id, created_by, title, joke_description, description, photo_url)
VALUES
    ((SELECT id FROM courses WHERE name='Основы программирования'),
     (SELECT id FROM users WHERE name='methodist_anna'),
     'Первый тест',
     'Ну вот и понеслось',
     'Выдается за первую сданную активность',
     'https://example.com/a1.png'),

    ((SELECT id FROM courses WHERE name='Алгоритмы'),
     (SELECT id FROM users WHERE name='methodist_boris'),
     'Алгоритмист',
     'Думает как компьютер',
     'Выдается за успешное прохождение теста',
     'https://example.com/a2.png');





INSERT INTO tests(course_id, created_by, title, topic, deadline, status)
VALUES
    ((SELECT id FROM courses WHERE name='Основы программирования'),
     (SELECT id FROM users WHERE name='methodist_anna'),
     'Введение в программирование',
     'Основы',
     NOW() + INTERVAL '7 days',
     'READY'),

    ((SELECT id FROM courses WHERE name='Алгоритмы'),
     (SELECT id FROM users WHERE name='methodist_boris'),
     'Сложность алгоритмов',
     'Big-O',
     NOW() + INTERVAL '5 days',
     'READY');






INSERT INTO test_questions(test_id, order_index, question_text, question_type, points,
                           option_1, option_2, option_3, correct_option)
VALUES
    ((SELECT id FROM tests WHERE title='Введение в программирование'),
     1, 'Что такое алгоритм?', 'SINGLE_CHOICE', 5,
     'Последовательность действий','Язык программирования','ОС',1),

    ((SELECT id FROM tests WHERE title='Введение в программирование'),
     2, 'Python — язык программирования?', 'SINGLE_CHOICE', 5,
     'Да','Нет',NULL,1);


INSERT INTO test_questions(test_id, order_index, question_text, question_type, points,
                           option_1, option_2, correct_option)
VALUES
    ((SELECT id FROM tests WHERE title='Сложность алгоритмов'),
     1, 'Что означает O(n)?', 'SINGLE_CHOICE', 10,
     'Линейная сложность','Квадратичная сложность',1);






INSERT INTO test_attempts(test_id, student_id, attempt_number, status)
VALUES
    ((SELECT id FROM tests WHERE title='Введение в программирование'),
     (SELECT id FROM users WHERE name='student_ivan'),
     1, 'GRADED');


INSERT INTO test_attempts(test_id, student_id, attempt_number, status)
VALUES
    ((SELECT id FROM tests WHERE title='Введение в программирование'),
     (SELECT id FROM users WHERE name='student_maria'),
     1, 'IN_PROGRESS');


INSERT INTO test_attempts(test_id, student_id, attempt_number, status)
VALUES
    ((SELECT id FROM tests WHERE title='Сложность алгоритмов'),
     (SELECT id FROM users WHERE name='student_oleg'),
     1, 'GRADED');





INSERT INTO test_attempt_answers(attempt_id, question_id, selected_option, is_correct, points_awarded, graded_at)
SELECT a.id, q.id, q.correct_option, TRUE, q.points, NOW()
FROM test_attempts a
         JOIN test_questions q ON q.test_id = a.test_id
WHERE a.status='GRADED';