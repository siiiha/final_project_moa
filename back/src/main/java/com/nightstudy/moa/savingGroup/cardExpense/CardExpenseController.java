package com.nightstudy.moa.savingGroup.cardExpense;

import com.nightstudy.moa.common.dto.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
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
}
