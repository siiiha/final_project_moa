package com.nightstudy.moa.saving_group.service;

import com.nightstudy.moa.common.service.FileStorageService;
import com.nightstudy.moa.saving_group.dto.SavingGroupCreationRequest;
import com.nightstudy.moa.saving_group.mapper.SavingGroupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SavingGroupServiceImpl implements SavingGroupService {

    private final SavingGroupMapper savingGroupMapper;
    private final FileStorageService fileStorageService;

    public SavingGroupServiceImpl(SavingGroupMapper savingGroupMapper, FileStorageService fileStorageService) {
        this.savingGroupMapper = savingGroupMapper;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void applyForCreationSavingGroup(SavingGroupCreationRequest savingGroupCreationRequest) {

        String imagePath = null;

        MultipartFile groupImage = savingGroupCreationRequest.getGroupImage();

        if (groupImage != null && !groupImage.isEmpty()) {
            imagePath = fileStorageService.storeGroupImage(groupImage);
            savingGroupCreationRequest.setGroupImagePath(imagePath);
        }

        int groupsResult = savingGroupMapper.insertGroups(savingGroupCreationRequest);
        int savingGroupResult = savingGroupMapper.insertSavingGroup(savingGroupCreationRequest);

        if (groupsResult != 1 || savingGroupResult != 1) {
            // TODO: 실제 DB에 심사중인 상태에 계모임으로 저장되었는지 구분 (안되어있을 경우 예외 발생)
            throw new IllegalStateException("계모임 개설 신청에 실패했습니다.");
        }

    }

    @Override
    public void approveForCreationSavingGroup(Long savingGroupId) {
        int result = savingGroupMapper.approveForCreationSavingGroup(savingGroupId);

        if (result != 1) {
            // TODO: DB에 실제 계모임의 상태와 시작일이 정상적으로 수정되어 정상적인 계모임으로 변경되었는지 구분 (안되어있을 경우 예외 발생)
            throw new IllegalStateException("approveForCreationSavingGroup() failed");
        }
    }

}