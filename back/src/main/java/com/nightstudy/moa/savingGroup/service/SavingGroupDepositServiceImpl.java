package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 모임→회원→계좌→요청 잠금 순서로 내부 원장만 변경하며 외부 금융 빈을 참조하지 않는다.
@Service
public class SavingGroupDepositServiceImpl implements SavingGroupDepositService {
    private final SavingGroupMapper mapper;
    private final Runnable delay;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public SavingGroupDepositServiceImpl(SavingGroupMapper mapper) {
        this(mapper, () -> {
            try { Thread.sleep(ThreadLocalRandom.current().nextInt(300, 801)); }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "DEPOSIT_INTERRUPTED", "테스트 입금 처리가 중단되었습니다.");
            }
        });
    }
    // AI작성: 테스트에서는 지연만 대체하고 검증·거래 처리 로직은 그대로 실행한다.
    SavingGroupDepositServiceImpl(SavingGroupMapper mapper, Runnable delay) {
        this.mapper = mapper; this.delay = delay;
    }

    @Override @Transactional
    public SavingGroupDepositResponse deposit(Long groupId, Long memberId, SavingGroupDepositRequest request) {
        Long accountId = target(groupId, memberId);
        if (request == null || request.amount() == null || request.amount().signum() <= 0
                || request.amount().stripTrailingZeros().scale() > 0
                || request.amount().compareTo(new BigDecimal("9999999999999")) > 0 || !validKey(request.requestKey()))
            throw error(HttpStatus.BAD_REQUEST, "DEPOSIT_INVALID_INPUT", "금액과 입금 요청 정보를 확인해주세요.");
        LocalDateTime now = mapper.selectDepositNow();
        var existing = mapper.selectDepositRequestForUpdate(memberId, request.requestKey());
        if (existing != null) {
            if (!accountId.equals(existing.getAccountId()) || existing.getAmount().compareTo(request.amount()) != 0)
                throw error(HttpStatus.CONFLICT, "DEPOSIT_REQUEST_CONFLICT", "같은 요청 정보로 다른 입금을 등록할 수 없습니다.");
            if ("COMPLETED".equals(existing.getStatus())) return completed(groupId, existing);
            return pending(existing, now, false);
        }
        var row = new SavingGroupDepositRow();
        row.setAccountId(accountId); row.setMemberId(memberId); row.setAmount(request.amount());
        row.setRequestKey(request.requestKey()); row.setRequestedAt(now);
        var anchor = mapper.selectDepositAnchorForUpdate(accountId, memberId, request.amount(), now.minusMinutes(5));
        if (anchor != null) {
            row.setWindowAnchorRequestId(anchor.getDepositRequestId());
            return pending(row, now, true);
        }
        saveTransaction(groupId, row, now);
        row.setStatus("COMPLETED");
        requireOne(mapper.insertDepositRequest(row));
        return completed(groupId, row);
    }

    @Override @Transactional
    public SavingGroupDepositResponse confirm(Long groupId, Long memberId, SavingGroupDepositConfirmRequest request) {
        Long accountId = target(groupId, memberId);
        if (request == null || !validKey(request.requestKey()) || request.confirmationToken() == null
                || !request.confirmationToken().matches("[A-Za-z0-9_-]{43}"))
            throw error(HttpStatus.BAD_REQUEST, "DEPOSIT_INVALID_INPUT", "입금 확인 정보를 확인해주세요.");
        var row = mapper.selectDepositRequestForUpdate(memberId, request.requestKey());
        if (row == null || !accountId.equals(row.getAccountId()))
            throw error(HttpStatus.NOT_FOUND, "DEPOSIT_REQUEST_NOT_FOUND", "입금 확인 요청을 찾을 수 없습니다.");
        // AI작성: 인증된 동일 요청의 완료 재전송은 기존 결과만 반환하여 추가 거래를 막는다.
        if ("COMPLETED".equals(row.getStatus())) return completed(groupId, row);
        LocalDateTime now = mapper.selectDepositNow();
        if (!"PENDING_CONFIRMATION".equals(row.getStatus()) || row.getConfirmationExpiresAt() == null
                || !now.isBefore(row.getConfirmationExpiresAt()))
            throw error(HttpStatus.CONFLICT, "DEPOSIT_CONFIRMATION_EXPIRED", "확인 시간이 지났습니다. 입금 요청부터 다시 확인해주세요.");
        if (row.getConfirmationTokenHash() == null || !MessageDigest.isEqual(
                hash(request.confirmationToken()).getBytes(StandardCharsets.US_ASCII),
                row.getConfirmationTokenHash().getBytes(StandardCharsets.US_ASCII)))
            throw error(HttpStatus.FORBIDDEN, "DEPOSIT_CONFIRMATION_INVALID", "유효하지 않은 입금 확인 정보입니다.");
        saveTransaction(groupId, row, now);
        requireOne(mapper.completeDepositRequest(row));
        return completed(groupId, row);
    }

    private Long target(Long groupId, Long memberId) {
        if (memberId == null || memberId <= 0) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        if (groupId == null || groupId <= 0) throw error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "모임 번호가 올바르지 않습니다.");
        delay.run(); // AI작성: DB 잠금을 잡기 전에만 Mock 지연을 적용한다.
        var group = mapper.selectPayoutTargetForUpdate(groupId);
        if (group == null) throw error(HttpStatus.NOT_FOUND, "NOT_FOUND", "계모임을 찾을 수 없습니다.");
        var relation = mapper.selectSavingGroupMembershipForUpdate(groupId, memberId);
        if (relation == null || !"APPROVED".equals(relation.getMembershipStatus())
                || !List.of("OWNER", "MANAGER", "MEMBER").contains(relation.getMemberRole() == null ? "" : relation.getMemberRole())
                || !mapper.existsMember(memberId))
            throw error(HttpStatus.FORBIDDEN, "SAVING_GROUP_ACCOUNT_FORBIDDEN", "승인된 현재 모임원만 테스트 입금을 요청할 수 있습니다.");
        if (!"02".equals(group.getStatus()))
            throw error(HttpStatus.CONFLICT, "DEPOSIT_INACTIVE", "진행중인 계모임에서만 테스트 입금할 수 있습니다.");
        Long accountId = mapper.selectDepositAccountForUpdate(groupId);
        if (accountId == null) throw error(HttpStatus.NOT_FOUND, "DEPOSIT_ACCOUNT_NOT_FOUND", "입금할 공용 계좌가 없습니다.");
        return accountId;
    }

    private SavingGroupDepositResponse pending(SavingGroupDepositRow row, LocalDateTime now, boolean insert) {
        byte[] bytes = new byte[32]; random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        row.setStatus("PENDING_CONFIRMATION"); row.setConfirmationTokenHash(hash(token));
        row.setConfirmationExpiresAt(now.plusMinutes(5));
        requireOne(insert ? mapper.insertDepositRequest(row) : mapper.refreshDepositConfirmation(row));
        // AI작성: 자정 경계에서도 사실과 맞도록 '오늘' 대신 최근 5분 문구를 사용한다.
        return new SavingGroupDepositResponse("PENDING_CONFIRMATION", row.getAmount(), null, token,
                row.getConfirmationExpiresAt(), "최근 5분 이내 동일한 계좌로 동일한 금액을 보낸 적이 있습니다. 테스트 입금을 한 번 더 등록하시겠습니까?");
    }
    private void saveTransaction(Long groupId, SavingGroupDepositRow row, LocalDateTime now) {
        // AI작성: 기존 모임·계좌 잠금 안에서 원장 합산 잔액에 이번 입금액을 더한다.
        BigDecimal before = mapper.selectSavingGroupBalance(groupId);
        BigDecimal after = (before == null ? BigDecimal.ZERO : before).add(row.getAmount());
        if (after.abs().compareTo(new BigDecimal("9999999999999.99")) > 0)
            throw error(HttpStatus.CONFLICT, "DEPOSIT_BALANCE_OVERFLOW", "거래 후 잔액의 저장 가능 범위를 초과합니다.");
        row.setBalanceAfter(after);
        row.setCompletedAt(now);
        requireOne(mapper.insertDepositTransaction(row));
        if (row.getTransactionId() == null) throw error(HttpStatus.INTERNAL_SERVER_ERROR, "DEPOSIT_SAVE_FAILED", "테스트 입금 저장에 실패했습니다.");
    }
    private SavingGroupDepositResponse completed(Long groupId, SavingGroupDepositRow row) {
        BigDecimal balance = mapper.selectSavingGroupBalance(groupId);
        return new SavingGroupDepositResponse("COMPLETED", row.getAmount(), balance == null ? BigDecimal.ZERO : balance,
                null, null, "테스트 입금이 등록되었습니다. 실제 돈은 이동하지 않습니다.");
    }
    private void requireOne(int count) {
        if (count != 1) throw error(HttpStatus.CONFLICT, "DEPOSIT_SAVE_CONFLICT", "입금 처리 상태가 변경되었습니다. 같은 요청으로 다시 확인해주세요.");
    }
    private boolean validKey(String key) { return key != null && key.matches("[A-Za-z0-9_-]{16,64}"); }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("확인값 검증 알고리즘을 사용할 수 없습니다."); }
    }
    private ApiException error(HttpStatus status, String code, String message) { return new ApiException(status, code, message); }
}
