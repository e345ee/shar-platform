



DROP TABLE IF EXISTS
  class_opened_tests,
  methodist_teachers,
  test_attempt_answers,
  test_attempts,
  test_questions,
  student_remedial_assignments,
  tests,
  class_achievement_feed,
  student_achievements,
  achievements,
  class_students,
  class_join_requests,
  class_opened_lessons,
  classes,
  lessons,
  courses,
  notifications,
  users,
  role
CASCADE;

DROP FUNCTION IF EXISTS validate_notification_consistency() CASCADE;
DROP FUNCTION IF EXISTS get_user_role_name(INT) CASCADE;
DROP FUNCTION IF EXISTS validate_class_teacher_role() CASCADE;
DROP FUNCTION IF EXISTS validate_class_student_role() CASCADE;
DROP FUNCTION IF EXISTS validate_methodist_teacher_roles() CASCADE;
DROP FUNCTION IF EXISTS validate_student_achievement_awarder_role() CASCADE;
DROP FUNCTION IF EXISTS validate_tests_course_matches_lesson() CASCADE;
DROP FUNCTION IF EXISTS validate_class_opened_test_consistency() CASCADE;
DROP FUNCTION IF EXISTS set_updated_at() CASCADE;
DROP FUNCTION IF EXISTS set_submitted_at_on_status() CASCADE;
DROP FUNCTION IF EXISTS recalc_attempt_score() CASCADE;
DROP FUNCTION IF EXISTS create_attempt_notifications() CASCADE;

DROP FUNCTION IF EXISTS create_student_by_tg(TEXT, TEXT, TEXT, TEXT) CASCADE;
DROP FUNCTION IF EXISTS create_join_request_by_code(TEXT, TEXT, TEXT, TEXT) CASCADE;
DROP FUNCTION IF EXISTS approve_join_request(INT, INT) CASCADE;
DROP FUNCTION IF EXISTS start_attempt(INT, INT) CASCADE;
DROP FUNCTION IF EXISTS submit_attempt(INT, JSONB) CASCADE;
DROP FUNCTION IF EXISTS award_achievement(INT, INT, INT) CASCADE;

DROP TYPE IF EXISTS notification_type CASCADE;
DROP TYPE IF EXISTS role_name CASCADE;
