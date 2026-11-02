package com.nightstudy.moa.smallGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsPatchRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 설정 조회·수정 Controller가 JWT 인증 회원 ID와 공통 응답·업무 오류를 그대로 연결하는지 검증한다.
class SmallGroupSettingsControllerTest {

    @Mock
    private SmallGroupCreationService smallGroupCreationService;

    @Mock
    private SmallGroupService smallGroupService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SmallGroupController(smallGroupCreationService, smallGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getSettingsUsesJwtMemberIdAndReturnsCommonResponse() throws Exception {
        SmallGroupSettingsResponse response = new SmallGroupSettingsResponse(
                "동네 걷기", "운동", "함께 걷는 소모임입니다.", 10,
                LocalDate.of(2026, 11, 1), "한강 공원", 2L);
        when(smallGroupService.getSmallGroupSettings(10L, 30L)).thenReturn(response);

        mockMvc.perform(get("/api/small-groups/10/settings").requestAttr("memberId", 30L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.groupName").value("동네 걷기"))
                .andExpect(jsonPath("$.data.approvedMemberCount").value(2));

        verify(smallGroupService).getSmallGroupSettings(10L, 30L);
    }

    @Test
    void getSettingsReturnsBadRequestForNonPositiveSmallGroupId() throws Exception {
        mockMvc.perform(get("/api/small-groups/0/settings").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void getSettingsPreservesServiceErrorResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.FORBIDDEN, "SMALL_GROUP_SETTINGS_FORBIDDEN",
                "개설자만 설정을 조회할 수 있습니다."))
                .when(smallGroupService).getSmallGroupSettings(10L, 30L);

        mockMvc.perform(get("/api/small-groups/10/settings").requestAttr("memberId", 30L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_SETTINGS_FORBIDDEN"));
    }

    @Test
    void updateSettingsUsesJwtMemberIdAndReturnsEmptyCommonResponse() throws Exception {
        mockMvc.perform(patch("/api/small-groups/10")
                        .requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "groupName": "주말 걷기",
                                  "likeCategory": "운동",
                                  "description": "함께 걸어요.",
                                  "maxMembers": 8,
                                  "meetDate": "2099-11-01",
                                  "location": "한강 공원"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        ArgumentCaptor<SmallGroupSettingsPatchRequest> requestCaptor =
                ArgumentCaptor.forClass(SmallGroupSettingsPatchRequest.class);
        verify(smallGroupService).updateSmallGroupSettings(eq(10L), eq(30L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().getMaxMembers()).isEqualTo(8);
        assertThat(requestCaptor.getValue().getMeetDate()).isEqualTo(LocalDate.of(2099, 11, 1));
    }

    @Test
    void updateSettingsRejectsInvalidRequestBeforeCallingService() throws Exception {
        mockMvc.perform(patch("/api/small-groups/10")
                        .requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupName\":\"가\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));

        verify(smallGroupService, never()).updateSmallGroupSettings(eq(10L), eq(30L), any());
    }

    @Test
    void updateSettingsPreservesCapacityConflictResponse() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "SMALL_GROUP_CAPACITY_BELOW_MEMBERS",
                "승인된 참여자 수보다 작게 설정할 수 없습니다."))
                .when(smallGroupService).updateSmallGroupSettings(eq(10L), eq(30L), any());

        mockMvc.perform(patch("/api/small-groups/10")
                        .requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxMembers\":2}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SMALL_GROUP_CAPACITY_BELOW_MEMBERS"));
    }
}
