package com.nightstudy.moa.smallGroup;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.smallGroup.controller.SmallGroupController;
import com.nightstudy.moa.smallGroup.dto.SmallGroupSettingsResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
// AI작성: 설정 조회 Controller가 JWT 인증 회원 ID와 공통 응답·업무 오류를 그대로 연결하는지 검증한다.
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
}
