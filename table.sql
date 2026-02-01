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