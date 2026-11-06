"""OpenStreetMap 공개 장소명으로 카드 지출 분류 검토 후보를 수집합니다."""

import csv
import json
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen


OVERPASS_URL = "https://overpass.kumi.systems/api/interpreter"
OUTPUT_PATH = Path(__file__).resolve().parent / "expense_osm_merchant_candidates_v2.csv"

# AI작성: 카테고리별로 따로 조회해 특정 업종이 응답 상한을 독점하지 않게 합니다.
QUERY_BY_CATEGORY = {
    "교통비": """
[out:json][timeout:60];
nwr["amenity"="parking"](37.50,126.90,37.60,127.10);
out tags 60;
""".strip(),
    "대관료": """
[out:json][timeout:60];
nwr["amenity"~"^(conference_centre|events_venue)$"](37.50,126.90,37.60,127.10);
out tags 60;
""".strip(),
    "물품비": """
[out:json][timeout:60];
nwr["shop"~"^(stationery|copyshop)$"](37.50,126.90,37.60,127.10);
out tags 60;
""".strip(),
}


def suggested_category(tags: dict[str, str]) -> str | None:
    """OSM 공개 업종 태그를 MOA 분류 후보로만 연결합니다."""
    if tags.get("amenity") == "parking":
        return "교통비"
    if tags.get("amenity") in {"conference_centre", "events_venue"}:
        return "대관료"
    if tags.get("shop") in {"stationery", "copyshop"}:
        return "물품비"
    return None


def fetch_elements(query: str) -> list[dict]:
    request = Request(
        OVERPASS_URL,
        data=urlencode({"data": query}).encode("utf-8"),
        headers={"User-Agent": "MOA-expense-category-study/0.1"},
        method="POST",
    )
    with urlopen(request, timeout=90) as response:
        payload = json.load(response)
    return payload.get("elements", [])


def build_rows(category: str, elements: list[dict], seen_names: set[str]) -> list[dict[str, str]]:
    """AI작성: 카드 결제 여부를 알 수 없으므로 모든 실제 장소 후보는 사람 검토 대상으로 남깁니다."""
    retrieved_at = datetime.now(timezone.utc).isoformat()
    rows: list[dict[str, str]] = []
    for element in elements:
        tags = element.get("tags", {})
        merchant_name = tags.get("name", "").strip()
        inferred_category = suggested_category(tags)
        if not merchant_name or inferred_category != category or merchant_name in seen_names:
            continue
        seen_names.add(merchant_name)
        rows.append({
            "merchant_name": merchant_name,
            "suggested_category": inferred_category,
            "review_status": "확인 필요",
            "source": "OpenStreetMap contributors (ODbL)",
            "source_tag": " / ".join(
                f"{key}={tags[key]}" for key in ("amenity", "tourism", "shop") if key in tags
            ),
            "retrieved_at_utc": retrieved_at,
        })
    return rows


def write_csv(rows: list[dict[str, str]]) -> None:
    with OUTPUT_PATH.open("w", newline="", encoding="utf-8-sig") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=[
            "merchant_name", "suggested_category", "review_status", "source", "source_tag", "retrieved_at_utc",
        ])
        writer.writeheader()
        writer.writerows(rows)


if __name__ == "__main__":
    candidate_rows = []
    seen_merchant_names: set[str] = set()
    for category_name, query in QUERY_BY_CATEGORY.items():
        candidate_rows.extend(build_rows(category_name, fetch_elements(query), seen_merchant_names))
    write_csv(candidate_rows)
    print(f"실제 장소 후보 {len(candidate_rows)}건 저장: {OUTPUT_PATH}")
