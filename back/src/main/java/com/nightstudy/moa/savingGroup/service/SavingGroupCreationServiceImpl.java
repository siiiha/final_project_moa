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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 기존 saving_group 구현을 savingGroup 도메인과 명확한 빈 이름으로 통합한다.
@Service
public class SavingGroupCreationServiceImpl implements SavingGroupCreationService {
    private static final int APPLICATION_PAGE_SIZE = 8;

    private final SavingGroupMapper savingGroupMapper;
    private final FileStorageService fileStorageService;

    public SavingGroupCreationServiceImpl(SavingGroupMapper savingGroupMapper, FileStorageService fileStorageService) {
        this.savingGroupMapper = savingGroupMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public boolean applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {
        Long regionId = savingGroupMapper.selectRegionIdByName(savingGroupCreationRequest.getRegionName());
        if (regionId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REGION_NOT_FOUND", "활동 지역을 다시 선택해 주세요.");
        }
        if (!savingGroupMapper.existsMember(savingGroupCreationRequest.getMemberId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "신청자 정보를 찾을 수 없습니다.");
        }
        savingGroupCreationRequest.setRegionId(regionId);
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
        int approvalResult = savingGroupMapper.insertSavingGroupCreationApproval(
                savingGroupCreationRequest.getGroupId());
        if (groupsResult != 1 || savingGroupResult != 1 || approvalResult != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "SAVING_GROUP_CREATION_FAILED",
                    "계모임 개설 신청에 실패했습니다."
            );
        }
        return true;
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
                    "계모임 개설 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    // AI작성: 심사 상태를 잠근 뒤 PENDING인 신청만 한 번 처리해 중복 승인·반려를 막는다.
    @Override
    @Transactional
    public void approveCreationApplication(Long approvalId) {
        SavingGroupCreationApplicationDetailResponse application = getCreationApplicationForUpdate(approvalId);
        requirePending(application);
        if (savingGroupMapper.approveSavingGroupCreationApplication(approvalId) != 1
                || savingGroupMapper.publishApprovedSavingGroup(application.getGroupId()) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_PROCESS_FAILED",
                    "이미 처리되었거나 승인할 수 없는 신청입니다.");
        }
    }

    @Override
    @Transactional
    public void rejectCreationApplication(Long approvalId, String rejectReason) {
        SavingGroupCreationApplicationDetailResponse application = getCreationApplicationForUpdate(approvalId);
        requirePending(application);
        if (savingGroupMapper.rejectSavingGroupCreationApplication(approvalId, rejectReason.strip()) != 1) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_PROCESS_FAILED",
                    "이미 처리되었거나 반려할 수 없는 신청입니다.");
        }
    }

    private SavingGroupCreationApplicationDetailResponse getCreationApplicationForUpdate(Long approvalId) {
        SavingGroupCreationApplicationDetailResponse application = savingGroupMapper
                .selectSavingGroupCreationApplicationForUpdate(approvalId);
        if (application == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_APPLICATION_NOT_FOUND",
                    "계모임 개설 신청을 찾을 수 없습니다.");
        }
        return application;
    }

    private void requirePending(SavingGroupCreationApplicationDetailResponse application) {
        if (!"PENDING".equals(application.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_APPLICATION_ALREADY_PROCESSED",
                    "대기 중인 개설 신청만 처리할 수 있습니다.");
        }
    }

    private SavingGroupCreationApplicationListCondition createListCondition(
            SavingGroupCreationApplicationSearchCondition condition, int offset) {
        String keyword = condition.getKeyword();
        String keywordPattern = keyword == null ? null : "%" + keyword.replace("!", "!!")
                .replace("%", "!%").replace("_", "!_") + "%";
        return new SavingGroupCreationApplicationListCondition(
                keywordPattern, condition.getStatus(), offset, APPLICATION_PAGE_SIZE);
    }
}
