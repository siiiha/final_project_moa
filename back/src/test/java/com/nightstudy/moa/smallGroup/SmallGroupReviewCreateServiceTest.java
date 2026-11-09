package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.smallGroup.dto.SmallGroupMemberRelationRow;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateCommand;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateRequest;
import com.nightstudy.moa.smallGroup.dto.SmallGroupReviewCreateTargetRow;
import com.nightstudy.moa.smallGroup.mapper.SmallGroupMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

// AI작성: 후기 등록의 권한·날짜·입력 경계·중복·저장 실패를 실제 DB 없이 검증한다.
@ExtendWith(MockitoExtension.class)
class SmallGroupReviewCreateServiceTest {
    private static final long SMALL_GROUP_ID = 10L;
    private static final long GROUP_ID = 20L;
    private static final long OWNER_ID = 30L;
    private static final long MEMBER_ID = 40L;
    private static final long REVIEW_ID = 50L;
    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private SmallGroupMapper mapper;

    @InjectMocks
    private SmallGroupServiceImpl service;

    @ParameterizedTest
    @CsvSource({"0, 1", "0.5, 30", "4.5, 10", "5, 30"})
    void registersApprovedMemberReviewAndReturnsGeneratedId(String rating, int contentLength) {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).minusDays(1));
        SmallGroupReviewCreateRequest request = request(rating, "가".repeat(contentLength));
        when(mapper.insertSmallGroupReview(any())).thenAnswer(invocation -> {
            SmallGroupReviewCreateCommand command = invocation.getArgument(0);
            command.setSmallGroupReviewId(REVIEW_ID);
            return 1;
        });

        assertThat(service.createSmallGroupReview(SMALL_GROUP_ID, MEMBER_ID, request)).isEqualTo(REVIEW_ID);

        ArgumentCaptor<SmallGroupReviewCreateCommand> captor =
                ArgumentCaptor.forClass(SmallGroupReviewCreateCommand.class);
        verify(mapper).insertSmallGroupReview(captor.capture());
        SmallGroupReviewCreateCommand saved = captor.getValue();
        assertThat(saved.getSmallGroupId()).isEqualTo(SMALL_GROUP_ID);
        assertThat(saved.getWriterId()).isEqualTo(MEMBER_ID);
        assertThat(saved.getRating()).isEqualByComparingTo(rating);
        assertThat(saved.getContent()).isEqualTo(request.getContent());
        verify(mapper).selectSmallGroupReviewId(SMALL_GROUP_ID, MEMBER_ID);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsUnauthenticatedMember(Long memberId) {
        assertFailure(SMALL_GROUP_ID, memberId, validRequest(), HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void rejectsInvalidSmallGroupId(Long smallGroupId) {
        assertFailure(smallGroupId, MEMBER_ID, validRequest(), HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsMissingSmallGroup() {
        assertFailure(HttpStatus.NOT_FOUND, "SMALL_GROUP_NOT_FOUND");
        verify(mapper, never()).selectSmallGroupMembershipForUpdate(any(), any());
    }

    @Test
    void rejectsCreatorEvenWhenMembershipWouldBeMember() {
        when(mapper.selectSmallGroupReviewCreateTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(target(LocalDate.now(KOREA_ZONE).minusDays(2)));
        assertFailure(SMALL_GROUP_ID, OWNER_ID, validRequest(), HttpStatus.FORBIDDEN,
                "SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED");
        // AI작성: 개설자는 회원 역할 데이터와 관계없이 차단되어 관계 조회에 도달하지 않는다.
        verify(mapper, never()).selectSmallGroupMembershipForUpdate(any(), any());
    }

    @Test
    void rejectsNonMember() {
        when(mapper.selectSmallGroupReviewCreateTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(target(LocalDate.now(KOREA_ZONE).minusDays(2)));
        assertFailure(HttpStatus.FORBIDDEN, "SMALL_GROUP_REVIEW_CREATE_FORBIDDEN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void rejectsMemberWithoutCurrentApproval(String status) {
        mockRelation("MEMBER", status);
        assertFailure(HttpStatus.FORBIDDEN, "SMALL_GROUP_REVIEW_CREATE_FORBIDDEN");
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "PENDING", "REJECTED", "LEFT", "EXPELLED"})
    void rejectsManagerRegardlessOfMembershipStatus(String status) {
        mockRelation("MANAGER", status);
        assertFailure(HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }

    @Test
    void rejectsOwnerRelationEvenWhenCreatorIdDoesNotMatch() {
        mockRelation("OWNER", "APPROVED");
        assertFailure(HttpStatus.FORBIDDEN, "SMALL_GROUP_REVIEW_CREATE_FORBIDDEN");
    }

    @Test
    void rejectsMissingMeetDate() {
        mockApprovedMember(null);
        assertFailure(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_MEET_DATE_REQUIRED");
        verify(mapper, never()).selectSmallGroupReviewId(any(), any());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void rejectsMeetingTodayOrInFuture(int daysFromToday) {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).plusDays(daysFromToday));
        assertFailure(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_NOT_AVAILABLE_YET");
        verify(mapper, never()).selectSmallGroupReviewId(any(), any());
    }

    @Test
    void rejectsExistingReview() {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).minusDays(2));
        when(mapper.selectSmallGroupReviewId(SMALL_GROUP_ID, MEMBER_ID)).thenReturn(REVIEW_ID);
        assertFailure(HttpStatus.CONFLICT, "SMALL_GROUP_REVIEW_ALREADY_EXISTS");
    }

    @Test
    void translatesDatabaseDuplicateToSameConflict() {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).minusDays(2));
        when(mapper.insertSmallGroupReview(any())).thenThrow(new DuplicateKeyException("duplicate"));
        ApiException exception = catchThrowableOfType(
                () -> service.createSmallGroupReview(SMALL_GROUP_ID, MEMBER_ID, validRequest()), ApiException.class);
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_REVIEW_ALREADY_EXISTS");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.5", "5.5", "0.1", "4.99"})
    void rejectsMissingOutOfRangeOrNonHalfPointRating(String rating) {
        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request(rating, "즐거웠어요"),
                HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "가나다라마바가나다라마바가나다라마바가나다라마바가나다라마바가"})
    void rejectsMissingBlankOrOverlongContent(String content) {
        assertFailure(SMALL_GROUP_ID, MEMBER_ID, request("4.5", content),
                HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsMissingRequest() {
        assertFailure(SMALL_GROUP_ID, MEMBER_ID, null, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 2})
    void rejectsUnexpectedInsertCount(int insertedRows) {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).minusDays(2));
        when(mapper.insertSmallGroupReview(any())).thenReturn(insertedRows);
        assertSaveFailure();
    }

    @Test
    void rejectsMissingGeneratedKey() {
        mockApprovedMember(LocalDate.now(KOREA_ZONE).minusDays(2));
        when(mapper.insertSmallGroupReview(any())).thenReturn(1);
        assertSaveFailure();
    }

    private void assertSaveFailure() {
        ApiException exception = catchThrowableOfType(
                () -> service.createSmallGroupReview(SMALL_GROUP_ID, MEMBER_ID, validRequest()), ApiException.class);
        assertThat(exception.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(exception.getCode()).isEqualTo("SMALL_GROUP_REVIEW_CREATE_FAILED");
    }

    // AI작성: 검증 실패 때 오류 코드뿐 아니라 저장 SQL에 도달하지 않는지도 확인한다.
    private void assertFailure(HttpStatus status, String code) {
        assertFailure(SMALL_GROUP_ID, MEMBER_ID, validRequest(), status, code);
    }

    private void assertFailure(Long smallGroupId, Long memberId, SmallGroupReviewCreateRequest request,
            HttpStatus status, String code) {
        ApiException exception = catchThrowableOfType(
                () -> service.createSmallGroupReview(smallGroupId, memberId, request), ApiException.class);
        assertThat(exception.getStatus()).isEqualTo(status);
        assertThat(exception.getCode()).isEqualTo(code);
        verify(mapper, never()).insertSmallGroupReview(any());
    }

    private void mockRelation(String role, String status) {
        when(mapper.selectSmallGroupReviewCreateTargetForUpdate(SMALL_GROUP_ID))
                .thenReturn(target(LocalDate.now(KOREA_ZONE).minusDays(2)));
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole(role);
        relation.setMembershipStatus(status);
        when(mapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID)).thenReturn(relation);
    }

    private void mockApprovedMember(LocalDate meetDate) {
        when(mapper.selectSmallGroupReviewCreateTargetForUpdate(SMALL_GROUP_ID)).thenReturn(target(meetDate));
        SmallGroupMemberRelationRow relation = new SmallGroupMemberRelationRow();
        relation.setMemberRole("MEMBER");
        relation.setMembershipStatus("APPROVED");
        when(mapper.selectSmallGroupMembershipForUpdate(GROUP_ID, MEMBER_ID)).thenReturn(relation);
    }

    private SmallGroupReviewCreateTargetRow target(LocalDate meetDate) {
        SmallGroupReviewCreateTargetRow target = new SmallGroupReviewCreateTargetRow();
        target.setSmallGroupId(SMALL_GROUP_ID);
        target.setGroupId(GROUP_ID);
        target.setCreatorMemberId(OWNER_ID);
        target.setMeetDate(meetDate);
        return target;
    }

    private SmallGroupReviewCreateRequest validRequest() {
        return request("4.5", "즐거웠어요");
    }

    private SmallGroupReviewCreateRequest request(String rating, String content) {
        SmallGroupReviewCreateRequest request = new SmallGroupReviewCreateRequest();
        request.setRating(rating == null ? null : new BigDecimal(rating));
        request.setContent(content);
        return request;
    }
}
