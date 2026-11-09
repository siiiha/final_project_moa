package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// AI작성: 역할 변경의 권한·소속·상태·회계 한 명 제한과 실패 시 UPDATE 차단을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberRoleServiceTest {
    @Mock SavingGroupMapper mapper;
    @Mock NotificationService notificationService;
    @InjectMocks SavingGroupServiceImpl service;
    void group(String status, long ownerId) {
        var target = new SavingGroupJoinTargetRow(); target.setGroupId(20L); target.setSavingGroupId(10L);
        target.setStatus(status); target.setOwnerMemberId(ownerId);
        when(mapper.selectSavingGroupJoinTargetForUpdate(10L)).thenReturn(target);
    }
    SavingGroupMembershipRelationRow relation(String role, String status, long crewId) {
        var row = new SavingGroupMembershipRelationRow(); row.setMemberRole(role); row.setMembershipStatus(status); row.setCrewMemberId(crewId);
        return row;
    }
    void owner() {
        group("01", 30L);
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation("OWNER", "APPROVED", 31L));
    }
    void member(String role, String status) {
        when(mapper.selectSavingGroupMembershipForUpdate(20L, 50L)).thenReturn(relation(role, status, 40L));
        if ("APPROVED".equals(status)) when(mapper.existsMember(50L)).thenReturn(true);
    }
    void fails(String code, Long groupId, Long targetId, Long actorId, String role) {
        assertThatThrownBy(() -> service.updateSavingGroupMemberRole(groupId, targetId, actorId, role))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
        verify(mapper, never()).updateSavingGroupMemberRole(anyLong(), anyLong(), anyString(), anyString());
    }
    @ParameterizedTest @ValueSource(strings = {"MEMBER", "MANAGER"})
    void ownerChangesRoleInBothDirections(String before) {
        owner(); member(before, "APPROVED"); String after = before.equals("MEMBER") ? "MANAGER" : "MEMBER";
        if (after.equals("MANAGER")) when(mapper.selectSavingGroupManagerIdsForUpdate(20L)).thenReturn(List.of());
        when(mapper.updateSavingGroupMemberRole(20L, 40L, before, after)).thenReturn(1);
        service.updateSavingGroupMemberRole(10L, 50L, 30L, after);
        var order = inOrder(mapper);
        order.verify(mapper).selectSavingGroupJoinTargetForUpdate(10L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(20L, 30L);
        order.verify(mapper).selectSavingGroupMembershipForUpdate(20L, 50L);
        order.verify(mapper).updateSavingGroupMemberRole(20L, 40L, before, after);
        verify(notificationService).send(50L, NotificationType.SAVING_GROUP_MEMBER_ROLE_CHANGED,
                after.equals("MANAGER") ? "계모임 회계로 지정되었습니다." : "계모임 일반 계원으로 변경되었습니다.",
                "/saving-groups/10");
    }
    @Test void ongoingGroupCanChange() {
        owner(); group("02", 30L); member("MANAGER", "APPROVED");
        when(mapper.updateSavingGroupMemberRole(20L, 40L, "MANAGER", "MEMBER")).thenReturn(1);
        service.updateSavingGroupMemberRole(10L, 50L, 30L, "MEMBER");
    }
    @Test void secondManagerIsRejected() {
        owner(); member("MEMBER", "APPROVED"); when(mapper.selectSavingGroupManagerIdsForUpdate(20L)).thenReturn(List.of(60L));
        fails("SAVING_GROUP_MANAGER_ALREADY_EXISTS", 10L, 50L, 30L, "MANAGER");
    }
    @ParameterizedTest @ValueSource(strings = {"MEMBER", "MANAGER"})
    void sameRoleIsIdempotent(String role) {
        owner(); member(role, "APPROVED");
        if (role.equals("MANAGER")) when(mapper.selectSavingGroupManagerIdsForUpdate(20L)).thenReturn(List.of(50L));
        service.updateSavingGroupMemberRole(10L, 50L, 30L, role);
        verify(mapper, never()).updateSavingGroupMemberRole(anyLong(), anyLong(), anyString(), anyString());
        verifyNoInteractions(notificationService);
    }
    @Test void loginRequired() { fails("UNAUTHORIZED", 10L, 50L, null, "MEMBER"); verifyNoInteractions(mapper); }
    @Test void missingGroup() { fails("SAVING_GROUP_NOT_FOUND", 10L, 50L, 30L, "MEMBER"); }
    @Test void invalidGroup() { fails("BAD_REQUEST", 0L, 50L, 30L, "MEMBER"); }
    @Test void otherGroupOwner() { group("01", 99L); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 50L, 30L, "MEMBER"); }
    @ParameterizedTest @ValueSource(strings = {"MANAGER", "MEMBER"})
    void otherRolesCannotManage(String role) {
        group("01", 30L); when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation(role, "APPROVED", 31L));
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 50L, 30L, "MEMBER");
    }
    @Test void nonMemberCannotManage() { group("01", 30L); fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 50L, 30L, "MEMBER"); }
    @Test void unapprovedOwnerCannotManage() {
        group("01", 30L); when(mapper.selectSavingGroupMembershipForUpdate(20L, 30L)).thenReturn(relation("OWNER", "PENDING", 31L));
        fails("SAVING_GROUP_OWNER_REQUIRED", 10L, 50L, 30L, "MEMBER");
    }
    @ParameterizedTest @ValueSource(strings = {"00", "03", "04", "05"})
    void inactiveGroup(String status) { group(status, 30L); fails("SAVING_GROUP_ACTIVITY_RESTRICTED", 10L, 50L, 30L, "MEMBER"); }
    @Test void missingOrOtherGroupMember() { owner(); fails("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND", 10L, 50L, 30L, "MEMBER"); }
    @Test void deletedMember() {
        owner(); when(mapper.selectSavingGroupMembershipForUpdate(20L, 50L)).thenReturn(relation("MEMBER", "APPROVED", 40L));
        fails("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND", 10L, 50L, 30L, "MANAGER");
    }
    @ParameterizedTest @ValueSource(strings = {"PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void unapprovedTarget(String status) { owner(); member("MEMBER", status); fails("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND", 10L, 50L, 30L, "MANAGER"); }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"OWNER", "ADMIN", "manager"})
    void invalidRole(String role) { owner(); fails("BAD_REQUEST", 10L, 50L, 30L, role); }
    @Test void invalidTargetId() { owner(); fails("BAD_REQUEST", 10L, 0L, 30L, "MEMBER"); }
    @Test void ownerTarget() { owner(); member("OWNER", "APPROVED"); fails("SAVING_GROUP_OWNER_ROLE_CHANGE_NOT_ALLOWED", 10L, 50L, 30L, "MEMBER"); }
    @Test void cannotChangeSelf() {
        owner(); when(mapper.existsMember(30L)).thenReturn(true);
        fails("SAVING_GROUP_OWNER_ROLE_CHANGE_NOT_ALLOWED", 10L, 30L, 30L, "MEMBER");
    }
    @Test void unexpectedExistingRole() { owner(); member("UNKNOWN", "APPROVED"); fails("SAVING_GROUP_MEMBER_ROLE_INVALID", 10L, 50L, 30L, "MEMBER"); }
    @Test void failedConditionalUpdateThrowsForRollback() {
        owner(); member("MANAGER", "APPROVED");
        assertThatThrownBy(() -> service.updateSavingGroupMemberRole(10L, 50L, 30L, "MEMBER"))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("SAVING_GROUP_MEMBER_ROLE_UPDATE_FAILED"));
    }
}
