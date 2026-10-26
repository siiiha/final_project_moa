package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListItemResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationSearchCondition;
import com.nightstudy.moa.common.dto.PageMeta;
import java.util.List;

// AI작성: 개설 신청 기능을 목록·상세 Service와 구분해 같은 도메인 패키지에서 관리한다.
public interface SavingGroupCreationService {
    boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest);

    List<SavingGroupCreationApplicationListItemResponse> getCreationApplications(
            SavingGroupCreationApplicationSearchCondition condition, PageMeta pageMeta);

    PageMeta getCreationApplicationPageMeta(SavingGroupCreationApplicationSearchCondition condition);

    SavingGroupCreationApplicationDetailResponse getCreationApplication(Long approvalId);

    void approveCreationApplication(Long approvalId, Long adminMemberId);

    void rejectCreationApplication(Long approvalId, String rejectReason);
}
