package com.nightstudy.moa.smallGroup;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.*;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// AI작성: 현재 소모임장도 타인의 공지를 삭제하지 못하며 소속 공지만 논리 삭제한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticeDeleteServiceTest {
 @Mock SmallGroupMapper mapper; @InjectMocks SmallGroupServiceImpl service;
 private void owner(Long writer) {
  var t=new SmallGroupJoinTargetRow();t.setGroupId(20L);t.setCreatorMemberId(3L);
  when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(t);
  var r=new SmallGroupMemberRelationRow();r.setMemberRole("OWNER");r.setMembershipStatus("APPROVED");
  when(mapper.selectSmallGroupMembershipForUpdate(20L,3L)).thenReturn(r);
  when(mapper.selectSmallGroupNoticeForUpdate(20L,9L)).thenReturn(new SmallGroupNoticeDetailResponse(9L,"제목","본문",writer,"작성자",null));
 }
 @Test void forbidsOtherAuthor() {
  owner(4L); assertThatThrownBy(() -> service.deleteSmallGroupNotice(10L,9L,3L)).isInstanceOf(ApiException.class);
  verify(mapper,never()).deleteSmallGroupNotice(anyLong(),anyLong(),anyLong());
 }
 @Test void deletesOnlyOwnNoticeUsingGroupId() {
  owner(3L); when(mapper.deleteSmallGroupNotice(20L,9L,3L)).thenReturn(1);
  service.deleteSmallGroupNotice(10L,9L,3L); verify(mapper).deleteSmallGroupNotice(20L,9L,3L);
 }
}
