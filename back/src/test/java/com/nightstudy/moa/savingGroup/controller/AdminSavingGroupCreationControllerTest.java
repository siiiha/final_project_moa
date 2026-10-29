package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationDetailResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationListItemResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCreationApplicationSearchCondition;
import com.nightstudy.moa.savingGroup.service.SavingGroupCreationService;
import java.math.BigDecimal;
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
// AI작성: 관리자 계모임 개설·삭제 심사 API의 요청 검증과 공통 응답 형식을 확인한다.
class AdminSavingGroupCreationControllerTest {

    @Mock
    private SavingGroupCreationService savingGroupCreationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminSavingGroupCreationController(savingGroupCreationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    // AI작성: 삭제 심사 상세가 관리자 판단에 필요한 사유와 삭제 차단 조건을 모두 반환하는지 검증한다.
    void getDeletionApplicationDetailReturnsDeletionConditions() throws Exception {
        SavingGroupCreationApplicationDetailResponse detail = new SavingGroupCreationApplicationDetailResponse();
        detail.setApprovalId(1L);
        detail.setType("DELETE");
        detail.setRequestReason("운영 종료");
        detail.setCurrentBalance(new BigDecimal("15000"));
        detail.setActiveRound(true);
        detail.setPendingPayout(true);
        when(savingGroupCreationService.getCreationApplication(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/saving-group-applications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("DELETE"))
                .andExpect(jsonPath("$.data.requestReason").value("운영 종료"))
                .andExpect(jsonPath("$.data.currentBalance").value(15000))
                .andExpect(jsonPath("$.data.activeRound").value(true))
                .andExpect(jsonPath("$.data.pendingPayout").value(true));
    }

    @Test
    // AI작성: 존재하지 않는 심사 번호는 빈 성공 응답 대신 명확한 404 오류를 반환하는지 검증한다.
    void getApplicationDetailReturnsNotFound() throws Exception {
        when(savingGroupCreationService.getCreationApplication(999L)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "SAVING_GROUP_APPLICATION_NOT_FOUND",
                "계모임 심사 신청을 찾을 수 없습니다."
        ));

        mockMvc.perform(get("/api/admin/saving-group-applications/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SAVING_GROUP_APPLICATION_NOT_FOUND"));
    }

    @Test
    // AI작성: 관리자 심사 목록이 DELETE 유형과 페이징 정보를 공통 응답으로 반환하는지 검증한다.
    void getApplicationListReturnsDeletionTypeAndPageMeta() throws Exception {
        SavingGroupCreationApplicationListItemResponse item = new SavingGroupCreationApplicationListItemResponse();
        item.setApprovalId("1");
        item.setType("DELETE");
        item.setStatus("PENDING");
        PageMeta pageMeta = PageMeta.of(1, 8, 1);
        when(savingGroupCreationService.getCreationApplicationPageMeta(any())).thenReturn(pageMeta);
        when(savingGroupCreationService.getCreationApplications(
                any(SavingGroupCreationApplicationSearchCondition.class), any(PageMeta.class)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/saving-group-applications")
                        .param("type", "DELETE")
                        .param("status", "PENDING")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].approvalId").value("1"))
                .andExpect(jsonPath("$.data[0].type").value("DELETE"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.size").value(8));
    }

    @Test
    // AI작성: CREATE와 DELETE 외의 심사 유형은 관리자 목록 조회 전에 요청 검증으로 차단한다.
    void getApplicationListRejectsInvalidType() throws Exception {
        mockMvc.perform(get("/api/admin/saving-group-applications")
                        .param("type", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    // AI작성: 대기·승인·반려 외의 심사 상태는 관리자 목록 조회 전에 요청 검증으로 차단한다.
    void getApplicationListRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/admin/saving-group-applications")
                        .param("status", "COMPLETED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    // AI작성: 관리자 심사 목록의 페이지 번호는 1부터 시작하며 0 이하는 요청 검증으로 차단한다.
    void getApplicationListRejectsInvalidPage() throws Exception {
        mockMvc.perform(get("/api/admin/saving-group-applications")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    // AI작성: 삭제 심사 승인자가 요청값이 아닌 인증 필터의 관리자 회원 번호로 전달되는지 검증한다.
    void approveApplicationUsesAuthenticatedAdminMemberId() throws Exception {
        mockMvc.perform(patch("/api/admin/saving-group-applications/1/approve")
                        .requestAttr("memberId", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(savingGroupCreationService).approveCreationApplication(1L, 10L);
    }

    @Test
    // AI작성: 관리자 삭제 심사 반려 API가 검증된 반려 사유를 Service에 전달하는지 확인한다.
    void rejectApplicationPassesRequiredReason() throws Exception {
        mockMvc.perform(patch("/api/admin/saving-group-applications/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rejectReason":"공동자금 정리 후 다시 신청해 주세요."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(savingGroupCreationService).rejectCreationApplication(
                1L, "공동자금 정리 후 다시 신청해 주세요.");
    }
}
