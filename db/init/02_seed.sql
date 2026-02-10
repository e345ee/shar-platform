-- 02_seed.sql
-- Minimal seed data executed automatically on first DB initialization.
--
-- IMPORTANT:
-- This file is intentionally minimal so automated tests start from a clean domain state.
-- The application will auto-create the default ADMIN account on startup
-- (login: admin, password: admin) via AdminInitializer.
--
-- If you want a richer demo dataset (course/class/test/etc), put it into a separate script
-- and apply it manually (psql) so it doesn't interfere with e2e flows.

-- Roles
INSERT INTO role(rolename, description) VALUES
 ('ADMIN', 'Администратор системы'),
 ('METHODIST', 'Методист'),
 ('TEACHER', 'Преподаватель'),
 ('STUDENT', 'Студент')
ON CONFLICT (rolename) DO NOTHING;
