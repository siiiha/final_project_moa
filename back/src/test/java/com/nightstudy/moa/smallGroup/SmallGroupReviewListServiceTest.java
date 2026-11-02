package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupDetailRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListCondition;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListItemResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewListResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewSearchCondition;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

// AI작성: 공개 후기 조회의 접근 정책·페이지 경계·조회 조건 전달을 실제 DB 없이 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupReviewListServiceTest {
    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long MEMBER_ID = 40L;

    @Mock
    private SmallGroupMapper mapper;

    @InjectMocks
    private SmallGroupServiceImpl service;

    @ParameterizedTest
    @CsvSource({"1, 0, 8", "2, 8, 8", "3, 16, 1"})
    void returnsRequestedPageAndPassesSameGroupWithFixedSize(int page, int offset, int count) {
        mockTarget();
        SmallGroupReviewListItemResponse item = new SmallGroupReviewListItemResponse();
        item.setSmallGroupReviewId(50L);
        item.setWriterNickname("모아회원");
        item.setContent("즐거운 모임이었어요.");
        item.setRating(new BigDecimal("4.5"));
        item.setCreatedAt(LocalDateTime.of(2026, 11, 1, 12, 0));
        List<SmallGroupReviewListItemResponse> items = java.util.Collections.nCopies(count, item);
        when(mapper.selectSmallGroupReviewCount(any())).thenAnswer(invocation -> {
            SmallGroupReviewListCondition condition = invocation.getArgument(0);
            assertThat(condition.getSmallGroupId()).isEqualTo(SMALL_GROUP_ID);
            assertThat(condition.getOffset()).isZero();
            assertThat(condition.getLimit()).isEqualTo(8);
            return 17;
        });
        when(mapper.selectSmallGroupReviewList(any())).thenReturn(items);

        SmallGroupReviewListResponse response = service.getSmallGroupReviews(SMALL_GROUP_ID, null, request(page));

        assertThat(response.getItems()).containsExactlyElementsOf(items);
        assertThat(response.getPageMeta().getPage()).isEqualTo(page);
        assertThat(response.getPageMeta().getSize()).isEqualTo(8);
        assertThat(response.getPageMeta().getTotal()).isEqualTo(17);
        assertThat(response.getPageMeta().getTotalPages()).isEqualTo(3);
        ArgumentCaptor<SmallGroupReviewListCondition> captor =
                ArgumentCaptor.forClass(SmallGroupReviewListCondition.class);
        verify(mapper).selectSmallGroupReviewList(captor.capture());
        assertThat(captor.getValue().getSmallGroupId()).isEqualTo(SMALL_GROUP_ID);
        assertThat(captor.getValue().getOffset()).isEqualTo(offset);
        assertThat(captor.getValue().getLimit()).isEqualTo(8);
        verify(mapper, never()).selectSmallGroupMemberRelations(any(), any());
    }

    @ParameterizedTest
    @CsvSource({"1, 0, 0", "4, 17, 3", "2147483647, 17, 3"})
    void returnsEmptyWithoutListQueryForEmptyOrOutOfRangePage(int page, int total, int totalPages) {
        mockTarget();
        when(mapper.selectSmallGroupReviewCount(any())).thenReturn(total);

        SmallGroupReviewListResponse response = service.getSmallGroupReviews(SMALL_GROUP_ID, null, request(page));

        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPageMeta().getPage()).isEqualTo(page);
        assertThat(response.getPageMeta().getSize()).isEqualTo(8);
        assertThat(response.getPageMeta().getTotal()).isEqualTo(total);
        assertThat(response.getPageMeta().getTotalPages()).isEqualTo(totalPages);
        verify(mapper, never()).selectSmallGroupReviewList(any());
    }

    @Test
    void allowsLoggedInNonMember() {
        mockTarget();
        when(mapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID)).thenReturn(List.of());

        assertThat(service.getSmallGroupReviews(SMALL_GROUP_ID, MEMBER_ID, request(1)).getItems()).isEmpty();
        verify(mapper).selectSmallGroupReviewCount(any());
    }

    @ParameterizedTest
    @CsvSource({"OWNER, APPROVED", "MEMBER, APPROVED", "MEMBER, PENDING", "MEMBER, REJECTED",
            "MEMBER, LEFT", "MEMBER, EXPELLED"})
    void allowsOwnerAndMembersRegardlessOfApproval(String role, String status) {
        mockTarget();
        when(mapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation(role, status)));

        assertThat(service.getSmallGroupReviews(SMALL_GROUP_ID, MEMBER_ID, request(1)).getItems()).isEmpty();
        verify(mapper).selectSmallGroupReviewCount(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void rejectsManagerRegardlessOfStatus(String status) {
        mockTarget();
        when(mapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("MANAGER", status)));

        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request(1), HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        verifyNoReviewQueries();
    }

    @Test
    void rejectsDuplicateRelationsUsingExistingDetailPolicy() {
        mockTarget();
        when(mapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("MEMBER", "APPROVED"), relation("MEMBER", "LEFT")));

        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request(1), HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        verifyNoReviewQueries();
    }

    @Test
    void rejectsUnknownRoleUsingExistingDetailPolicy() {
        mockTarget();
        when(mapper.selectSmallGroupMemberRelations(GROUP_ID, MEMBER_ID))
                .thenReturn(List.of(relation("UNKNOWN", "APPROVED")));

        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request(1), HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        verifyNoReviewQueries();
    }

    @Test
    void rejectsMissingOrDeletedTargetExcludedByMapper() {
        // AI작성: 삭제된 모임·개설자 제외는 기존 SQL 책임이므로 Service에서는 조회 결과가 없는 경우를 검증한다.
        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request(1), HttpStatus.NOT_FOUND, "NOT_FOUND");
        verify(mapper, never()).selectSmallGroupMemberRelations(any(), any());
        verifyNoReviewQueries();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidSmallGroupId(Long smallGroupId) {
        assertFailure(smallGroupId, null, request(1), HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsInvalidPage(Integer page) {
        assertFailure(SMALL_GROUP_ID, null, request(page), HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsMissingRequest() {
        assertFailure(SMALL_GROUP_ID, null, null, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsInvalidAuthenticatedMemberId(Long memberId) {
        assertFailure(SMALL_GROUP_ID, memberId, request(1), HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }

    // AI작성: Mock 대상은 공개 상세와 같은 공통 모임 ID를 제공하고 모임 날짜 조건은 설정하지 않는다.
    private void mockTarget() {
        SmallGroupDetailRow target = new SmallGroupDetailRow();
        target.setSmallGroupId(SMALL_GROUP_ID);
        target.setGroupId(GROUP_ID);
        when(mapper.selectSmallGroupDetail(SMALL_GROUP_ID)).thenReturn(target);
    }

    private SmallGroupReviewSearchCondition request(Integer page) {
        SmallGroupReviewSearchCondition request = new SmallGroupReviewSearchCondition();
        request.setPage(page);
        return request;
    }

    private SmallGroupMemberRelationRow relation(String role, String status) {
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        return relation;
    }

    private void assertFailure(Long smallGroupId, Long memberId, SmallGroupReviewSearchCondition request,
            HttpStatus status, String code) {
        ApiException exception = catchThrowableOfType(
                () -> service.getSmallGroupReviews(smallGroupId, memberId, request), ApiException.class);
        assertThat(exception).isNotNull();
        assertThat(exception.getStatus()).isEqualTo(status);
        assertThat(exception.getCode()).isEqualTo(code);
    }

    private void verifyNoReviewQueries() {
        verify(mapper, never()).selectSmallGroupReviewCount(any());
        verify(mapper, never()).selectSmallGroupReviewList(any());
    }
}
