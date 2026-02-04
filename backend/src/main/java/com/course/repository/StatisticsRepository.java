package com.course.repository;

import com.course.entity.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Native (read-only) aggregations for analytics/statistics.
 */
public interface StatisticsRepository extends JpaRepository<TestAttempt, Integer> {

    @Query(value = """
            WITH best AS (
              SELECT
                t.course_id AS courseId,
                c.name AS courseName,
                t.topic AS topic,
                ta.test_id AS testId,
                ta.status AS status,
                CASE
                  WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) * 100
                  ELSE NULL
                END AS percent,
                ROW_NUMBER() OVER (
                  PARTITION BY ta.test_id
                  ORDER BY
                    CASE WHEN ta.status = 'GRADED' THEN 1 ELSE 0 END DESC,
                    CASE WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) ELSE -1 END DESC,
                    ta.created_at DESC,
                    ta.id DESC
                ) AS rn
              FROM test_attempts ta
              JOIN tests t ON t.id = ta.test_id
              JOIN courses c ON c.id = t.course_id
              WHERE ta.student_id = :studentId
                AND ta.status IN ('SUBMITTED', 'GRADED')
                AND (:courseId IS NULL OR t.course_id = :courseId)
            ), attempts AS (
              SELECT
                t.course_id AS courseId,
                t.topic AS topic,
                COUNT(*) AS attemptsCount,
                COUNT(*) FILTER (WHERE ta.status = 'GRADED') AS gradedAttemptsCount
              FROM test_attempts ta
              JOIN tests t ON t.id = ta.test_id
              WHERE ta.student_id = :studentId
                AND ta.status IN ('SUBMITTED', 'GRADED')
                AND (:courseId IS NULL OR t.course_id = :courseId)
              GROUP BY t.course_id, t.topic
            )
            SELECT
              b.courseId AS courseId,
              b.courseName AS courseName,
              b.topic AS topic,
              COUNT(*) FILTER (WHERE b.rn = 1) AS testsAttempted,
              COUNT(*) FILTER (WHERE b.rn = 1 AND b.status = 'GRADED') AS gradedTests,
              a.attemptsCount AS attemptsCount,
              a.gradedAttemptsCount AS gradedAttemptsCount,
              AVG(b.percent) FILTER (WHERE b.rn = 1) AS avgBestPercent
            FROM best b
            JOIN attempts a ON a.courseId = b.courseId AND a.topic = b.topic
            GROUP BY b.courseId, b.courseName, b.topic, a.attemptsCount, a.gradedAttemptsCount
            ORDER BY b.courseId ASC, avgBestPercent DESC NULLS LAST, b.topic ASC
            """, nativeQuery = true)
    List<StudentTopicStatsProjection> findStudentTopicStats(
            @Param("studentId") Integer studentId,
            @Param("courseId") Integer courseId
    );

    @Query(value = """
            WITH cls AS (
              SELECT c.id AS classId, c.name AS className, c.course_id AS courseId
              FROM classes c
              WHERE c.id = :classId
            ),
            best AS (
              SELECT
                cls.classId AS classId,
                cls.className AS className,
                cls.courseId AS courseId,
                ta.student_id AS studentId,
                t.topic AS topic,
                ta.test_id AS testId,
                CASE
                  WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) * 100
                  ELSE NULL
                END AS percent,
                ROW_NUMBER() OVER (
                  PARTITION BY ta.student_id, ta.test_id
                  ORDER BY
                    CASE WHEN ta.status = 'GRADED' THEN 1 ELSE 0 END DESC,
                    CASE WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) ELSE -1 END DESC,
                    ta.created_at DESC,
                    ta.id DESC
                ) AS rn
              FROM cls
              JOIN class_students cs ON cs.class_id = cls.classId
              JOIN test_attempts ta ON ta.student_id = cs.student_id AND ta.status IN ('SUBMITTED', 'GRADED')
              JOIN tests t ON t.id = ta.test_id AND t.course_id = cls.courseId
            ),
            student_topic AS (
              SELECT
                classId,
                className,
                courseId,
                studentId,
                topic,
                AVG(percent) AS studentAvgPercent,
                COUNT(*) AS testsAttempted
              FROM best
              WHERE rn = 1
              GROUP BY classId, className, courseId, studentId, topic
            ),
            totals AS (
              SELECT
                cls.classId AS classId,
                COUNT(*) AS studentsTotal
              FROM cls
              JOIN class_students cs ON cs.class_id = cls.classId
              GROUP BY cls.classId
            )
            SELECT
              st.classId AS classId,
              st.className AS className,
              st.courseId AS courseId,
              st.topic AS topic,
              tot.studentsTotal AS studentsTotal,
              COUNT(*) AS studentsWithActivity,
              CAST(AVG(st.studentAvgPercent) AS double precision) AS avgPercent,
              CAST(COALESCE(SUM(st.testsAttempted), 0) AS bigint) AS testsAttempted
            FROM student_topic st
            JOIN totals tot ON tot.classId = st.classId
            GROUP BY st.classId, st.className, st.courseId, st.topic, tot.studentsTotal
            ORDER BY avgPercent DESC NULLS LAST, st.topic ASC
            """, nativeQuery = true)
    List<ClassTopicStatsProjection> findClassTopicStats(@Param("classId") Integer classId);


    @Query(value = """
            WITH enrolled AS (
              SELECT DISTINCT cs.student_id AS studentId
              FROM class_students cs
              JOIN classes c ON c.id = cs.class_id
              WHERE c.course_id = :courseId
            ),
            best AS (
              SELECT
                t.course_id AS courseId,
                c.name AS courseName,
                ta.student_id AS studentId,
                t.topic AS topic,
                ta.test_id AS testId,
                CASE
                  WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) * 100
                  ELSE NULL
                END AS percent,
                ROW_NUMBER() OVER (
                  PARTITION BY ta.student_id, ta.test_id
                  ORDER BY
                    CASE WHEN ta.status = 'GRADED' THEN 1 ELSE 0 END DESC,
                    CASE WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) ELSE -1 END DESC,
                    ta.created_at DESC,
                    ta.id DESC
                ) AS rn
              FROM test_attempts ta
              JOIN tests t ON t.id = ta.test_id AND t.course_id = :courseId
              JOIN courses c ON c.id = t.course_id
              WHERE ta.status IN ('SUBMITTED', 'GRADED')
                AND ta.student_id IN (SELECT studentId FROM enrolled)
            ),
            student_topic AS (
              SELECT
                courseId,
                courseName,
                studentId,
                topic,
                AVG(percent) AS studentAvgPercent,
                COUNT(*) AS testsAttempted
              FROM best
              WHERE rn = 1
              GROUP BY courseId, courseName, studentId, topic
            ),
            totals AS (
              SELECT CAST(:courseId AS int) AS courseId, COUNT(*) AS studentsTotal
              FROM enrolled
            )
            SELECT
              st.courseId AS courseId,
              st.courseName AS courseName,
              st.topic AS topic,
              tot.studentsTotal AS studentsTotal,
              COUNT(*) AS studentsWithActivity,
              CAST(AVG(st.studentAvgPercent) AS double precision) AS avgPercent,
              CAST(COALESCE(SUM(st.testsAttempted), 0) AS bigint) AS testsAttempted
            FROM student_topic st
            CROSS JOIN totals tot
            GROUP BY st.courseId, st.courseName, st.topic, tot.studentsTotal
            ORDER BY avgPercent ASC NULLS LAST, st.topic ASC
            """, nativeQuery = true)
    List<CourseTopicStatsProjection> findCourseTopicStats(@Param("courseId") Integer courseId);


    @Query(value = """
            WITH class_totals AS (
              SELECT c.id AS classId, COUNT(cs.student_id) AS studentsTotal
              FROM classes c
              LEFT JOIN class_students cs ON cs.class_id = c.id
              WHERE c.course_id = :courseId
              GROUP BY c.id
            ),
            best AS (
              SELECT
                c.course_id AS courseId,
                c.id AS classId,
                c.name AS className,
                c.teacher_id AS teacherId,
                ut.name AS teacherName,
                ta.student_id AS studentId,
                t.topic AS topic,
                ta.test_id AS testId,
                CASE
                  WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) * 100
                  ELSE NULL
                END AS percent,
                ROW_NUMBER() OVER (
                  PARTITION BY c.id, ta.student_id, ta.test_id
                  ORDER BY
                    CASE WHEN ta.status = 'GRADED' THEN 1 ELSE 0 END DESC,
                    CASE WHEN COALESCE(ta.max_score, 0) > 0 THEN (CAST(COALESCE(ta.score, 0) AS double precision) / ta.max_score) ELSE -1 END DESC,
                    ta.created_at DESC,
                    ta.id DESC
                ) AS rn
              FROM classes c
              JOIN class_students cs ON cs.class_id = c.id
              JOIN test_attempts ta ON ta.student_id = cs.student_id AND ta.status IN ('SUBMITTED', 'GRADED')
              JOIN tests t ON t.id = ta.test_id AND t.course_id = c.course_id
              LEFT JOIN users ut ON ut.id = c.teacher_id
              WHERE c.course_id = :courseId
            ),
            student_topic AS (
              SELECT
                courseId,
                classId,
                className,
                teacherId,
                teacherName,
                studentId,
                topic,
                AVG(percent) AS studentAvgPercent,
                COUNT(*) AS testsAttempted
              FROM best
              WHERE rn = 1
              GROUP BY courseId, classId, className, teacherId, teacherName, studentId, topic
            )
            SELECT
              st.courseId AS courseId,
              st.classId AS classId,
              st.className AS className,
              st.teacherId AS teacherId,
              st.teacherName AS teacherName,
              st.topic AS topic,
              ct.studentsTotal AS studentsTotal,
              COUNT(*) AS studentsWithActivity,
              CAST(AVG(st.studentAvgPercent) AS double precision) AS avgPercent,
              CAST(COALESCE(SUM(st.testsAttempted), 0) AS bigint) AS testsAttempted
            FROM student_topic st
            JOIN class_totals ct ON ct.classId = st.classId
            GROUP BY st.courseId, st.classId, st.className, st.teacherId, st.teacherName, st.topic, ct.studentsTotal
            ORDER BY st.teacherName NULLS LAST, st.className ASC, avgPercent ASC NULLS LAST, st.topic ASC
            """, nativeQuery = true)
    List<TeacherClassTopicStatsProjection> findTeacherClassTopicStatsForCourse(@Param("courseId") Integer courseId);


    @Query(value = """
            WITH enrolled_courses AS (
              SELECT DISTINCT c.course_id AS courseId
              FROM class_students cs
              JOIN classes c ON c.id = cs.class_id
              WHERE cs.student_id = :studentId
            ),
            attempt_stats AS (
              SELECT
                COUNT(*) AS attemptsTotal,
                COUNT(*) FILTER (WHERE status = 'IN_PROGRESS') AS attemptsInProgress,
                COUNT(*) FILTER (WHERE status IN ('SUBMITTED','GRADED')) AS attemptsFinished,
                COUNT(*) FILTER (WHERE status = 'GRADED') AS attemptsGraded,
                COUNT(DISTINCT test_id) FILTER (WHERE status IN ('SUBMITTED','GRADED')) AS testsFinished,
                COUNT(DISTINCT test_id) FILTER (WHERE status = 'GRADED') AS testsGraded
              FROM test_attempts
              WHERE student_id = :studentId
            ),
            required AS (
              SELECT
                t.course_id AS courseId,
                COUNT(DISTINCT t.id) AS requiredTests
              FROM tests t
              WHERE t.course_id IN (SELECT courseId FROM enrolled_courses)
                AND t.status = 'READY'
                AND t.activity_type IN ('HOMEWORK_TEST','CONTROL_WORK')
                AND t.lesson_id IS NOT NULL
              GROUP BY t.course_id
            ),
            done AS (
              SELECT
                t.course_id AS courseId,
                COUNT(DISTINCT t.id) AS completedTests
              FROM tests t
              JOIN test_attempts ta ON ta.test_id = t.id
              WHERE t.course_id IN (SELECT courseId FROM enrolled_courses)
                AND t.status = 'READY'
                AND t.activity_type IN ('HOMEWORK_TEST','CONTROL_WORK')
                AND t.lesson_id IS NOT NULL
                AND ta.student_id = :studentId
                AND ta.status IN ('SUBMITTED','GRADED')
              GROUP BY t.course_id
            ),
            course_totals AS (
              SELECT
                (SELECT COUNT(*) FROM enrolled_courses) AS coursesStarted,
                COUNT(*) FILTER (
                  WHERE COALESCE(d.completedTests, 0) >= COALESCE(r.requiredTests, 0)
                    AND COALESCE(r.requiredTests, 0) > 0
                ) AS coursesCompleted
              FROM enrolled_courses ec
              LEFT JOIN required r ON r.courseId = ec.courseId
              LEFT JOIN done d ON d.courseId = ec.courseId
            )
            SELECT
              ast.attemptsTotal AS attemptsTotal,
              ast.attemptsInProgress AS attemptsInProgress,
              ast.attemptsFinished AS attemptsFinished,
              ast.attemptsGraded AS attemptsGraded,
              ast.testsFinished AS testsFinished,
              ast.testsGraded AS testsGraded,
              ct.coursesStarted AS coursesStarted,
              ct.coursesCompleted AS coursesCompleted
            FROM attempt_stats ast
            CROSS JOIN course_totals ct
            """, nativeQuery = true)
    StudentOverviewStatsProjection getStudentOverviewStats(@Param("studentId") Integer studentId);


    @Query(value = """
            WITH enrolled_courses AS (
              SELECT DISTINCT c.course_id AS courseId
              FROM class_students cs
              JOIN classes c ON c.id = cs.class_id
              WHERE cs.student_id = :studentId
            ),
            required AS (
              SELECT
                t.course_id AS courseId,
                COUNT(DISTINCT t.id) AS requiredTests
              FROM tests t
              WHERE t.course_id IN (SELECT courseId FROM enrolled_courses)
                AND t.status = 'READY'
                AND t.activity_type IN ('HOMEWORK_TEST','CONTROL_WORK')
                AND t.lesson_id IS NOT NULL
              GROUP BY t.course_id
            ),
            done AS (
              SELECT
                t.course_id AS courseId,
                COUNT(DISTINCT t.id) AS completedTests
              FROM tests t
              JOIN test_attempts ta ON ta.test_id = t.id
              WHERE t.course_id IN (SELECT courseId FROM enrolled_courses)
                AND t.status = 'READY'
                AND t.activity_type IN ('HOMEWORK_TEST','CONTROL_WORK')
                AND t.lesson_id IS NOT NULL
                AND ta.student_id = :studentId
                AND ta.status IN ('SUBMITTED','GRADED')
              GROUP BY t.course_id
            )
            SELECT
              c.id AS courseId,
              c.name AS courseName,
              COALESCE(r.requiredTests, 0) AS requiredTests,
              COALESCE(d.completedTests, 0) AS completedTests,
              CASE WHEN COALESCE(r.requiredTests, 0) > 0
                THEN (CAST(COALESCE(d.completedTests, 0) AS double precision) / r.requiredTests) * 100
                ELSE 0
              END AS percent,
              (COALESCE(r.requiredTests, 0) > 0 AND COALESCE(d.completedTests, 0) >= r.requiredTests) AS completed
            FROM enrolled_courses ec
            JOIN courses c ON c.id = ec.courseId
            LEFT JOIN required r ON r.courseId = ec.courseId
            LEFT JOIN done d ON d.courseId = ec.courseId
            ORDER BY c.name ASC
            """, nativeQuery = true)
    List<StudentCourseProgressProjection> findStudentCourseProgress(@Param("studentId") Integer studentId);
}
