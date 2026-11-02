// AI작성: 신고 코드와 표시명을 API 변환과 관리자 필터가 함께 사용하도록 한 곳에서 관리한다.
export const ADMIN_REPORT_TYPE_OPTIONS = [
  { value: 'POST', label: '게시글', filterable: true },
  { value: 'COMMENT', label: '댓글', filterable: true },
  { value: 'MEMBER', label: '회원', filterable: true },
  { value: 'GROUP', label: '모임', filterable: true },
  { value: 'GROUP_MEMBER', label: '모임 회원', filterable: true },
  // AI작성: 채팅 신고 기능이 완성되기 전까지 응답 표시는 지원하되 필터에는 노출하지 않는다.
  { value: 'CHAT_MESSAGE', label: '채팅 메시지', filterable: false },
];

export const ADMIN_REPORT_STATUS_OPTIONS = [
  { value: 'PENDING', label: '대기' },
  { value: 'CONFIRMED', label: '승인' },
  { value: 'REJECTED', label: '반려' },
];

export function findAdminReportOptionLabel(options, value, fallback) {
  return options.find((option) => option.value === value)?.label || fallback;
}
