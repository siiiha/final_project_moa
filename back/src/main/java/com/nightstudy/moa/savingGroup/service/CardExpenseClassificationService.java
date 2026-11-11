package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseCategory;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassificationSource;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassificationStatus;
import com.nightstudy.moa.savingGroup.cardExpense.CardExpenseClassifierClient;
import com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionClassificationRow;
import com.nightstudy.moa.savingGroup.dto.SavingGroupTransactionRow;
import com.nightstudy.moa.savingGroup.mapper.SavingGroupMapper;
import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardExpenseClassificationService {
    private final SavingGroupMapper savingGroupMapper;
    private final CardExpenseClassifierClient classifierClient;
    // AI작성: 같은 거래가 목록과 대시보드에서 동시에 조회되어도 Python 분류를 한 번만 요청한다.
    private final Set<Long> pendingTransactionIds = ConcurrentHashMap.newKeySet();

    public CardExpenseClassificationService(
            SavingGroupMapper savingGroupMapper,
            CardExpenseClassifierClient classifierClient
    ) {
        this.savingGroupMapper = savingGroupMapper;
        this.classifierClient = classifierClient;
    }

    // AI작성: 바깥의 읽기 전용 트랜잭션을 중단하고 분류 조회·생성·저장을 독립 쓰기 트랜잭션에서 처리한다.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CardExpenseClassifierClient.ClassificationResult classify(SavingGroupTransactionRow row) {
        return classifyAndSave(row);
    }

    // AI작성: 조회 경로는 저장된 결과만 즉시 사용하고, 새 분류는 HTTP 응답과 분리해 처리한다.
    @Transactional(readOnly = true)
    public CardExpenseClassifierClient.ClassificationResult findStoredClassification(SavingGroupTransactionRow row) {
        if (!isClassifiableMockExpense(row) || row.transactionId() == null) {
            return null;
        }
        try {
            SavingGroupTransactionClassificationRow saved =
                    savingGroupMapper.selectSavingGroupTransactionClassification(row.transactionId());
            return saved == null ? null : toStoredClassification(saved);
        } catch (ApiException exception) {
            return null;
        }
    }

    // AI작성: 조회 요청을 기다리지 않도록 Python 호출과 저장을 별도 트랜잭션에서 실행한다.
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void classifyAndSaveAsync(SavingGroupTransactionRow row) {
        if (!isClassifiableMockExpense(row) || row.transactionId() == null
                || !pendingTransactionIds.add(row.transactionId())) {
            return;
        }
        try {
            classifyAndSave(row);
        } finally {
            pendingTransactionIds.remove(row.transactionId());
        }
    }

    // AI작성: 카테고리 수정 명령에서는 기존 쓰기 트랜잭션에 참여해 전체 변경의 원자성을 유지한다.
    @Transactional
    public CardExpenseClassifierClient.ClassificationResult classifyInCurrentTransaction(SavingGroupTransactionRow row) {
        return classifyAndSave(row);
    }

    private CardExpenseClassifierClient.ClassificationResult classifyAndSave(SavingGroupTransactionRow row) {
        if (!isClassifiableMockExpense(row)) {
            return null;
        }
        try {
            SavingGroupTransactionClassificationRow saved = row.transactionId() == null ? null
                    : savingGroupMapper.selectSavingGroupTransactionClassification(row.transactionId());
            if (saved != null) {
                return toStoredClassification(saved);
            }

            CardExpenseClassifierClient.ClassificationResult classification =
                    classifierClient.classify(row.description().strip(), row.amount());
            if (row.transactionId() != null) {
                savingGroupMapper.insertSavingGroupTransactionClassification(new SavingGroupTransactionClassificationRow(
                        row.transactionId(),
                        classification.suggestedCategory() == null ? null : classification.suggestedCategory().name(),
                        classification.classificationStatus().name(),
                        classification.confidence() == null ? null : BigDecimal.valueOf(classification.confidence()),
                        classification.classificationSource().name(), classification.reviewReason(), null));
            }
            return classification;
        } catch (ApiException exception) {
            // AI작성: Python 분류 장애는 거래내역 조회 전체를 실패시키지 않고 미분류 상태로 반환한다.
            return null;
        }
    }

    private boolean isClassifiableMockExpense(SavingGroupTransactionRow row) {
        return "EXPENSE".equals(row.purposeType()) && "MOCK".equals(row.sourceProvider());
    }

    // AI작성: 저장값은 Python 응답과 같은 타입으로 복원해 기존 거래내역·대시보드 계산식을 재사용한다.
    private CardExpenseClassifierClient.ClassificationResult toStoredClassification(
            SavingGroupTransactionClassificationRow classification) {
        try {
            return new CardExpenseClassifierClient.ClassificationResult(
                    classification.suggestedCategory() == null ? null
                            : CardExpenseCategory.valueOf(classification.suggestedCategory()),
                    classification.confidence() == null ? null : classification.confidence().doubleValue(),
                    CardExpenseClassificationStatus.valueOf(classification.classificationStatus()),
                    CardExpenseClassificationSource.valueOf(classification.predictionSource()),
                    classification.reviewReason());
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "CARD_EXPENSE_CLASSIFIER_RESPONSE_INVALID",
                    "저장된 자동 분류 결과를 확인할 수 없습니다.");
        }
    }
}
