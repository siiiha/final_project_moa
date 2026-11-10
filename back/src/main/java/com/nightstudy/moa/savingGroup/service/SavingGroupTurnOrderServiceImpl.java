package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 모임 공통 잠금 아래 승인 회원 전체를 검증하고 다음 달 예약을 원자적으로 교체한다.
@Service
public class SavingGroupTurnOrderServiceImpl implements SavingGroupTurnOrderService {
    private final SavingGroupMapper mapper;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public SavingGroupTurnOrderServiceImpl(SavingGroupMapper mapper) { this(mapper, Clock.system(ZoneId.of("Asia/Seoul"))); }
    SavingGroupTurnOrderServiceImpl(SavingGroupMapper mapper, Clock clock) { this.mapper = mapper; this.clock = clock; }
    private static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TURN_ORDER_CONFLICT", message);
    }
    private SavingGroupTurnOrderPlanRow authorize(Long id, Long user) {
        if (user == null || user <= 0) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (id == null || id <= 0) throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "계모임 번호를 확인해주세요.");
        var group = mapper.selectTurnOrderGroupForUpdate(id);
        if (group == null) throw new ApiException(HttpStatus.NOT_FOUND, "SAVING_GROUP_NOT_FOUND", "계모임을 찾을 수 없습니다.");
        var owner = mapper.selectSavingGroupMembershipForUpdate(group.getGroupId(), user);
        if (owner == null || !"OWNER".equals(owner.getMemberRole()) || !"APPROVED".equals(owner.getMembershipStatus()) || !mapper.existsMember(user))
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_OWNER_REQUIRED", "승인된 계주만 순번을 지정할 수 있습니다.");
        if (!"순번계".equals(group.getGroupType())) throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TYPE_INVALID", "순번계에서만 이용할 수 있습니다.");
        if (!List.of("01", "02").contains(group.getStatus())) throw conflict("모집중 또는 진행중에만 순번을 지정할 수 있습니다.");
        return group;
    }
    private LocalDate effectiveMonth(SavingGroupTurnOrderPlanRow group, LocalDate today) {
        return YearMonth.from(today).plusMonths("02".equals(group.getStatus()) ? 1 : 0).atDay(1);
    }
    @Override @Transactional
    public SavingGroupTurnOrderResponse get(Long id, Long user) { return response(authorize(id, user), LocalDate.now(clock)); }

    @Override @Transactional
    public SavingGroupTurnOrderResponse update(Long id, Long user, SavingGroupTurnOrderUpdateRequest request) {
        var group = authorize(id, user);
        var members = mapper.selectTurnOrderAssignmentMembers(group.getGroupId());
        // AI작성: 잠금 후 한 번 읽은 날짜로 회차와 적용월을 함께 계산한다.
        var today = LocalDate.now(clock);
        int start = SavingGroupTurnOrderPolicy.nextRound(group.getStatus(), group.getStartDate(), today);
        if (request == null || request.turnOrders() == null || request.turnOrders().isEmpty()) throw badRequest();
        if (start > 1 && !Objects.equals(request.currentRoundNumber(), start - 1))
            throw new ApiException(HttpStatus.CONFLICT, "SAVING_GROUP_TURN_ORDER_ROUND_CHANGED", "현재 회차가 변경되었습니다. 최신 순번을 확인해주세요.");
        var effectiveMonth = effectiveMonth(group, today);
        var currentPlan = mapper.selectTurnOrderPlan(id, today.withDayOfMonth(1));
        var current = currentAssignments(members, currentPlan);
        var fixed = fixedMembers(members, current, start);
        var eligible = members.stream().map(SavingGroupTurnOrderAssignmentRow::getMemberId)
                .filter(memberId -> !fixed.contains(memberId)).collect(Collectors.toSet());
        if (start > 1 && eligible.size() < 2)
            throw conflict("변경할 수 있는 순번이 없습니다.");
        var ids = new HashSet<String>(); var rounds = new HashSet<Integer>();
        for (var entry : request.turnOrders()) {
            if (entry == null || entry.memberId() == null || entry.memberId() <= 0 || entry.sequenceNumber() == null
                    || entry.sequenceNumber() < start || entry.sequenceNumber() >= (long) start + eligible.size()
                    || !ids.add(entry.memberId().toString()) || !rounds.add(entry.sequenceNumber())) throw badRequest();
        }
        if (!ids.equals(eligible)) throw badRequest();
        if (start > 1) {
            var targetPlan = mapper.selectTurnOrderPlan(id, effectiveMonth);
            var previous = targetPlan != null && effectiveMonth.equals(targetPlan.getEffectiveMonth())
                    ? assignments(targetPlan) : current;
            if (request.turnOrders().stream().allMatch(entry -> Objects.equals(
                    previous.get(entry.memberId().toString()), entry.sequenceNumber())))
                throw conflict("이전과 동일한 순번입니다.");
        }
        if (mapper.existsUpcomingTurnOrderPayout(group.getGroupId(), start))
            throw conflict("배정할 회차에 기존 지급 요청이 있습니다. 기존 요청을 확인해주세요.");
        // AI작성: 종료일을 자동 연장하지 않는다. 남은 기간보다 배정이 길면 명시적으로 재확인을 요청한다.
        if (group.getEndDate() != null && "02".equals(group.getStatus())
                && YearMonth.from(effectiveMonth).plusMonths(eligible.size() - 1L).isAfter(YearMonth.from(group.getEndDate())))
            throw conflict("남은 운영 기간보다 배정할 회원이 많습니다. 운영 기간을 확인해주세요.");
        if ("01".equals(group.getStatus())) {
            for (var entry : request.turnOrders()) {
                // AI작성: 같은 값 재저장은 SQL 변경 행 수 설정에 관계없이 성공하는 멱등 요청으로 처리한다.
                boolean unchanged = members.stream().anyMatch(m -> m.getMemberId().equals(entry.memberId().toString())
                        && Objects.equals(m.getSequenceNumber(), entry.sequenceNumber()));
                if (!unchanged && mapper.updateTurnOrderSequence(group.getGroupId(), entry.memberId(), entry.sequenceNumber()) != 1)
                    throw conflict("회원 상태가 변경되어 전체 순번을 저장하지 못했습니다.");
            }
        } else {
            var plan = new SavingGroupTurnOrderPlanRow(); plan.setSavingGroupId(id);
            plan.setEffectiveMonth(effectiveMonth); plan.setStartRoundNumber(start); plan.setAssignedBy(user);
            mapper.insertTurnOrderPlan(plan);
            if (plan.getTurnOrderPlanId() == null) throw conflict("예약 목록을 저장하지 못했습니다.");
            mapper.deleteTurnOrderPlanMembers(plan.getTurnOrderPlanId());
            // AI작성: 새 예약에도 이미 확정된 과거·현재 순번을 이어서 보존한다.
            for (var memberId : fixed) {
                Integer sequence = current.get(memberId);
                if (sequence != null && sequence < start
                        && mapper.insertTurnOrderPlanMember(plan.getTurnOrderPlanId(), Long.valueOf(memberId), sequence) != 1)
                    throw conflict("고정 순번을 보존하지 못했습니다.");
            }
            for (var entry : request.turnOrders()) if (mapper.insertTurnOrderPlanMember(plan.getTurnOrderPlanId(), entry.memberId(), entry.sequenceNumber()) != 1)
                throw conflict("예약 순번을 모두 저장하지 못했습니다.");
        }
        return response(group, today);
    }
    // AI작성: 이번 달 예약이 있으면 해당 예약에서 현재 지급 회차의 계원을 찾는다.
    private Map<String, Integer> currentAssignments(List<SavingGroupTurnOrderAssignmentRow> members,
            SavingGroupTurnOrderPlanRow plan) {
        if (plan != null) return assignments(plan);
        return members.stream().filter(member -> member.getSequenceNumber() != null).collect(Collectors.toMap(
                SavingGroupTurnOrderAssignmentRow::getMemberId, SavingGroupTurnOrderAssignmentRow::getSequenceNumber));
    }

    private Set<String> fixedMembers(List<SavingGroupTurnOrderAssignmentRow> members,
            Map<String, Integer> current, int start) {
        return SavingGroupTurnOrderPolicy.fixedMembers(members, SavingGroupTurnOrderAssignmentRow::getMemberId,
                SavingGroupTurnOrderAssignmentRow::isPaid, member -> current.get(member.getMemberId()), start);
    }

    private static ApiException badRequest() { return new ApiException(HttpStatus.BAD_REQUEST, "SAVING_GROUP_TURN_ORDER_INVALID", "재배정 대상 계원 전원에게 중복 없이 남은 회차를 배정해주세요."); }
    private SavingGroupTurnOrderResponse response(SavingGroupTurnOrderPlanRow group, LocalDate today) {
        int start = SavingGroupTurnOrderPolicy.nextRound(group.getStatus(), group.getStartDate(), today);
        var effectiveMonth = effectiveMonth(group, today);
        var members = mapper.selectTurnOrderAssignmentMembers(group.getGroupId());
        var currentPlan = mapper.selectTurnOrderPlan(group.getSavingGroupId(), today.withDayOfMonth(1));
        var targetPlan = mapper.selectTurnOrderPlan(group.getSavingGroupId(), effectiveMonth);
        var current = currentAssignments(members, currentPlan);
        var planned = assignments(targetPlan);
        boolean exact = targetPlan != null && targetPlan.getEffectiveMonth().equals(effectiveMonth);
        var fixed = fixedMembers(members, current, start);
        var eligible = members.stream().map(SavingGroupTurnOrderAssignmentRow::getMemberId)
                .filter(memberId -> !fixed.contains(memberId)).collect(Collectors.toSet());
        var remaining = planned.entrySet().stream().filter(e -> e.getValue() >= start)
                .map(Map.Entry::getKey).collect(Collectors.toSet());
        var rows = members.stream().map(m -> new SavingGroupTurnOrderResponse.Member(m.getMemberId(), m.getNickname(), m.getRole(), m.isPaid(),
                current.get(m.getMemberId()), exact ? planned.get(m.getMemberId()) : null,
                m.isPaid() ? "PAID" : fixed.contains(m.getMemberId()) ? "CURRENT_ROUND" : null)).toList();
        return new SavingGroupTurnOrderResponse(effectiveMonth, start, start > 1,
                start > 1 && eligible.size() >= 2 && (!exact || !remaining.equals(eligible)), rows,
                start > 1 ? start - 1 : null);
    }
    private Map<String, Integer> assignments(SavingGroupTurnOrderPlanRow plan) {
        if (plan == null) return Map.of();
        return mapper.selectTurnOrderPlanMembers(plan.getTurnOrderPlanId()).stream().collect(Collectors.toMap(
                SavingGroupTurnOrderAssignmentRow::getMemberId, SavingGroupTurnOrderAssignmentRow::getSequenceNumber));
    }

    // AI작성: 기존 지급 Service의 모임 잠금 트랜잭션 안에서 호출한다. 월별 예약을 지연 조회하므로 스케줄러 지연이 없다.
    static Integer resolveSequence(SavingGroupMapper mapper, Long groupId, Long memberId, Integer legacy, int round) {
        if (mapper.existsCompletedTurnOrderPayout(groupId, memberId)) throw conflict("이미 지급 완료된 회원은 다시 지급받을 수 없습니다.");
        Long savingId = mapper.selectTurnOrderSavingGroupId(groupId);
        if (savingId == null || savingId <= 0) return legacy;
        var group = mapper.selectTurnOrderGroupForUpdate(savingId);
        if (group == null || group.getStartDate() == null) throw conflict("계모임 시작일을 확인해주세요.");
        // AI작성: 지난 회차의 심사에는 그 회차 당시 적용월을 사용하여 미래 예약이 과거 요청을 바꾸지 않게 한다.
        var month = YearMonth.from(group.getStartDate()).plusMonths(round - 1L).atDay(1);
        var plan = mapper.selectTurnOrderPlan(savingId, month);
        if (plan == null) return legacy;
        var members = mapper.selectTurnOrderAssignmentMembers(groupId);
        var eligible = members.stream().filter(m -> !m.isPaid()).map(SavingGroupTurnOrderAssignmentRow::getMemberId).collect(Collectors.toSet());
        var rows = mapper.selectTurnOrderPlanMembers(plan.getTurnOrderPlanId());
        var unpaid = rows.stream().filter(r -> eligible.contains(r.getMemberId())).map(SavingGroupTurnOrderAssignmentRow::getMemberId).collect(Collectors.toSet());
        var currentIds = members.stream().map(SavingGroupTurnOrderAssignmentRow::getMemberId).collect(Collectors.toSet());
        if (!unpaid.equals(eligible) || rows.stream().anyMatch(r -> !currentIds.contains(r.getMemberId()) && r.getSequenceNumber() >= round))
            throw conflict("모임원이 변경되어 순번 재지정이 필요합니다.");
        if (rows.stream().anyMatch(r -> r.getSequenceNumber() >= round && members.stream()
                .anyMatch(m -> m.getMemberId().equals(r.getMemberId()) && m.isPaid())))
            throw conflict("예약 이후 지급 상태가 변경되어 순번 재지정이 필요합니다.");
        // AI작성: 미지급 과거 회차는 자동 이월하지 않는다. 기존 요청 처리 정책을 별도로 확인한다.
        return rows.stream().filter(r -> r.getMemberId().equals(memberId.toString())).map(SavingGroupTurnOrderAssignmentRow::getSequenceNumber).findFirst().orElse(null);
    }
}
