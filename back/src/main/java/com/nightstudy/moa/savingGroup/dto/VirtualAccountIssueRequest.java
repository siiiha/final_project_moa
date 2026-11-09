package com.nightstudy.moa.savingGroup.dto;

// AI작성: 개인정보 없이 서버가 생성한 식별자만 발급 계층으로 전달한다.
public record VirtualAccountIssueRequest(Long approvalId, Long groupId, String orderId) {}
