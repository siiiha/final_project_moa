package com.nightstudy.moa.notification.dto;

// AI작성: 모아 서비스에서 발생할 수 있는 알림 종류를 정의합니다.
// 오타가 날 수 있는 문자열 대신 컴파일러가 검사 할 수 있는 enum을 사용한다.
public enum NotificationType {

    SAVING_GROUP_JOIN_REQUEST,
    SAVING_GROUP_JOIN_APPROVED,
    SAVING_GROUP_JOIN_REJECTED,

    SMALL_GROUP_JOIN_REQUEST,
    SMALL_GROUP_JOIN_APPROVED,
    SMALL_GROUP_JOIN_REJECTED,

    SCHEDULE_CREATED,
    NOTICE_CREATED,
    VOTE_CREATED,

    DUES_PAYMENT_DUE,
    REPORT_PROCESSED,

    TEST
}