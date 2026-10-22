package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import com.nightstudy.moa.savingGroup.service.SavingGroupServiceImpl;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 인증 완료 요청 속성은 테스트에서만 주입한다. JWT 검증 자체나 실제 DB 승인을 검증하는 테스트는 아니다.
class SavingGroupJoinApprovalControllerTest {
    private static final String PATH = "/api/saving-groups/10/join-requests/50/approve";
    private SavingGroupService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(SavingGroupService.class);
        mvc = controller(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void 인증완료회원은본문없이승인하고공통응답을받는다(String root) throws Exception {
        mvc.perform(patch(root + "/10/join-requests/50/approve").requestAttr("memberId", 40L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty());
        verify(service).approveSavingGroupJoin(10L, 50L, 40L);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void 인증정보없으면Service호출없이401(String root) throws Exception {
        mvc.perform(patch(root + "/10/join-requests/50/approve"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void 유효하지않은인증회원도차단(long memberId) throws Exception {
        mvc.perform(patch(PATH).requestAttr("memberId", memberId))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void 클라이언트회원ID와헤더만으로인증되지않는다() throws Exception {
        mvc.perform(patch(PATH).param("memberId", "40")
                        .header("memberId", "40").header("Authorization", "Bearer test-placeholder")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memberId\":40}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verifyNoInteractions(service);
    }

    @Test
    void 클라이언트가전달한승인자는무시한다() throws Exception {
        mvc.perform(patch(PATH).requestAttr("memberId", 40L).param("memberId", "99")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"memberId\":99,\"approvedBy\":99}"))
                .andExpect(status().isOk());
        verify(service).approveSavingGroupJoin(10L, 50L, 40L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/saving-groups/abc/join-requests/50/approve",
            "/api/saving-groups/10/join-requests/abc/approve",
            "/api/saving-groups/9223372036854775808/join-requests/50/approve"
    })
    void 숫자로변환할수없는경로는400(String path) throws Exception {
        mvc.perform(patch(path).requestAttr("memberId", 40L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/saving-groups/0/join-requests/50/approve",
            "/api/saving-groups/10/join-requests/-1/approve"
    })
    void 영음수경로는기존Service검증으로400(String path) throws Exception {
        // AI작성: 값 범위 검증을 Controller에 복제하지 않고 실제 Service의 입력 검증을 재사용한다.
        SavingGroupMapper mapper = mock(SavingGroupMapper.class);
        controller(new SavingGroupServiceImpl(mapper))
                .perform(patch(path).requestAttr("memberId", 40L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(mapper);
    }

    @ParameterizedTest
    @MethodSource("businessErrors")
    void 기존Service예외를공통오류응답으로반환(HttpStatus httpStatus, String code) throws Exception {
        doThrow(new ApiException(httpStatus, code, "승인 조건을 확인해주세요."))
                .when(service).approveSavingGroupJoin(10L, 50L, 40L);
        mvc.perform(patch(PATH).requestAttr("memberId", 40L))
                .andExpect(status().is(httpStatus.value()))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta").isEmpty())
                .andExpect(jsonPath("$.error.code").value(code))
                .andExpect(jsonPath("$.error.message").value("승인 조건을 확인해주세요."));
    }

    private static Stream<Arguments> businessErrors() {
        return Stream.of(
                Arguments.of(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED"),
                Arguments.of(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND"),
                Arguments.of(HttpStatus.NOT_FOUND, "JOIN_REQUEST_NOT_FOUND"),
                Arguments.of(HttpStatus.NOT_FOUND, "JOIN_REQUEST_MEMBER_NOT_FOUND"),
                Arguments.of(HttpStatus.CONFLICT, "JOIN_REQUEST_NOT_PENDING"),
                Arguments.of(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_UNAVAILABLE"),
                Arguments.of(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_NOT_CONFIGURED"),
                Arguments.of(HttpStatus.CONFLICT, "SAVING_GROUP_CAPACITY_FULL"),
                Arguments.of(HttpStatus.CONFLICT, "SAVING_GROUP_JOIN_LIMIT_EXCEEDED"),
                Arguments.of(HttpStatus.CONFLICT, "JOIN_REQUEST_CHANGED"),
                Arguments.of(HttpStatus.CONFLICT, "JOIN_REQUEST_APPROVAL_FAILED"));
    }

    // AI작성: 신청 목록도 두 경로 모두 서버 인증 정보를 사용하며 data/meta 구조를 유지한다.
    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void 계주목록정상응답(String root) throws Exception {
        var row = new com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestRow();
        row.setCrewMemberId("9007199254740993");
        row.setApplicant("가**");
        row.setMembershipStatus("PENDING");
        when(service.getSavingGroupJoinRequests(10L, 40L, "PENDING", 1)).thenReturn(
                new com.nightstudy.moa.savingGroup.dto.SavingGroupJoinRequestListResponse(java.util.List.of(row),
                        com.nightstudy.moa.common.dto.PageMeta.of(1, 10, 1)));
        mvc.perform(get(root + "/10/join-requests").param("status", "PENDING").requestAttr("memberId", 40L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].crewMemberId").value("9007199254740993"))
                .andExpect(jsonPath("$.data[0].applicant").value("가**"))
                .andExpect(jsonPath("$.meta.size").value(10));
        verify(service).getSavingGroupJoinRequests(10L, 40L, "PENDING", 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/saving-groups", "/saving-groups"})
    void 목록비로그인은401(String root) throws Exception {
        mvc.perform(get(root + "/10/join-requests").param("memberId", "40"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "2147483648"})
    void 목록잘못된페이지타입400(String page) throws Exception {
        mvc.perform(get("/api/saving-groups/10/join-requests").param("page", page).requestAttr("memberId", 40L))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }

    @Test
    void 목록비계주는403공통응답() throws Exception {
        when(service.getSavingGroupJoinRequests(10L, 40L, null, 1))
                .thenThrow(new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "계주만 조회할 수 있습니다."));
        mvc.perform(get("/api/saving-groups/10/join-requests").requestAttr("memberId", 40L))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("SAVING_GROUP_OWNER_REQUIRED"));
    }

    private static MockMvc controller(SavingGroupService service) {
        return MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
}
