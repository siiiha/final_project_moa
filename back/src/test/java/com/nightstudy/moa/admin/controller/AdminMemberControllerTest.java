package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.dto.AdminMemberDetailResponse;
import com.nightstudy.moa.admin.dto.AdminMemberListResult;
import com.nightstudy.moa.admin.service.AdminMemberService;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 관리자 회원 Controller의 요청 검증과 공통 응답 형식을 확인한다.
class AdminMemberControllerTest {
    @Mock
    private AdminMemberService adminMemberService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminMemberController(adminMemberService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMemberListReturnsPageMeta() throws Exception {
        when(adminMemberService.getMemberList(any()))
                .thenReturn(new AdminMemberListResult(List.of(), PageMeta.of(1, 10, 0)));

        mockMvc.perform(get("/api/admin/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.page").value(1));
    }

    @Test
    void getMemberListRejectsInvalidAccountStatus() throws Exception {
        mockMvc.perform(get("/api/admin/members").param("accountStatus", "BLOCKED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getMemberDetailReturnsDetail() throws Exception {
        AdminMemberDetailResponse detail = new AdminMemberDetailResponse();
        detail.setMemberId(1L);
        when(adminMemberService.getMemberDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/members/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberId").value(1));
    }

    @Test
    void getMemberDetailReturnsNotFound() throws Exception {
        when(adminMemberService.getMemberDetail(999L)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "MEMBER_NOT_FOUND",
                "회원을 찾을 수 없습니다."
        ));

        mockMvc.perform(get("/api/admin/members/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void restrictMemberAcceptsAuthenticatedAdministratorId() throws Exception {
        mockMvc.perform(patch("/api/admin/members/2/restriction")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restrictionType":"7_DAYS","reason":"운영정책 위반"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void restrictMemberRejectsMissingReasonAndInvalidType() throws Exception {
        mockMvc.perform(patch("/api/admin/members/2/restriction")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"restrictionType":"ONE_DAY","reason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void releaseMemberRestrictionRejectsMissingReason() throws Exception {
        mockMvc.perform(patch("/api/admin/members/2/restriction/release")
                        .requestAttr("memberId", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }
}
