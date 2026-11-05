package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.notification.dto.NotificationType;
import com.nightstudy.moa.notification.service.NotificationService;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

// AI작성: 인증・유형・역할・순번・중복 검증을 한 트랜잭션에서 수행하고 금융 원장은 변경하지 않는다.
@Service
public class SavingGroupPayoutServiceImpl implements SavingGroupPayoutService {
    private final SavingGroupMapper mapper;
    private final SavingGroupReceiptStorage storage;
    private final NotificationService notificationService;
    public SavingGroupPayoutServiceImpl(SavingGroupMapper mapper, SavingGroupReceiptStorage storage,
            NotificationService notificationService) {
        this.mapper = mapper;
        this.storage = storage;
        this.notificationService = notificationService;
    }
    private ApiException error(HttpStatus status, String code, String message) { return new ApiException(status, code, message); }
    private SavingGroupPayoutTargetRow target(Long groupId, Long memberId) {
        if (memberId == null || memberId <= 0) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (groupId == null || groupId <= 0) throw error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호가 올바르지 않습니다.");
        var group = mapper.selectPayoutTargetForUpdate(groupId);
        if (group == null) throw error(HttpStatus.NOT_FOUND, "NOT_FOUND", "계모임을 찾을 수 없습니다.");
        return group;
    }
    private String membership(Long groupId, Long memberId) {
        var relation = mapper.selectSavingGroupMembershipForUpdate(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || !List.of("OWNER", "MANAGER", "MEMBER").contains(relation.getMemberRole() == null ? "" : relation.getMemberRole()))
            throw error(HttpStatus.FORBIDDEN, "PAYOUT_FORBIDDEN", "현재 승인된 모임원만 이용할 수 있습니다.");
        return relation.getMemberRole();
    }
    private record Context(SavingGroupPayoutTargetRow group, SavingGroupPayoutRoundRow round) {}
    private Context context(Long groupId, Long memberId) {
        var group = target(groupId, memberId);
        String role = membership(groupId, memberId);
        if (!"02".equals(group.getStatus())) throw error(HttpStatus.CONFLICT, "PAYOUT_INACTIVE", "진행 중인 계모임에서만 요청할 수 있습니다.");
        if ("친목계".equals(group.getGroupType())) {
            if (!List.of("OWNER", "MANAGER").contains(role)) throw error(HttpStatus.FORBIDDEN, "PAYOUT_FORBIDDEN", "계주 또는 회계만 요청할 수 있습니다.");
            return new Context(group, null);
        }
        if (!"순번계".equals(group.getGroupType())) throw error(HttpStatus.CONFLICT, "PAYOUT_INVALID_TYPE", "계모임 유형을 확인해주세요.");
        var today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (group.getStartDate() == null || today.isBefore(group.getStartDate()))
            throw error(HttpStatus.CONFLICT, "PAYOUT_NOT_STARTED", "아직 계모임이 시작되지 않았습니다.");
        int number = Math.toIntExact(ChronoUnit.MONTHS.between(YearMonth.from(group.getStartDate()), YearMonth.from(today)) + 1);
        var round = mapper.selectPayoutRoundForUpdate(groupId, number);
        if (round == null) throw error(HttpStatus.CONFLICT, "PAYOUT_ROUND_MISSING", "현재 회차가 준비되지 않았습니다.");
        if (!Integer.valueOf(number).equals(mapper.selectPayoutSequence(groupId, memberId)))
            throw error(HttpStatus.FORBIDDEN, "PAYOUT_NOT_YOUR_TURN", "현재 본인 순번인 회원만 요청할 수 있습니다.");
        validateSnapshot(round);
        if (mapper.existsBlockingRoundPayout(round.getRoundId())) throw error(HttpStatus.CONFLICT, "PAYOUT_DUPLICATE", "이미 요청되었거나 지급 완료된 회차입니다.");
        return new Context(group, round);
    }
    // AI작성: 보존된 값의 정합성만 검증한다. 현재 인원·회비나 지급 이력으로 과거 회차를 재산정하지 않는다.
    private void validateSnapshot(SavingGroupPayoutRoundRow r) {
        boolean valid = r.getBaseDuesAmount() != null && r.getBaseDuesAmount().signum() > 0
                && r.getBaseDuesAmount().stripTrailingZeros().scale() <= 0
                && r.getApprovedMemberCount() != null && r.getApprovedMemberCount() > 0
                && r.getPreviousPaidMemberCount() != null && r.getPreviousPaidMemberCount() >= 0
                && r.getPreviousPaidMemberCount() <= r.getApprovedMemberCount()
                && r.getAdditionalDuesAmount() != null && r.getPayoutAmount() != null;
        if (valid) {
            var extra = r.getBaseDuesAmount().multiply(new BigDecimal("0.01")).setScale(0, RoundingMode.DOWN);
            var total = r.getBaseDuesAmount().multiply(BigDecimal.valueOf(r.getApprovedMemberCount()))
                    .add(extra.multiply(BigDecimal.valueOf(r.getPreviousPaidMemberCount())));
            valid = extra.compareTo(r.getAdditionalDuesAmount()) == 0 && total.compareTo(r.getPayoutAmount()) == 0
                    && total.compareTo(new BigDecimal("9999999999999")) <= 0;
        }
        if (!valid) throw error(HttpStatus.CONFLICT, "PAYOUT_SNAPSHOT_MISSING", "회차의 확정 금액을 확인해주세요.");
    }
    private SavingGroupPayoutResponse response(Context ctx, Long id) {
        return new SavingGroupPayoutResponse(ctx.group().getGroupType(), ctx.round() == null ? null : ctx.round().getRoundNumber(),
                ctx.round() == null ? null : ctx.round().getPayoutAmount(), ctx.round() == null, id, id == null ? null : "PENDING");
    }
    @Override @Transactional
    public SavingGroupPayoutResponse getPayoutOptions(Long groupId, Long memberId) { return response(context(groupId, memberId), null); }

    @Override @Transactional
    public SavingGroupPayoutResponse createPayout(Long groupId, Long memberId, SavingGroupPayoutRequest request, MultipartFile receipt) {
        var ctx = context(groupId, memberId);
        var row = new SavingGroupPayoutInsertRow();
        row.setGroupId(groupId); row.setRequesterId(memberId);
        if (ctx.round() != null) {
            if (receipt != null || (request != null && (request.getAmount() != null || request.getTitle() != null)))
                throw error(HttpStatus.BAD_REQUEST, "PAYOUT_INVALID_INPUT", "순번계의 금액은 서버에서 결정하며 영수증은 받지 않습니다.");
            row.setRoundId(ctx.round().getRoundId()); row.setAmount(ctx.round().getPayoutAmount());
            if (mapper.insertRoundPayout(row) != 1 || row.getId() == null) throw error(HttpStatus.INTERNAL_SERVER_ERROR, "PAYOUT_SAVE_FAILED", "요청 저장에 실패했습니다.");
        } else {
            if (request == null || request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ONE) < 0
                    || request.getAmount().stripTrailingZeros().scale() > 0 || request.getAmount().compareTo(new BigDecimal("9999999999999")) > 0
                    || request.getTitle() == null || request.getTitle().isBlank() || request.getTitle().length() > 100)
                throw error(HttpStatus.BAD_REQUEST, "PAYOUT_INVALID_INPUT", "사용 금액과 100자 이하 제목을 확인해주세요.");
            var prepared = storage.prepare(receipt);
            if (mapper.existsBlockingReceiptExpense(groupId, prepared.hash())) throw error(HttpStatus.CONFLICT, "PAYOUT_DUPLICATE", "이미 요청한 영수증입니다.");
            row.setAmount(request.getAmount()); row.setTitle(request.getTitle().trim()); row.setReceiptSha256(prepared.hash());
            String name = storage.store(prepared);
            row.setReceiptImgUrl(name);
            // AI작성: DB 커밋 실패를 포함해 롤백되면 신규 파일도 정리한다. 기존 거절 요청의 증빙은 보존한다.
            boolean synchronization = TransactionSynchronizationManager.isSynchronizationActive();
            if (synchronization) TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) storage.delete(name); }
            });
            try {
                if (mapper.insertReceiptExpense(row) != 1 || row.getId() == null) throw error(HttpStatus.INTERNAL_SERVER_ERROR, "PAYOUT_SAVE_FAILED", "요청 저장에 실패했습니다.");
            } catch (RuntimeException e) { if (!synchronization) storage.delete(name); throw e; }
        }
        // AI작성: 저장이 완료된 뒤 현재 심사 권한이 있는 승인 계주에게 새 요청을 알린다.
        mapper.selectSavingGroupPayoutReviewerIds(groupId).stream()
                .filter(reviewerId -> !reviewerId.equals(memberId))
                .forEach(reviewerId -> notificationService.send(
                        reviewerId,
                        NotificationType.SAVING_GROUP_PAYOUT_REQUESTED,
                        "새로운 계금·지출 요청이 등록되었습니다.",
                        "/saving-groups/" + groupId
                ));
        return new SavingGroupPayoutResponse(ctx.group().getGroupType(), ctx.round() == null ? null : ctx.round().getRoundNumber(),
                row.getAmount(), ctx.round() == null, row.getId(), "PENDING");
    }
    @Override @Transactional
    public Resource getReceipt(Long groupId, Long expenseId, Long memberId) {
        target(groupId, memberId);
        if (!"OWNER".equals(membership(groupId, memberId))) throw error(HttpStatus.FORBIDDEN, "RECEIPT_FORBIDDEN", "계주만 영수증을 열람할 수 있습니다.");
        if (expenseId == null || expenseId <= 0) throw error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "지출 번호를 확인해주세요.");
        String name = mapper.selectPayoutReceiptForOwner(groupId, expenseId);
        if (name == null) throw error(HttpStatus.NOT_FOUND, "RECEIPT_NOT_FOUND", "영수증을 찾을 수 없습니다.");
        return storage.read(name);
    }
}
