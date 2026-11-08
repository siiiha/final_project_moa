package com.nightstudy.moa.savingGroup.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.nightstudy.moa.common.exception.GlobalExceptionHandler;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.savingGroup.service.SavingGroupCalendarServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// AI작성: 실제 서비스와 웹 요청 검증을 함께 실행하고 DB 호출을 대체한다.
class SavingGroupCalendarControllerTest {
    private static final String CALENDAR = "/api/saving-groups/10/calendar";
    private static final String EVENT = """
            {"title":"정기 모임","date":"2026-11-20","time":"19:00"}
            """;
    private final SavingGroupMapper mapper = mock(SavingGroupMapper.class);
    private final org.springframework.test.web.servlet.MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new SavingGroupCalendarController(new SavingGroupCalendarServiceImpl(mapper)))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();

    private void membership(String role) {
        when(mapper.selectSavingGroupDetail(10L)).thenReturn(mock(SavingGroupRow.class));
        var member = new SavingGroupMembershipRelationRow();
        member.setMembershipStatus("APPROVED");
        member.setMemberRole(role);
        when(mapper.selectSavingGroupMembership(10L, 30L)).thenReturn(member);
    }

    @Test void memberReadsButCannotCreate() throws Exception {
        membership("MEMBER");
        var start = LocalDateTime.of(2026, 11, 1, 0, 0);
        when(mapper.selectCalendarEvents(10L, start, start.plusMonths(1))).thenReturn(List.of(
                new SavingGroupCalendarDto.EventRow(42L, "정기 모임", start.plusDays(19).plusHours(19))));
        mvc.perform(get(CALENDAR).requestAttr("memberId", 30L).param("month", "2026-11"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.canCreate").value(false))
                .andExpect(jsonPath("$.data.events[0].id").value("42"))
                .andExpect(jsonPath("$.data.events[0].date").value("2026-11-20"))
                .andExpect(jsonPath("$.data.events[0].time").value("19:00"));
        mvc.perform(post(CALENDAR + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isForbidden());
        verify(mapper, never()).insertCalendarEvent(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MANAGER"})
    void organizerCanCreate(String role) throws Exception {
        membership(role);
        when(mapper.insertCalendarEvent(any())).thenAnswer(invocation -> {
            SavingGroupCalendarDto.Insert row = invocation.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(row.writerId).isEqualTo(30L);
            org.assertj.core.api.Assertions.assertThat(row.groupId).isEqualTo(10L);
            row.scheduleId = 42L;
            return 1;
        });
        mvc.perform(post(CALENDAR + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value("42"));
    }

    @Test void rejectsInvalidInputsAndCrossGroupAccess() throws Exception {
        membership("OWNER");
        when(mapper.selectSavingGroupDetail(11L)).thenReturn(mock(SavingGroupRow.class));
        var other = "/api/saving-groups/11/calendar";
        mvc.perform(get(other).requestAttr("memberId", 30L).param("month", "2026-11"))
                .andExpect(status().isForbidden());
        mvc.perform(post(other + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isForbidden());
        mvc.perform(get(CALENDAR).param("month", "2026-11")).andExpect(status().isUnauthorized());
        mvc.perform(get(CALENDAR).requestAttr("memberId", 30L).param("month", "2026-13"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(CALENDAR + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT.replace("정기 모임", " ")))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).selectCalendarEvents(any(), any(), any());
        verify(mapper, never()).insertCalendarEvent(any());
    }
}
