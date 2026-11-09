package com.nightstudy.moa.savingGroup.cardExpense;

import com.nightstudy.moa.common.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI작성: 업로드 단계는 자동 분류 미리보기만 반환하며 DB 저장·통계 반영은 하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping({"/saving-groups", "/api/saving-groups"})
public class CardExpenseController {
    private final CardExpensePreviewService cardExpensePreviewService;
    private final CardExpenseUploadService cardExpenseUploadService;

    @PostMapping(
            value = "/{groupId}/card-expenses/preview",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<List<CardExpensePreview>> createPreview(
            @PathVariable Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(cardExpensePreviewService.createPreview(groupId, memberId, file));
    }

    // AI작성: 업로드 직후 자동 추천 결과를 저장하지만 사용자 확정 전에는 통계에 반영하지 않는다.
    @PostMapping(
            value = "/{groupId}/card-expenses/uploads",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<List<CardExpensePreview>> uploadAndClassify(
            @PathVariable Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.ok(cardExpenseUploadService.uploadAndClassify(groupId, memberId, file));
    }

    // AI작성: 최종 항목은 계주·회계가 직접 선택한 값으로만 확정한다.
    @PatchMapping("/{groupId}/card-expenses/{cardExpenseId}/classification")
    public ApiResponse<Void> confirmCategory(
            @PathVariable Long groupId,
            @PathVariable Long cardExpenseId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody CardExpenseClassificationConfirmRequest request
    ) {
        cardExpenseUploadService.confirmCategory(groupId, cardExpenseId, memberId, request);
        return ApiResponse.ok();
    }

    // AI작성: 행별 버튼 대신 목록 위 확정하기가 현재 선택된 모든 항목을 저장한다.
    @PatchMapping("/{groupId}/card-expenses/classifications")
    public ApiResponse<Void> confirmCategories(
            @PathVariable Long groupId,
            @RequestAttribute(name = "memberId", required = false) Long memberId,
            @Valid @RequestBody CardExpenseBatchConfirmationRequest request
    ) {
        cardExpenseUploadService.confirmCategories(groupId, memberId, request);
        return ApiResponse.ok();
    }
}
