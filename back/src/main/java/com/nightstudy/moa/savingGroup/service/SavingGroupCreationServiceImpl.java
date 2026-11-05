package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListItemResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationSearchCondition;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.member.dto.region.MemberRegionDto;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 기존 saving_group 구현을 savingGroup 도메인과 명확한 빈 이름으로 통합한다.
@Service
public class SavingGroupCreationServiceImpl implements SavingGroupCreationService {
    private static final int APPLICATION_PAGE_SIZE = 8;
    // AI작성: 관리자 심사 분기와 상태 검증에서 같은 문자열을 반복하지 않도록 공통 상수로 관리한다.
    private static final String APPLICATION_TYPE_DELETE = "DELETE";
    private static final String APPLICATION_STATUS_PENDING = "PENDING";

    private final SavingGroupMapper savingGroupMapper;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public SavingGroupCreationServiceImpl(SavingGroupMapper savingGroupMapper, FileStorageService fileStorageService,
            NotificationService notificationService) {
        this.savingGroupMapper = savingGroupMapper;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {
        // AI작성: 화면 검증을 우회해도 광역 지역만 저장할 수 없으며 클라이언트의 regionId는 신뢰하지 않는다.
        String regionName = savingGroupCreationRequest.getRegionName();
        if (regionName == null || regionName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }
        List<MemberRegionDto> regions = savingGroupMapper.selectCreationRegionsByName(regionName.strip());
        if (regions.size() != 1 || !isCreationRegion(regions.get(0))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }
        MemberRegionDto region = regions.get(0);
        if (!savingGroupMapper.existsMember(savingGroupCreationRequest.getMemberId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "신청자 정보를 찾을 수 없습니다.");
        }
        savingGroupCreationRequest.setRegionId(region.getRegionId());
        savingGroupCreationRequest.setRegionName(region.getRegionName());
        savingGroupCreationRequest.setGroupName(savingGroupCreationRequest.getGroupName().strip());
        savingGroupCreationRequest.setDescription(savingGroupCreationRequest.getDescription().strip());
        MultipartFile groupImage = savingGroupCreationRequest.getGroupImage();
        if (groupImage != null && !groupImage.isEmpty()) {
            String imagePath = fileStorageService.storeGroupImage(groupImage);
            savingGroupCreationRequest.setGroupImagePath(imagePath);
        }

        int groupsResult = savingGroupMapper.insertGroups(savingGroupCreationRequest);
        System.out.println(savingGroupCreationRequest.getGroupId());
        int savingGroupResult = savingGroupMapper.insertSavingGroup(savingGroupCreationRequest);
        System.out.println(savingGroupCreationRequest.getGroupId());
        int ownerMembershipResult = savingGroupMapper.insertSavingGroupCreationOwnerMembership(
                savingGroupCreationRequest.getGroupId(), savingGroupCreationRequest.getMemberId());
        int approvalResult = savingGroupMapper.insertSavingGroupCreationApproval(
                savingGroupCreationRequest.getGroupId());
        if (groupsResult != 1 || savingGroupResult != 1 || ownerMembershipResult != 1 || approvalResult != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SAVING_GROUP_CREATION_FAILED",
                    "계모임 개설 신청에 실패했습니다."
            );
        }
        return true;
    }

    // AI작성: 구 단위와 그 하위 동을 허용한다. 구가 없는 세종만 빈 구 이름을 허용하고 null은 거절한다.
    private boolean isCreationRegion(MemberRegionDto region) {
        return region.getRegionId() != null
                && region.getProvinceName() != null && !region.getProvinceName().isBlank()
                && region.getDistrictName() != null
                && (!region.getDistrictName().isBlank()
                    || ("세종특별자치시".equals(region.getProvinceName()) && region.getDistrictName().isEmpty()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageMeta getCreationApplicationPageMeta(SavingGroupCreationApplicationSearchCondition condition) {
        SavingGroupCreationApplicationListCondition listCondition = createListCondition(condition, 0);
        return PageMeta.of(condition.getPage(), APPLICATION_PAGE_SIZE,
                savingGroupMapper.selectSavingGroupCreationApplicationCount(listCondition));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavingGroupCreationApplicationListItemResponse> getCreationApplications(
            SavingGroupCreationApplicationSearchCondition condition, PageMeta pageMeta) {
        if (pageMeta.getPage() > pageMeta.getTotalPages()) {
            return List.of();
        }
        return savingGroupMapper.selectSavingGroupCreationApplications(
                createListCondition(condition, pageMeta.getOffset()));
    }

    @Override
    @Transactional(readOnly = true)
    public SavingGroupCreationApplicationDetailResponse getCreationApplication(Long approvalId) {
        SavingGroupCreationApplicationDetailResponse application = savingGroupMapper
                .selectSavingGroupCreationApplication(approvalId);
        if (application == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPLICATION_NOT_FOUND",
                    "계모임 심사 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    // AI작성: 심사 상태를 잠근 뒤 PENDING인 신청만 한 번 처리해 중복 승인·반려를 막는다.
    @Override
    @Transactional
    public void approveCreationApplication(Long approvalId, Long adminMemberId) {
        if (adminMemberId == null || adminMemberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        SavingGroupCreationApplicationDetailResponse application = getCreationApplicationForUpdate(approvalId);
        requirePending(application);
        if (APPLICATION_TYPE_DELETE.equals(application.getType())) {
            approveDeletionApplication(application);
            return;
        }
        if (savingGroupMapper.approveSavingGroupCreationApplication(approvalId) != 1
                || savingGroupMapper.publishApprovedSavingGroup(application.getGroupId()) != 1
                || savingGroupMapper.approveSavingGroupCreationOwnerMembership(
                        application.getGroupId(), adminMemberId) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_PROCESS_FAILED",
                    "이미 처리되었거나 승인할 수 없는 신청입니다.");
        }
        // AI작성: 개설 심사의 모든 상태 변경이 성공한 뒤 신청자에게 승인 결과와 공개 상세 경로를 알린다.
        notificationService.send(
                application.getApplicantMemberId(),
                NotificationType.SAVING_GROUP_CREATION_APPROVED,
                "계모임 개설 신청이 승인되었습니다.",
                "/saving-groups/" + application.getSavingGroupId()
        );
    }

    @Override
    @Transactional
    public void rejectCreationApplication(Long approvalId, String rejectReason) {
        SavingGroupCreationApplicationDetailResponse application = getCreationApplicationForUpdate(approvalId);
        requirePending(application);
        // AI작성: 검증된 반려 사유의 앞뒤 공백을 한 번만 제거해 CREATE·DELETE 처리에서 동일하게 사용한다.
        String normalizedRejectReason = rejectReason.strip();
        if (APPLICATION_TYPE_DELETE.equals(application.getType())) {
            if (savingGroupMapper.rejectSavingGroupDeletionApplication(approvalId, normalizedRejectReason) != 1) {
                throw applicationProcessFailed();
            }
            // AI작성: 삭제 신청이 반려된 뒤 신청자인 계주에게 유지된 계모임의 상세 경로와 결과를 알린다.
            notificationService.send(
                    application.getApplicantMemberId(),
                    NotificationType.SAVING_GROUP_DELETION_REJECTED,
                    "계모임 삭제 신청이 반려되었습니다.",
                    "/saving-groups/" + application.getSavingGroupId()
            );
            return;
        }
        if (savingGroupMapper.rejectSavingGroupCreationApplication(approvalId, normalizedRejectReason) != 1
                || savingGroupMapper.rejectSavingGroupCreationOwnerMembership(application.getGroupId()) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_PROCESS_FAILED",
                    "이미 처리되었거나 반려할 수 없는 신청입니다.");
        }
        // AI작성: 개설 심사와 대기 계주 관계가 모두 반려된 뒤 신청자에게 결과를 알리며 비공개 모임 링크는 제공하지 않는다.
        notificationService.send(
                application.getApplicantMemberId(),
                NotificationType.SAVING_GROUP_CREATION_REJECTED,
                "계모임 개설 신청이 반려되었습니다.",
                null
        );
    }

    // AI작성: 승인 시점에 상태·공동자금·출금을 다시 확인해 신청 이후 변경으로 인한 잘못된 삭제를 막는다.
    private void approveDeletionApplication(SavingGroupCreationApplicationDetailResponse application) {
        Long groupId = application.getGroupId();
        if (savingGroupMapper.existsActiveSavingGroup(groupId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETE_ACTIVE_ROUND",
                    "진행 중이거나 정지된 계모임은 삭제할 수 없습니다.");
        }
        java.math.BigDecimal balance = savingGroupMapper.selectSavingGroupBalance(groupId);
        if (balance != null && balance.compareTo(java.math.BigDecimal.ZERO) != 0) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETE_BALANCE_REMAINS",
                    "공동자금이 남아 있어 삭제할 수 없습니다.");
        }
        if (savingGroupMapper.existsPendingPayout(groupId)) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DELETE_PAYOUT_PENDING",
                    "처리되지 않은 출금 요청이 있어 삭제할 수 없습니다.");
        }
        if (savingGroupMapper.approveSavingGroupDeletionApplication(application.getApprovalId()) != 1
                || savingGroupMapper.softDeleteSavingGroup(groupId) != 1) {
            throw applicationProcessFailed();
        }
        // AI작성: 삭제 심사 승인과 소프트 삭제가 모두 성공한 뒤 신청자인 계주에게 결과만 알린다.
        notificationService.send(
                application.getApplicantMemberId(),
                NotificationType.SAVING_GROUP_DELETION_APPROVED,
                "계모임 삭제 신청이 승인되었습니다.",
                null
        );
    }

    private ApiException applicationProcessFailed() {
        return new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_PROCESS_FAILED",
                "이미 처리되었거나 처리할 수 없는 신청입니다.");
    }

    private SavingGroupCreationApplicationDetailResponse getCreationApplicationForUpdate(Long approvalId) {
        SavingGroupCreationApplicationDetailResponse application = savingGroupMapper
                .selectSavingGroupCreationApplicationForUpdate(approvalId);
        if (application == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPLICATION_NOT_FOUND",
                    "계모임 심사 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    private void requirePending(SavingGroupCreationApplicationDetailResponse application) {
        if (!APPLICATION_STATUS_PENDING.equals(application.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_ALREADY_PROCESSED",
                    "대기 중인 심사 신청만 처리할 수 있습니다.");
        }
    }

    private SavingGroupCreationApplicationListCondition createListCondition(
            SavingGroupCreationApplicationSearchCondition condition, int offset) {
        String keyword = condition.getKeyword();
        String keywordPattern = keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
        return new SavingGroupCreationApplicationListCondition(
                keywordPattern, condition.getStatus(), condition.getType(), offset, APPLICATION_PAGE_SIZE);
    }
}
