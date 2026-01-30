----------------------------------------------------------------------
-- 0. УДАЛЕНИЕ ТАБЛИЦ 
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "SubmissionAnswer",
    "Submission",
    "AnswerOption",
    "Question",
    "UserClass",
    "UserAchievement",
    "Achievement",
    "Material",
    "Lesson",
    "Activity",
    "Class",
    "Course",
    "User",
    "Role"
CASCADE;


----------------------------------------------------------------------
-- 1. СОЗДАНИЕ ТАБЛИЦ 
----------------------------------------------------------------------

CREATE TABLE "Role" (
    id          SERIAL PRIMARY KEY,
    rolename    VARCHAR(63)  NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE "User" (
    id          SERIAL PRIMARY KEY,
    role_id     INT NOT NULL REFERENCES "Role"(id),
    name        VARCHAR(63)  NOT NULL UNIQUE,
    email       VARCHAR(127) NOT NULL UNIQUE,
    password    VARCHAR(127) NOT NULL,
    bio         TEXT,
    photo       TEXT,
    tg_id       VARCHAR(127) UNIQUE
);

CREATE TABLE "Course" (
    id          SERIAL PRIMARY KEY,
    user_id     INT NOT NULL REFERENCES "User"(id),
    title       VARCHAR(127) NOT NULL,
    description TEXT
);

CREATE TABLE "Class" (
    id          SERIAL PRIMARY KEY,
    course_id   INT NOT NULL REFERENCES "Course"(id),
    name        VARCHAR(127) NOT NULL,
    description TEXT
);

CREATE TABLE "Activity" (
    id         SERIAL PRIMARY KEY,
    type       VARCHAR(63)  NOT NULL,
    topic      VARCHAR(127) NOT NULL,
    max_score  INT NOT NULL CHECK (max_score > 0),
    open_date  TIMESTAMP NOT NULL,
    due_date   TIMESTAMP,
    CHECK (due_date IS NULL OR due_date >= open_date)
);

CREATE TABLE "Lesson" (
    id          SERIAL PRIMARY KEY,
    activity_id INT REFERENCES "Activity"(id),
    course_id   INT NOT NULL REFERENCES "Course"(id),
    title       VARCHAR(127) NOT NULL,
    description TEXT,
    position    INT NOT NULL,
    CONSTRAINT lesson_course_position_uniq
        UNIQUE (course_id, position)
);

CREATE TABLE "Material" (
    id             SERIAL PRIMARY KEY,
    lesson_id      INT NOT NULL REFERENCES "Lesson"(id),
    title          VARCHAR(127) NOT NULL,
    content        TEXT NOT NULL,
    published_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE "Achievement" (
    id          SERIAL PRIMARY KEY,
    title       VARCHAR(127) NOT NULL,
    description TEXT,
    icon        TEXT
);

CREATE TABLE "UserAchievement" (
    id             SERIAL PRIMARY KEY,
    user_id        INT NOT NULL REFERENCES "User"(id),
    achievement_id INT NOT NULL REFERENCES "Achievement"(id),
    awarded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_achievement_uniq UNIQUE (user_id, achievement_id)
);

CREATE TABLE "UserClass" (
    id          SERIAL PRIMARY KEY,
    user_id     INT NOT NULL REFERENCES "User"(id),
    class_id    INT NOT NULL REFERENCES "Class"(id),
    joined_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at     TIMESTAMP,
    status      VARCHAR(63) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT user_class_uniq UNIQUE (user_id, class_id),
    CHECK (left_at IS NULL OR left_at >= joined_at)
);

CREATE TABLE "Question" (
    id          SERIAL PRIMARY KEY,
    activity_id INT NOT NULL REFERENCES "Activity"(id),
    text        TEXT NOT NULL,
    qtype       VARCHAR(63) NOT NULL,
    score       INT NOT NULL CHECK (score > 0),
    position    INT NOT NULL,
    CONSTRAINT question_activity_position_uniq
        UNIQUE (activity_id, position)
);

CREATE TABLE "AnswerOption" (
    id          SERIAL PRIMARY KEY,
    question_id INT NOT NULL REFERENCES "Question"(id),
    text        TEXT NOT NULL,
    is_correct  BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE "Submission" (
    id            SERIAL PRIMARY KEY,
    user_id       INT NOT NULL REFERENCES "User"(id),
    activity_id   INT NOT NULL REFERENCES "Activity"(id),
    submit_date   TIMESTAMP,
    status        VARCHAR(63) NOT NULL,
    auto_score    INT,
    teacher_score INT,
    total_score   INT CHECK (total_score >= 0)
);

CREATE TABLE "SubmissionAnswer" (
    id            SERIAL PRIMARY KEY,
    submission_id INT NOT NULL REFERENCES "Submission"(id),
    answer_text   TEXT,
    is_correct    BOOLEAN,
    score_awarded INT
);


----------------------------------------------------------------------
-- 2. ТРИГГЕРЫ 
----------------------------------------------------------------------

-- 2.1. Авторасчёт total_score в Submission
CREATE OR REPLACE FUNCTION calc_submission_total_score()
RETURNS TRIGGER AS $$
BEGIN
    NEW.total_score :=
        COALESCE(NEW.auto_score, 0) + COALESCE(NEW.teacher_score, 0);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_submission_calc_total
BEFORE INSERT OR UPDATE ON "Submission"
FOR EACH ROW
EXECUTE FUNCTION calc_submission_total_score();


-- 2.2. Автоустановка submit_date при статусе SUBMITTED
CREATE OR REPLACE FUNCTION set_submit_date_on_submitted()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status = 'SUBMITTED' AND NEW.submit_date IS NULL THEN
        NEW.submit_date := CURRENT_TIMESTAMP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_submission_set_date
BEFORE INSERT OR UPDATE ON "Submission"
FOR EACH ROW
EXECUTE FUNCTION set_submit_date_on_submitted();


-- 2.3. Нормализация статуса в UserClass
-- Автоматически синхронизируем статус пользователя в классе с датой выхода.
CREATE OR REPLACE FUNCTION normalize_userclass_status()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.left_at IS NOT NULL AND NEW.status = 'ACTIVE' THEN
        NEW.status := 'INACTIVE';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_userclass_normalize_status
BEFORE INSERT OR UPDATE ON "UserClass"
FOR EACH ROW
EXECUTE FUNCTION normalize_userclass_status();


----------------------------------------------------------------------
-- 3. ТЕСТОВЫЕ ДАННЫЕ
----------------------------------------------------------------------

-- роли
INSERT INTO "Role"(rolename, description) VALUES
 ('ADMIN',   'Администратор системы'),
 ('TEACHER', 'Преподаватель'),
 ('STUDENT', 'Студент');

-- пользователи
INSERT INTO "User"(role_id, name, email, password) VALUES
 (1, 'admin',   'admin@example.com',   'hash_admin'),
 (2, 'teacher', 'teacher@example.com', 'hash_teacher'),
 (3, 'student', 'student@example.com', 'hash_student');

-- курсы
INSERT INTO "Course"(user_id, title, description) VALUES
 (2, 'Основы программирования', 'Базовый курс по программированию');

-- классы
INSERT INTO "Class"(course_id, name, description) VALUES
 (1, 'Группа 101', 'Очная группа');

-- состав класса
INSERT INTO "UserClass"(user_id, class_id) VALUES
 (3, 1);

-- урок
INSERT INTO "Lesson"(activity_id, course_id, title, description, position) VALUES
 (NULL, 1, 'Введение', 'Знакомство с курсом', 1);

-- материалы
INSERT INTO "Material"(lesson_id, title, content) VALUES
 (1, 'Презентация', 'Ссылка или текст презентации');

-- активность
INSERT INTO "Activity"(type, topic, max_score, open_date, due_date) VALUES
 ('QUIZ', 'Тест по введению', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '7 days');

UPDATE "Lesson" SET activity_id = 1 WHERE id = 1;

-- вопросы и варианты
INSERT INTO "Question"(activity_id, text, qtype, score, position) VALUES
 (1, 'Что такое алгоритм?', 'SINGLE', 5, 1),
 (1, 'Выберите языки программирования', 'MULTI', 5, 2);

INSERT INTO "AnswerOption"(question_id, text, is_correct) VALUES
 (1, 'Последовательность действий', TRUE),
 (1, 'Компьютерная игра', FALSE),
 (2, 'Python', TRUE),
 (2, 'C++', TRUE),
 (2, 'HTML', FALSE);

-- достижения
INSERT INTO "Achievement"(title, description) VALUES
 ('Первое задание', 'Пользователь сдал своё первое задание'),
 ('Отличник', 'Набрал максимум баллов по активности');


----------------------------------------------------------------------
-- 4. PL/PGSQL ФУНКЦИИ / ПРОЦЕДУРЫ
----------------------------------------------------------------------

-- 4.1. Регистрация пользователя
CREATE OR REPLACE FUNCTION register_user(
    p_name      VARCHAR(63),
    p_email     VARCHAR(127),
    p_password  VARCHAR(127),
    p_role_name VARCHAR(63) DEFAULT 'STUDENT'
) RETURNS INTEGER AS $$
DECLARE
    v_user_id  INTEGER;
    v_role_id  INTEGER;
BEGIN
    SELECT id INTO v_role_id
    FROM "Role"
    WHERE rolename = p_role_name;

    IF v_role_id IS NULL THEN
        RAISE EXCEPTION 'Роль % не найдена', p_role_name;
    END IF;

    INSERT INTO "User"(role_id, name, email, password)
    VALUES (v_role_id, p_name, p_email, p_password)
    RETURNING id INTO v_user_id;

    RETURN v_user_id;
EXCEPTION
    WHEN unique_violation THEN
        RAISE EXCEPTION 'Пользователь с таким email или именем уже существует';
END;
$$ LANGUAGE plpgsql;


-- 4.2. Аутентификация пользователя
CREATE OR REPLACE FUNCTION authenticate_user(
    p_email    VARCHAR(127),
    p_password VARCHAR(127)
) RETURNS TABLE(id INT, name VARCHAR, role VARCHAR) AS $$
BEGIN
    RETURN QUERY
    SELECT u.id, u.name, r.rolename
    FROM "User" u
    JOIN "Role" r ON r.id = u.role_id
    WHERE u.email = p_email
      AND u.password = p_password;
END;
$$ LANGUAGE plpgsql;


-- 4.3. Запись студента в класс
CREATE OR REPLACE FUNCTION enroll_user_to_class(
    p_user_id  INT,
    p_class_id INT
) RETURNS VOID AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM "User" WHERE id = p_user_id) THEN
        RAISE EXCEPTION 'Пользователь % не найден', p_user_id;
    END IF;

    INSERT INTO "UserClass"(user_id, class_id)
    VALUES (p_user_id, p_class_id)
    ON CONFLICT (user_id, class_id) DO NOTHING;
END;
$$ LANGUAGE plpgsql;


-- 4.4. Создание попытки
CREATE OR REPLACE FUNCTION create_submission(
    p_user_id     INT,
    p_activity_id INT
) RETURNS INT AS $$
DECLARE
    v_id INT;
BEGIN
    INSERT INTO "Submission"(user_id, activity_id, status)
    VALUES (p_user_id, p_activity_id, 'DRAFT')
    RETURNING id INTO v_id;

    RETURN v_id;
END;
$$ LANGUAGE plpgsql;


-- 4.5. Автопроверка теста
CREATE OR REPLACE FUNCTION grade_submission_auto(
    p_submission_id INT
) RETURNS VOID AS $$
DECLARE
    v_score INT;
BEGIN
    SELECT COALESCE(SUM(q.score), 0) INTO v_score
    FROM "SubmissionAnswer" sa
    JOIN "AnswerOption" ao ON ao.id = sa.answer_text::INT
    JOIN "Question"     q  ON q.id  = ao.question_id
    WHERE sa.submission_id = p_submission_id
      AND ao.is_correct = TRUE;

    UPDATE "Submission"
    SET auto_score = v_score,
        status     = 'AUTO_GRADED'
    WHERE id = p_submission_id;
END;
$$ LANGUAGE plpgsql;



----------------------------------------------------------------------
-- 5. ИНДЕКСЫ
----------------------------------------------------------------------

-- Индекс для быстрого поиска пользователя по email.
-- UNIQUE гарантирует, что в базе не появится двух пользователей с одинаковым email,
-- что критично для авторизации и регистрации.
CREATE UNIQUE INDEX idx_user_email_lower
    ON "User"(LOWER(email));


-- Индекс для выборки уроков конкретного курса в правильном порядке.
-- Почти каждый просмотр курса запрашивает: SELECT ... FROM Lesson WHERE course_id = ? ORDER BY position.
-- Такой составной индекс позволяет избежать полной сортировки и сканирования таблицы.
CREATE INDEX idx_lesson_course_position
    ON "Lesson"(course_id, position);


-- Индекс для получения всех материалов конкретного урока.
-- Открытие урока всегда подразумевает загрузку его материалов.
-- Индекс по lesson_id делает эту операцию быстрым point lookup без полного сканирования Material.
CREATE INDEX idx_material_lesson
    ON "Material"(lesson_id);


-- Индекс для получения всех вопросов активности по порядку.
-- При прохождении теста система делает: SELECT * FROM Question WHERE activity_id = ? ORDER BY position.
-- Индекс обеспечивает быстрое получение и гарантирует использование правильного порядка без сортировки.
CREATE INDEX idx_question_activity_position
    ON "Question"(activity_id, position);


-- Индекс для получения всех попыток пользователя.
-- Это нужно в личном кабинете студента, в аналитике, в отчётах об успеваемости.
-- Без индекса каждая выборка требовала бы полного сканирования Submission.
CREATE INDEX idx_submission_user
    ON "Submission"(user_id);


-- Индекс для получения всех попыток по конкретной активности.
-- Это ключевой запрос преподавателя при проверке работ.
-- Значительно ускоряет операции модерации, просмотра всех сдач и выставления оценок.
CREATE INDEX idx_submission_activity
    ON "Submission"(activity_id);


-- Индекс для быстрого получения всех ответов конкретной попытки.
-- Нужен при автопроверке, ручной проверке, просмотре попытки студентом.
-- Таблица SubmissionAnswer может быть очень большой (по числу вопросов × попытки).
CREATE INDEX idx_submissionanswer_submission
    ON "SubmissionAnswer"(submission_id);


-- Индекс по class_id ускоряет выборку списка студентов внутри определённого класса.
-- Применяется при формировании журнала, отображении группы, анализе успеваемости.
-- Без индекса система будет вынуждена просматривать всю UserClass.
CREATE INDEX idx_userclass_class
    ON "UserClass"(class_id);


-- Индекс для получения всех достижений конкретного пользователя.
-- Используется на странице профиля, в аналитике прогресса, в выдаче наград.
-- Ускоряет выборку по user_id, убирая полный скан UserAchievement.
CREATE INDEX idx_userachievement_user
    ON "UserAchievement"(user_id);
