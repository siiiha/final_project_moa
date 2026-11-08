package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDateTime;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.common.exception.ApiException;

// AI작성: 가입 승인 상태와 역할을 서버에서 확인한 뒤 대상 모임 일정에 접근한다.
@Service
public class SavingGroupCalendarServiceImpl implements SavingGroupCalendarService {
    private static final int DAILY_EVENT_LIMIT = 10;
    private final SavingGroupMapper mapper;

    public SavingGroupCalendarServiceImpl(SavingGroupMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Month getMonth(Long groupId, Long memberId, YearMonth month) {
        String role = requireRole(groupId, memberId, false);
        if (month.getYear() < 1000 || month.getYear() > 9999) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "조회 월이 올바르지 않습니다.");
        }
        var start = month.atDay(1).atStartOfDay();
        var events = mapper.selectCalendarEvents(groupId, start, start.plusMonths(1)).stream()
                .map(row -> toEvent(row.scheduleId(), row.title(), row.eventDate())).toList();
        return new Month(canCreate(role), events);
    }

    @Override
    @Transactional
    public Event create(Long groupId, Long memberId, CreateRequest request) {
        if (!canCreate(requireRole(groupId, memberId, true))) {
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
        return toEvent(row.scheduleId, row.title, row.eventDate);
    }

    private String requireRole(Long groupId, Long memberId, boolean forCreate) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        // AI작성: 등록은 일반 SELECT로 읽기 스냅샷을 만들기 전에 기존 모임 행을 잠근다.
        boolean exists = forCreate ? mapper.selectSavingGroupStatusForUpdate(groupId) != null
                : mapper.selectSavingGroupDetail(groupId) != null;
        if (!exists) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "조회할 수 있는 계모임이 없습니다.");
        }
        var relation = mapper.selectSavingGroupMembership(groupId, memberId);
        if (relation != null && "APPROVED".equals(relation.getMembershipStatus())) {
            String role = relation.getMemberRole();
            if (canCreate(role) || "MEMBER".equals(role)) {
                return role;
            }
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "승인된 모임원만 일정을 확인할 수 있습니다.");
    }

    private boolean canCreate(String role) {
        return "OWNER".equals(role) || "MANAGER".equals(role);
    }

    private Event toEvent(Long id, String title, LocalDateTime dateTime) {
        return new Event(id.toString(), title, dateTime.toLocalDate().toString(), dateTime.toLocalTime().toString());
    }
}
