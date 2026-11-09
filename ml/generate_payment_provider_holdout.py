"""결제대행사 감지 성능을 별도로 확인하는 합성 holdout 데이터를 생성합니다."""

import csv
import random
from pathlib import Path


OUTPUT_PATH = Path(__file__).resolve().parent / "expense_payment_provider_holdout_v1_100.csv"
RANDOM_SEED = 20261105
RECORD_COUNT = 100

# AI작성: 실제 구매 항목을 알 수 없는 결제대행사 표기만 넣어 지출 카테고리 학습에는 사용하지 않는다.
MERCHANT_PATTERNS = (
    "네이버페이",
    "네이버페이 주문",
    "네이버페이 스마트스토어",
    "카카오페이",
    "카카오페이 결제",
    "토스페이",
    "토스페이 결제",
    "PAYCO",
    "페이코",
    "KG이니시스",
    "KG이니시스 결제",
    "다날",
    "다날 결제",
    "스마트로",
    "스마트로 결제",
)


def main() -> None:
    random_generator = random.Random(RANDOM_SEED)
    records = []
    for index in range(1, RECORD_COUNT + 1):
        records.append({
            "record_id": f"payment-provider-{index:03d}",
            "merchant_name": random_generator.choice(MERCHANT_PATTERNS),
            "amount": random_generator.randrange(1_000, 300_001, 100),
            "expected_route": "PAYMENT_PROVIDER",
        })

    with OUTPUT_PATH.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=records[0].keys())
        writer.writeheader()
        writer.writerows(records)

    print(f"결제대행사 holdout {len(records)}건 생성: {OUTPUT_PATH}")


if __name__ == "__main__":
    main()
