package com.nightstudy.moa.notification.dto;

// AI작성: 모아 서비스에서 발생할 수 있는 알림 종류를 정의합니다.
// 오타가 날 수 있는 문자열 대신 컴파일러가 검사 할 수 있는 enum을 사용한다.
public enum NotificationType {

    // AI작성: 계모임 가입 신청이 접수되거나 신청 결과가 결정되었을 때 사용하는 알림입니다.
    SAVING_GROUP_JOIN_REQUEST,
    SAVING_GROUP_JOIN_APPROVED,
    SAVING_GROUP_JOIN_REJECTED,
    // AI작성: 계모임에서 회원이 추방되었을 때 처리 결과를 알립니다.
    SAVING_GROUP_MEMBER_EXPELLED,
    // AI작성: 계주가 계원의 회계 역할을 지정하거나 해제했을 때 결과를 알립니다.
    SAVING_GROUP_MEMBER_ROLE_CHANGED,
    // AI작성: 관리자가 계모임 개설 신청을 승인했을 때 신청자에게 결과를 알립니다.
    SAVING_GROUP_CREATION_APPROVED,
    // AI작성: 관리자가 계모임 개설 신청을 반려했을 때 신청자에게 결과를 알립니다.
    SAVING_GROUP_CREATION_REJECTED,
    // AI작성: 관리자가 계모임 삭제 신청을 승인했을 때 신청자인 계주에게 결과를 알립니다.
    SAVING_GROUP_DELETION_APPROVED,
    // AI작성: 관리자가 계모임 삭제 신청을 반려했을 때 신청자인 계주에게 결과를 알립니다.
    SAVING_GROUP_DELETION_REJECTED,

    // AI작성: 소모임 가입 신청이 접수되거나 신청 결과가 결정되었을 때 사용하는 알림입니다.
    SMALL_GROUP_JOIN_REQUEST,
    SMALL_GROUP_JOIN_APPROVED,
    SMALL_GROUP_JOIN_REJECTED,

    // AI작성: 참여 중인 모임에 일정, 공지 또는 투표가 새로 등록되었을 때 사용하는 알림입니다.
    SCHEDULE_CREATED,
    NOTICE_CREATED,
    VOTE_CREATED,

    // AI작성: 회비 납부 안내·입금 완료·미납 결과를 회원에게 전달할 때 사용하는 알림입니다.
    DUES_PAYMENT_DUE,
    DUES_PAYMENT_CONFIRMED,
    DUES_PAYMENT_OVERDUE,
    // AI작성: 관리자가 신고 처리를 완료했을 때 신고자에게 결과를 알립니다.
    REPORT_PROCESSED,

    // AI작성: 계금·지출 요청의 접수와 계주의 승인·반려 결과를 전달할 때 사용합니다.
    SAVING_GROUP_PAYOUT_REQUESTED,
    SAVING_GROUP_PAYOUT_APPROVED,
    SAVING_GROUP_PAYOUT_REJECTED,
    // AI작성: 모집 중인 계모임이 삭제되었을 때 참여 회원에게 결과를 알립니다.
    SAVING_GROUP_DELETED,

    // AI작성: 관리자가 회원 제재를 적용하거나 해제한 결과를 당사자에게 알립니다.
    MEMBER_RESTRICTED,
    MEMBER_RESTRICTION_RELEASED,

    // AI작성: 작성한 게시글에 댓글이 등록되거나 댓글에 답글이 등록되었을 때 사용하는 알림입니다.
    POST_COMMENT_CREATED,
    COMMENT_REPLY_CREATED
}
