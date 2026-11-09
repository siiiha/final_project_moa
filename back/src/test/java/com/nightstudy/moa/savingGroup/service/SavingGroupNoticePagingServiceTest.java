package com.nightstudy.moa.savingGroup.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

// AI작성: 실제 DB 없이 공지 페이징 계산과 기존 공개 상세·회원 권한 정보 보존을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticePagingServiceTest {
    @Mock private SavingGroupMapper mapper;
    @InjectMocks private SavingGroupServiceImpl service;

    @ParameterizedTest
    @ValueSource(strings = {"LATEST", "OLDEST"})
    void returnsSecondPageAndMetaWithoutRequiringLogin(String sort) {
        target();
        when(mapper.selectSavingGroupNoticeCount(20L)).thenReturn(9);
        var notice = new SavingGroupNoticeSummaryResponse(40L, "공지 제목", "작성자", LocalDateTime.of(2026, 11, 4, 10, 0));
        when(mapper.selectSavingGroupNotices(20L, sort, 4)).thenReturn(List.of(notice));

        var result = service.getSavingGroupDetail(20L, null, condition(2, sort));

        assertThat(result.getDetail().getNotices()).containsExactly(notice);
        assertThat(result.getDetail().getGroupId()).isEqualTo(20L);
        assertThat(result.getDetail().getJoinRestrictionCode()).isEqualTo("UNAUTHORIZED");
        assertThat(result.getPageMeta().getPage()).isEqualTo(2);
        assertThat(result.getPageMeta().getSize()).isEqualTo(4);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(9);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(3);
        verify(mapper, never()).selectRecentNotices(any());
    }

    @Test
    void legacyCallUsesLatestFirstPage() {
        target();
        when(mapper.selectSavingGroupNoticeCount(20L)).thenReturn(1);
        service.getSavingGroupDetail(20L, null);
        verify(mapper).selectSavingGroupNotices(20L, "LATEST", 0);
    }

    @ParameterizedTest
    @CsvSource({"1,0,0", "4,9,3", "2147483647,9,3"})
    void emptyOrOutOfRangePageSkipsOffsetQuery(int page, int total, int totalPages) {
        target();
        when(mapper.selectSavingGroupNoticeCount(20L)).thenReturn(total);

        var result = service.getSavingGroupDetail(20L, null, condition(page, "LATEST"));

        assertThat(result.getDetail().getNotices()).isEmpty();
        assertThat(result.getPageMeta().getPage()).isEqualTo(page);
        assertThat(result.getPageMeta().getTotal()).isEqualTo(total);
        assertThat(result.getPageMeta().getTotalPages()).isEqualTo(totalPages);
        verify(mapper, never()).selectSavingGroupNotices(any(), any(), anyInt());
    }

    @ParameterizedTest
    @CsvSource({",LATEST", "0,LATEST", "-1,LATEST", "1,", "1,latest", "1,UNKNOWN"})
    void rejectsInvalidConditionsBeforeDatabaseAccess(Integer page, String sort) {
        failure(() -> service.getSavingGroupDetail(20L, null, condition(page, sort)), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsMissingCondition() {
        failure(() -> service.getSavingGroupDetail(20L, null, null), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidGroupId(Long groupId) {
        failure(() -> service.getSavingGroupDetail(groupId, null, condition(1, "LATEST")), HttpStatus.BAD_REQUEST);
        verifyNoInteractions(mapper);
    }

    @Test
    void missingGroupDoesNotQueryNotices() {
        failure(() -> service.getSavingGroupDetail(20L, null, condition(1, "LATEST")), HttpStatus.NOT_FOUND);
        verify(mapper).selectSavingGroupDetail(20L);
        verifyNoMoreInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MANAGER", "MEMBER"})
    void preservesApprovedMembershipAndRole(String role) {
        target();
        var relation = new SavingGroupMembershipRelationRow();
        relation.setMembershipStatus("APPROVED");
        relation.setMemberRole(role);
        when(mapper.selectApprovedMemberRoles(20L, 30L)).thenReturn(List.of(role));
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(relation);

        var detail = service.getSavingGroupDetail(20L, 30L, condition(1, "LATEST")).getDetail();

        assertThat(detail.getMyRole()).isEqualTo(role);
        assertThat(detail.getMembershipStatus()).isEqualTo("APPROVED");
        assertThat(detail.isCanApply()).isFalse();
        assertThat(detail.getJoinRestrictionCode()).isEqualTo("ALREADY_JOINED_SAVING_GROUP");
    }

    // AI작성: 미정의·중복 역할은 기존 상세와 동일하게 서버 데이터 오류로 처리한다.
    @Test
    void rejectsConflictingRoles() {
        target();
        when(mapper.selectApprovedMemberRoles(20L, 30L)).thenReturn(List.of("OWNER", "MEMBER"));
        failure(() -> service.getSavingGroupDetail(20L, 30L, condition(1, "LATEST")), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private void target() {
        var row = new SavingGroupRow();
        row.setGroupId(20L);
        row.setSavingGroupId(10L);
        row.setStatus("01");
        row.setMaxMembers(10);
        when(mapper.selectSavingGroupDetail(20L)).thenReturn(row);
    }

    private SavingGroupNoticeSearchCondition condition(Integer page, String sort) {
        var condition = new SavingGroupNoticeSearchCondition();
        condition.setNoticePage(page);
        condition.setNoticeSort(sort);
        return condition;
    }

    private void failure(Runnable action, HttpStatus status) {
        var error = catchThrowableOfType(action::run, ApiException.class);
        assertThat(error.getStatus()).isEqualTo(status);
    }
}
