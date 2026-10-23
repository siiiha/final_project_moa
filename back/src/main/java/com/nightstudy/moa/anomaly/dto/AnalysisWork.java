package com.nightstudy.moa.anomaly.dto;

// AI작성: 등록 시점 입력을 모델 호출 동안 고정한다. duplicate면 신규 분석을 시작하지 않는다.
public record AnalysisWork(long transactionId, int revisionNo, boolean duplicate,
        String fingerprint, AnomalyHistoryRequest request) {}
