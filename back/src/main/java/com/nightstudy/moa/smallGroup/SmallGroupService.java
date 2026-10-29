package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupJoinRequestListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;

// AI작성: 소모임 공개 목록의 검색·필터 조회 계약이다.
public interface SmallGroupService {
    SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition condition);

    // AI작성: memberId가 null인 비로그인 공개 조회도 허용하는 상세 조회 계약이다.
    SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId);

    // AI작성: 인증된 일반 회원 본인 명의로 소모임 가입 신청을 대기 상태로 등록한다.
    void requestSmallGroupJoin(Long smallGroupId, Long memberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 상태별 가입 신청 목록을 조회한다.
    SmallGroupJoinRequestListResponse getSmallGroupJoinRequests(
            Long smallGroupId, Long memberId, String status, int page);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 소모임 설정의 현재 수정 가능 정보를 조회한다.
    SmallGroupSettingsResponse getSmallGroupSettings(Long smallGroupId, Long loginMemberId);

    // AI작성: 개설자이면서 승인된 OWNER인 회원만 전달한 소모임 설정 필드를 수정한다.
    void updateSmallGroupSettings(
            Long smallGroupId, Long loginMemberId, SmallGroupSettingsPatchRequest request);
}
