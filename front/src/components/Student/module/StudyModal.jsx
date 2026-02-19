// import { useEffect, useMemo, useState } from "react";
// import { getMyCoursePage, listMyCourses, sendCompletionEmail } from "../../api/studentApi";
// import { activityTypeLabel, formatDateTime } from "./studentFormatters";
// import ActivityAttemptModal from "./ActivityAttemptModal";
//
// function StudyModule() {
//     const [isLoadingCourses, setIsLoadingCourses] = useState(false);
//     const [errorMessage, setErrorMessage] = useState("");
//     const [courses, setCourses] = useState([]);
//     const [selectedCourseId, setSelectedCourseId] = useState(null);
//     const [coursePagesById, setCoursePagesById] = useState({});
//     const [isSendingCertificate, setIsSendingCertificate] = useState(false);
//     const [activeAttemptActivity, setActiveAttemptActivity] = useState(null);
//
//     useEffect(() => {
//         let isCancelled = false;
//
//         const loadCourses = async () => {
//             setIsLoadingCourses(true);
//             setErrorMessage("");
//             try {
//                 const data = await listMyCourses();
//                 if (isCancelled) return;
//                 setCourses(data);
//                 if (data.length > 0) {
//                     setSelectedCourseId((prev) => prev || data[0].id);
//                 }
//             } catch (e) {
//                 if (!isCancelled) {
//                     setErrorMessage(e?.message || "Не удалось загрузить курсы");
//                 }
//             } finally {
//                 if (!isCancelled) {
//                     setIsLoadingCourses(false);
//                 }
//             }
//         };
//
//         loadCourses();
//         return () => {
//             isCancelled = true;
//         };
//     }, []);
//
//     const selectedCourse = useMemo(
//         () => courses.find((course) => course.id === selectedCourseId) || null,
//         [courses, selectedCourseId]
//     );
//     const selectedCoursePage = selectedCourseId ? coursePagesById[selectedCourseId] : null;
//
//     const loadCoursePage = async (courseId, force = false) => {
//         if (!courseId) return;
//         if (!force && coursePagesById[courseId]) return;
//         const page = await getMyCoursePage(courseId);
//         setCoursePagesById((prev) => ({
//             ...prev,
//             [courseId]: page,
//         }));
//     };
//
//     useEffect(() => {
//         if (!selectedCourseId) return;
//         let isCancelled = false;
//
//         setErrorMessage("");
//         loadCoursePage(selectedCourseId).catch((e) => {
//             if (!isCancelled) {
//                 setErrorMessage(e?.message || "Не удалось загрузить курс");
//             }
//         });
//         return () => {
//             isCancelled = true;
//         };
//     }, [selectedCourseId]);
//
//     const refreshSelectedCoursePage = async () => {
//         if (!selectedCourseId) return;
//         try {
//             await loadCoursePage(selectedCourseId, true);
//         } catch (e) {
//             setErrorMessage(e?.message || "Не удалось обновить курс");
//         }
//     };
//
//     const handleSendCertificate = async () => {
//         if (!selectedCourseId) return;
//         setIsSendingCertificate(true);
//         setErrorMessage("");
//         try {
//             await sendCompletionEmail(selectedCourseId);
//             setErrorMessage("Сертификат отправлен на вашу почту");
//         } catch (e) {
//             setErrorMessage(e?.message || "Не удалось отправить сертификат");
//         } finally {
//             setIsSendingCertificate(false);
//         }
//     };
//
//     const renderActivityCard = (item) => {
//         const activity = item.activity || {};
//         const latestAttempt = item.latestAttempt || null;
//         return (
//             <div className="student-activity-card" key={activity.id}>
//                 <div className="student-activity-header">
//                     <strong>{activity.title || "Без названия"}</strong>
//                     <span className="student-chip">{activityTypeLabel(activity.activityType)}</span>
//                 </div>
//                 <p className="student-muted">{activity.description || "Описание отсутствует"}</p>
//                 <div className="student-activity-meta">
//                     <span>Дедлайн: {formatDateTime(activity.deadline)}</span>
//                     <span>Статус: {latestAttempt?.status || "Не начато"}</span>
//                     <span>
//             Балл:{" "}
//                         {latestAttempt && latestAttempt.score !== null && latestAttempt.score !== undefined
//                             ? `${latestAttempt.score}/${latestAttempt.maxScore ?? "?"}`
//                             : "—"}
//           </span>
//                 </div>
//                 <button
//                     type="button"
//                     className="student-btn student-btn-primary"
//                     onClick={() => setActiveAttemptActivity(activity)}
//                 >
//                     {latestAttempt?.status === "IN_PROGRESS" ? "Продолжить" : "Выполнить"}
//                 </button>
//             </div>
//         );
//     };
//
//     return (
//         <>
//             {errorMessage ? <p className="student-info">{errorMessage}</p> : null}
//             <section className="student-layout">
//                 <aside className="student-courses-sidebar">
//                     <h3>Мои курсы</h3>
//                     {isLoadingCourses ? <p className="student-muted">Загрузка...</p> : null}
//                     {!isLoadingCourses && courses.length === 0 ? (
//                         <p className="student-muted">Вы пока не добавлены ни в один курс.</p>
//                     ) : null}
//                     <div className="student-course-list">
//                         {courses.map((course) => (
//                             <button
//                                 type="button"
//                                 className={`student-course-item ${selectedCourseId === course.id ? "active" : ""}`}
//                                 key={course.id}
//                                 onClick={() => setSelectedCourseId(course.id)}
//                             >
//                                 <strong>{course.name}</strong>
//                                 <span>{course.description || "Без описания"}</span>
//                             </button>
//                         ))}
//                     </div>
//                 </aside>
//
//                 <div className="student-course-content">
//                     {!selectedCourse ? <p className="student-muted">Выберите курс слева.</p> : null}
//                     {selectedCourse ? (
//                         <>
//                             <div className="student-course-header">
//                                 <div>
//                                     <h2>{selectedCourse.name}</h2>
//                                     <p className="student-muted">{selectedCourse.description}</p>
//                                 </div>
//                                 <button
//                                     type="button"
//                                     className="student-btn student-btn-secondary"
//                                     onClick={handleSendCertificate}
//                                     disabled={isSendingCertificate}
//                                 >
//                                     {isSendingCertificate ? "Отправка..." : "Отправить сертификат на почту"}
//                                 </button>
//                             </div>
//
//                             {!selectedCoursePage ? (
//                                 <p className="student-muted">Загрузка содержимого курса...</p>
//                             ) : (
//                                 <div className="student-course-sections">
//                                     <div className="student-card">
//                                         <h3>Учебные материалы</h3>
//                                         {selectedCoursePage.lessons?.length ? (
//                                             <div className="student-lesson-list">
//                                                 {selectedCoursePage.lessons.map((lessonBlock) => (
//                                                     <div key={lessonBlock.lesson?.id} className="student-lesson-item">
//                                                         <div className="student-lesson-header">
//                                                             <strong>{lessonBlock.lesson?.title || "Урок без названия"}</strong>
//                                                             {lessonBlock.lesson?.presentationUrl ? (
//                                                                 <a href={lessonBlock.lesson.presentationUrl} target="_blank" rel="noreferrer">
//                                                                     Открыть презентацию
//                                                                 </a>
//                                                             ) : (
//                                                                 <span className="student-muted">Презентация отсутствует</span>
//                                                             )}
//                                                         </div>
//                                                         <p className="student-muted">{lessonBlock.lesson?.description || "Без описания"}</p>
//                                                         {lessonBlock.activities?.length ? (
//                                                             <div className="student-activity-grid">
//                                                                 {lessonBlock.activities.map(renderActivityCard)}
//                                                             </div>
//                                                         ) : (
//                                                             <p className="student-muted">Открытых активностей по уроку пока нет.</p>
//                                                         )}
//                                                     </div>
//                                                 ))}
//                                             </div>
//                                         ) : (
//                                             <p className="student-muted">Открытых уроков пока нет.</p>
//                                         )}
//                                     </div>
//
//                                     <div className="student-card">
//                                         <h3>Еженедельные активности</h3>
//                                         {selectedCoursePage.weeklyThisWeek?.length ? (
//                                             <div className="student-activity-grid">
//                                                 {selectedCoursePage.weeklyThisWeek.map(renderActivityCard)}
//                                             </div>
//                                         ) : (
//                                             <p className="student-muted">На эту неделю активностей нет.</p>
//                                         )}
//                                     </div>
//
//                                     <div className="student-card">
//                                         <h3>Дополнительные активности</h3>
//                                         {selectedCoursePage.remedialThisWeek?.length ? (
//                                             <div className="student-activity-grid">
//                                                 {selectedCoursePage.remedialThisWeek.map(renderActivityCard)}
//                                             </div>
//                                         ) : (
//                                             <p className="student-muted">Дополнительных активностей нет.</p>
//                                         )}
//                                     </div>
//                                 </div>
//                             )}
//                         </>
//                     ) : null}
//                 </div>
//             </section>
//
//             {activeAttemptActivity ? (
//                 <ActivityAttemptModal
//                     activitySummary={activeAttemptActivity}
//                     onClose={() => setActiveAttemptActivity(null)}
//                     onSubmitted={refreshSelectedCoursePage}
//                 />
//             ) : null}
//         </>
//     );
// }
//
// export default StudyModule;
