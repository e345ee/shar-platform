// import { useEffect, useMemo, useState } from "react";
// import { getActivityById, startAttempt, submitAttempt } from "../../api/studentApi";
// import { questionTypeLabel } from "./studentFormatters";
//
// function ActivityAttemptModal({ activitySummary, onClose, onSubmitted }) {
//     const [isLoading, setIsLoading] = useState(true);
//     const [isSubmitting, setIsSubmitting] = useState(false);
//     const [errorMessage, setErrorMessage] = useState("");
//     const [activity, setActivity] = useState(null);
//     const [attempt, setAttempt] = useState(null);
//     const [answersByQuestionId, setAnswersByQuestionId] = useState({});
//
//     useEffect(() => {
//         let isCancelled = false;
//
//         const load = async () => {
//             setIsLoading(true);
//             setErrorMessage("");
//             try {
//                 const [activityDetails, startedAttempt] = await Promise.all([
//                     getActivityById(activitySummary.id),
//                     startAttempt(activitySummary.id),
//                 ]);
//
//                 if (isCancelled) return;
//
//                 setActivity(activityDetails);
//                 setAttempt(startedAttempt);
//                 setAnswersByQuestionId(() => {
//                     const initial = {};
//                     const questions = Array.isArray(activityDetails?.questions) ? activityDetails.questions : [];
//                     questions.forEach((q) => {
//                         initial[q.id] = {
//                             selectedOption: null,
//                             textAnswer: "",
//                         };
//                     });
//                     return initial;
//                 });
//             } catch (e) {
//                 if (!isCancelled) {
//                     setErrorMessage(e?.message || "Не удалось загрузить задание");
//                 }
//             } finally {
//                 if (!isCancelled) {
//                     setIsLoading(false);
//                 }
//             }
//         };
//
//         load();
//         return () => {
//             isCancelled = true;
//         };
//     }, [activitySummary.id]);
//
//     const questions = useMemo(
//         () => (Array.isArray(activity?.questions) ? activity.questions : []),
//         [activity]
//     );
//
//     const handleOptionChange = (questionId, option) => {
//         setAnswersByQuestionId((prev) => ({
//             ...prev,
//             [questionId]: {
//                 ...(prev[questionId] || {}),
//                 selectedOption: option,
//             },
//         }));
//     };
//
//     const handleTextChange = (questionId, text) => {
//         setAnswersByQuestionId((prev) => ({
//             ...prev,
//             [questionId]: {
//                 ...(prev[questionId] || {}),
//                 textAnswer: text,
//             },
//         }));
//     };
//
//     const validateAnswers = () => {
//         for (const q of questions) {
//             const current = answersByQuestionId[q.id] || {};
//             if (q.questionType === "SINGLE_CHOICE") {
//                 if (![1, 2, 3, 4].includes(current.selectedOption)) {
//                     return `Выберите вариант ответа для вопроса #${q.orderIndex || "?"}`;
//                 }
//             } else {
//                 const text = (current.textAnswer || "").trim();
//                 if (!text) {
//                     return `Введите текстовый ответ для вопроса #${q.orderIndex || "?"}`;
//                 }
//             }
//         }
//         return "";
//     };
//
//     const handleSubmit = async () => {
//         if (!attempt?.id) return;
//
//         const validationError = validateAnswers();
//         if (validationError) {
//             setErrorMessage(validationError);
//             return;
//         }
//
//         setIsSubmitting(true);
//         setErrorMessage("");
//         try {
//             const payload = questions.map((q) => {
//                 const current = answersByQuestionId[q.id] || {};
//                 if (q.questionType === "SINGLE_CHOICE") {
//                     return {
//                         questionId: q.id,
//                         selectedOption: current.selectedOption,
//                     };
//                 }
//                 return {
//                     questionId: q.id,
//                     textAnswer: (current.textAnswer || "").trim(),
//                 };
//             });
//
//             await submitAttempt(attempt.id, payload);
//             onSubmitted();
//             onClose();
//         } catch (e) {
//             setErrorMessage(e?.message || "Не удалось отправить ответы");
//         } finally {
//             setIsSubmitting(false);
//         }
//     };
//
//     return (
//         <div className="student-modal-overlay" role="dialog" aria-modal="true">
//             <div className="student-modal">
//                 <div className="student-modal-header">
//                     <h3>{activitySummary.title || "Активность"}</h3>
//                     <button type="button" className="student-modal-close" onClick={onClose}>
//                         x
//                     </button>
//                 </div>
//
//                 {isLoading ? <p className="student-muted">Загрузка...</p> : null}
//                 {!isLoading && errorMessage ? <p className="student-error">{errorMessage}</p> : null}
//
//                 {!isLoading && !errorMessage ? (
//                     <>
//                         <p className="student-modal-description">{activity?.description || "Описание отсутствует"}</p>
//                         {questions.length === 0 ? (
//                             <p className="student-muted">В этой активности пока нет вопросов.</p>
//                         ) : (
//                             <div className="student-question-list">
//                                 {questions.map((q) => (
//                                     <div className="student-question-card" key={q.id}>
//                                         <div className="student-question-header">
//                                             <span>Вопрос #{q.orderIndex ?? "?"}</span>
//                                             <span>{questionTypeLabel(q.questionType)}</span>
//                                         </div>
//                                         <p className="student-question-text">{q.questionText}</p>
//
//                                         {q.questionType === "SINGLE_CHOICE" ? (
//                                             <div className="student-options">
//                                                 {[q.option1, q.option2, q.option3, q.option4].map((option, index) => {
//                                                     const optionNumber = index + 1;
//                                                     return (
//                                                         <label key={optionNumber} className="student-option-item">
//                                                             <input
//                                                                 type="radio"
//                                                                 name={`question-${q.id}`}
//                                                                 checked={(answersByQuestionId[q.id]?.selectedOption || null) === optionNumber}
//                                                                 onChange={() => handleOptionChange(q.id, optionNumber)}
//                                                             />
//                                                             <span>{option || `Вариант ${optionNumber}`}</span>
//                                                         </label>
//                                                     );
//                                                 })}
//                                             </div>
//                                         ) : (
//                                             <textarea
//                                                 className="student-text-answer"
//                                                 placeholder="Введите ваш ответ..."
//                                                 value={answersByQuestionId[q.id]?.textAnswer || ""}
//                                                 onChange={(e) => handleTextChange(q.id, e.target.value)}
//                                             />
//                                         )}
//                                     </div>
//                                 ))}
//                             </div>
//                         )}
//
//                         <div className="student-modal-actions">
//                             <button type="button" className="student-btn student-btn-secondary" onClick={onClose}>
//                                 Отмена
//                             </button>
//                             <button
//                                 type="button"
//                                 className="student-btn student-btn-primary"
//                                 onClick={handleSubmit}
//                                 disabled={isSubmitting || questions.length === 0}
//                             >
//                                 {isSubmitting ? "Отправка..." : "Отправить ответы"}
//                             </button>
//                         </div>
//                     </>
//                 ) : null}
//             </div>
//         </div>
//     );
// }
//
// export default ActivityAttemptModal;
