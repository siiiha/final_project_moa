package com.nightstudy.moa.savingGroup.service;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// AI작성: OWNER 권한·모집 상태·현재 소속 및 MEMBER/MANAGER 추방을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberExpulsionServiceTest {
 @Mock SavingGroupMapper mapper;
 @InjectMocks SavingGroupServiceImpl service;
 SavingGroupMembershipRelationRow row(String role,String status) {
  var r=new SavingGroupMembershipRelationRow();r.setCrewMemberId(40L);r.setMemberRole(role);r.setMembershipStatus(status);return r;
 }
 void group(String status,long owner) {
  var g=new SavingGroupJoinTargetRow();g.setGroupId(20L);g.setSavingGroupId(10L);g.setStatus(status);g.setOwnerMemberId(owner);
  when(mapper.selectSavingGroupExpulsionTargetForUpdate(20L)).thenReturn(g);
 }
 void owner() {group("01",30L);when(mapper.selectSavingGroupMembershipForUpdate(20L,30L)).thenReturn(row("OWNER","APPROVED"));}
 void target(String role) {when(mapper.selectSavingGroupMembershipForUpdate(20L,50L)).thenReturn(row(role,"APPROVED"));when(mapper.existsMember(50L)).thenReturn(true);}
 void failure(String code,Long target,Long actor) {
  assertThatThrownBy(()->service.expelApprovedSavingGroupMember(20L,target,actor)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo(code));
  verify(mapper,never()).expelApprovedSavingGroupMember(anyLong(),anyLong());
 }
 @ParameterizedTest @ValueSource(strings={"MEMBER","MANAGER"}) void success(String role) {
  owner();target(role);when(mapper.expelApprovedSavingGroupMember(40L,20L)).thenReturn(1);
  service.expelApprovedSavingGroupMember(20L,50L,30L);
  var order=inOrder(mapper);order.verify(mapper).selectSavingGroupExpulsionTargetForUpdate(20L);
  order.verify(mapper).selectSavingGroupMembershipForUpdate(20L,30L);order.verify(mapper).selectSavingGroupMembershipForUpdate(20L,50L);
  order.verify(mapper).existsMember(50L);order.verify(mapper).expelApprovedSavingGroupMember(40L,20L);verifyNoMoreInteractions(mapper);
 }
 @Test void noLogin(){failure("UNAUTHORIZED",50L,null);verifyNoInteractions(mapper);}
 @Test void invalidTarget(){failure("BAD_REQUEST",0L,30L);}
 @Test void missingGroup(){failure("SAVING_GROUP_NOT_FOUND",50L,30L);}
 @ParameterizedTest @ValueSource(strings={"00","02","03","04","05"}) void inactiveGroup(String status){group(status,30L);failure("SAVING_GROUP_ACTIVITY_RESTRICTED",50L,30L);}
 @Test void otherGroupOwner(){group("01",99L);failure("SAVING_GROUP_OWNER_REQUIRED",50L,30L);}
 @Test void nonMember(){group("01",30L);failure("SAVING_GROUP_OWNER_REQUIRED",50L,30L);}
 @ParameterizedTest @ValueSource(strings={"MEMBER","MANAGER"}) void actorRole(String role){group("01",30L);when(mapper.selectSavingGroupMembershipForUpdate(20L,30L)).thenReturn(row(role,"APPROVED"));failure("SAVING_GROUP_OWNER_REQUIRED",50L,30L);}
 @ParameterizedTest @ValueSource(strings={"PENDING","REJECTED","LEFT","EXPELLED"}) void actorStatus(String status){group("01",30L);when(mapper.selectSavingGroupMembershipForUpdate(20L,30L)).thenReturn(row("OWNER",status));failure("SAVING_GROUP_OWNER_REQUIRED",50L,30L);}
 @Test void missingOrOtherGroupTarget(){owner();failure("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",50L,30L);}
 @Test void deletedAccount(){owner();when(mapper.selectSavingGroupMembershipForUpdate(20L,50L)).thenReturn(row("MEMBER","APPROVED"));failure("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",50L,30L);}
 @ParameterizedTest @ValueSource(strings={"PENDING","REJECTED","LEFT","EXPELLED"}) void targetStatus(String status){owner();when(mapper.selectSavingGroupMembershipForUpdate(20L,50L)).thenReturn(row("MEMBER",status));failure("SAVING_GROUP_APPROVED_MEMBER_NOT_FOUND",50L,30L);}
 @Test void self(){owner();when(mapper.existsMember(30L)).thenReturn(true);failure("SAVING_GROUP_OWNER_EXPULSION_NOT_ALLOWED",30L,30L);}
 @Test void otherOwner(){owner();target("OWNER");failure("SAVING_GROUP_OWNER_EXPULSION_NOT_ALLOWED",50L,30L);}
 @Test void invalidRole(){owner();target("UNKNOWN");failure("SAVING_GROUP_MEMBER_ROLE_INVALID",50L,30L);}
 @ParameterizedTest @ValueSource(ints={0,2}) void updateFailure(int count){owner();target("MANAGER");when(mapper.expelApprovedSavingGroupMember(40L,20L)).thenReturn(count);assertThatThrownBy(()->service.expelApprovedSavingGroupMember(20L,50L,30L)).isInstanceOfSatisfying(ApiException.class,e->assertThat(e.getCode()).isEqualTo("SAVING_GROUP_MEMBER_EXPULSION_FAILED"));}
}
