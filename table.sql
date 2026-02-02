----------------------------------------------------------------------
-- 0. УДАЛЕНИЕ ТАБЛИЦ 
----------------------------------------------------------------------

DROP TABLE IF EXISTS
    "users",
    "role"
    CASCADE;

----------------------------------------------------------------------
-- 1. СОЗДАНИЕ ТАБЛИЦ 
----------------------------------------------------------------------
CREATE TABLE "role" (
    id          SERIAL PRIMARY KEY,
    rolename    VARCHAR(63)  NOT NULL UNIQUE,
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
