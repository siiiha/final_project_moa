package com.nightstudy.moa.savingGroup.controller;

import com.nightstudy.moa.common.dto.ApiResponse;
import com.nightstudy.moa.savingGroup.dto.SavingGroupCalendarDto.*;
import com.nightstudy.moa.savingGroup.service.SavingGroupCalendarService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.YearMonth;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// AI작성: 월간 조회와 등록 요청을 검증하고 로그인 회원 정보를 서비스로 전달한다.
@RestController
@RequestMapping({"/saving-groups/{groupId}/calendar", "/api/saving-groups/{groupId}/calendar"})
public class SavingGroupCalendarController {
    private final SavingGroupCalendarService service;

    public SavingGroupCalendarController(SavingGroupCalendarService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<Month> getMonth(@PathVariable @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @RequestParam @DateTimeFormat(pattern = "uuuu-MM") YearMonth month,
            jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return ApiResponse.ok(service.getMonth(groupId, memberId, month));
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Event> create(@PathVariable @Positive Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody CreateRequest request) {
        return ApiResponse.ok(service.create(groupId, memberId, request));
    }
}
