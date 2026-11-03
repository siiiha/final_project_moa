package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.region.MemberRegionDto;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCategoryResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupRegionResponse;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 공통 모임·소모임 상세·생성자 권한 관계를 하나의 트랜잭션으로 생성한다.
@Service
public class SmallGroupCreationServiceImpl implements SmallGroupCreationService {
    private static final long MAX_ACTIVE_CREATED_SMALL_GROUPS = 5L;

    private final SmallGroupMapper smallGroupMapper;

    public SmallGroupCreationServiceImpl(SmallGroupMapper smallGroupMapper) {
        this.smallGroupMapper = smallGroupMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SmallGroupRegionResponse> getSmallGroupRegions() {
        // AI작성: 화면의 선택지와 저장 기준을 일치시키기 위해 DB의 지역 기준 데이터만 반환한다.
        return smallGroupMapper.selectSmallGroupRegions();
    }

    // AI작성: 목록 필터와 생성 화면이 같은 interest 기준 데이터를 사용하도록 조회한다.
    @Override
    @Transactional(readOnly = true)
    public List<SmallGroupCategoryResponse> getSmallGroupCategories() {
        // TODO: 운영 DB 연결 설정이 제공되면 기존 small_group.like_category 값과 interest 기준 데이터의 불일치를 점검한다.
        return smallGroupMapper.selectSmallGroupCategories();
    }

    @Override
    @Transactional
    public SmallGroupCreationResponse createSmallGroup(SmallGroupCreationRequest request) {
        // AI작성: Controller가 JWT 필터에서 검증한 회원 ID를 request에 설정해 전달한다.
        if (request == null || request.getMemberId() == null || request.getMemberId() <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        if (!smallGroupMapper.existsActiveMember(request.getMemberId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원 정보를 찾을 수 없습니다.");
        }

        // AI작성: 화면 검증을 우회한 광역 지역·누락·모호한 이름은 저장 전에 거절한다.
        if (request.getRegionName() == null || request.getRegionName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }
        List<MemberRegionDto> regions = smallGroupMapper.selectCreationRegionsByName(request.getRegionName().strip());
        if (regions.size() != 1 || !isCreationRegion(regions.get(0))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }
        MemberRegionDto region = regions.get(0);

        // AI작성: 종료 상태가 없는 현재 구조에서는 삭제되지 않은 생성 소모임을 최대 5개 제한 대상으로 본다.
        if (smallGroupMapper.selectActiveCreatedSmallGroupCount(request.getMemberId())
                >= MAX_ACTIVE_CREATED_SMALL_GROUPS) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CREATION_LIMIT_EXCEEDED",
                    "소모임은 최대 5개까지 만들 수 있습니다.");
        }

        // AI작성: 입력된 지역 ID는 신뢰하지 않고 DB에서 검증한 ID와 정규 이름을 사용한다.
        request.setRegionName(region.getRegionName());
        normalizeRequest(request, region.getRegionId());
        if (smallGroupMapper.insertGroup(request) != 1
                || smallGroupMapper.insertSmallGroup(request) != 1
                || smallGroupMapper.insertOwnerMembership(request.getGroupId(), request.getMemberId()) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "SMALL_GROUP_CREATION_FAILED",
                    "소모임 생성에 실패했습니다.");
        }

        return new SmallGroupCreationResponse(
                request.getSmallGroupId(), request.getGroupId(), request.getGroupName(), request.getRegionName(),
                request.getLikeCategory(), request.getDescription(), request.getMaxMembers(), request.getMeetDate(),
                request.getLocation(), "OWNER");
    }

    // AI작성: 구 또는 그 하위 동을 허용하며 구가 없는 세종만 빈 구 이름을 허용한다.
    private boolean isCreationRegion(MemberRegionDto region) {
        return region.getRegionId() != null
                && region.getProvinceName() != null && !region.getProvinceName().isBlank()
                && region.getDistrictName() != null
                && (!region.getDistrictName().isBlank()
                    || ("세종특별자치시".equals(region.getProvinceName()) && region.getDistrictName().isEmpty()));
    }

    private void normalizeRequest(SmallGroupCreationRequest request, Long regionId) {
        request.setRegionId(regionId);
        request.setGroupName(request.getGroupName().strip());
        request.setRegionName(request.getRegionName().strip());
        request.setLikeCategory(request.getLikeCategory().strip());
        request.setDescription(request.getDescription().strip());
        request.setLocation(request.getLocation().strip());
    }
}
