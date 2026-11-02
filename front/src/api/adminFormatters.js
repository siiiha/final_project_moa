// AI작성: 관리자 API 화면의 날짜 파싱·표시 규칙을 한 곳에서 재사용합니다.
export function formatAdminDate(
  value,
  { fallback = '날짜 확인 필요', includeTime = false, compact = true } = {},
) {
  if (!value) return fallback;

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return fallback;

  const options = {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    ...(includeTime ? { hour: '2-digit', minute: '2-digit' } : {}),
  };
  const formatted = new Intl.DateTimeFormat('ko-KR', options).format(date);

  return compact
    ? formatted.replace(/\.\s?/g, '.').replace(/\.$/, '')
    : formatted;
}
