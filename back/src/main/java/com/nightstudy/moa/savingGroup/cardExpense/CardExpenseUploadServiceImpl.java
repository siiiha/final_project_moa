package com.nightstudy.moa.savingGroup.cardExpense;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI작성: 카드 지출은 자동 추천 단계에서 저장하되, 사용자 확정 전에는 통계 반영 상태로 바꾸지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CardExpenseUploadServiceImpl implements CardExpenseUploadService {
    private final CardExpenseStatementParser statementParser;
    private final CardExpenseClassifierClient classifierClient;
    private final CardExpenseMapper cardExpenseMapper;
    private final SavingGroupMapper savingGroupMapper;

    @Override
    @Transactional
    public List<CardExpensePreview> uploadAndClassify(Long groupId, Long memberId, MultipartFile file) {
        validateFinancialManager(groupId, memberId);
        CardExpenseUploadInsert upload = new CardExpenseUploadInsert(groupId, memberId);
        if (cardExpenseMapper.insertCardExpenseUpload(upload) != 1 || upload.getUploadId() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_EXPENSE_UPLOAD_SAVE_FAILED",
                    "카드 지출 업로드 정보를 저장하지 못했습니다.");
        }

        return statementParser.parse(file).stream()
                .map(row -> saveAndCreatePreview(upload.getUploadId(), row))
                .toList();
    }

    @Override
    @Transactional
    public void confirmCategory(Long groupId, Long cardExpenseId, Long memberId,
                                CardExpenseClassificationConfirmRequest request) {
        validateFinancialManager(groupId, memberId);
        CardExpenseConfirmationTarget target = cardExpenseMapper
                .selectCardExpenseConfirmationTargetForUpdate(cardExpenseId);
        if (target == null || !groupId.equals(target.getGroupId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CARD_EXPENSE_NOT_FOUND", "카드 지출 내역을 찾을 수 없습니다.");
        }
        if (cardExpenseMapper.confirmCardExpenseCategory(
                cardExpenseId, request.getFinalCategory(), memberId, LocalDateTime.now()) != 1) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_EXPENSE_CONFIRM_FAILED",
                    "카드 지출 항목을 확정하지 못했습니다.");
        }
    }

    @Override
    @Transactional
    public void confirmCategories(Long groupId, Long memberId, CardExpenseBatchConfirmationRequest request) {
        validateFinancialManager(groupId, memberId);
        for (CardExpenseBatchConfirmationItem item : request.getItems()) {
            confirmCategory(groupId, item.getCardExpenseId(), memberId,
                    toConfirmationRequest(item.getFinalCategory()));
        }
    }

    // AI작성: 금융 관리 권한과 양쪽 기록의 소속을 확인한 뒤 연결한다.
    @Override
    @Transactional
    public void linkTransaction(Long groupId, Long cardExpenseId, Long transactionId, Long memberId) {
        validateFinancialManager(groupId, memberId);
        if (cardExpenseId <= 0 || transactionId <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TRANSACTION_LINK", "연결할 거래 ID를 확인해 주세요.");
        }
        CardExpenseConfirmationTarget target = cardExpenseMapper
                .selectCardExpenseConfirmationTargetForUpdate(cardExpenseId);
        if (target == null || !groupId.equals(target.getGroupId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CARD_EXPENSE_NOT_FOUND", "카드 지출을 찾을 수 없습니다.");
        }
        if (target.getApprovalStatus() != CardExpenseApprovalStatus.APPROVED) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_EXPENSE_NOT_APPROVED", "승인된 카드 지출만 연결할 수 있습니다.");
        }
        if (cardExpenseMapper.selectWithdrawal(groupId, transactionId) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "WITHDRAWAL_NOT_FOUND", "모임의 출금 거래를 찾을 수 없습니다.");
        }
        if (transactionId.equals(target.getTransactionId())) {
            return;
        }
        if (target.getTransactionId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "CARD_EXPENSE_ALREADY_LINKED", "이미 다른 거래에 연결된 카드 지출입니다.");
        }
        try {
            cardExpenseMapper.linkTransaction(cardExpenseId, transactionId);
        } catch (org.springframework.dao.DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "TRANSACTION_ALREADY_LINKED", "이미 카드 지출에 연결된 출금 거래입니다.");
        }
    }

    private CardExpenseClassificationConfirmRequest toConfirmationRequest(CardExpenseCategory finalCategory) {
        CardExpenseClassificationConfirmRequest request = new CardExpenseClassificationConfirmRequest();
        request.setFinalCategory(finalCategory);
        return request;
    }

    private CardExpensePreview saveAndCreatePreview(Long uploadId, CardExpenseParsedRow row) {
        CardExpenseClassifierClient.ClassificationResult result = row.getApprovalStatus() == CardExpenseApprovalStatus.CANCELED
                ? null : classifierClient.classify(row.getMerchantName(), row.getAmount());
        CardExpenseInsert expense = new CardExpenseInsert(
                uploadId,
                row.getUsedDate(),
                row.getMerchantName(),
                row.getAmount(),
                row.getApprovalStatus(),
                result == null ? null : result.suggestedCategory(),
                result == null ? CardExpenseClassificationStatus.CANCEL_PENDING : result.classificationStatus(),
                result == null ? null : result.confidence(),
                result == null ? null : result.classificationSource(),
                result == null ? "취소 건 처리 기준이 정해지기 전까지 통계에 반영하지 않습니다." : result.reviewReason()
        );
        if (cardExpenseMapper.insertCardExpense(expense) != 1 || expense.getCardExpenseId() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CARD_EXPENSE_SAVE_FAILED",
                    "카드 지출 내역을 저장하지 못했습니다.");
        }
        return new CardExpensePreview(
                expense.getCardExpenseId(),
                expense.getUsedDate(),
                expense.getMerchantName(),
                expense.getAmount(),
                expense.getApprovalStatus(),
                expense.getSuggestedCategory(),
                expense.getConfidenceScore(),
                expense.getClassificationStatus(),
                expense.getClassificationSource(),
                expense.getReviewReason()
        );
    }

    private void validateFinancialManager(Long groupId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        List<String> roles = savingGroupMapper.selectApprovedMemberRoles(groupId, memberId);
        if (roles == null || roles.stream().noneMatch(role -> "OWNER".equals(role) || "MANAGER".equals(role))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "SAVING_GROUP_FINANCIAL_MANAGER_REQUIRED",
                    "계주 또는 회계 담당자만 카드 지출을 분류할 수 있습니다.");
        }
    }
}
