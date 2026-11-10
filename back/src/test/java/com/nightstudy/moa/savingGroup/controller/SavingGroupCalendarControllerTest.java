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
            MockMvcBuilders.standaloneSetup(new SavingGroupCalendarController(new SavingGroupCalendarServiceImpl(mapper, mock(com.nightstudy.moa.notification.service.NotificationService.class))))
                    .setControllerAdvice(new GlobalExceptionHandler()).build();

    // AI작성: 요청 ID와 DB 공통 모임 ID를 다르게 설정한다.
    private void calendarTarget(long savingGroupId, long groupId) {
        var row = new SavingGroupRow();
        row.setGroupId(groupId);
        when(mapper.selectSavingGroupDetailBySavingGroupId(savingGroupId)).thenReturn(row);
        var target = new com.nightstudy.moa.savingGroup.dto.SavingGroupJoinTargetRow();
        target.setGroupId(groupId);
        when(mapper.selectSavingGroupJoinTargetForUpdate(savingGroupId)).thenReturn(target);
    }

    private void membership(String role) {
        calendarTarget(10L, 20L);
        var member = new SavingGroupMembershipRelationRow();
        member.setMembershipStatus("APPROVED");
        member.setMemberRole(role);
        when(mapper.selectSavingGroupMembership(20L, 30L)).thenReturn(member);
    }

    @Test void memberReadsButCannotCreate() throws Exception {
        membership("MEMBER");
        var start = LocalDateTime.of(2026, 11, 1, 0, 0);
        when(mapper.selectCalendarEvents(20L, start, start.plusMonths(1))).thenReturn(List.of(
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
        // AI작성: 9건이 저장된 날짜의 10번째 등록을 허용한다.
        var start = LocalDateTime.of(2026, 11, 20, 0, 0);
        when(mapper.countCalendarEvents(20L, start, start.plusDays(1))).thenReturn(9);
        when(mapper.insertCalendarEvent(any())).thenAnswer(invocation -> {
            SavingGroupCalendarDto.Insert row = invocation.getArgument(0);
            org.assertj.core.api.Assertions.assertThat(row.writerId).isEqualTo(30L);
            org.assertj.core.api.Assertions.assertThat(row.groupId).isEqualTo(20L);
            row.scheduleId = 42L;
            return 1;
        });
        mvc.perform(post(CALENDAR + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value("42"));
    }

    @Test void rejectsEleventhEventWithLimitCode() throws Exception {
        membership("OWNER");
        var start = LocalDateTime.of(2026, 11, 20, 0, 0);
        when(mapper.countCalendarEvents(20L, start, start.plusDays(1))).thenReturn(10);
        mvc.perform(post(CALENDAR + "/events").requestAttr("memberId", 30L)
                        .contentType(MediaType.APPLICATION_JSON).content(EVENT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SAVING_GROUP_DAILY_EVENT_LIMIT"));
        verify(mapper, never()).insertCalendarEvent(any());
    }

    // AI작성: 계주·회계는 삭제할 수 있으며 삭제된 일정의 재요청은 404다.
    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MANAGER"})
    void organizerDeletesOnlyMatchingEvent(String role) throws Exception {
        membership(role);
        when(mapper.deleteCalendarEvent(20L, 42L)).thenReturn(1, 0);
        var path = CALENDAR + "/events/42";
        mvc.perform(delete(path).requestAttr("memberId", 30L))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
        mvc.perform(delete(path).requestAttr("memberId", 30L))
                .andExpect(status().isNotFound());
        mvc.perform(delete(CALENDAR + "/events/99").requestAttr("memberId", 30L))
                .andExpect(status().isNotFound());
    }

    @Test void rejectsUnauthorizedAndInvalidDeletes() throws Exception {
        membership("MEMBER");
        var path = CALENDAR + "/events/42";
        mvc.perform(delete(path)).andExpect(status().isUnauthorized());
        mvc.perform(delete(path).requestAttr("memberId", 31L)).andExpect(status().isForbidden());
        mvc.perform(delete(path).requestAttr("memberId", 30L)).andExpect(status().isForbidden());
        mvc.perform(delete(CALENDAR + "/events/0").requestAttr("memberId", 30L))
                .andExpect(status().isBadRequest());
        verify(mapper, never()).deleteCalendarEvent(any(), any());
    }

    @Test void rejectsInvalidInputsAndCrossGroupAccess() throws Exception {
        membership("OWNER");
        calendarTarget(11L, 21L);
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
