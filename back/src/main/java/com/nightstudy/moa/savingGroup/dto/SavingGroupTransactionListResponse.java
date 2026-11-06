package com.nightstudy.moa.savingGroup.dto;
import java.util.List;
import com.nightstudy.moa.common.dto.PageMeta;
import lombok.AllArgsConstructor;
import lombok.Getter;
// AI작성: 기존 목록과 동일한 배열 및 페이지 메타데이터를 전달한다.
@Getter @AllArgsConstructor
public class SavingGroupTransactionListResponse {
    private final List<SavingGroupTransactionResponse> items;
    private final PageMeta pageMeta;
}
