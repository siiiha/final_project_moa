package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.*;
import java.time.YearMonth;

// AI작성: 계모임 일정 조회와 등록의 서비스 계약이다.
public interface SavingGroupCalendarService {
    Month getMonth(Long savingGroupId, Long memberId, YearMonth month);
    Event create(Long savingGroupId, Long memberId, CreateRequest request);
    // AI작성: 승인된 관리자가 대상 모임 일정을 삭제한다.
    void delete(Long savingGroupId, Long memberId, Long scheduleId);
}
