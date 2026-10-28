package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberCount;
import com.nightstudy.moa.smallGroup.dto.SmallGroupListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupNoticeSummaryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSearchCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsAuthorityRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSummaryResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 목록 조건·페이징·승인 계원 수 조합을 담당한다.
@Service
public class SmallGroupServiceImpl implements SmallGroupService {
    private static final int PAGE_SIZE = 6;
    private static final List<String> REGIONS = List.of(
            "서울", "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주도");
    private final SmallGroupMapper smallGroupMapper;

    public SmallGroupServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    // AI작성: 공개 상세는 비회원도 반환하고, 관계 데이터가 있는 로그인 사용자만 역할을 판단한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupDetailResponse getSmallGroupDetail(Long smallGroupId, Long memberId) {
        SmallGroupDetailRow row = smallGroupMapper.selectSmallGroupDetail(smallGroupId);
        if (row == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 소모임이 없습니다.");
        }

        String myRole = memberId == null ? null : resolveMemberRole(row.getGroupId(), memberId);
        long memberCount = smallGroupMapper.selectApprovedSmallGroupMemberCount(row.getGroupId());
        // AI작성: 비회원도 제목·작성자·작성일을 볼 수 있도록 역할과 무관하게 공지 요약을 함께 조회한다.
        List<SmallGroupNoticeSummaryResponse> notices =
                smallGroupMapper.selectRecentSmallGroupNotices(row.getGroupId());

        return new SmallGroupDetailResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getLikeCategory(), row.getMeetDate(), row.getLocation(),
                row.getMaxMembers(), memberCount, row.getCreatorNickname(), row.getCreatedAt(), myRole,
                notices);
    }

    // AI작성: 소모임에는 상태별 설정 제한이 없으므로 삭제 여부와 개설자·승인 OWNER 권한만 확인한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupSettingsResponse getSmallGroupSettings(Long smallGroupId, Long loginMemberId) {
        if (loginMemberId == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }

        SmallGroupSettingsAuthorityRow authority =
                smallGroupMapper.selectSmallGroupSettingsAuthority(smallGroupId);
        if (authority == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임 정보를 찾을 수 없습니다.");
        }

        boolean isOwner = Objects.equals(authority.getGroupCreatorMemberId(), loginMemberId)
                && Objects.equals(authority.getOwnerMemberId(), loginMemberId);
        if (!isOwner) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_SETTINGS_FORBIDDEN",
                    "개설자만 설정을 조회할 수 있습니다.");
        }

        SmallGroupSettingsResponse settings =
                smallGroupMapper.selectSmallGroupCurrentSettings(smallGroupId);
        if (settings == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND", "소모임 정보를 찾을 수 없습니다.");
        }
        return settings;
    }

    // AI작성: MANAGER는 상태와 무관한 오류 데이터이며, 중복·미정의 역할은 권한을 임의로 선택하지 않는다.
    private String resolveMemberRole(Long groupId, Long memberId) {
        List<SmallGroupMemberRelationRow> relations =
                smallGroupMapper.selectSmallGroupMemberRelations(groupId, memberId);

        if (relations.stream().anyMatch(relation -> "MANAGER".equals(relation.getMemberRole()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "접근 권한이 없습니다.");
        }
        if (relations.size() > 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        if (relations.isEmpty()) {
            return null;
        }

        SmallGroupMemberRelationRow relation = relations.get(0);
        if (!List.of("OWNER", "MEMBER").contains(relation.getMemberRole())) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
        return "APPROVED".equals(relation.getMembershipStatus()) ? relation.getMemberRole() : null;
    }

    // AI작성: 계모임과 동일한 지역 코드 기준으로 변환하고, 카테고리는 소모임 전용 컬럼으로만 필터링한다.
    @Override
    @Transactional(readOnly = true)
    public SmallGroupListResponse getSmallGroupList(SmallGroupSearchCondition request) {
        String regionName = request.getRegionCode() == null ? null
                : REGIONS.get(Integer.parseInt(request.getRegionCode()) - 1);
        SmallGroupListCondition condition = new SmallGroupListCondition(
                escapeKeyword(request.getKeyword()), regionName, request.getLikeCategory(), 0, PAGE_SIZE);
        int total = smallGroupMapper.selectSmallGroupCount(condition);
        PageMeta pageMeta = PageMeta.of(request.getPage(), PAGE_SIZE, total);
        // AI작성: 계모임 목록과 동일하게 범위 초과는 offset 계산 전에 빈 목록으로 반환한다.
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return new SmallGroupListResponse(List.of(), pageMeta);
        }
        condition.setOffset(pageMeta.getOffset());
        List<SmallGroupRow> rows = smallGroupMapper.selectSmallGroupList(condition);
        if (rows.isEmpty()) {
            return new SmallGroupListResponse(List.of(), pageMeta);
        }

        Map<Long, Long> memberCounts = smallGroupMapper.selectApprovedMemberCounts(
                        rows.stream().map(SmallGroupRow::getGroupId).distinct().toList())
                .stream().collect(Collectors.toMap(
                        SmallGroupMemberCount::getGroupId, SmallGroupMemberCount::getMemberCount));

        List<SmallGroupSummaryResponse> items = rows.stream().map(row -> new SmallGroupSummaryResponse(
                row.getSmallGroupId(), row.getGroupId(), row.getGroupName(), row.getDescription(),
                row.getRegionName(), row.getMaxMembers(), memberCounts.getOrDefault(row.getGroupId(), 0L),
                row.getLikeCategory(), row.getMeetDate(), row.getLocation())).toList();
        return new SmallGroupListResponse(items, pageMeta);
    }

    // AI작성: 계모임 목록과 동일하게 LIKE 와일드카드를 일반 검색 문자로 처리한다.
    private String escapeKeyword(String keyword) {
        return keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
    }
}
