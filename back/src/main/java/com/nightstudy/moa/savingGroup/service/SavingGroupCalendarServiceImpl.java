package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.common.exception.ApiException;

// AI작성: 가입 승인 상태와 역할을 서버에서 확인한 뒤 대상 모임 일정에 접근한다.
@Service
public class SavingGroupCalendarServiceImpl implements SavingGroupCalendarService {
    private static final int DAILY_EVENT_LIMIT = 10;
    private final SavingGroupMapper mapper;
    private final NotificationService notificationService;

    public SavingGroupCalendarServiceImpl(
            SavingGroupMapper mapper,
            NotificationService notificationService) {
        this.mapper = mapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Month getMonth(Long savingGroupId, Long memberId, YearMonth month) {
        var access = requireAccess(savingGroupId, memberId, false);
        Long groupId = access.groupId();
        if (month.getYear() < 1000 || month.getYear() > 9999) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "조회 월이 올바르지 않습니다.");
        }
        var start = month.atDay(1).atStartOfDay();
        var events = mapper.selectCalendarEvents(groupId, start, start.plusMonths(1)).stream()
                .map(row -> toEvent(row.scheduleId(), row.title(), row.eventDate())).toList();
        return new Month(canCreate(access.role()), events);
    }

    @Override
    @Transactional
    public Event create(Long savingGroupId, Long memberId, CreateRequest request) {
        var access = requireAccess(savingGroupId, memberId, true);
        Long groupId = access.groupId();
        if (!canCreate(access.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "계주와 회계만 일정을 등록할 수 있습니다.");
        }
        if (request.date().getYear() < 1000 || request.date().getYear() > 9999
                || request.time().getSecond() != 0 || request.time().getNano() != 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "날짜와 시각을 분 단위로 입력해 주세요.");
        }
        // AI작성: 모임 잠금을 획득한 트랜잭션에서 조회와 등록을 연속으로 처리한다.
        var start = request.date().atStartOfDay();
        if (mapper.countCalendarEvents(groupId, start, start.plusDays(1)) >= DAILY_EVENT_LIMIT) {
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_DAILY_EVENT_LIMIT",
                    "하루에 최대 10개까지 등록할 수 있습니다.");
        }
        var row = new Insert(groupId, memberId, request.title().strip(), request.date().atTime(request.time()));
        if (mapper.insertCalendarEvent(row) != 1 || row.scheduleId == null) {
            throw new IllegalStateException("일정 등록 결과를 확인할 수 없습니다.");
        }
        // AI작성: 일정 저장 성공 후 작성자를 제외한 승인 계모임 회원에게 새 일정 알림을 보낸다.
        mapper.selectApprovedSavingGroupMemberIds(groupId).stream()
                .filter(receiverId -> !Objects.equals(receiverId, memberId))
                .forEach(receiverId -> notificationService.send(
                        receiverId,
                        NotificationType.SCHEDULE_CREATED,
                        "계모임에 새 일정이 등록되었습니다.",
                        "/saving-groups/" + savingGroupId + "?menu=calendar"
                ));
        return toEvent(row.scheduleId, row.title, row.eventDate);
    }

    // AI작성: 등록과 같은 모임 잠금을 사용해 삭제와 하루 상한 집계를 직렬화한다.
    @Override
    @Transactional
    public void delete(Long savingGroupId, Long memberId, Long scheduleId) {
        var access = requireAccess(savingGroupId, memberId, true);
        Long groupId = access.groupId();
        if (!canCreate(access.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "계주와 회계만 일정을 삭제할 수 있습니다.");
        }
        if (mapper.deleteCalendarEvent(groupId, scheduleId) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "삭제할 일정을 찾을 수 없습니다.");
        }
    }

    private CalendarAccess requireAccess(Long savingGroupId, Long memberId, boolean forWrite) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        // AI작성: 쓰기는 계모임 ID로 행을 먼저 잠그고, 일정과 회원 소속에는 공통 모임 ID를 사용한다.
        Long groupId;
        if (forWrite) {
            var target = mapper.selectSavingGroupJoinTargetForUpdate(savingGroupId);
            groupId = target == null ? null : target.getGroupId();
        } else {
            var target = mapper.selectSavingGroupDetailBySavingGroupId(savingGroupId);
            groupId = target == null ? null : target.getGroupId();
        }
        if (groupId == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        var relation = mapper.selectSavingGroupMembership(groupId, memberId);
        if (relation != null && "APPROVED".equals(relation.getMembershipStatus())) {
            String role = relation.getMemberRole();
            if (canCreate(role) || "MEMBER".equals(role)) {
                return new CalendarAccess(groupId, role);
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "승인된 모임원만 일정을 확인할 수 있습니다.");
    }

    private record CalendarAccess(Long groupId, String role) {}

    private boolean canCreate(String role) {
        return "OWNER".equals(role) || "MANAGER".equals(role);
    }

    private Event toEvent(Long id, String title, LocalDateTime dateTime) {
        return new Event(id.toString(), title, dateTime.toLocalDate().toString(), dateTime.toLocalTime().toString());
    }
}
