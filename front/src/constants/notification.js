// AI작성: 알림 유형별 사용자 표시 문구를 여러 알림 화면에서 재사용하도록 관리합니다.
export const NOTIFICATION_UPDATED_EVENT = 'moa:notification-updated';
export const NOTIFICATION_RECEIVED_EVENT = 'moa:notification-received';

// AI작성: 과거 DB에 링크가 남아 있어도 가입 거절·계모임 추방 알림은 화면 이동을 제공하지 않습니다.
export function hasNavigableNotificationTarget(notification) {
  return ![
    'SAVING_GROUP_JOIN_REJECTED',
    'SMALL_GROUP_JOIN_REJECTED',
    'SAVING_GROUP_MEMBER_EXPELLED',
  ].includes(notification?.type)
    && notification?.targetUrl?.startsWith('/');
}

export const NOTIFICATION_TYPE_LABELS = {
  SAVING_GROUP_JOIN_REQUEST: '계모임 가입 신청',
  SAVING_GROUP_JOIN_APPROVED: '계모임 가입 승인',
  SAVING_GROUP_JOIN_REJECTED: '계모임 가입 거절',
  // AI작성: 계모임 회원 관리와 관리자 심사 결과 알림의 사용자 표시 문구입니다.
  SAVING_GROUP_MEMBER_EXPELLED: '계모임 회원 제외',
  SAVING_GROUP_MEMBER_ROLE_CHANGED: '계모임 역할 변경',
  SAVING_GROUP_CREATION_APPROVED: '계모임 개설 승인',
  SAVING_GROUP_CREATION_REJECTED: '계모임 개설 반려',
  SAVING_GROUP_DELETION_APPROVED: '계모임 삭제 승인',
  SAVING_GROUP_DELETION_REJECTED: '계모임 삭제 반려',
  SMALL_GROUP_JOIN_REQUEST: '소모임 가입 신청',
  SMALL_GROUP_JOIN_APPROVED: '소모임 가입 승인',
  SMALL_GROUP_JOIN_REJECTED: '소모임 가입 거절',
  SCHEDULE_CREATED: '새 일정',
  NOTICE_CREATED: '새 공지',
  VOTE_CREATED: '새 투표',
  DUES_PAYMENT_DUE: '회비 납부 안내',
  REPORT_PROCESSED: '신고 처리 결과',
  // AI작성: 계금·지출 요청 처리와 계모임 삭제 결과 알림의 사용자 표시 문구입니다.
  SAVING_GROUP_PAYOUT_REQUESTED: '계금·지출 요청',
  SAVING_GROUP_PAYOUT_APPROVED: '계금·지출 승인',
  SAVING_GROUP_PAYOUT_REJECTED: '계금·지출 반려',
  SAVING_GROUP_DELETED: '계모임 삭제',
  MEMBER_RESTRICTED: '이용 제한 안내',
  MEMBER_RESTRICTION_RELEASED: '이용 제한 해제',
  // AI작성: 댓글과 대댓글 실시간 알림에 표시할 사용자용 유형 문구입니다.
  POST_COMMENT_CREATED: '새 댓글',
  COMMENT_REPLY_CREATED: '새 답글',
  CHAT_MESSAGE_CREATED: '새 채팅 메시지',
};
