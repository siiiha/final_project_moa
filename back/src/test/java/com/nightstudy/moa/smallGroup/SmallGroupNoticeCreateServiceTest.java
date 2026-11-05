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
// AI작성: 모임 식별자 변환과 승인 소모임장만 저장할 수 있는 경계를 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupNoticeCreateServiceTest {
    @Mock SmallGroupMapper mapper;
    @InjectMocks SmallGroupServiceImpl service;
    private void owner() {
        var target = new SmallGroupJoinTargetRow(); target.setGroupId(20L); target.setCreatorMemberId(3L);
        when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(target);
        var relation = new SmallGroupMemberRelationRow(); relation.setMemberRole("OWNER"); relation.setMembershipStatus("APPROVED");
        when(mapper.selectSmallGroupMembershipForUpdate(20L,3L)).thenReturn(relation);
    }
    @Test void savesUsingCommonGroupIdAndAuthenticatedWriter() {
        owner(); when(mapper.insertSmallGroupNotice(any())).thenAnswer(call -> {
            SmallGroupNoticeCreateRow row=call.getArgument(0);
            assertThat(row.getGroupId()).isEqualTo(20L); assertThat(row.getWriterId()).isEqualTo(3L);
            assertThat(row.getTitle()).isEqualTo("제목"); row.setNoticeId(9L); return 1;
        }); service.createSmallGroupNotice(10L,3L," 제목 "," 본문 ");
    }
    @Test void rejectsAnonymous() {
        assertThatThrownBy(() -> service.createSmallGroupNotice(10L,null,"제목","본문")).isInstanceOf(ApiException.class);
        verifyNoInteractions(mapper);
    }
    @Test void rejectsMember() {
        var target=new SmallGroupJoinTargetRow(); target.setGroupId(20L); target.setCreatorMemberId(4L);
        when(mapper.selectSmallGroupJoinTargetForUpdate(10L)).thenReturn(target);
        assertThatThrownBy(() -> service.createSmallGroupNotice(10L,3L,"제목","본문")).isInstanceOf(ApiException.class);
        verify(mapper,never()).insertSmallGroupNotice(any());
    }
    @Test void rejectsBlankText() {
        owner(); assertThatThrownBy(() -> service.createSmallGroupNotice(10L,3L," ","본문")).isInstanceOf(ApiException.class);
        verify(mapper,never()).insertSmallGroupNotice(any());
    }
}
