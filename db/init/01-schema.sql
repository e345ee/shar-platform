----------------------------------------------------------------------
-- 0. УДАЛЕНИЕ ТАБЛИЦ 
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "users",
    "role"
    CASCADE;

DROP TYPE IF EXISTS role_name CASCADE;

----------------------------------------------------------------------
-- 1. СОЗДАНИЕ ТАБЛИЦ 
----------------------------------------------------------------------

-- Postgres-native enum for roles
CREATE TYPE role_name AS ENUM ('ADMIN', 'METHODIST', 'TEACHER', 'STUDENT');

CREATE TABLE "role" (
    id          SERIAL PRIMARY KEY,
    rolename    role_name NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE "users" (
    id          SERIAL PRIMARY KEY,
    role_id     INT NOT NULL REFERENCES "role"(id),
    name        VARCHAR(63)  NOT NULL UNIQUE,
    email       VARCHAR(127) NOT NULL UNIQUE,
    password    VARCHAR(127) NOT NULL,
    bio         TEXT,
    photo       TEXT,
    tg_id       VARCHAR(127) UNIQUE
);

----------------------------------------------------------------------
-- 2. ХАРДКОД
----------------------------------------------------------------------
INSERT INTO "role"(rolename, description) VALUES
 ('ADMIN',   'Администратор системы'),
 ('METHODIST', 'Методист'),
 ('TEACHER', 'Преподаватель'),
 ('STUDENT', 'Студент');




-- Пользователей не хардкодим: ADMIN создаётся при первом старте приложения
-- (login: admin, password: admin). Методистов/преподавателей создаёт ADMIN/METHODIST через API.

----------------------------------------------------------------------
-- 1.1. COURSE / CLASSES
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "classes",
    "lessons",
    "courses"
    CASCADE;

CREATE TABLE "courses" (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(127) NOT NULL,
    description  TEXT,
    created_by   INT NOT NULL REFERENCES "users"(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE "lessons" (
    id                SERIAL PRIMARY KEY,
    course_id          INT NOT NULL REFERENCES "courses"(id) ON DELETE CASCADE,
    created_by         INT NOT NULL REFERENCES "users"(id),
    order_index        INT NOT NULL,
    title              VARCHAR(127) NOT NULL,
    description        VARCHAR(2048),
    presentation_url   VARCHAR(512),
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_lesson_title_in_course UNIQUE (course_id, title),
    CONSTRAINT uq_lesson_order_in_course UNIQUE (course_id, order_index),
    CONSTRAINT chk_lesson_title_len CHECK (char_length(title) BETWEEN 1 AND 127),
    CONSTRAINT chk_lesson_order_idx CHECK (order_index >= 1),
    CONSTRAINT chk_lesson_desc_len CHECK (description IS NULL OR char_length(description) <= 2048),
    CONSTRAINT chk_lesson_presentation_len CHECK (presentation_url IS NULL OR char_length(presentation_url) BETWEEN 1 AND 512)
);

CREATE TABLE "classes" (
    id           SERIAL PRIMARY KEY,
    name         VARCHAR(127) NOT NULL,
    join_code    VARCHAR(8) NOT NULL UNIQUE,
    course_id    INT NOT NULL REFERENCES "courses"(id) ON DELETE CASCADE,
    teacher_id   INT REFERENCES "users"(id),
    created_by   INT NOT NULL REFERENCES "users"(id),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_class_name_in_course UNIQUE (course_id, name)
);

----------------------------------------------------------------------
-- 1.1.1. CLASS OPENED LESSONS (Teacher opens lessons for a class)
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "class_opened_lessons"
    CASCADE;

CREATE TABLE "class_opened_lessons" (
    id         SERIAL PRIMARY KEY,
    class_id   INT NOT NULL REFERENCES "classes"(id) ON DELETE CASCADE,
    lesson_id  INT NOT NULL REFERENCES "lessons"(id) ON DELETE CASCADE,
    opened_at  TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_class_opened_lesson UNIQUE (class_id, lesson_id)
);

----------------------------------------------------------------------
-- 1.2. JOIN REQUESTS / CLASS STUDENTS
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "class_join_requests",
    "class_students"
    CASCADE;

CREATE TABLE "class_join_requests" (
    id           SERIAL PRIMARY KEY,
    class_id     INT NOT NULL REFERENCES "classes"(id) ON DELETE CASCADE,
    name         VARCHAR(63) NOT NULL,
    email        VARCHAR(127) NOT NULL,
    tg_id        VARCHAR(127),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_join_request_class_email UNIQUE (class_id, email)
);

CREATE TABLE "class_students" (
    id           SERIAL PRIMARY KEY,
    class_id     INT NOT NULL REFERENCES "classes"(id) ON DELETE CASCADE,
    student_id   INT NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_class_student UNIQUE (class_id, student_id)
);

----------------------------------------------------------------------
-- 1.3. ACHIEVEMENTS / STUDENT ACHIEVEMENTS
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "class_achievement_feed",
    "student_achievements",
    "achievements"
    CASCADE;

CREATE TABLE "achievements" (
    id                SERIAL PRIMARY KEY,
    course_id          INT NOT NULL REFERENCES "courses"(id) ON DELETE CASCADE,
    created_by         INT NOT NULL REFERENCES "users"(id),
    title              VARCHAR(127) NOT NULL,
    joke_description   VARCHAR(1024) NOT NULL,
    description        VARCHAR(2048) NOT NULL,
    photo_url          VARCHAR(512) NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_achievement_title_in_course UNIQUE (course_id, title),
    CONSTRAINT chk_achievement_title_len CHECK (char_length(title) BETWEEN 1 AND 127),
    CONSTRAINT chk_achievement_joke_len CHECK (char_length(joke_description) BETWEEN 1 AND 1024),
    CONSTRAINT chk_achievement_desc_len CHECK (char_length(description) BETWEEN 1 AND 2048),
    CONSTRAINT chk_achievement_photo_len CHECK (char_length(photo_url) BETWEEN 1 AND 512)
);

CREATE TABLE "student_achievements" (
    id                SERIAL PRIMARY KEY,
    student_id        INT NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    achievement_id    INT NOT NULL REFERENCES "achievements"(id) ON DELETE CASCADE,
    awarded_by        INT REFERENCES "users"(id) ON DELETE SET NULL,
    awarded_at        TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_student_achievement UNIQUE (student_id, achievement_id)
);

-- Class-wide feed for awarded achievements
CREATE TABLE "class_achievement_feed" (
    id             SERIAL PRIMARY KEY,
    class_id       INT NOT NULL REFERENCES "classes"(id) ON DELETE CASCADE,
    student_id     INT NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    achievement_id INT NOT NULL REFERENCES "achievements"(id) ON DELETE CASCADE,
    awarded_by        INT REFERENCES "users"(id) ON DELETE SET NULL,
    awarded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_class_achievement_feed_class_created_at
    ON "class_achievement_feed"(class_id, created_at DESC);

----------------------------------------------------------------------
-- 1.4. TESTS / TEST QUESTIONS (Домашка к уроку)
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "test_questions",
    "tests"
    CASCADE;

CREATE TABLE "tests" (
    id           SERIAL PRIMARY KEY,
    -- lesson attachment is optional (for WEEKLY_STAR lesson_id is NULL)
    lesson_id    INT REFERENCES "lessons"(id) ON DELETE CASCADE,
    -- course is always required (for lesson activities it matches lesson.course_id)
    course_id    INT NOT NULL REFERENCES "courses"(id) ON DELETE CASCADE,
    created_by   INT NOT NULL REFERENCES "users"(id),

    activity_type      VARCHAR(32) NOT NULL DEFAULT 'HOMEWORK_TEST',
    weight_multiplier  INT NOT NULL DEFAULT 1,
    assigned_week_start DATE,

    title        VARCHAR(127) NOT NULL,
    description  VARCHAR(2048),
    topic        VARCHAR(127) NOT NULL,
    deadline     TIMESTAMP NOT NULL,

    status       VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at TIMESTAMP,

    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

    -- multiple activities per lesson are allowed
    CONSTRAINT chk_activity_type CHECK (activity_type IN ('HOMEWORK_TEST','CONTROL_WORK','WEEKLY_STAR')),
    CONSTRAINT chk_weight_multiplier CHECK (weight_multiplier BETWEEN 1 AND 100),
    CONSTRAINT chk_weekly_requires_no_lesson CHECK (activity_type <> 'WEEKLY_STAR' OR lesson_id IS NULL),
    CONSTRAINT chk_weekly_requires_week_monday CHECK (
        assigned_week_start IS NULL OR EXTRACT(ISODOW FROM assigned_week_start) = 1
    ),

    CONSTRAINT chk_test_title_len CHECK (char_length(title) BETWEEN 1 AND 127),
    CONSTRAINT chk_test_topic_len CHECK (char_length(topic) BETWEEN 1 AND 127),
    CONSTRAINT chk_test_desc_len CHECK (description IS NULL OR char_length(description) <= 2048)
);

-- frequently used in topic aggregations
CREATE INDEX IF NOT EXISTS idx_tests_course_topic ON "tests"(course_id, topic);
CREATE INDEX IF NOT EXISTS idx_tests_topic ON "tests"(topic);


CREATE TABLE "test_questions" (
    id             SERIAL PRIMARY KEY,
    test_id        INT NOT NULL REFERENCES "tests"(id) ON DELETE CASCADE,
    order_index    INT NOT NULL,

    question_text  VARCHAR(2048) NOT NULL,
    question_type  VARCHAR(32) NOT NULL DEFAULT 'SINGLE_CHOICE',
    points         INT NOT NULL DEFAULT 1,

    -- for SINGLE_CHOICE
    option_1       VARCHAR(512),
    option_2       VARCHAR(512),
    option_3       VARCHAR(512),
    option_4       VARCHAR(512),
    correct_option INT,

    -- for TEXT
    correct_text_answer VARCHAR(512),

    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_test_question_order UNIQUE (test_id, order_index),
    CONSTRAINT chk_test_q_order_idx CHECK (order_index >= 1),
    CONSTRAINT chk_test_q_text_len CHECK (char_length(question_text) BETWEEN 1 AND 2048),
    CONSTRAINT chk_test_q_points CHECK (points >= 1),
    CONSTRAINT chk_test_q_type CHECK (question_type IN ('SINGLE_CHOICE','TEXT','OPEN')),
    CONSTRAINT chk_test_q_opt1_len CHECK (option_1 IS NULL OR char_length(option_1) BETWEEN 1 AND 512),
    CONSTRAINT chk_test_q_opt2_len CHECK (option_2 IS NULL OR char_length(option_2) BETWEEN 1 AND 512),
    CONSTRAINT chk_test_q_opt3_len CHECK (option_3 IS NULL OR char_length(option_3) BETWEEN 1 AND 512),
    CONSTRAINT chk_test_q_opt4_len CHECK (option_4 IS NULL OR char_length(option_4) BETWEEN 1 AND 512),
    CONSTRAINT chk_test_q_correct CHECK (correct_option IS NULL OR correct_option BETWEEN 1 AND 4),
    CONSTRAINT chk_test_q_correct_text_len CHECK (correct_text_answer IS NULL OR char_length(correct_text_answer) BETWEEN 1 AND 512)
);

----------------------------------------------------------------------
-- 1.5. TEST ATTEMPTS / TEST ATTEMPT ANSWERS (сдача теста студентом)
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "test_attempt_answers",
    "test_attempts"
    CASCADE;

CREATE TABLE "test_attempts" (
    id             SERIAL PRIMARY KEY,
    test_id        INT NOT NULL REFERENCES "tests"(id) ON DELETE CASCADE,
    student_id     INT NOT NULL REFERENCES "users"(id) ON DELETE CASCADE,
    attempt_number INT NOT NULL,

    status         VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    submitted_at   TIMESTAMP,

    score          INT,
    max_score      INT,

    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_test_attempt_student_num UNIQUE (test_id, student_id, attempt_number)
);

CREATE TABLE "test_attempt_answers" (
    id              SERIAL PRIMARY KEY,
    attempt_id      INT NOT NULL REFERENCES "test_attempts"(id) ON DELETE CASCADE,
    question_id     INT NOT NULL REFERENCES "test_questions"(id) ON DELETE CASCADE,
    selected_option INT,
    text_answer     VARCHAR(4096),
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    points_awarded  INT NOT NULL DEFAULT 0,
    feedback        VARCHAR(2048),
    graded_at       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id),
    CONSTRAINT chk_attempt_selected CHECK (selected_option IS NULL OR selected_option BETWEEN 1 AND 4),
    CONSTRAINT chk_attempt_text_len CHECK (text_answer IS NULL OR char_length(text_answer) <= 4096),
    CONSTRAINT chk_attempt_feedback_len CHECK (feedback IS NULL OR char_length(feedback) <= 2048),
    CONSTRAINT chk_attempt_points_awarded CHECK (points_awarded >= 0)
);
