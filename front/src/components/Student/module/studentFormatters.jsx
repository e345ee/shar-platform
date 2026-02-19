// export function formatDateTime(value) {
//     if (!value) return "—";
//     const parsed = new Date(value);
//     if (Number.isNaN(parsed.getTime())) return value;
//     return parsed.toLocaleString();
// }
//
// export function formatPercent(value) {
//     if (value === null || value === undefined || Number.isNaN(Number(value))) {
//         return "—";
//     }
//     return `${Math.round(Number(value) * 100) / 100}%`;
// }
//
// export function activityTypeLabel(type) {
//     if (type === "HOMEWORK_TEST") return "Домашнее задание";
//     if (type === "CONTROL_WORK") return "Контрольная";
//     if (type === "WEEKLY_STAR") return "Еженедельная активность";
//     if (type === "REMEDIAL_TASK") return "Дополнительная активность";
//     return type || "Активность";
// }
//
// export function questionTypeLabel(type) {
//     if (type === "SINGLE_CHOICE") return "Один вариант";
//     if (type === "TEXT") return "Текстовый ответ";
//     if (type === "OPEN") return "Открытый ответ";
//     return type || "Вопрос";
// }
