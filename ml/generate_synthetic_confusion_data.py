"""서로 혼동되기 쉬운 지출 항목을 보강하는 합성 사용자 수정 데이터를 생성합니다."""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path


OUTPUT_PATH = Path(__file__).resolve().parent / "expense_synthetic_confusion_corrections_v1_400.csv"

# AI작성: 상호명만으로 완전히 확정되지는 않지만, 최종 분류의 근거가 있는 사례를 항목별로 구성합니다.
MERCHANTS_BY_CATEGORY = {
    "식비": ["한상다이닝", "골목키친", "온기밥상", "마루국수", "오늘식당", "소반주방", "담소키친", "한끼마당", "정성다이닝", "밥상연구소"],
    "교통비": ["모빌패스", "도시라이드", "한결주차", "로드이동", "드라이브패스", "마을모빌", "그린주차", "웨이라이드", "트랜짓온", "길동행패스"],
    "대관료": ["세미나홀", "회의마루", "모임스페이스", "공유세미나", "워크홀", "행사라운지", "강의스튜디오", "포럼룸", "대관마당", "회의플러스"],
    "물품비": ["장비스토어", "준비마켓", "리빙도구", "행사소품", "생활기어", "문구팩", "재료상점", "워크도구", "소품플러스", "준비코너"],
    "활동비": ["전시라운지", "체험스튜디오", "문화클래스", "공연마당", "영화워크숍", "예술체험", "창작라운지", "플레이클래스", "전시홀", "문화프로그램"],
    "숙박비": ["스테이하우스", "여행스테이", "휴식호텔", "나이트스테이", "힐게스트", "하루모텔", "여행하우스", "쉼터호텔", "도시스테이", "휴식게스트"],
    "수수료": ["예약수수료", "예매정산", "결제이용료", "티켓서비스", "플랫폼이용료", "간편결제료", "예약처리비", "예매대행료", "서비스수수료", "이용처리비"],
    "기타": ["수선상점", "사진프레임", "꽃소품", "리페어마당", "기념상점", "생활수선", "액자코너", "기록사진", "수리플러스", "기념소품"],
}

WRONG_INITIAL_CATEGORY = {
    "식비": "활동비", "교통비": "물품비", "대관료": "활동비", "물품비": "기타",
    "활동비": "식비", "숙박비": "교통비", "수수료": "교통비", "기타": "물품비",
}
AMOUNT_RANGES = {
    "식비": (5_000, 180_000), "교통비": (1_500, 160_000), "대관료": (30_000, 350_000),
    "물품비": (3_000, 180_000), "활동비": (5_000, 200_000), "숙박비": (60_000, 500_000),
    "수수료": (500, 30_000), "기타": (5_000, 150_000),
}


def generate_records(seed: int = 20261105) -> list[dict[str, str | int | float]]:
    """AI작성: 항목별 50건의 혼동 집중 사용자 수정 사례를 생성합니다."""
    random_generator = random.Random(seed)
    start = datetime(2026, 2, 1, 9, 0)
    records: list[dict[str, str | int | float]] = []
    for category, merchants in MERCHANTS_BY_CATEGORY.items():
        minimum, maximum = AMOUNT_RANGES[category]
        for index in range(50):
            merchant_name = random_generator.choice(merchants)
            if index % 4 == 1:
                merchant_name = f"(주){merchant_name}"
            elif index % 4 == 2:
                merchant_name = f"{merchant_name} 서부점"
            elif index % 4 == 3:
                merchant_name = f"{merchant_name} Co.,Ltd"
            records.append({
                "merchant_name": merchant_name,
                "amount": random_generator.randrange(minimum, maximum + 1, 100),
                "initial_suggested_category": WRONG_INITIAL_CATEGORY[category],
                "initial_confidence": round(random_generator.uniform(0.3, 0.9), 4),
                "review_status": "확인 필요",
                "user_confirmed_category": category,
                "transacted_at": (start + timedelta(minutes=random_generator.randint(0, 525_599))).strftime("%Y-%m-%d %H:%M"),
            })
    random_generator.shuffle(records)
    return records


if __name__ == "__main__":
    records = generate_records()
    with OUTPUT_PATH.open("w", newline="", encoding="utf-8-sig") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=[
            "merchant_name", "amount", "initial_suggested_category", "initial_confidence",
            "review_status", "user_confirmed_category", "transacted_at",
        ])
        writer.writeheader()
        writer.writerows(records)
    print(f"혼동 집중 합성 수정 데이터 {len(records)}건 저장: {OUTPUT_PATH}")
