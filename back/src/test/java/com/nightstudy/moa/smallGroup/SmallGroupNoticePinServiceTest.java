package com.nightstudy.moa.smallGroup;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.*;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// AI작성: 두 개 제한·동일 요청·고정 해제 경계를 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticePinServiceTest {
 @Mock SmallGroupMapper mapper; @InjectMocks SmallGroupServiceImpl service;
 private void owner(boolean pinned) {
  var t=new SmallGroupJoinTargetRow();t.setGroupId(20L);t.setCreatorMemberId(3L);
  when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(t);
  var r=new SmallGroupMemberRelationRow();r.setMemberRole("OWNER");r.setMembershipStatus("APPROVED");
  when(mapper.selectSmallGroupMembershipForUpdate(20L,3L)).thenReturn(r);
  var n=new SmallGroupNoticePinRow();n.setNoticeId(9L);n.setPinned(pinned);
  when(mapper.selectSmallGroupNoticePinForUpdate(20L,9L)).thenReturn(n);
 }
 @Test void rejectsThirdPin() {
  owner(false);when(mapper.selectSmallGroupPinnedNoticeIdsForUpdate(20L)).thenReturn(List.of(7L,8L));
  assertThatThrownBy(() -> service.updateSmallGroupNoticePinned(10L,9L,3L,true)).isInstanceOf(ApiException.class).hasMessageContaining("최대 2개");
  verify(mapper,never()).updateSmallGroupNoticePinned(anyLong(),anyLong(),anyBoolean());
 }
 @Test void samePinIsIdempotent() {
  owner(true);service.updateSmallGroupNoticePinned(10L,9L,3L,true);
  verify(mapper,never()).selectSmallGroupPinnedNoticeIdsForUpdate(anyLong());
  verify(mapper,never()).updateSmallGroupNoticePinned(anyLong(),anyLong(),anyBoolean());
 }
 @Test void unpinDoesNotCheckLimit() {
  owner(true);when(mapper.updateSmallGroupNoticePinned(20L,9L,false)).thenReturn(1);
  service.updateSmallGroupNoticePinned(10L,9L,3L,false);
  verify(mapper,never()).selectSmallGroupPinnedNoticeIdsForUpdate(anyLong());
 }
 @Test void pinsSecondNotice() {
  owner(false);when(mapper.selectSmallGroupPinnedNoticeIdsForUpdate(20L)).thenReturn(List.of(7L));
  when(mapper.updateSmallGroupNoticePinned(20L,9L,true)).thenReturn(1);
  service.updateSmallGroupNoticePinned(10L,9L,3L,true);
 }
}
