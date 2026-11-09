package com.nightstudy.moa.smallGroup;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.nightstudy.moa.smallGroup.dto.*;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
// AI작성: 과도한 페이지 요청이 안전하게 마지막 페이지로 보정되는지 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticeListServiceTest {
 @Mock SmallGroupMapper mapper; @InjectMocks SmallGroupServiceImpl service;
 @Test void clampsPageBeforeOffsetCalculation() {
  var c=new SmallGroupNoticeSearchCondition();c.setNoticePage(Integer.MAX_VALUE);c.setNoticeSort("OLDEST");
  when(mapper.countSmallGroupNotices(20L)).thenReturn(5);
  var r=service.getSmallGroupNotices(20L,c);
  assertThat(r.getPageMeta().getPage()).isEqualTo(2);
  verify(mapper).selectSmallGroupNoticePage(20L,4,"OLDEST");
 }
 @Test void emptyListUsesFirstPage() {
  var r=service.getSmallGroupNotices(20L,new SmallGroupNoticeSearchCondition());
  assertThat(r.getPageMeta().getTotalPages()).isZero();
  assertThat(r.getPageMeta().getPage()).isEqualTo(1);
 }
}
