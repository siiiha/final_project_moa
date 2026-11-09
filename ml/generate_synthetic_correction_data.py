"""사용자 수정 흐름을 검토하기 위한 합성 카드 지출 수정 데이터를 생성합니다."""

import csv
import random
from datetime import datetime, timedelta
from pathlib import Path


OUTPUT_PATH = Path(__file__).resolve().parent / "expense_synthetic_user_corrections_v1_400.csv"

# AI작성: 모호하지만 분류 근거가 있는 상호를 항목별로 분리해 사용자 수정 사례를 만듭니다.
MERCHANTS_BY_CATEGORY = {
    "식비": ["마루한상", "사계식탁", "온기국수", "담소밥집", "골목한끼", "해뜰주방", "소반마당", "정담식탁", "바른찬방", "오늘국수"],
    "교통비": ["그린라이드", "시티무브", "도로패스", "한결이동", "블루모빌", "길동행", "드라이브온", "마을라이드", "도시패스", "새길모빌"],
    "대관료": ["마루대관", "스페이스온", "모임라운지", "모임마당", "한칸회의", "열린세미나", "커넥트홀", "루프마루", "다정회의", "타운룸"],
    "물품비": ["생활준비", "프로젝트팩", "오브제준비", "마켓플러스", "소품상점", "리빙팩", "문구코너", "행사준비", "살림마켓", "준비상점"],
    "활동비": ["스테이지플러스", "컬처타임", "아트마당", "플레이존", "전시모먼트", "체험마루", "영화시간", "클래스온", "공연마당", "문화놀이터"],
    "숙박비": ["힐하우스", "블루스테이", "트래블쉼터", "온더숙소", "루프스테이", "야간하우스", "휴식마루", "더나인", "여행하루", "고요스테이"],
    "수수료": ["티켓링크", "예약패스", "결제게이트", "이용정산", "간편수수료", "사용정산", "예매링크", "결제플러스", "플랫폼수수료", "원클릭패스"],
    "기타": ["고침공방", "아트상점", "꽃기록", "사진노트", "동네수선", "기념프레임", "한땀수선", "필름기록", "생활케어", "기억하는날"],
}

WRONG_INITIAL_CATEGORY = {
    "식비": "활동비", "교통비": "물품비", "대관료": "활동비", "물품비": "활동비",
    "활동비": "식비", "숙박비": "물품비", "수수료": "교통비", "기타": "활동비",
}
AMOUNT_RANGES = {
    "식비": (5_000, 180_000), "교통비": (1_500, 160_000), "대관료": (30_000, 350_000),
    "물품비": (3_000, 180_000), "활동비": (5_000, 200_000), "숙박비": (60_000, 500_000),
    "수수료": (500, 30_000), "기타": (5_000, 150_000),
}


def generate_records(seed: int = 20261104) -> list[dict[str, str | int | float]]:
    """AI작성: 항목별 50건, 총 400건의 합성 사용자 수정 사례를 생성합니다."""
    random_generator = random.Random(seed)
    start = datetime(2026, 1, 1, 9, 0)
    records: list[dict[str, str | int | float]] = []
    for category, merchants in MERCHANTS_BY_CATEGORY.items():
        minimum, maximum = AMOUNT_RANGES[category]
        for index in range(50):
            merchant_name = random_generator.choice(merchants)
            if index % 3 == 1:
                merchant_name = f"(주){merchant_name}"
            elif index % 3 == 2:
                merchant_name = f"{merchant_name} 중앙점"
            records.append({
                "merchant_name": merchant_name,
                "amount": random_generator.randrange(minimum, maximum + 1, 100),
                "initial_suggested_category": WRONG_INITIAL_CATEGORY[category],
                "initial_confidence": round(random_generator.uniform(0.2, 0.85), 4),
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
    print(f"합성 사용자 수정 데이터 {len(records)}건 저장: {OUTPUT_PATH}")
