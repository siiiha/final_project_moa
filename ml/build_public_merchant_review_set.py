"""공개 장소 후보를 사람이 검토할 단일 목록으로 통합합니다."""

import csv
from pathlib import Path


BASE_DIR = Path(__file__).resolve().parent
OUTPUT_PATH = BASE_DIR / "expense_public_merchant_review_v2.csv"
SOURCE_FILES = (
    "expense_public_transport_candidates_v1.csv",
    "expense_public_rental_candidates_v1.csv",
    "expense_public_culture_candidates_v1.csv",
    "expense_public_restaurant_candidates_v1.csv",
    "expense_public_lodging_candidates_v1.csv",
)


def normalized_name(name: str) -> str:
    return "".join(name.lower().split())


def build_review_rows() -> list[dict[str, str]]:
    """AI작성: 실제 장소명은 카드 결제 상호와 다를 수 있어 모두 검토 대기로 둡니다."""
    review_rows: list[dict[str, str]] = []
    seen: set[tuple[str, str]] = set()

    for source_file in SOURCE_FILES:
        with (BASE_DIR / source_file).open(encoding="utf-8-sig", newline="") as file:
            for row in csv.DictReader(file):
                merchant_name = (row.get("merchant_name") or "").strip()
                category = (row.get("suggested_category") or "").strip()
                key = (normalized_name(merchant_name), category)
                if not merchant_name or not category or key in seen:
                    continue
                seen.add(key)
                review_rows.append({
                    "merchant_name": merchant_name,
                    "suggested_category": category,
                    "review_decision": "검토 대기",
                    "review_note": "실제 장소명이며 카드 명세서 표기·결제 목적 확인 필요",
                    "source": row.get("source", ""),
                    "source_tag": row.get("source_tag") or row.get("source_business_type", ""),
                    "source_file": source_file,
                })
    return review_rows


def write_review_csv(rows: list[dict[str, str]]) -> None:
    with OUTPUT_PATH.open("w", encoding="utf-8-sig", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=(
            "merchant_name", "suggested_category", "review_decision", "review_note",
            "source", "source_tag", "source_file",
        ))
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    rows = build_review_rows()
    write_review_csv(rows)
    print(f"검토용 실제 장소 후보 {len(rows)}건 저장: {OUTPUT_PATH.name}")
