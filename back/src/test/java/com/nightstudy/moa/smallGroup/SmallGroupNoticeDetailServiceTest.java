package com.nightstudy.moa.smallGroup;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.*;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// AI작성: 공개 요약과 달리 본문은 승인 회원 전용이며 타 모임 공지는 404 처리한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticeDetailServiceTest {
 @Mock SmallGroupMapper mapper; @Spy @InjectMocks SmallGroupServiceImpl service;
 private void member(String role, String status) {
  var group=mock(SmallGroupDetailResponse.class);
  doReturn(group).when(service).getSmallGroupDetail(10L,3L);
  when(group.getMembershipStatus()).thenReturn(status);
  if ("APPROVED".equals(status)) when(group.getMyRole()).thenReturn(role);
  if ("APPROVED".equals(status) && ("OWNER".equals(role)||"MEMBER".equals(role))) when(group.getGroupId()).thenReturn(20L);
 }
 @Test void anonymousCannotReadBody() {
  assertThatThrownBy(() -> service.getSmallGroupNoticeDetail(10L,9L,null)).isInstanceOf(ApiException.class);
  verifyNoInteractions(mapper);
 }
 @Test void pendingMemberCannotReadBody() {
  member(null,"PENDING");assertThatThrownBy(() -> service.getSmallGroupNoticeDetail(10L,9L,3L)).isInstanceOf(ApiException.class);
  verifyNoInteractions(mapper);
 }
 @Test void readsOnlyMatchingGroupNotice() {
  member("MEMBER","APPROVED");var notice=new SmallGroupNoticeDetailResponse(9L,"제목","본문",4L,"작성자",null);
  when(mapper.selectSmallGroupNoticeDetail(20L,9L)).thenReturn(notice);
  assertThat(service.getSmallGroupNoticeDetail(10L,9L,3L)).isSameAs(notice);
 }
 @Test void foreignOrDeletedNoticeIsNotFound() {
  member("OWNER","APPROVED");
  assertThatThrownBy(() -> service.getSmallGroupNoticeDetail(10L,9L,3L)).isInstanceOf(ApiException.class).hasMessageContaining("찾을 수 없습니다");
 }
}
