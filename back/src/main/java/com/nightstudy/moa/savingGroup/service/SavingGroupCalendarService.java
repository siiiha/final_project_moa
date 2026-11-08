package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.*;
import java.time.YearMonth;

// AI작성: 계모임 일정 조회와 등록의 서비스 계약이다.
public interface SavingGroupCalendarService {
    Month getMonth(Long groupId, Long memberId, YearMonth month);
    Event create(Long groupId, Long memberId, CreateRequest request);
}
