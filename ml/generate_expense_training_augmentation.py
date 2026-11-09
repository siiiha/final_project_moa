"""v12 데이터에 학습 전용 821건을 보강해 최종 학습 수를 5,000건으로 맞춥니다."""

import csv
import random
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path

from generate_expense_synthetic_data import AMOUNT_RANGES


SOURCE_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v14_5645.csv"
OUTPUT_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v15_5689.csv"
RANDOM_SEED = 20261105

# AI작성: 총 821건을 8개 항목에 102~103건씩 균형 보강한다.
TRAIN_AUGMENTATION_COUNTS = {
    "식비": 103, "교통비": 103, "대관료": 103, "물품비": 103,
    "활동비": 103, "숙박비": 102, "수수료": 102, "기타": 102,
}

# AI작성: 기존 검사·검증 분할에 없는 상호 계열만 학습에 추가한다.
TRAIN_AUGMENTATION_MERCHANTS = {
    "식비": ["모락한상", "정오의식탁", "담소키친", "미담국수"],
    "교통비": ["루트웨이", "모브링크", "시티패스24", "링크모빌"],
    "대관료": ["스페이스로", "모임플랜", "룸마루", "공유홀"],
    "물품비": ["에브리데이랩", "컨셉마켓", "픽앤준비", "스토어온"],
    "활동비": ["프로그램온", "라운드컬처", "아트모먼트", "플레이씬"],
    "숙박비": ["그린스테이", "테라하우스", "루미에르호텔", "브리즈인"],
    "수수료": ["예약링크", "서비스게이트", "티켓플로우", "정산포털"],
    "기타": ["생활수선", "프레임스토리", "꽃기록", "기념상점"],
}

# AI작성: 공개 데이터에서 확인한 실제 장소명 중 v13 학습에 없던 교통·대관 후보만 추가한다.
PUBLIC_ENRICHMENT_FILES = (
    "expense_public_transport_candidates_v1.csv",
    "expense_public_rental_candidates_v1.csv",
)
CARD_STATEMENT_VARIANTS = ("{}", "(주){}", "{} Co.,Ltd", "{} 강남점", "{} 중앙점")
OSM_CANDIDATE_PATH = Path(__file__).resolve().parent / "expense_osm_merchant_candidates_v1.csv"
OSM_MAXIMUMS = {"식비": 4, "활동비": 10, "숙박비": 30}


def load_records() -> list[dict[str, str]]:
    with SOURCE_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def build_augmentation(existing_records: list[dict[str, str]]) -> list[dict[str, str | int]]:
    random_generator = random.Random(RANDOM_SEED)
    start = datetime(2026, 1, 1, 9, 0)
    protected_names = {
        record["merchant_name"]
        for record in existing_records
        if record["split"] in {"validation", "test"}
    }
    records = []

    for category, count in TRAIN_AUGMENTATION_COUNTS.items():
        minimum, maximum = AMOUNT_RANGES[category]
        for _ in range(count):
            merchant_name = random_generator.choice(TRAIN_AUGMENTATION_MERCHANTS[category])
            display_style = random_generator.choice(("기본", "기본", "법인", "지점", "영문"))
            if display_style == "법인":
                merchant_name = f"(주){merchant_name}"
            elif display_style == "지점":
                merchant_name = f"{merchant_name} {random_generator.choice(('강남', '성수', '중앙', '서부'))}점"
            elif display_style == "영문":
                merchant_name = f"{merchant_name} Co.,Ltd"
            if merchant_name in protected_names:
                raise ValueError(f"검사·검증 가맹점과 중복됩니다: {merchant_name}")

            records.append({
                "split": "train",
                "merchant_name": merchant_name,
                "amount": random_generator.randrange(minimum, maximum + 1, 100),
                "transacted_at": (start + timedelta(minutes=random_generator.randint(0, 525_599))).strftime("%Y-%m-%d %H:%M"),
                "confirmed_category": category,
            })
    return records


def build_public_enrichment(existing_records: list[dict[str, str]]) -> list[dict[str, str | int]]:
    random_generator = random.Random(RANDOM_SEED + 1)
    start = datetime(2026, 1, 1, 9, 0)
    protected_names = {
        record["merchant_name"]
        for record in existing_records
        if record["split"] in {"validation", "test"}
    }
    records = []

    for file_name in PUBLIC_ENRICHMENT_FILES:
        with (Path(__file__).resolve().parent / file_name).open(encoding="utf-8-sig", newline="") as input_file:
            for candidate in csv.DictReader(input_file):
                merchant_name = candidate["merchant_name"]
                category = candidate["suggested_category"]
                minimum, maximum = AMOUNT_RANGES[category]
                for display_format in CARD_STATEMENT_VARIANTS:
                    displayed_name = display_format.format(merchant_name)
                    if displayed_name in protected_names:
                        raise ValueError(f"검사·검증 가맹점과 중복됩니다: {displayed_name}")
                    records.append({
                        "split": "train",
                        "merchant_name": displayed_name,
                        "amount": random_generator.randrange(minimum, maximum + 1, 100),
                        "transacted_at": (start + timedelta(minutes=random_generator.randint(0, 525_599))).strftime("%Y-%m-%d %H:%M"),
                        "confirmed_category": category,
                    })
    return records


def build_osm_enrichment(existing_records: list[dict[str, str]]) -> list[dict[str, str | int]]:
    """AI작성: OSM 업종 태그가 있는 실제 장소명만 카테고리별 상한을 두고 학습에 추가한다."""
    random_generator = random.Random(RANDOM_SEED + 2)
    start = datetime(2026, 1, 1, 9, 0)
    existing_names = {record["merchant_name"] for record in existing_records}
    grouped_candidates: dict[str, list[str]] = {category: [] for category in OSM_MAXIMUMS}

    with OSM_CANDIDATE_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        for candidate in csv.DictReader(input_file):
            category = candidate["suggested_category"]
            merchant_name = candidate["merchant_name"]
            if category in grouped_candidates and merchant_name not in existing_names:
                grouped_candidates[category].append(merchant_name)

    records = []
    for category, maximum in OSM_MAXIMUMS.items():
        minimum, maximum_amount = AMOUNT_RANGES[category]
        selected_names = random_generator.sample(grouped_candidates[category], min(maximum, len(grouped_candidates[category])))
        for merchant_name in selected_names:
            records.append({
                "split": "train",
                "merchant_name": merchant_name,
                "amount": random_generator.randrange(minimum, maximum_amount + 1, 100),
                "transacted_at": (start + timedelta(minutes=random_generator.randint(0, 525_599))).strftime("%Y-%m-%d %H:%M"),
                "confirmed_category": category,
            })
    return records


def main() -> None:
    source_records = load_records()
    records = source_records + build_osm_enrichment(source_records)
    random.Random(RANDOM_SEED).shuffle(records)

    split_counts = Counter(record["split"] for record in records)
    with OUTPUT_PATH.open("w", encoding="utf-8-sig", newline="") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=[
            "split", "merchant_name", "amount", "transacted_at", "confirmed_category",
        ])
        writer.writeheader()
        writer.writerows(records)

    print(f"총 {len(records)}건 / 분할별 {dict(split_counts)}")
    print(f"최종 학습(train+validation)={split_counts['train'] + split_counts['validation']}건")


if __name__ == "__main__":
    main()
