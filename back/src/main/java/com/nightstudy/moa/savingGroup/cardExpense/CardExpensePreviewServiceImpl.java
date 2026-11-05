package com.nightstudy.moa.savingGroup.cardExpense;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI작성: 업로드 원본을 저장하거나 확정하지 않고, 파싱·자동 분류 결과만 사용자 검토용으로 조합한다.
 */
@Service
@RequiredArgsConstructor
public class CardExpensePreviewServiceImpl implements CardExpensePreviewService {
    private final CardExpenseStatementParser statementParser;
    private final CardExpenseClassifierClient classifierClient;
    private final SavingGroupMapper savingGroupMapper;

    @Override
    public List<CardExpensePreview> createPreview(Long groupId, Long memberId, MultipartFile file) {
        validateFinancialManager(groupId, memberId);
        return statementParser.parse(file).stream()
                .map(this::createPreviewRow)
                .toList();
    }

    private void validateFinancialManager(Long groupId, Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
        }
        List<String> roles = savingGroupMapper.selectApprovedMemberRoles(groupId, memberId);
        if (roles == null || roles.stream().noneMatch(role -> "OWNER".equals(role) || "MANAGER".equals(role))) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "SAVING_GROUP_FINANCIAL_MANAGER_REQUIRED",
                    "계주 또는 회계 담당자만 카드 지출을 분류할 수 있습니다."
            );
        }
    }

    private CardExpensePreview createPreviewRow(CardExpenseParsedRow row) {
        if (row.getApprovalStatus() == CardExpenseApprovalStatus.CANCELED) {
            return new CardExpensePreview(
                    row.getUsedDate(),
                    row.getMerchantName(),
                    row.getAmount(),
                    row.getApprovalStatus(),
                    null,
                    null,
                    CardExpenseClassificationStatus.CANCEL_PENDING,
                    null,
                    "취소 건 처리 기준이 정해지기 전까지 통계에 반영하지 않습니다."
            );
        }

        CardExpenseClassifierClient.ClassificationResult result = classifierClient.classify(
                row.getMerchantName(),
                row.getAmount()
        );
        return new CardExpensePreview(
                row.getUsedDate(),
                row.getMerchantName(),
                row.getAmount(),
                row.getApprovalStatus(),
                result.suggestedCategory(),
                result.confidence(),
                result.classificationStatus(),
                result.classificationSource(),
                result.reviewReason()
        );
    }
}
