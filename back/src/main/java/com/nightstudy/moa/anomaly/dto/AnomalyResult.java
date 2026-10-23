package com.nightstudy.moa.anomaly.dto;
import java.util.List;

// AI작성: boolean/숫자 래퍼로 누락 응답을 정상·0점으로 오인하지 않는다.
public record AnomalyResult(String account_id, String transaction_id, String transacted_at,
        String transaction_type, Double anomaly_score, Double threshold, Boolean is_anomaly,
        String score_definition, String model_version, String feature_schema_version,
        Boolean cold_start, List<String> quality_flags, Boolean requires_review) {}
