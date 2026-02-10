-- 01_schema.sql
-- Schema + constraints + triggers + functions/procedures (PostgreSQL)

------------------------------------------------------------
-- 1) TYPES (enums)
------------------------------------------------------------
CREATE TYPE role_name AS ENUM ('ADMIN', 'METHODIST', 'TEACHER', 'STUDENT');

CREATE TYPE notification_type AS ENUM (
  'CLASS_JOIN_REQUEST',
  'MANUAL_GRADING_REQUIRED',
  'GRADE_RECEIVED',
  'OPEN_ANSWER_CHECKED',
  'WEEKLY_ASSIGNMENT_AVAILABLE',
  'ACHIEVEMENT_AWARDED'
);

------------------------------------------------------------
-- 2) CORE TABLES: roles + users + notifications
------------------------------------------------------------
CREATE TABLE role (
  id          SERIAL PRIMARY KEY,
  rolename    role_name NOT NULL UNIQUE,
  description TEXT
);

CREATE TABLE users (
  id          SERIAL PRIMARY KEY,
  role_id     INT NOT NULL REFERENCES role(id),
  name        VARCHAR(63)  NOT NULL UNIQUE,
  email       VARCHAR(127) NOT NULL UNIQUE,
  password    VARCHAR(127) NOT NULL,
  bio         TEXT,
  photo       TEXT,
  tg_id       VARCHAR(127) UNIQUE,
  created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_users_name_len  CHECK (char_length(name) BETWEEN 1 AND 63),
  CONSTRAINT chk_users_email_len CHECK (char_length(email) BETWEEN 3 AND 127),
  CONSTRAINT chk_users_tg_len    CHECK (tg_id IS NULL OR char_length(tg_id) BETWEEN 1 AND 127)
);

-- Case-insensitive uniqueness for email (recommended)
CREATE UNIQUE INDEX ux_users_email_lower ON users (LOWER(email));

-- Link between METHODIST and their managed TEACHERs (ownership for access control)
CREATE TABLE methodist_teachers (
  id           SERIAL PRIMARY KEY,
  methodist_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  teacher_id   INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_methodist_teacher UNIQUE (methodist_id, teacher_id)
);

CREATE INDEX idx_methodist_teachers_methodist ON methodist_teachers(methodist_id);
CREATE INDEX idx_methodist_teachers_teacher   ON methodist_teachers(teacher_id);

CREATE TABLE notifications (
  id              SERIAL PRIMARY KEY,
  user_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  type            notification_type NOT NULL,
  title           VARCHAR(255) NOT NULL,
  message         TEXT,
  is_read         BOOLEAN NOT NULL DEFAULT FALSE,

  course_id       INT,
  class_id        INT,
  test_id         INT,
  attempt_id      INT,
  achievement_id  INT,

  created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

------------------------------------------------------------
-- 3) DOMAIN TABLES: courses / lessons / classes
------------------------------------------------------------
CREATE TABLE courses (
  id           SERIAL PRIMARY KEY,
  name         VARCHAR(127) NOT NULL,
  description  TEXT,
  created_by   INT NOT NULL REFERENCES users(id),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_courses_name_len CHECK (char_length(name) BETWEEN 1 AND 127)
);

CREATE TABLE lessons (
  id                SERIAL PRIMARY KEY,
  course_id          INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  created_by         INT NOT NULL REFERENCES users(id),
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

CREATE TABLE classes (
  id           SERIAL PRIMARY KEY,
  name         VARCHAR(127) NOT NULL,
  join_code    VARCHAR(8) NOT NULL UNIQUE,
  course_id    INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  teacher_id   INT REFERENCES users(id) ON DELETE SET NULL,
  created_by   INT NOT NULL REFERENCES users(id),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_class_name_in_course UNIQUE (course_id, name),
  CONSTRAINT chk_class_join_code_len CHECK (char_length(join_code) = 8)
);

CREATE INDEX idx_classes_course ON classes(course_id);
CREATE INDEX idx_lessons_course_order ON lessons(course_id, order_index);

------------------------------------------------------------
-- 3.1) Teacher opens lessons for a class
------------------------------------------------------------
CREATE TABLE class_opened_lessons (
  id         SERIAL PRIMARY KEY,
  class_id   INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  lesson_id  INT NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
  opened_at  TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_class_opened_lesson UNIQUE (class_id, lesson_id)
);

CREATE INDEX idx_class_opened_lessons_class ON class_opened_lessons(class_id);

------------------------------------------------------------
-- 4) Join requests + class students (M:N)
------------------------------------------------------------
CREATE TABLE class_join_requests (
  id           SERIAL PRIMARY KEY,
  class_id     INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  name         VARCHAR(63) NOT NULL,
  email        VARCHAR(127) NOT NULL,
  tg_id        VARCHAR(127),
  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_join_request_class_email UNIQUE (class_id, email),
  CONSTRAINT chk_cjr_name_len CHECK (char_length(name) BETWEEN 1 AND 63),
  CONSTRAINT chk_cjr_email_len CHECK (char_length(email) BETWEEN 3 AND 127),
  CONSTRAINT chk_cjr_tg_len CHECK (tg_id IS NULL OR char_length(tg_id) BETWEEN 1 AND 127)
);

CREATE INDEX idx_join_requests_class ON class_join_requests(class_id);

CREATE TABLE class_students (
  id              SERIAL PRIMARY KEY,
  class_id         INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  student_id       INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
  course_closed_at TIMESTAMP,

  CONSTRAINT uq_class_student UNIQUE (class_id, student_id)
);

CREATE INDEX idx_class_students_class ON class_students(class_id);
CREATE INDEX idx_class_students_student ON class_students(student_id);

------------------------------------------------------------
-- 5) Achievements (course-scoped) + awarded achievements (M:N)
------------------------------------------------------------
CREATE TABLE achievements (
  id                SERIAL PRIMARY KEY,
  course_id          INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  created_by         INT NOT NULL REFERENCES users(id),
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

CREATE TABLE student_achievements (
  id                SERIAL PRIMARY KEY,
  student_id        INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  achievement_id    INT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
  awarded_by        INT REFERENCES users(id) ON DELETE SET NULL,
  awarded_at        TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_student_achievement UNIQUE (student_id, achievement_id)
);

CREATE INDEX idx_student_achievements_student ON student_achievements(student_id);

-- Class-wide feed for awarded achievements (denormalized for fast feed queries)
CREATE TABLE class_achievement_feed (
  id             SERIAL PRIMARY KEY,
  class_id       INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  student_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  achievement_id INT NOT NULL REFERENCES achievements(id) ON DELETE CASCADE,
  awarded_by     INT REFERENCES users(id) ON DELETE SET NULL,
  awarded_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  created_at     TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_class_feed_item UNIQUE (class_id, student_id, achievement_id)
);

CREATE INDEX idx_class_achievement_feed_class_created_at
  ON class_achievement_feed(class_id, created_at DESC);

------------------------------------------------------------
-- 6) Tests (aka activities) + questions
------------------------------------------------------------
CREATE TABLE tests (
  id           SERIAL PRIMARY KEY,
  lesson_id    INT REFERENCES lessons(id) ON DELETE SET NULL,
  course_id    INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  created_by   INT NOT NULL REFERENCES users(id),

  activity_type      VARCHAR(32) NOT NULL DEFAULT 'HOMEWORK_TEST',
  weight_multiplier  INT NOT NULL DEFAULT 1,
  assigned_week_start DATE,
  time_limit_seconds INT,

  title        VARCHAR(127) NOT NULL,
  description  VARCHAR(2048),
  topic        VARCHAR(127) NOT NULL,
  deadline     TIMESTAMP NOT NULL,

  status       VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
  published_at TIMESTAMP,

  created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_activity_type CHECK (activity_type IN ('HOMEWORK_TEST','CONTROL_WORK','WEEKLY_STAR','REMEDIAL_TASK')),
  CONSTRAINT chk_weight_multiplier CHECK (weight_multiplier BETWEEN 1 AND 100),
  CONSTRAINT chk_time_limit_seconds CHECK (time_limit_seconds IS NULL OR time_limit_seconds BETWEEN 1 AND 86400),
  CONSTRAINT chk_weekly_requires_no_lesson CHECK (activity_type NOT IN ('WEEKLY_STAR','REMEDIAL_TASK') OR lesson_id IS NULL),
  CONSTRAINT chk_weekly_requires_week_monday CHECK (
    assigned_week_start IS NULL OR EXTRACT(ISODOW FROM assigned_week_start) = 1
  ),
  CONSTRAINT chk_test_status CHECK (status IN ('DRAFT','READY')),
  CONSTRAINT chk_test_title_len CHECK (char_length(title) BETWEEN 1 AND 127),
  CONSTRAINT chk_test_topic_len CHECK (char_length(topic) BETWEEN 1 AND 127),
  CONSTRAINT chk_test_desc_len CHECK (description IS NULL OR char_length(description) <= 2048)
);

CREATE INDEX idx_tests_course_topic ON tests(course_id, topic);
CREATE INDEX idx_tests_lesson ON tests(lesson_id);

------------------------------------------------------------
-- 3.2) Teacher opens tests for a class
------------------------------------------------------------
CREATE TABLE class_opened_tests (
  id         SERIAL PRIMARY KEY,
  class_id   INT NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
  test_id    INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
  opened_at  TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_class_opened_test UNIQUE (class_id, test_id)
);

CREATE INDEX idx_class_opened_tests_class ON class_opened_tests(class_id);
CREATE INDEX idx_class_opened_tests_test  ON class_opened_tests(test_id);

CREATE TABLE test_questions (
  id             SERIAL PRIMARY KEY,
  test_id        INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
  order_index    INT NOT NULL,

  question_text  VARCHAR(2048) NOT NULL,
  question_type  VARCHAR(32) NOT NULL DEFAULT 'SINGLE_CHOICE',
  points         INT NOT NULL DEFAULT 1,

  option_1       VARCHAR(512),
  option_2       VARCHAR(512),
  option_3       VARCHAR(512),
  option_4       VARCHAR(512),
  correct_option INT,

  correct_text_answer VARCHAR(512),

  created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_test_question_order UNIQUE (test_id, order_index),
  CONSTRAINT chk_test_q_order_idx CHECK (order_index >= 1),
  CONSTRAINT chk_test_q_text_len CHECK (char_length(question_text) BETWEEN 1 AND 2048),
  CONSTRAINT chk_test_q_points CHECK (points >= 1),
  CONSTRAINT chk_test_q_type CHECK (question_type IN ('SINGLE_CHOICE','TEXT','OPEN')),
  CONSTRAINT chk_test_q_correct CHECK (correct_option IS NULL OR correct_option BETWEEN 1 AND 4),
  CONSTRAINT chk_test_q_correct_text_len CHECK (correct_text_answer IS NULL OR char_length(correct_text_answer) BETWEEN 1 AND 512)
);

CREATE INDEX idx_test_questions_test_order ON test_questions(test_id, order_index);

------------------------------------------------------------
-- 6.1) Remedial assignments (optional feature)
------------------------------------------------------------
CREATE TABLE student_remedial_assignments (
  id                 SERIAL PRIMARY KEY,
  student_id         INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  test_id            INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
  course_id          INT NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
  topic              VARCHAR(127) NOT NULL,
  assigned_week_start DATE,
  assigned_at        TIMESTAMP NOT NULL DEFAULT NOW(),
  completed_at       TIMESTAMP,

  CONSTRAINT uq_student_remedial_assignment UNIQUE (student_id, test_id),
  CONSTRAINT chk_sra_topic_len CHECK (char_length(topic) BETWEEN 1 AND 127),
  CONSTRAINT chk_sra_week_monday CHECK (
    assigned_week_start IS NULL OR EXTRACT(ISODOW FROM assigned_week_start) = 1
  )
);

CREATE INDEX idx_sra_student_course_week ON student_remedial_assignments(student_id, course_id, assigned_week_start);

------------------------------------------------------------
-- 7) Attempts + answers
------------------------------------------------------------
CREATE TABLE test_attempts (
  id             SERIAL PRIMARY KEY,
  test_id        INT NOT NULL REFERENCES tests(id) ON DELETE CASCADE,
  student_id     INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  attempt_number INT NOT NULL,

  status         VARCHAR(24) NOT NULL DEFAULT 'IN_PROGRESS',
  started_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  submitted_at   TIMESTAMP,

  score          INT,
  max_score      INT,

  created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at     TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uq_test_attempt_student_num UNIQUE (test_id, student_id, attempt_number),
  CONSTRAINT chk_attempt_status CHECK (status IN ('IN_PROGRESS','SUBMITTED','NEEDS_MANUAL_GRADING','GRADED'))
);

CREATE INDEX idx_attempts_test_student_status ON test_attempts(test_id, student_id, status);

CREATE TABLE test_attempt_answers (
  id              SERIAL PRIMARY KEY,
  attempt_id      INT NOT NULL REFERENCES test_attempts(id) ON DELETE CASCADE,
  question_id     INT NOT NULL REFERENCES test_questions(id) ON DELETE CASCADE,
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

CREATE INDEX idx_attempt_answers_attempt ON test_attempt_answers(attempt_id);

------------------------------------------------------------
-- 8) Add FK constraints to notifications (data integrity)
------------------------------------------------------------
ALTER TABLE notifications
  ADD CONSTRAINT fk_notifications_course
    FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notifications_class
    FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notifications_test
    FOREIGN KEY (test_id) REFERENCES tests(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notifications_attempt
    FOREIGN KEY (attempt_id) REFERENCES test_attempts(id) ON DELETE SET NULL,
  ADD CONSTRAINT fk_notifications_achievement
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE SET NULL;

-- Required refs by notification type (examples, extend as needed)
ALTER TABLE notifications
  ADD CONSTRAINT chk_notifications_required_refs
  CHECK (
    (type = 'CLASS_JOIN_REQUEST' AND class_id IS NOT NULL)
    OR
    (type IN ('MANUAL_GRADING_REQUIRED','GRADE_RECEIVED','OPEN_ANSWER_CHECKED') AND attempt_id IS NOT NULL)
    OR
    (type = 'WEEKLY_ASSIGNMENT_AVAILABLE' AND course_id IS NOT NULL AND test_id IS NOT NULL)
    OR
    (type = 'ACHIEVEMENT_AWARDED' AND achievement_id IS NOT NULL)
  );

------------------------------------------------------------
-- 9) TRIGGERS: updated_at auto-maintenance
------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_courses_updated_at
BEFORE UPDATE ON courses
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_lessons_updated_at
BEFORE UPDATE ON lessons
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_classes_updated_at
BEFORE UPDATE ON classes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_achievements_updated_at
BEFORE UPDATE ON achievements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_tests_updated_at
BEFORE UPDATE ON tests
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_test_questions_updated_at
BEFORE UPDATE ON test_questions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_test_attempts_updated_at
BEFORE UPDATE ON test_attempts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_test_attempt_answers_updated_at
BEFORE UPDATE ON test_attempt_answers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

------------------------------------------------------------
-- 10) Helper: get user's role_name (to enforce role-based invariants)
------------------------------------------------------------
CREATE OR REPLACE FUNCTION get_user_role_name(p_user_id INT)
RETURNS role_name AS $$
DECLARE
  v_role role_name;
BEGIN
  SELECT r.rolename INTO v_role
  FROM users u
  JOIN role r ON r.id = u.role_id
  WHERE u.id = p_user_id;

  IF v_role IS NULL THEN
    RAISE EXCEPTION 'User % not found (role check)', p_user_id;
  END IF;

  RETURN v_role;
END;
$$ LANGUAGE plpgsql;

------------------------------------------------------------
-- 11) TRIGGERS: role invariants
------------------------------------------------------------
-- 11.1 classes.teacher_id must be TEACHER or METHODIST (or NULL)
CREATE OR REPLACE FUNCTION validate_class_teacher_role()
RETURNS TRIGGER AS $$
DECLARE
  v_role role_name;
BEGIN
  IF NEW.teacher_id IS NULL THEN
    RETURN NEW;
  END IF;

  v_role := get_user_role_name(NEW.teacher_id);
  IF v_role NOT IN ('TEACHER','METHODIST') THEN
    RAISE EXCEPTION 'classes.teacher_id must be TEACHER or METHODIST (got %)', v_role;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_classes_teacher_role
BEFORE INSERT OR UPDATE ON classes
FOR EACH ROW EXECUTE FUNCTION validate_class_teacher_role();

-- 11.2 class_students.student_id must be STUDENT
CREATE OR REPLACE FUNCTION validate_class_student_role()
RETURNS TRIGGER AS $$
DECLARE
  v_role role_name;
BEGIN
  v_role := get_user_role_name(NEW.student_id);
  IF v_role <> 'STUDENT' THEN
    RAISE EXCEPTION 'class_students.student_id must be STUDENT (got %)', v_role;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_class_students_student_role
BEFORE INSERT OR UPDATE ON class_students
FOR EACH ROW EXECUTE FUNCTION validate_class_student_role();

-- 11.3 student_achievements.awarded_by must NOT be STUDENT (or NULL)
CREATE OR REPLACE FUNCTION validate_student_achievement_awarder_role()
RETURNS TRIGGER AS $$
DECLARE
  v_role role_name;
BEGIN
  IF NEW.awarded_by IS NULL THEN
    RETURN NEW;
  END IF;

  v_role := get_user_role_name(NEW.awarded_by);
  IF v_role = 'STUDENT' THEN
    RAISE EXCEPTION 'student_achievements.awarded_by cannot be STUDENT';
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_student_achievements_awarder_role
BEFORE INSERT OR UPDATE ON student_achievements
FOR EACH ROW EXECUTE FUNCTION validate_student_achievement_awarder_role();

-- 11.4 methodist_teachers.methodist_id must be METHODIST;
--     methodist_teachers.teacher_id must be TEACHER or METHODIST (methodist may manage themselves).
CREATE OR REPLACE FUNCTION validate_methodist_teacher_roles()
RETURNS TRIGGER AS $$
DECLARE
  v_methodist_role role_name;
  v_teacher_role   role_name;
BEGIN
  v_methodist_role := get_user_role_name(NEW.methodist_id);
  IF v_methodist_role <> 'METHODIST' THEN
    RAISE EXCEPTION 'methodist_teachers.methodist_id must be METHODIST (got %)', v_methodist_role;
  END IF;

  v_teacher_role := get_user_role_name(NEW.teacher_id);
  IF v_teacher_role NOT IN ('TEACHER','METHODIST') THEN
    RAISE EXCEPTION 'methodist_teachers.teacher_id must be TEACHER or METHODIST (got %)', v_teacher_role;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_methodist_teachers_roles
BEFORE INSERT OR UPDATE ON methodist_teachers
FOR EACH ROW EXECUTE FUNCTION validate_methodist_teacher_roles();

------------------------------------------------------------
-- 12) TRIGGER: tests.course_id must match lessons.course_id when lesson_id is set
------------------------------------------------------------
CREATE OR REPLACE FUNCTION validate_tests_course_matches_lesson()
RETURNS TRIGGER AS $$
DECLARE
  v_lesson_course_id INT;
BEGIN
  IF NEW.lesson_id IS NULL THEN
    RETURN NEW;
  END IF;

  SELECT course_id INTO v_lesson_course_id
  FROM lessons
  WHERE id = NEW.lesson_id;

  IF v_lesson_course_id IS NULL THEN
    RAISE EXCEPTION 'Lesson % not found for tests.lesson_id', NEW.lesson_id;
  END IF;

  IF NEW.course_id <> v_lesson_course_id THEN
    RAISE EXCEPTION 'tests.course_id % must match lessons.course_id % (lesson_id=%)',
      NEW.course_id, v_lesson_course_id, NEW.lesson_id;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_tests_course_matches_lesson
BEFORE INSERT OR UPDATE ON tests
FOR EACH ROW EXECUTE FUNCTION validate_tests_course_matches_lesson();

------------------------------------------------------------
-- 12.1) TRIGGER: class_opened_tests integrity
--   - opened test must belong to the same course as the class
--   - test must be READY (published)
------------------------------------------------------------
CREATE OR REPLACE FUNCTION validate_class_opened_test_consistency()
RETURNS TRIGGER AS $$
DECLARE
  v_class_course_id INT;
  v_test_course_id  INT;
  v_test_status     VARCHAR(16);
BEGIN
  SELECT course_id INTO v_class_course_id FROM classes WHERE id = NEW.class_id;
  IF v_class_course_id IS NULL THEN
    RAISE EXCEPTION 'Class % not found for class_opened_tests.class_id', NEW.class_id;
  END IF;

  SELECT course_id, status INTO v_test_course_id, v_test_status FROM tests WHERE id = NEW.test_id;
  IF v_test_course_id IS NULL THEN
    RAISE EXCEPTION 'Test % not found for class_opened_tests.test_id', NEW.test_id;
  END IF;

  IF v_class_course_id <> v_test_course_id THEN
    RAISE EXCEPTION 'class_opened_tests: class % (course %) and test % (course %) mismatch',
      NEW.class_id, v_class_course_id, NEW.test_id, v_test_course_id;
  END IF;

  IF v_test_status IS DISTINCT FROM 'READY' THEN
    RAISE EXCEPTION 'class_opened_tests: test % must be READY to be opened (status=%)', NEW.test_id, v_test_status;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_class_opened_tests_consistency
BEFORE INSERT OR UPDATE ON class_opened_tests
FOR EACH ROW EXECUTE FUNCTION validate_class_opened_test_consistency();

------------------------------------------------------------
-- 13) TRIGGER: notifications consistency (cross-table invariants)
------------------------------------------------------------
CREATE OR REPLACE FUNCTION validate_notification_consistency()
RETURNS TRIGGER AS $$
DECLARE
  v_class_course_id INT;
  v_test_course_id  INT;
  v_attempt_test_id INT;
  v_attempt_student_id INT;
  v_ach_course_id   INT;
BEGIN
  -- class -> course
  IF NEW.class_id IS NOT NULL THEN
    SELECT course_id INTO v_class_course_id FROM classes WHERE id = NEW.class_id;

    IF NEW.course_id IS NOT NULL AND v_class_course_id IS NOT NULL AND v_class_course_id <> NEW.course_id THEN
      RAISE EXCEPTION 'notifications: class_id % does not belong to course_id %', NEW.class_id, NEW.course_id;
    END IF;
  END IF;

  -- test -> course (+ class/course match if both present)
  IF NEW.test_id IS NOT NULL THEN
    SELECT course_id INTO v_test_course_id FROM tests WHERE id = NEW.test_id;

    IF NEW.course_id IS NOT NULL AND v_test_course_id IS NOT NULL AND v_test_course_id <> NEW.course_id THEN
      RAISE EXCEPTION 'notifications: test_id % does not belong to course_id %', NEW.test_id, NEW.course_id;
    END IF;

    IF NEW.class_id IS NOT NULL AND v_class_course_id IS NOT NULL AND v_test_course_id IS NOT NULL
       AND v_class_course_id <> v_test_course_id THEN
      RAISE EXCEPTION 'notifications: class_id % and test_id % belong to different courses', NEW.class_id, NEW.test_id;
    END IF;
  END IF;

  -- attempt belongs to user and (optionally) test
  IF NEW.attempt_id IS NOT NULL THEN
    SELECT test_id, student_id INTO v_attempt_test_id, v_attempt_student_id
    FROM test_attempts WHERE id = NEW.attempt_id;

  -- ownership check: только для "студенческих" уведомлений
    IF NEW.type IN ('GRADE_RECEIVED', 'OPEN_ANSWER_CHECKED')
      AND v_attempt_student_id IS NOT NULL
      AND v_attempt_student_id <> NEW.user_id THEN
      RAISE EXCEPTION 'notifications: attempt_id % does not belong to user_id %',
        NEW.attempt_id, NEW.user_id;
    END IF;

    IF NEW.test_id IS NOT NULL AND v_attempt_test_id IS NOT NULL AND v_attempt_test_id <> NEW.test_id THEN
      RAISE EXCEPTION 'notifications: attempt_id % is for test_id %, but notifications.test_id is %',
        NEW.attempt_id, v_attempt_test_id, NEW.test_id;
    END IF;
  END IF;

  -- achievement -> course
  IF NEW.achievement_id IS NOT NULL THEN
    SELECT course_id INTO v_ach_course_id FROM achievements WHERE id = NEW.achievement_id;

    IF NEW.course_id IS NOT NULL AND v_ach_course_id IS NOT NULL AND v_ach_course_id <> NEW.course_id THEN
      RAISE EXCEPTION 'notifications: achievement_id % does not belong to course_id %', NEW.achievement_id, NEW.course_id;
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_notification_consistency
BEFORE INSERT OR UPDATE ON notifications
FOR EACH ROW EXECUTE FUNCTION validate_notification_consistency();

------------------------------------------------------------
-- 14) TRIGGERS: attempt lifecycle helpers
------------------------------------------------------------
-- 14.1 set submitted_at when status becomes SUBMITTED/NEEDS_MANUAL_GRADING/GRADED
CREATE OR REPLACE FUNCTION set_submitted_at_on_status()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.status IN ('SUBMITTED','NEEDS_MANUAL_GRADING','GRADED') AND NEW.submitted_at IS NULL THEN
    NEW.submitted_at := NOW();
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_attempts_set_submitted_at
BEFORE UPDATE ON test_attempts
FOR EACH ROW EXECUTE FUNCTION set_submitted_at_on_status();

-- 14.2 Recalculate attempt score/max_score after answers change
CREATE OR REPLACE FUNCTION recalc_attempt_score()
RETURNS TRIGGER AS $$
DECLARE
  v_attempt_id INT;
  v_test_id INT;
  v_score INT;
  v_max INT;
  v_needs_manual BOOLEAN;
BEGIN
  v_attempt_id := COALESCE(NEW.attempt_id, OLD.attempt_id);

  SELECT test_id INTO v_test_id FROM test_attempts WHERE id = v_attempt_id;

  SELECT COALESCE(SUM(points_awarded),0) INTO v_score
  FROM test_attempt_answers
  WHERE attempt_id = v_attempt_id;

  SELECT COALESCE(SUM(points),0) INTO v_max
  FROM test_questions
  WHERE test_id = v_test_id;

  -- needs manual if there exists OPEN question with graded_at NULL
  SELECT EXISTS (
    SELECT 1
    FROM test_attempt_answers a
    JOIN test_questions q ON q.id = a.question_id
    WHERE a.attempt_id = v_attempt_id
      AND q.question_type = 'OPEN'
      AND a.graded_at IS NULL
  ) INTO v_needs_manual;

  UPDATE test_attempts
  SET score = v_score,
      max_score = v_max,
      status = CASE
        WHEN status = 'IN_PROGRESS' THEN status
        WHEN v_needs_manual THEN 'NEEDS_MANUAL_GRADING'
        ELSE 'GRADED'
      END,
      updated_at = NOW()
  WHERE id = v_attempt_id;

  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_attempt_answers_recalc
AFTER INSERT OR UPDATE OR DELETE ON test_attempt_answers
FOR EACH ROW EXECUTE FUNCTION recalc_attempt_score();

------------------------------------------------------------
-- 15) PROCEDURES / FUNCTIONS for critical use-cases (SRS)
------------------------------------------------------------

-- 15.1 Create STUDENT by tg_id (unique) + email (case-insensitive unique)
CREATE OR REPLACE FUNCTION create_student_by_tg(
  p_tg_id TEXT,
  p_name TEXT,
  p_email TEXT,
  p_password TEXT
) RETURNS INT AS $$
DECLARE
  v_role_id INT;
  v_user_id INT;
BEGIN
  IF p_tg_id IS NULL OR char_length(p_tg_id) = 0 THEN
    RAISE EXCEPTION 'tg_id is required';
  END IF;

  SELECT id INTO v_role_id FROM role WHERE rolename = 'STUDENT';
  IF v_role_id IS NULL THEN
    RAISE EXCEPTION 'Role STUDENT not found in role table';
  END IF;

  IF EXISTS (SELECT 1 FROM users WHERE tg_id = p_tg_id) THEN
    RAISE EXCEPTION 'tg_id already exists: %', p_tg_id USING ERRCODE = '23505';
  END IF;

  IF EXISTS (SELECT 1 FROM users WHERE LOWER(email) = LOWER(p_email)) THEN
    RAISE EXCEPTION 'email already exists: %', p_email USING ERRCODE = '23505';
  END IF;

  INSERT INTO users(role_id, name, email, password, tg_id)
  VALUES (v_role_id, p_name, p_email, p_password, p_tg_id)
  RETURNING id INTO v_user_id;

  RETURN v_user_id;
END;
$$ LANGUAGE plpgsql;

-- 15.2 Create join request by join_code (creates notification to teacher if set)
CREATE OR REPLACE FUNCTION create_join_request_by_code(
  p_join_code TEXT,
  p_name TEXT,
  p_email TEXT,
  p_tg_id TEXT
) RETURNS INT AS $$
DECLARE
  v_class_id INT;
  v_teacher_id INT;
  v_course_id INT;
  v_req_id INT;
BEGIN
  SELECT c.id, c.teacher_id, c.course_id
    INTO v_class_id, v_teacher_id, v_course_id
  FROM classes c
  WHERE c.join_code = p_join_code;

  IF v_class_id IS NULL THEN
    RAISE EXCEPTION 'Class with join_code % not found', p_join_code;
  END IF;

  INSERT INTO class_join_requests(class_id, name, email, tg_id)
  VALUES (v_class_id, p_name, p_email, p_tg_id)
  RETURNING id INTO v_req_id;

  IF v_teacher_id IS NOT NULL THEN
    INSERT INTO notifications(user_id, type, title, message, course_id, class_id)
    VALUES (
      v_teacher_id,
      'CLASS_JOIN_REQUEST',
      'New class join request',
      format('New join request for class_id=%s from %s <%s>', v_class_id, p_name, p_email),
      v_course_id,
      v_class_id
    );
  END IF;

  RETURN v_req_id;
EXCEPTION
  WHEN unique_violation THEN
    RAISE EXCEPTION 'Join request already exists for this class and email';
END;
$$ LANGUAGE plpgsql;

-- 15.3 Approve join request (enroll student, delete request, notify student)
CREATE OR REPLACE FUNCTION approve_join_request(
  p_request_id INT,
  p_teacher_id INT
) RETURNS VOID AS $$
DECLARE
  v_class_id INT;
  v_email TEXT;
  v_tg_id TEXT;
  v_course_id INT;
  v_student_id INT;
  v_class_teacher_id INT;
BEGIN
  SELECT r.class_id, r.email, r.tg_id
  INTO v_class_id, v_email, v_tg_id
  FROM class_join_requests r
  WHERE r.id = p_request_id;

  IF v_class_id IS NULL THEN
    RAISE EXCEPTION 'Join request % not found', p_request_id;
  END IF;

  SELECT c.teacher_id, c.course_id INTO v_class_teacher_id, v_course_id
  FROM classes c WHERE c.id = v_class_id;

  IF v_class_teacher_id IS NOT NULL AND v_class_teacher_id <> p_teacher_id THEN
    RAISE EXCEPTION 'Only assigned teacher can approve join requests for this class';
  END IF;

  IF v_tg_id IS NOT NULL THEN
    SELECT id INTO v_student_id FROM users WHERE tg_id = v_tg_id;
  END IF;

  IF v_student_id IS NULL THEN
    SELECT id INTO v_student_id FROM users WHERE LOWER(email) = LOWER(v_email);
  END IF;

  IF v_student_id IS NULL THEN
    RAISE EXCEPTION 'Student user not found for join request (email=%)', v_email;
  END IF;

  INSERT INTO class_students(class_id, student_id)
  VALUES (v_class_id, v_student_id)
  ON CONFLICT (class_id, student_id) DO NOTHING;

  DELETE FROM class_join_requests WHERE id = p_request_id;

  INSERT INTO notifications(user_id, type, title, message, course_id, class_id)
  VALUES (
    v_student_id,
    'CLASS_JOIN_REQUEST',
    'You were enrolled to class',
    format('Your join request was approved. class_id=%s', v_class_id),
    v_course_id,
    v_class_id
  );
END;
$$ LANGUAGE plpgsql;

-- 15.4 Start attempt (auto attempt_number)
CREATE OR REPLACE FUNCTION start_attempt(
  p_test_id INT,
  p_student_id INT
) RETURNS INT AS $$
DECLARE
  v_role role_name;
  v_next_num INT;
  v_attempt_id INT;
BEGIN
  v_role := get_user_role_name(p_student_id);
  IF v_role <> 'STUDENT' THEN
    RAISE EXCEPTION 'Only STUDENT can start attempt (got %)', v_role;
  END IF;

  SELECT COALESCE(MAX(attempt_number), 0) + 1
    INTO v_next_num
  FROM test_attempts
  WHERE test_id = p_test_id AND student_id = p_student_id;

  INSERT INTO test_attempts(test_id, student_id, attempt_number, status)
  VALUES (p_test_id, p_student_id, v_next_num, 'IN_PROGRESS')
  RETURNING id INTO v_attempt_id;

  RETURN v_attempt_id;
END;
$$ LANGUAGE plpgsql;

-- 15.5 Submit attempt with answers (JSONB array)
CREATE OR REPLACE FUNCTION submit_attempt(
  p_attempt_id INT,
  p_answers JSONB
) RETURNS VOID AS $$
DECLARE
  v_test_id INT;
  v_has_open BOOLEAN := FALSE;
  v_item JSONB;
  v_qid INT;
  v_sel INT;
  v_txt TEXT;

  v_qtype TEXT;
  v_points INT;
  v_correct_opt INT;
  v_correct_txt TEXT;

  v_awarded INT;
  v_is_correct BOOLEAN;
BEGIN
  SELECT test_id INTO v_test_id FROM test_attempts WHERE id = p_attempt_id;
  IF v_test_id IS NULL THEN
    RAISE EXCEPTION 'Attempt % not found', p_attempt_id;
  END IF;

  IF jsonb_typeof(p_answers) <> 'array' THEN
    RAISE EXCEPTION 'answers must be JSON array';
  END IF;

  FOR v_item IN SELECT * FROM jsonb_array_elements(p_answers)
  LOOP
    v_qid := (v_item->>'questionId')::INT;
    v_sel := NULLIF(v_item->>'selectedOption','')::INT;
    v_txt := NULLIF(v_item->>'textAnswer','');

    SELECT question_type, points, correct_option, correct_text_answer
      INTO v_qtype, v_points, v_correct_opt, v_correct_txt
    FROM test_questions
    WHERE id = v_qid AND test_id = v_test_id;

    IF v_qtype IS NULL THEN
      RAISE EXCEPTION 'Question % not found in test %', v_qid, v_test_id;
    END IF;

    v_awarded := 0;
    v_is_correct := FALSE;

    IF v_qtype = 'SINGLE_CHOICE' THEN
      IF v_sel IS NOT NULL AND v_correct_opt IS NOT NULL AND v_sel = v_correct_opt THEN
        v_awarded := v_points;
        v_is_correct := TRUE;
      END IF;

      INSERT INTO test_attempt_answers(attempt_id, question_id, selected_option, text_answer, is_correct, points_awarded, graded_at)
      VALUES (p_attempt_id, v_qid, v_sel, v_txt, v_is_correct, v_awarded, NOW())
      ON CONFLICT (attempt_id, question_id) DO UPDATE
        SET selected_option = EXCLUDED.selected_option,
            text_answer = EXCLUDED.text_answer,
            is_correct = EXCLUDED.is_correct,
            points_awarded = EXCLUDED.points_awarded,
            graded_at = EXCLUDED.graded_at,
            updated_at = NOW();

    ELSIF v_qtype = 'TEXT' THEN
      IF v_txt IS NOT NULL AND v_correct_txt IS NOT NULL AND LOWER(TRIM(v_txt)) = LOWER(TRIM(v_correct_txt)) THEN
        v_awarded := v_points;
        v_is_correct := TRUE;
      END IF;

      INSERT INTO test_attempt_answers(attempt_id, question_id, selected_option, text_answer, is_correct, points_awarded, graded_at)
      VALUES (p_attempt_id, v_qid, v_sel, v_txt, v_is_correct, v_awarded, NOW())
      ON CONFLICT (attempt_id, question_id) DO UPDATE
        SET selected_option = EXCLUDED.selected_option,
            text_answer = EXCLUDED.text_answer,
            is_correct = EXCLUDED.is_correct,
            points_awarded = EXCLUDED.points_awarded,
            graded_at = EXCLUDED.graded_at,
            updated_at = NOW();

    ELSIF v_qtype = 'OPEN' THEN
      v_has_open := TRUE;

      INSERT INTO test_attempt_answers(attempt_id, question_id, selected_option, text_answer, is_correct, points_awarded, graded_at)
      VALUES (p_attempt_id, v_qid, v_sel, v_txt, FALSE, 0, NULL)
      ON CONFLICT (attempt_id, question_id) DO UPDATE
        SET selected_option = EXCLUDED.selected_option,
            text_answer = EXCLUDED.text_answer,
            updated_at = NOW();
    ELSE
      RAISE EXCEPTION 'Unknown question_type %', v_qtype;
    END IF;
  END LOOP;

  UPDATE test_attempts
  SET status = CASE WHEN v_has_open THEN 'NEEDS_MANUAL_GRADING' ELSE 'GRADED' END,
      updated_at = NOW()
  WHERE id = p_attempt_id;

  IF v_has_open THEN
    INSERT INTO notifications(user_id, type, title, message, test_id, attempt_id)
    SELECT c.teacher_id,
           'MANUAL_GRADING_REQUIRED',
           'Manual grading required',
           format('Attempt %s requires manual grading', p_attempt_id),
           v_test_id,
           p_attempt_id
    FROM classes c
    JOIN class_students cs ON cs.class_id = c.id
    WHERE cs.student_id = (SELECT student_id FROM test_attempts WHERE id = p_attempt_id)
      AND c.course_id = (SELECT course_id FROM tests WHERE id = v_test_id)
      AND c.teacher_id IS NOT NULL
    LIMIT 1;
  END IF;
END;
$$ LANGUAGE plpgsql;

-- 15.6 Award achievement
CREATE OR REPLACE FUNCTION award_achievement(
  p_achievement_id INT,
  p_student_id INT,
  p_awarder_id INT
) RETURNS VOID AS $$
DECLARE
  v_course_id INT;
  v_awarder_role role_name;
BEGIN
  v_awarder_role := get_user_role_name(p_awarder_id);
  IF v_awarder_role = 'STUDENT' THEN
    RAISE EXCEPTION 'Students cannot award achievements';
  END IF;

  SELECT course_id INTO v_course_id FROM achievements WHERE id = p_achievement_id;
  IF v_course_id IS NULL THEN
    RAISE EXCEPTION 'Achievement % not found', p_achievement_id;
  END IF;

  INSERT INTO student_achievements(student_id, achievement_id, awarded_by)
  VALUES (p_student_id, p_achievement_id, p_awarder_id);

  INSERT INTO class_achievement_feed(class_id, student_id, achievement_id, awarded_by)
  SELECT c.id, p_student_id, p_achievement_id, p_awarder_id
  FROM classes c
  JOIN class_students cs ON cs.class_id = c.id
  WHERE cs.student_id = p_student_id
    AND c.course_id = v_course_id
  ON CONFLICT (class_id, student_id, achievement_id) DO NOTHING;

  INSERT INTO notifications(user_id, type, title, message, course_id, achievement_id)
  VALUES (
    p_student_id,
    'ACHIEVEMENT_AWARDED',
    'Achievement awarded',
    format('You received achievement id=%s', p_achievement_id),
    v_course_id,
    p_achievement_id
  );
END;
$$ LANGUAGE plpgsql;

------------------------------------------------------------
-- 16) Index for notifications (common read patterns)
------------------------------------------------------------
CREATE INDEX idx_notifications_user_read_created
  ON notifications(user_id, is_read, created_at DESC);
