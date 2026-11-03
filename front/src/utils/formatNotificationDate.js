// AI작성: 알림 화면별 표시 범위에 맞춰 동일한 날짜 변환 규칙을 재사용합니다.
export default function formatNotificationDate(createdAt, { includeYear = false } = {}) {
  if (!createdAt) return '';

  return new Intl.DateTimeFormat('ko-KR', {
    ...(includeYear ? { year: 'numeric' } : {}),
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(createdAt));
}
