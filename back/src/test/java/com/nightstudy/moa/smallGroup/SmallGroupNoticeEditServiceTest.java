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
// AI작성: 현재 소모임장도 타인의 공지를 수정하지 못하며 동일 내용은 재저장하지 않는다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticeEditServiceTest {
 @Mock SmallGroupMapper mapper; @InjectMocks SmallGroupServiceImpl service;
 private void owner(Long writer) {
  var t=new SmallGroupJoinTargetRow();t.setGroupId(20L);t.setCreatorMemberId(3L);
  when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(t);
  var r=new SmallGroupMemberRelationRow();r.setMemberRole("OWNER");r.setMembershipStatus("APPROVED");
  when(mapper.selectSmallGroupMembershipForUpdate(20L,3L)).thenReturn(r);
  when(mapper.selectSmallGroupNoticeForUpdate(20L,9L)).thenReturn(new SmallGroupNoticeDetailResponse(9L,"제목","본문",writer,"작성자",null));
 }
 @Test void forbidsOtherAuthor() {
  owner(4L); assertThatThrownBy(() -> service.updateSmallGroupNotice(10L,9L,3L,"변경","내용")).isInstanceOf(ApiException.class);
  verify(mapper,never()).updateSmallGroupNotice(anyLong(),anyLong(),anyLong(),anyString(),anyString());
 }
 @Test void sameTextIsSuccessfulWithoutUpdate() {
  owner(3L);assertThat(service.updateSmallGroupNotice(10L,9L,3L," 제목 "," 본문 ")).isEqualTo(9L);
  verify(mapper,never()).updateSmallGroupNotice(anyLong(),anyLong(),anyLong(),anyString(),anyString());
 }
 @Test void updatesOnlyOwnNoticeUsingGroupId() {
  owner(3L);when(mapper.updateSmallGroupNotice(20L,9L,3L,"변경","내용")).thenReturn(1);
  service.updateSmallGroupNotice(10L,9L,3L,"변경","내용");verify(mapper).updateSmallGroupNotice(20L,9L,3L,"변경","내용");
 }
}
