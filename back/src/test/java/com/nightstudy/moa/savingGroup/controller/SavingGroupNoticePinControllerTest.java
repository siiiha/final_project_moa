package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.common.dto.PageMeta;
import com.nightstudy.moa.savingGroup.service.SavingGroupService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 JWT·DB 없이 고정 변경 경로·입력·인증 정보 전달 및 공통 응답을 확인한다.
@ExtendWith(MockitoExtension.class)
class SavingGroupNoticePinControllerTest {
    @Mock private SavingGroupService service;
    private MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new SavingGroupController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }
    @ParameterizedTest
    @CsvSource({"/saving-groups,true", "/saving-groups,false", "/api/saving-groups,true", "/api/saving-groups,false"})
    void forwardsStateAndOnlyAuthenticatedMember(String base, boolean pinned) throws Exception {
        mvc.perform(patch(base + "/20/notices/40/pin").requestAttr("memberId", 30L).param("memberId", "999")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":" + pinned + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        verify(service).updateSavingGroupNoticePinned(20L, 40L, 30L, pinned);
    }
    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"pinned\":null}", "{", ""})
    void rejectsMissingOrInvalidBody(String body) throws Exception {
        mvc.perform(patch("/saving-groups/20/notices/40/pin").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(service);
    }
    @ParameterizedTest
    @CsvSource({"0,40", "-1,40", "abc,40", "20,0", "20,-1", "20,abc"})
    void rejectsInvalidPath(String groupId, String noticeId) throws Exception {
        mvc.perform(patch("/saving-groups/" + groupId + "/notices/" + noticeId + "/pin")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
        verifyNoInteractions(service);
    }
    @Test void missingAuthenticationIsDelegatedWithoutUsingMemberQuery() throws Exception {
        doThrow(new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다."))
                .when(service).updateSavingGroupNoticePinned(20L, 40L, null, true);
        mvc.perform(patch("/saving-groups/20/notices/40/pin").param("memberId", "30")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(status().isUnauthorized());
        verify(service).updateSavingGroupNoticePinned(20L, 40L, null, true);
    }
    @ParameterizedTest
    @CsvSource({"403,SAVING_GROUP_NOTICE_PIN_FORBIDDEN", "404,SAVING_GROUP_NOTICE_NOT_FOUND",
            "409,SAVING_GROUP_NOTICE_PIN_LIMIT_EXCEEDED", "409,SAVING_GROUP_ACTIVITY_RESTRICTED", "500,SAVING_GROUP_NOTICE_PIN_FAILED"})
    void preservesServiceFailure(int status, String code) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(status), code, "변경 실패"))
                .when(service).updateSavingGroupNoticePinned(20L, 40L, 30L, true);
        mvc.perform(patch("/saving-groups/20/notices/40/pin").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"pinned\":true}"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.error.code").value(code));
    }
    @Test void detailReturnsPinnedListSeparatelyFromNormalMeta() throws Exception {
        var detail = new SavingGroupDetailResponse(10L, 20L, "모임", "설명", "서울", 10, 2L,
                null, null, "01", List.of(), null, null, false, null, null);
        detail.setPinnedNotices(List.of(new SavingGroupNoticeSummaryResponse(40L, "고정 공지", "작성자", null)));
        when(service.getSavingGroupDetail(eq(20L), isNull(), any(SavingGroupNoticeSearchCondition.class)))
                .thenReturn(new SavingGroupDetailPageResponse(detail, PageMeta.of(1, 4, 0)));
        mvc.perform(get("/saving-groups/20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.pinnedNotices[0].noticeId").value(40))
                .andExpect(jsonPath("$.data.notices").isEmpty()).andExpect(jsonPath("$.meta.total").value(0));
    }
}
