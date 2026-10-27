package com.nightstudy.moa.smallGroup;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupCreationResponse;
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

        Long regionId = smallGroupMapper.selectRegionIdByName(request.getRegionName().strip());
        if (regionId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }

        // AI작성: 종료 상태가 없는 현재 구조에서는 삭제되지 않은 생성 소모임을 최대 5개 제한 대상으로 본다.
        if (smallGroupMapper.selectActiveCreatedSmallGroupCount(request.getMemberId())
                >= MAX_ACTIVE_CREATED_SMALL_GROUPS) {
            throw new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CREATION_LIMIT_EXCEEDED",
                    "소모임은 최대 5개까지 만들 수 있습니다.");
        }

        normalizeRequest(request, regionId);
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

    private void normalizeRequest(SmallGroupCreationRequest request, Long regionId) {
        request.setRegionId(regionId);
        request.setGroupName(request.getGroupName().strip());
        request.setRegionName(request.getRegionName().strip());
        request.setLikeCategory(request.getLikeCategory().strip());
        request.setDescription(request.getDescription().strip());
        request.setLocation(request.getLocation().strip());
    }
}
