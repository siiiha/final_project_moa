package com.nightstudy.moa.savingGroup;

import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// AI작성: DB 연결 없이 응답 조합, 경계 페이지와 고정 조회 횟수를 검증한다.
class SavingGroupServiceTest {
    private final SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    private final SavingGroupServiceImpl service = new SavingGroupServiceImpl(mapper);

    // AI작성: 승인 모임장을 다시 더하지 않고 집계 없는 모임도 유지한다.
    @Test
    void combinesCountsWithoutAddingOwnerOrDroppingEmptyGroup() {
        var request = new SavingGroupSearchCondition();
        request.setKeyword("  !%_  ");
        request.setRegionCode("01");
        var first = row(11L, 1L);
        var second = row(12L, 2L);
        var count = new SavingGroupMemberCount();
        count.setGroupId(1L);
        count.setMemberCount(3);
        when(mapper.selectSavingGroupCount(any())).thenReturn(7L);
        when(mapper.selectSavingGroupList(any())).thenReturn(List.of(first, second));
        when(mapper.selectApprovedMemberCounts(List.of(1L, 2L))).thenReturn(List.of(count));

        var result = service.getSavingGroupList(request);

        assertEquals(7, result.getTotalElements());
        assertEquals(2, result.getTotalPages());
        assertEquals(6, result.getPageSize());
        assertEquals(3, result.getItems().get(0).getMemberCount());
        assertEquals(0, result.getItems().get(1).getMemberCount());
        assertEquals(new BigDecimal("50000.00"), result.getItems().get(0).getDuesAmount());
        var capture = ArgumentCaptor.forClass(SavingGroupListCondition.class);
        verify(mapper).selectSavingGroupList(capture.capture());
        assertEquals("%!!!%!_%", capture.getValue().getKeywordPattern());
        assertEquals("서울", capture.getValue().getRegionName());
        verify(mapper).selectSavingGroupCount(same(capture.getValue()));
        verify(mapper).selectApprovedMemberCounts(List.of(1L, 2L));
        verifyNoMoreInteractions(mapper);
    }

    // AI작성: 0건과 범위 초과에서는 불필요한 목록·집계 쿼리를 생략한다.
    @Test
    void returnsEmptyForNoResultsAndOutOfRangeWithoutClampingPage() {
        var request = new SavingGroupSearchCondition();
        var empty = service.getSavingGroupList(request);
        assertTrue(empty.getItems().isEmpty());
        assertEquals(0, empty.getTotalPages());
        request.setPage(Integer.MAX_VALUE);
        when(mapper.selectSavingGroupCount(any())).thenReturn(7L);
        var overflow = service.getSavingGroupList(request);
        assertEquals(Integer.MAX_VALUE, overflow.getCurrentPage());
        assertEquals(2, overflow.getTotalPages());
        var capture = ArgumentCaptor.forClass(SavingGroupListCondition.class);
        verify(mapper, times(2)).selectSavingGroupCount(capture.capture());
        assertEquals((Integer.MAX_VALUE - 1L) * 6, capture.getValue().getOffset());
        verifyNoMoreInteractions(mapper);
    }

    // AI작성: 마지막 페이지와 공백 검색은 서버에서 조건 없이 처리한다.
    @Test
    void readsLastPageAndSkipsAggregationWhenConcurrentRemovalLeavesItEmpty() {
        var request = new SavingGroupSearchCondition();
        request.setKeyword("   ");
        request.setPage(2);
        when(mapper.selectSavingGroupCount(any())).thenReturn(7L);
        when(mapper.selectSavingGroupList(any())).thenReturn(List.of());
        var result = service.getSavingGroupList(request);
        assertTrue(result.getItems().isEmpty());
        var capture = ArgumentCaptor.forClass(SavingGroupListCondition.class);
        verify(mapper).selectSavingGroupList(capture.capture());
        assertNull(capture.getValue().getKeywordPattern());
        assertEquals(6, capture.getValue().getOffset());
        verify(mapper, never()).selectApprovedMemberCounts(anyList());
    }

    // AI작성: SQL 장애를 빈 목록으로 위장하지 않고 전역 예외 처리로 전달한다.
    @Test
    void propagatesDatabaseFailure() {
        when(mapper.selectSavingGroupCount(any())).thenThrow(new IllegalStateException("test failure"));
        assertThrows(IllegalStateException.class, () -> service.getSavingGroupList(new SavingGroupSearchCondition()));
    }

    // AI작성: 개인정보 없는 카드 조회 결과를 구성한다.
    private SavingGroupRow row(Long id, Long groupId) {
        var row = new SavingGroupRow();
        row.setSavingGroupId(id);
        row.setGroupId(groupId);
        row.setGroupName("테스트 모임");
        row.setRegionName("서울");
        row.setMaxMembers(8);
        row.setDuesAmount(new BigDecimal("50000.00"));
        row.setDuesDay(25);
        row.setStatus("01");
        return row;
    }
}
