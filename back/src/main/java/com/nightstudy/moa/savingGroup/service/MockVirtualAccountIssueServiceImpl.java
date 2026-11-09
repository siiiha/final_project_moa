package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.*;
import java.time.OffsetDateTime;
import java.util.concurrent.ThreadLocalRandom;

// AI작성: 테스트에서 명시적으로만 사용하는 대역이다. Spring 빈으로 등록하거나 설정 오류 시 자동 대체하지 않는다.
public class MockVirtualAccountIssueServiceImpl implements VirtualAccountIssueService {
    @Override public void requireConfigured() {}
    @Override public String accountHolderName() { return "모아테스트"; }
    @Override public VirtualAccountIssueResult issue(VirtualAccountIssueRequest request) {
        try { Thread.sleep(ThreadLocalRandom.current().nextInt(300, 801)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("테스트 발급 중단"); }
        return new VirtualAccountIssueResult("00", "X000000000000", OffsetDateTime.now().plusHours(168));
    }
}
