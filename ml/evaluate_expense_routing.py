"""지출 항목 분류와 결제대행사 처리 경로를 분리해 평가합니다."""

import csv
from pathlib import Path

from predict_expense_category import predict_expense_category


EXPENSE_TEST_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v14_5645.csv"
PAYMENT_PROVIDER_TEST_PATH = Path(__file__).resolve().parent / "expense_payment_provider_holdout_v1_100.csv"


def load_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def main() -> None:
    expense_rows = [row for row in load_csv(EXPENSE_TEST_PATH) if row["split"] == "test"]
    expense_results = [
        (row, predict_expense_category(row["merchant_name"]))
        for row in expense_rows
    ]
    automatically_classified = [
        (row, result)
        for row, result in expense_results
        if result["review_status"] == "검토 대기"
    ]
    correct_categories = sum(
        row["confirmed_category"] == result["suggested_category"]
        for row, result in automatically_classified
    )

    payment_provider_rows = load_csv(PAYMENT_PROVIDER_TEST_PATH)
    correctly_routed_providers = sum(
        predict_expense_category(row["merchant_name"])["prediction_source"]
        == "payment_intermediary_rule"
        for row in payment_provider_rows
    )

    total_rows = len(expense_rows) + len(payment_provider_rows)
    total_successes = correct_categories + correctly_routed_providers

    print(f"지출 항목 자동 분류: {len(automatically_classified)}/{len(expense_rows)}건")
    print(f"지출 항목 자동 분류 정확도: {correct_categories}/{len(automatically_classified)}건 ({correct_categories / len(automatically_classified):.1%})")
    print(f"결제대행사 경로 감지: {correctly_routed_providers}/{len(payment_provider_rows)}건 ({correctly_routed_providers / len(payment_provider_rows):.1%})")
    print(f"전체 자동 처리 성공: {total_successes}/{total_rows}건 ({total_successes / total_rows:.1%})")


if __name__ == "__main__":
    main()
