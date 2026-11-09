"""계주가 내려받은 확정 카드 지출 CSV를 재학습 후보로 검사합니다.

이 스크립트는 모델을 학습·교체하지 않습니다. 결과 CSV와 항목별 건수를 확인한 뒤
기존 최종 검사 데이터와 분리된 검증 계획을 세운 경우에만 별도 재학습을 진행합니다.
"""

import csv
import sys
from collections import Counter
from decimal import Decimal, InvalidOperation
from pathlib import Path


ALLOWED_CATEGORIES = {"FOOD", "TRANSPORTATION", "RENTAL", "SUPPLIES", "ACTIVITY", "ACCOMMODATION", "FEE", "OTHER"}
REQUIRED_COLUMNS = {"merchant_name", "amount", "suggested_category", "final_category", "classification_source", "confidence_score", "confirmed_at"}


def validate_and_prepare(input_path: Path) -> list[dict[str, str]]:
    """AI작성: 사용자 확정 결과만 받아 취소·확인 필요·빈 값은 재학습 후보에서 제외한다."""
    with input_path.open(encoding="utf-8-sig", newline="") as file:
        reader = csv.DictReader(file)
        if reader.fieldnames is None or not REQUIRED_COLUMNS.issubset(reader.fieldnames):
            raise ValueError("확정 지출 CSV 형식이 올바르지 않습니다.")

        records: list[dict[str, str]] = []
        for row_number, row in enumerate(reader, start=2):
            merchant_name = row["merchant_name"].strip()
            final_category = row["final_category"].strip()
            if not merchant_name or final_category not in ALLOWED_CATEGORIES:
                continue
            try:
                amount = Decimal(row["amount"])
            except (InvalidOperation, ValueError):
                raise ValueError(f"{row_number}행의 금액이 올바르지 않습니다.") from None
            if amount <= 0:
                continue
            records.append({
                "merchant_name": merchant_name,
                "amount": str(amount),
                "confirmed_category": final_category,
                "suggested_category": row["suggested_category"].strip(),
                "classification_source": row["classification_source"].strip(),
                "confirmed_at": row["confirmed_at"].strip(),
            })
    return records


def write_candidate_csv(records: list[dict[str, str]], output_path: Path) -> None:
    with output_path.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=(
            "merchant_name", "amount", "confirmed_category", "suggested_category",
            "classification_source", "confirmed_at",
        ))
        writer.writeheader()
        writer.writerows(records)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("사용법: py -3 ml/prepare_confirmed_card_expense_training.py 입력.csv 출력.csv")

    prepared_records = validate_and_prepare(Path(sys.argv[1]))
    write_candidate_csv(prepared_records, Path(sys.argv[2]))
    counts = Counter(record["confirmed_category"] for record in prepared_records)
    print(f"재학습 검토 후보 {len(prepared_records)}건 저장")
    for category in sorted(ALLOWED_CATEGORIES):
        print(f"{category}: {counts[category]}건")
