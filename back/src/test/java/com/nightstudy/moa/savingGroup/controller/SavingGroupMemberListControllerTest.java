package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.*;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 JWT 필터와 구분하여 두 경로의 입력·인증 속성·공통 응답 계약을 검증한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupMemberListControllerTest {
    @Mock SavingGroupService service;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void returnsContractAndOnlyTrustsServerIdentity(String base) throws Exception {
        var row = new SavingGroupMemberResponse();
        row.setMemberId("9223372036854775807"); row.setName("테스트계원"); row.setEmail("t***@example.com");
        row.setRole("MEMBER"); row.setJoinedAt(LocalDateTime.of(2026, 11, 5, 10, 0));
        when(service.getSavingGroupMembers(10L, 30L, null, 1)).thenReturn(new SavingGroupMemberListResponse(List.of(row), PageMeta.of(1, 10, 1)));
        mvc.perform(get(base + "/10/members").requestAttr("memberId", 30L).param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].memberId").value("9223372036854775807"))
                .andExpect(jsonPath("$.data[0].name").value("테스트계원"))
                .andExpect(jsonPath("$.data[0].email").value("t***@example.com"))
                .andExpect(jsonPath("$.data[0].role").value("MEMBER"))
                .andExpect(jsonPath("$.data[0].joinedAt").exists())
                .andExpect(jsonPath("$.meta.size").value(10));
    }
    @Test void outOfRangeReturnsEmptyArray() throws Exception {
        when(service.getSavingGroupMembers(10L, 30L, null, 3)).thenReturn(new SavingGroupMemberListResponse(List.of(), PageMeta.of(3, 10, 20)));
        mvc.perform(get("/api/saving-groups/10/members").requestAttr("memberId", 30L).param("page", "3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty()).andExpect(jsonPath("$.meta.totalPages").value(2));
    }
    @ParameterizedTest @ValueSource(strings = {"0/members", "abc/members", "10/members?page=0", "10/members?page=abc"})
    void invalidInput(String path) throws Exception {
        mvc.perform(get("/saving-groups/" + path)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
    @ParameterizedTest @ValueSource(ints = {401, 403, 404, 409})
    void preservesServiceErrors(int status) throws Exception {
        when(service.getSavingGroupMembers(10L, null, null, 1)).thenThrow(new ApiException(HttpStatus.valueOf(status), "TEST_ERROR", "조회 실패"));
        mvc.perform(get("/saving-groups/10/members").param("memberId", "999"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value("TEST_ERROR"));
    }
    // AI작성: 두 API 경로에서 검색어 정규화·페이지·인증 속성이 그대로 Service에 전달된다.
    @ParameterizedTest @ValueSource(strings = {"/saving-groups", "/api/saving-groups"})
    void keywordAndPageAreValidatedAndForwarded(String base) throws Exception {
        when(service.getSavingGroupMembers(10L, 30L, "test@example.com", 2))
                .thenReturn(new SavingGroupMemberListResponse(List.of(), PageMeta.of(2, 10, 0)));
        mvc.perform(get(base + "/10/members").requestAttr("memberId", 30L).param("keyword", "  test@example.com  ")
                .param("page", "2").param("memberId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        verify(service).getSavingGroupMembers(10L, 30L, "test@example.com", 2);
    }
    @ParameterizedTest @ValueSource(strings = {"", " ", "　"})
    void blankKeywordClearsSearch(String keyword) throws Exception {
        when(service.getSavingGroupMembers(10L, 30L, null, 1))
                .thenReturn(new SavingGroupMemberListResponse(List.of(), PageMeta.of(1, 10, 0)));
        mvc.perform(get("/api/saving-groups/10/members").requestAttr("memberId", 30L).param("keyword", keyword))
                .andExpect(status().isOk());
        verify(service).getSavingGroupMembers(10L, 30L, null, 1);
    }
    @Test void twentyCharactersAfterTrimmingAreAccepted() throws Exception {
        String keyword = "가".repeat(20);
        when(service.getSavingGroupMembers(10L, 30L, keyword, 1))
                .thenReturn(new SavingGroupMemberListResponse(List.of(), PageMeta.of(1, 10, 0)));
        mvc.perform(get("/api/saving-groups/10/members").requestAttr("memberId", 30L).param("keyword", " " + keyword + " "))
                .andExpect(status().isOk());
    }
    @Test void longKeywordFailsValidation() throws Exception {
        mvc.perform(get("/api/saving-groups/10/members").param("keyword", "가".repeat(21)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test void emptyPageIsInvalid() throws Exception {
        mvc.perform(get("/api/saving-groups/10/members").param("page", ""))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
