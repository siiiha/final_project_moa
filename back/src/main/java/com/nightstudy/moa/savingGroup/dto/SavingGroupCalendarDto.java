package com.nightstudy.moa.savingGroup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// AI작성: 일정 저장 일시와 화면에 전달할 날짜·시각을 구분한다.
public final class SavingGroupCalendarDto {
    private SavingGroupCalendarDto() {}

    public record CreateRequest(@NotBlank @Size(max = 100) String title,
                                @NotNull LocalDate date, @NotNull LocalTime time) {}
    public record EventRow(Long scheduleId, String title, LocalDateTime eventDate) {}
    public record Event(String id, String title, LocalDate date, LocalTime time) {}
    public record Month(boolean canCreate, List<Event> events) {}

    // AI작성: MyBatis가 등록 직후 생성된 일정 ID를 기록한다.
    public static class Insert {
        public Long scheduleId;
        public final Long groupId;
        public final Long writerId;
        public final String title;
        public final LocalDateTime eventDate;

        public Insert(Long groupId, Long writerId, String title, LocalDateTime eventDate) {
            this.groupId = groupId;
            this.writerId = writerId;
            this.title = title;
            this.eventDate = eventDate;
        }
    }
}
