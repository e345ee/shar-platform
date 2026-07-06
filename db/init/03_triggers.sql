CREATE OR REPLACE FUNCTION recalc_attempt_score()
RETURNS TRIGGER AS $$
DECLARE
    v_attempt_id INT;
BEGIN
    v_attempt_id := COALESCE(NEW.attempt_id, OLD.attempt_id);

    UPDATE test_attempts ta
    SET score = (
            SELECT COALESCE(SUM(COALESCE(taa.points_awarded, 0)), 0)
            FROM test_attempt_answers taa
            WHERE taa.attempt_id = v_attempt_id
        ),
        max_score = (
            SELECT COALESCE(SUM(GREATEST(COALESCE(tq.points, 1), 1)), 0)
            FROM test_questions tq
            WHERE tq.test_id = ta.test_id
        ),
        updated_at = CURRENT_TIMESTAMP
    WHERE ta.id = v_attempt_id;

    RETURN COALESCE(NEW, OLD);
EXCEPTION
    WHEN OTHERS THEN
        RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_test_attempt_recalc_score ON test_attempt_answers;
CREATE TRIGGER trg_test_attempt_recalc_score
AFTER INSERT OR UPDATE OR DELETE ON test_attempt_answers
FOR EACH ROW
EXECUTE FUNCTION recalc_attempt_score();

CREATE OR REPLACE FUNCTION create_attempt_notifications()
RETURNS TRIGGER AS $$
DECLARE
    v_course_id INT;
    v_test_title TEXT;
    v_student_name TEXT;
    v_score INT;
    v_max_score INT;
    v_has_open BOOLEAN;
BEGIN
    IF TG_OP = 'UPDATE' AND (OLD.status IS NOT DISTINCT FROM NEW.status) THEN
        RETURN NEW;
    END IF;

    SELECT t.course_id, t.title
    INTO v_course_id, v_test_title
    FROM tests t
    WHERE t.id = NEW.test_id;

    SELECT u.name
    INTO v_student_name
    FROM users u
    WHERE u.id = NEW.student_id;

    SELECT
        COALESCE((SELECT SUM(COALESCE(points_awarded, 0)) FROM test_attempt_answers WHERE attempt_id = NEW.id), 0),
        COALESCE((SELECT SUM(GREATEST(COALESCE(points, 1), 1)) FROM test_questions WHERE test_id = NEW.test_id), 0)
    INTO v_score, v_max_score;

    SELECT EXISTS(
        SELECT 1
        FROM test_questions
        WHERE test_id = NEW.test_id AND question_type = 'OPEN'
    )
    INTO v_has_open;

    IF NEW.status = 'SUBMITTED' THEN
        INSERT INTO notifications (user_id, type, title, message, course_id, test_id, attempt_id)
        SELECT r.user_id,
               'MANUAL_GRADING_REQUIRED'::notification_type,
               'Новая задача на проверку',
               'Есть задание на проверку: ' || COALESCE(v_test_title, '') || ' от ' || COALESCE(v_student_name, ''),
               v_course_id,
               NEW.test_id,
               NEW.id
        FROM (
            SELECT DISTINCT c.teacher_id AS user_id
            FROM class_students cs
            JOIN classes c ON c.id = cs.class_id
            WHERE cs.student_id = NEW.student_id
              AND c.course_id = v_course_id
              AND c.teacher_id IS NOT NULL

            UNION

            SELECT co.created_by AS user_id
            FROM courses co
            WHERE co.id = v_course_id
        ) r
        WHERE NOT EXISTS (
            SELECT 1
            FROM notifications n
            WHERE n.user_id = r.user_id
              AND n.type = 'MANUAL_GRADING_REQUIRED'::notification_type
              AND n.attempt_id = NEW.id
        );
    END IF;

    IF NEW.status = 'GRADED' THEN
        INSERT INTO notifications (user_id, type, title, message, course_id, test_id, attempt_id)
        SELECT NEW.student_id,
               'GRADE_RECEIVED'::notification_type,
               'Получена оценка',
               'Проверено задание: ' || COALESCE(v_test_title, '') || '. Оценка: ' || v_score::text || '/' || v_max_score::text,
               v_course_id,
               NEW.test_id,
               NEW.id
        WHERE NOT EXISTS (
            SELECT 1
            FROM notifications n
            WHERE n.user_id = NEW.student_id
              AND n.type = 'GRADE_RECEIVED'::notification_type
              AND n.attempt_id = NEW.id
        );

        IF v_has_open THEN
            INSERT INTO notifications (user_id, type, title, message, course_id, test_id, attempt_id)
            SELECT NEW.student_id,
                   'OPEN_ANSWER_CHECKED'::notification_type,
                   'Открытый ответ проверен',
                   'Учитель проверил открытый ответ в задании: ' || COALESCE(v_test_title, ''),
                   v_course_id,
                   NEW.test_id,
                   NEW.id
            WHERE NOT EXISTS (
                SELECT 1
                FROM notifications n
                WHERE n.user_id = NEW.student_id
                  AND n.type = 'OPEN_ANSWER_CHECKED'::notification_type
                  AND n.attempt_id = NEW.id
            );
        END IF;
    END IF;

    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_attempt_notifications ON test_attempts;
CREATE TRIGGER trg_attempt_notifications
AFTER INSERT OR UPDATE ON test_attempts
FOR EACH ROW
EXECUTE FUNCTION create_attempt_notifications();
