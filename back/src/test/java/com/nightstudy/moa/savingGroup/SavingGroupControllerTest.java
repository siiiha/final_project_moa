package com.nightstudy.moa.savingGroup;

import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.controller.SavingGroupController;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// AI작성: 애플리케이션 DB 설정을 읽지 않고 공개 요청과 실제 Validation/공통 예외를 검증한다.
class SavingGroupControllerTest {
    private SavingGroupService service;
    private MockMvc mvc;

    // AI작성: 실제 Validator와 전역 예외 처리기를 Controller에 연결한다.
    @BeforeEach
    void setUp() {
        service = mock(SavingGroupService.class);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).setValidator(validator).build();
        when(service.getSavingGroupList(any())).thenReturn(new SavingGroupListResponse(List.of(), 0, 0, 1, 6));
    }

    // AI작성: 토큰 없이 두 공개 경로에서 기존 공통 응답을 반환한다.
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void publicList(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isEmpty())
                .andExpect(jsonPath("$.data.totalPages").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(6));
    }

    // AI작성: 공백 제거 후 20자는 허용하고 지역·페이지를 그대로 전달한다.
    @Test
    void normalizesBeforeValidation() throws Exception {
        String keyword = "가".repeat(20);
        mvc.perform(get("/saving-groups").param("keyword", "  " + keyword + "  ")
                .param("regionCode", "10").param("page", "2")).andExpect(status().isOk());
        var capture = ArgumentCaptor.forClass(SavingGroupSearchCondition.class);
        verify(service).getSavingGroupList(capture.capture());
        assertEquals(keyword, capture.getValue().getKeyword());
        assertEquals("10", capture.getValue().getRegionCode());
        assertEquals(2, capture.getValue().getPage());
    }

    // AI작성: 잘못된 입력은 Service 호출 전에 BAD_REQUEST로 반환한다.
    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectsInvalidInput(String field, String value) throws Exception {
        mvc.perform(get("/saving-groups").param(field, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    // AI작성: 숫자 바인딩 실패와 Validation 경계를 함께 확인한다.
    static Stream<Arguments> invalidInputs() {
        return Stream.of(Arguments.of("page", "0"), Arguments.of("page", "-1"),
                Arguments.of("page", "abc"), Arguments.of("page", "1.5"),
                Arguments.of("page", "2147483648"), Arguments.of("page", ""),
                Arguments.of("regionCode", "00"), Arguments.of("regionCode", "11"),
                Arguments.of("regionCode", "1"), Arguments.of("regionCode", ""),
                Arguments.of("keyword", "가".repeat(21)));
    }

    // AI작성: 서버 오류 상세를 노출하지 않고 기존 INTERNAL_ERROR를 사용한다.
    @Test
    void hidesUnexpectedFailure() throws Exception {
        when(service.getSavingGroupList(any())).thenThrow(new IllegalStateException("internal detail"));
        mvc.perform(get("/saving-groups")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
    }

    // AI작성: 공개 상세는 토큰이나 클라이언트가 주장한 사용자 정보 없이 서비스에 전달한다.
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/saving-groups/42", "/api/saving-groups/42"})
    void publicDetail(String path) throws Exception {
        when(service.getSavingGroupDetail(42L, null)).thenReturn(new SavingGroupDetailResponse(
                42L, "테스트 모임", null, "서울", 8, 1L, null, null, "01", List.of(), null));
        mvc.perform(get(path).param("memberId", "99").param("myRole", "OWNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupId").value(42))
                .andExpect(jsonPath("$.data.memberCount").value(1))
                .andExpect(jsonPath("$.data.notices").isEmpty())
                .andExpect(jsonPath("$.data.myRole").isEmpty());
        verify(service).getSavingGroupDetail(42L, null);
    }

    // AI작성: 잘못된 ID는 DB 조회 전 400으로 거부하며 Long의 상한도 검사한다.
    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"abc", "0", "-1", "1.5", "9223372036854775808"})
    void rejectsInvalidDetailId(String id) throws Exception {
        mvc.perform(get("/saving-groups/" + id)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    // AI작성: 조회 불가와 서버 장애는 기존 예외 봉투를 사용한다.
    @Test
    void detailFailures() throws Exception {
        when(service.getSavingGroupDetail(42L, null)).thenThrow(new com.nightstudy.moa.common.exception.ApiException(
                org.springframework.http.HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다."));
        mvc.perform(get("/saving-groups/42")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        doThrow(new IllegalStateException("private database detail")).when(service).getSavingGroupDetail(42L, null);
        mvc.perform(get("/saving-groups/42")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."));
    }
}
