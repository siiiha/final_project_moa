package com.nightstudy.moa.anomaly.dto;

// AI작성: 모델 경고와 독립된 현재 DB 기록 문맥. 정상/사기 판정이나 경고 해제 지시가 아니다.
public record TransactionBusinessContext(State state, Long payoutRequestId, String message) {
    public enum State { NO_LINK, LINKED_COMPLETED_PAYOUT, LINK_REQUIRES_REVIEW }
}
