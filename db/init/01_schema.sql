DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'role_name') THEN
        CREATE TYPE role_name AS ENUM ('ADMIN','METHODIST','TEACHER','STUDENT');
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'notification_type') THEN
        CREATE TYPE notification_type AS ENUM (
            'CLASS_JOIN_REQUEST',
            'MANUAL_GRADING_REQUIRED',
            'GRADE_RECEIVED',
            'OPEN_ANSWER_CHECKED',
            'WEEKLY_ASSIGNMENT_AVAILABLE',
            'ACHIEVEMENT_AWARDED'
        );
    END IF;
END$$;


CREATE TABLE IF NOT EXISTS role (
    id          SERIAL PRIMARY KEY,
    rolename    role_name NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS users (
    id        SERIAL PRIMARY KEY,
    role_id   INT NOT NULL REFERENCES role(id),
    name      VARCHAR(63)  NOT NULL UNIQUE,
    email     VARCHAR(127) NOT NULL UNIQUE,
    password  VARCHAR(127) NOT NULL,
    bio       TEXT,
    photo     TEXT,
    tg_id     VARCHAR(127) UNIQUE,
    deleted   BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS courses (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(127) NOT NULL,
    description TEXT,
    created_by  INT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lessons (
    id               SERIAL PRIMARY KEY,
    title            VARCHAR(127) NOT NULL,
    description      VARCHAR(2048),
    presentation_url VARCHAR(512),
    order_index      INT NOT NULL,
    course_id        INT NOT NULL REFERENCES courses(id),
    created_by       INT NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lesson_title_in_course UNIQUE (course_id, title),
    CONSTRAINT uq_lesson_order_in_course UNIQUE (course_id, order_index)
);

CREATE TABLE IF NOT EXISTS classes (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(127) NOT NULL,
    course_id   INT NOT NULL REFERENCES courses(id),
    join_code   VARCHAR(8) NOT NULL UNIQUE,
    teacher_id  INT REFERENCES users(id),
    created_by  INT NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_name_in_course UNIQUE (course_id, name)
);

CREATE TABLE IF NOT EXISTS class_students (
    id               SERIAL PRIMARY KEY,
    class_id         INT NOT NULL REFERENCES classes(id),
    student_id       INT NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    course_closed_at TIMESTAMP,
    CONSTRAINT uq_class_student UNIQUE (class_id, student_id)
);

CREATE TABLE IF NOT EXISTS class_join_requests (
    id         SERIAL PRIMARY KEY,
    class_id   INT NOT NULL REFERENCES classes(id),
    name       VARCHAR(63)  NOT NULL,
    email      VARCHAR(127) NOT NULL,
    tg_id      VARCHAR(127),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_join_request_class_email UNIQUE (class_id, email)
);

CREATE TABLE IF NOT EXISTS methodist_teachers (
    id           SERIAL PRIMARY KEY,
    methodist_id INT NOT NULL REFERENCES users(id),
    teacher_id   INT NOT NULL REFERENCES users(id),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_methodist_teacher UNIQUE (methodist_id, teacher_id)
);

CREATE TABLE IF NOT EXISTS achievements (
    id               SERIAL PRIMARY KEY,
    title            VARCHAR(127)  NOT NULL,
    joke_description VARCHAR(1024) NOT NULL,
    description      VARCHAR(2048) NOT NULL,
    photo_url        VARCHAR(512)  NOT NULL,
    course_id        INT NOT NULL REFERENCES courses(id),
    created_by       INT NOT NULL REFERENCES users(id),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_achievement_title_in_course UNIQUE (course_id, title)
);

CREATE TABLE IF NOT EXISTS student_achievements (
    id             SERIAL PRIMARY KEY,
    student_id     INT NOT NULL REFERENCES users(id),
    achievement_id INT NOT NULL REFERENCES achievements(id),
    awarded_by     INT REFERENCES users(id),
    awarded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_achievement UNIQUE (student_id, achievement_id)
);

CREATE TABLE IF NOT EXISTS class_achievement_feed (
    id             SERIAL PRIMARY KEY,
    class_id       INT NOT NULL REFERENCES classes(id),
    student_id     INT NOT NULL REFERENCES users(id),
    achievement_id INT NOT NULL REFERENCES achievements(id),
    awarded_by     INT REFERENCES users(id),
    awarded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE IF NOT EXISTS tests (
    id                 SERIAL PRIMARY KEY,
    lesson_id           INT REFERENCES lessons(id),
    course_id           INT NOT NULL REFERENCES courses(id),
    activity_type       VARCHAR(32) NOT NULL DEFAULT 'HOMEWORK_TEST',
    weight_multiplier   INT NOT NULL DEFAULT 1,
    assigned_week_start DATE,
    time_limit_seconds  INT,
    created_by          INT NOT NULL REFERENCES users(id),
    title               VARCHAR(127) NOT NULL,
    description         VARCHAR(2048),
    topic               VARCHAR(127) NOT NULL,
    deadline            TIMESTAMP NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    published_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS test_questions (
    id                 SERIAL PRIMARY KEY,
    test_id             INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    order_index         INT NOT NULL,
    question_text       VARCHAR(2048) NOT NULL,
    question_type       VARCHAR(32) NOT NULL DEFAULT 'SINGLE_CHOICE',
    points              INT NOT NULL DEFAULT 1,
    option_1            VARCHAR(512),
    option_2            VARCHAR(512),
    option_3            VARCHAR(512),
    option_4            VARCHAR(512),
    correct_option      INT,
    correct_text_answer VARCHAR(512),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_test_question_order UNIQUE (test_id, order_index)
);

CREATE TABLE IF NOT EXISTS test_attempts (
    id             SERIAL PRIMARY KEY,
    test_id         INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
    student_id      INT NOT NULL REFERENCES users(id),
    attempt_number  INT NOT NULL DEFAULT 1,
    status          VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at    TIMESTAMP,
    score           INT,
    max_score       INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_test_attempt_student_num UNIQUE (test_id, student_id, attempt_number)
);

CREATE TABLE IF NOT EXISTS test_attempt_answers (
    id              SERIAL PRIMARY KEY,
    attempt_id      INT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
    question_id     INT NOT NULL REFERENCES test_questions(id) ON DELETE CASCADE,
    selected_option INT,
    text_answer     VARCHAR(4096),
    is_correct      BOOLEAN NOT NULL DEFAULT FALSE,
    points_awarded  INT NOT NULL DEFAULT 0,
    feedback        VARCHAR(2048),
    graded_at       TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attempt_question UNIQUE (attempt_id, question_id)
);

CREATE TABLE IF NOT EXISTS student_remedial_assignments (
    id                 SERIAL PRIMARY KEY,
    student_id          INT NOT NULL REFERENCES users(id),
    test_id             INT NOT NULL REFERENCES tests(id),
    course_id           INT NOT NULL REFERENCES courses(id),
    topic               VARCHAR(127) NOT NULL,
    assigned_week_start DATE,
    assigned_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    CONSTRAINT uq_student_remedial_assignment UNIQUE (student_id, test_id)
);



CREATE TABLE IF NOT EXISTS notifications (
    id              SERIAL PRIMARY KEY,
    user_id         INT NOT NULL REFERENCES users(id),
    type            notification_type NOT NULL,
    title           VARCHAR(255) NOT NULL,
    message         TEXT,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    course_id       INT,
    class_id        INT,
    test_id         INT,
    attempt_id      INT,
    achievement_id  INT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);



CREATE TABLE IF NOT EXISTS class_opened_lessons (
    id        SERIAL PRIMARY KEY,
    class_id  INT NOT NULL REFERENCES classes(id),
    lesson_id INT NOT NULL REFERENCES lessons(id),
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_opened_lesson UNIQUE (class_id, lesson_id)
);

CREATE TABLE IF NOT EXISTS class_opened_tests (
    id        SERIAL PRIMARY KEY,
    class_id  INT NOT NULL REFERENCES classes(id),
    test_id   INT NOT NULL REFERENCES tests(id),
    opened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_class_opened_test UNIQUE (class_id, test_id)
);


CREATE INDEX IF NOT EXISTS idx_class_opened_lessons_class ON class_opened_lessons(class_id);
CREATE INDEX IF NOT EXISTS idx_class_opened_lessons_lesson ON class_opened_lessons(lesson_id);

CREATE INDEX IF NOT EXISTS idx_class_opened_tests_class ON class_opened_tests(class_id);
CREATE INDEX IF NOT EXISTS idx_class_opened_tests_test ON class_opened_tests(test_id);

CREATE INDEX IF NOT EXISTS idx_test_attempt_test ON test_attempts(test_id);
CREATE INDEX IF NOT EXISTS idx_test_attempt_student ON test_attempts(student_id);
CREATE INDEX IF NOT EXISTS idx_test_attempt_status ON test_attempts(status);

CREATE INDEX IF NOT EXISTS idx_attempt_answer_attempt ON test_attempt_answers(attempt_id);
CREATE INDEX IF NOT EXISTS idx_attempt_answer_question ON test_attempt_answers(question_id);

CREATE INDEX IF NOT EXISTS idx_sra_student ON student_remedial_assignments(student_id);
CREATE INDEX IF NOT EXISTS idx_sra_course ON student_remedial_assignments(course_id);
CREATE INDEX IF NOT EXISTS idx_sra_topic ON student_remedial_assignments(topic);
CREATE INDEX IF NOT EXISTS idx_sra_student_course_week ON student_remedial_assignments(student_id, course_id, assigned_week_start);
