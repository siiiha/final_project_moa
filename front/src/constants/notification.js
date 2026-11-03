// AI작성: 알림 유형별 사용자 표시 문구를 여러 알림 화면에서 재사용하도록 관리합니다.
export const NOTIFICATION_UPDATED_EVENT = 'moa:notification-updated';
export const NOTIFICATION_RECEIVED_EVENT = 'moa:notification-received';

export const NOTIFICATION_TYPE_LABELS = {
  SAVING_GROUP_JOIN_REQUEST: '계모임 가입 신청',
  SAVING_GROUP_JOIN_APPROVED: '계모임 가입 승인',
  SAVING_GROUP_JOIN_REJECTED: '계모임 가입 거절',
  SMALL_GROUP_JOIN_REQUEST: '소모임 가입 신청',
  SMALL_GROUP_JOIN_APPROVED: '소모임 가입 승인',
  SMALL_GROUP_JOIN_REJECTED: '소모임 가입 거절',
  SCHEDULE_CREATED: '새 일정',
  NOTICE_CREATED: '새 공지',
  VOTE_CREATED: '새 투표',
  DUES_PAYMENT_DUE: '회비 납부 안내',
  REPORT_PROCESSED: '신고 처리 결과',
  MEMBER_RESTRICTED: '이용 제한 안내',
  MEMBER_RESTRICTION_RELEASED: '이용 제한 해제',
  // AI작성: 댓글과 대댓글 실시간 알림에 표시할 사용자용 유형 문구입니다.
  POST_COMMENT_CREATED: '새 댓글',
  COMMENT_REPLY_CREATED: '새 답글',
};
