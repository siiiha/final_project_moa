package com.nightstudy.moa.admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.admin.dto.AdminSavingGroupDetailResponse;
import com.nightstudy.moa.admin.dto.AdminSavingGroupListResult;
import com.nightstudy.moa.admin.service.AdminSavingGroupService;
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
// AI작성: 관리자 계모임 Controller의 요청 검증과 공통 응답 형식을 확인한다.
class AdminSavingGroupControllerTest {
    @Mock
    private AdminSavingGroupService adminSavingGroupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminSavingGroupController(adminSavingGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSavingGroupListReturnsPageMeta() throws Exception {
        when(adminSavingGroupService.getSavingGroupList(any()))
                .thenReturn(new AdminSavingGroupListResult(List.of(), PageMeta.of(1, 10, 0)));

        mockMvc.perform(get("/api/admin/saving-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.page").value(1));
    }

    @Test
    void getSavingGroupListRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/admin/saving-groups")
                        .param("status", "STATUS_VALUE_OVER_20_CHARS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getSavingGroupDetailReturnsDetail() throws Exception {
        AdminSavingGroupDetailResponse detail = new AdminSavingGroupDetailResponse();
        detail.setSavingGroupId(1L);
        when(adminSavingGroupService.getSavingGroupDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/saving-groups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.savingGroupId").value(1));
    }

    @Test
    void getSavingGroupDetailReturnsNotFound() throws Exception {
        when(adminSavingGroupService.getSavingGroupDetail(999L)).thenThrow(new ApiException(
                HttpStatus.NOT_FOUND,
                "SAVING_GROUP_NOT_FOUND",
                "계모임을 찾을 수 없습니다."
        ));

        mockMvc.perform(get("/api/admin/saving-groups/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SAVING_GROUP_NOT_FOUND"));
    }

    @Test
    void restrictSavingGroupAcceptsRequiredReason() throws Exception {
        mockMvc.perform(patch("/api/admin/saving-groups/1/restriction")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":"운영정책 위반"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void releaseSavingGroupRestrictionRejectsMissingReason() throws Exception {
        mockMvc.perform(patch("/api/admin/saving-groups/1/restriction/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }
}
