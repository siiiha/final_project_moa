"""엑셀 열 매핑 이후 사용할 카드 지출 행 단위 분류 함수입니다."""

from typing import Any

from predict_expense_category import predict_expense_category


def classify_card_expense_row(row: dict[str, Any]) -> dict[str, Any]:
    """카드 지출 행을 분류 후보와 사용자 검토 상태로 변환합니다."""
    merchant_name = str(row.get("merchant_name", "")).strip()
    approval_status = str(row.get("approval_status", "")).strip()
    amount = row.get("amount")

    if not merchant_name:
        raise ValueError("가맹점명이 필요합니다.")
    if not isinstance(amount, (int, float)) or amount < 0:
        raise ValueError("이용금액은 0 이상의 숫자여야 합니다.")

    # AI작성: 취소 처리 기준이 확정되지 않았으므로 취소 건은 분류·통계에서 보류합니다.
    if "취소" in approval_status:
        return {
            "merchant_name": merchant_name,
            "amount": amount,
            "approval_status": approval_status,
            "suggested_category": None,
            "review_status": "처리 보류",
            "review_reason": "카드 결제 취소 처리 기준이 아직 정해지지 않았습니다.",
            "is_confirmed": False,
        }

    result = predict_expense_category(merchant_name)
    return {
        "merchant_name": merchant_name,
        "amount": amount,
        "approval_status": approval_status,
        **result,
    }


if __name__ == "__main__":
    # AI작성: 실제 카드사 엑셀 열이 확정되기 전, 행 분류 핵심 로직만 가상 데이터로 확인합니다.
    examples = [
        {"merchant_name": "동네 미술재료", "amount": 42_000, "approval_status": "승인"},
        {"merchant_name": "네이버페이 주문", "amount": 36_400, "approval_status": "승인"},
        {"merchant_name": "그린호텔", "amount": 140_000, "approval_status": "승인"},
        {"merchant_name": "사강식당", "amount": 58_000, "approval_status": "취소"},
    ]
    for example in examples:
        print(classify_card_expense_row(example))
