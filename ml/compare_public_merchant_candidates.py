"""공개 실제 장소 후보와 현재 분류 모델의 예측 차이를 검토합니다."""

import csv
from pathlib import Path

from predict_expense_category import predict_expense_category


BASE_DIR = Path(__file__).resolve().parent
INPUT_PATH = BASE_DIR / "expense_public_merchant_review_v2.csv"
OUTPUT_PATH = BASE_DIR / "expense_public_merchant_prediction_review_v1.csv"


def build_rows() -> list[dict[str, str]]:
    """AI작성: 장소 업종 제안과 모델 예측의 불일치만 사람이 먼저 살펴보도록 남깁니다."""
    rows: list[dict[str, str]] = []
    with INPUT_PATH.open(encoding="utf-8-sig", newline="") as file:
        for candidate in csv.DictReader(file):
            prediction = predict_expense_category(candidate["merchant_name"])
            predicted_category = prediction["suggested_category"] or "확인 필요"
            if candidate["suggested_category"] == predicted_category:
                continue
            rows.append({
                "merchant_name": candidate["merchant_name"],
                "public_suggested_category": candidate["suggested_category"],
                "model_suggested_category": predicted_category,
                "model_confidence": str(prediction["confidence"] or ""),
                "model_review_status": str(prediction["review_status"]),
                "model_source": str(prediction["prediction_source"]),
                "review_decision": "검토 대기",
                "source": candidate["source"],
                "source_file": candidate["source_file"],
            })
    return rows


def write_rows(rows: list[dict[str, str]]) -> None:
    with OUTPUT_PATH.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=(
            "merchant_name", "public_suggested_category", "model_suggested_category",
            "model_confidence", "model_review_status", "model_source", "review_decision",
            "source", "source_file",
        ))
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    result_rows = build_rows()
    write_rows(result_rows)
    print(f"불일치 후보 {len(result_rows)}건 저장: {OUTPUT_PATH.name}")
