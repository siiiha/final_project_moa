"""카드 지출 분류 실험용 합성 데이터를 생성합니다."""

import csv
import random
from collections import Counter
from datetime import datetime, timedelta
from pathlib import Path


# AI작성: 같은 가맹점 계열이 서로 다른 분할에 나오지 않도록 카테고리별 가맹점 목록을 분리합니다.
MERCHANTS_BY_SPLIT = {
    "train": {
        "식비": ["행복한식당", "소담김밥", "우리동네밥상", "동네카페", "모임도시락"],
        "교통비": ["서울교통공사", "한빛택시", "고속버스", "공영주차장", "서울역KTX"],
        "대관료": ["주민센터회의실", "복지관강당", "마을회관대관", "문화센터세미나실", "시민회관"],
        "물품비": ["다이소", "모아문구", "프린트센터", "사무용품마트", "행사소품점"],
        "활동비": ["지역문화센터", "박물관체험", "마을영화관", "예술공연장", "노래연습장"],
        "숙박비": ["바다펜션", "OO호텔", "숲속리조트", "한강게스트하우스", "여행모텔"],
        "수수료": ["카드연회비", "예약서비스수수료", "예매수수료", "결제수수료", "이체수수료"],
        "기타": ["꽃집", "사진관", "세탁소", "기부센터", "수선집"],
    },
    "validation": {
        "식비": ["정든식당", "마실카페"],
        "교통비": ["시외버스", "행복주차장"],
        "대관료": ["동네모임방", "구민회관"],
        "물품비": ["알뜰문구", "복사인쇄소"],
        "활동비": ["역사체험관", "시민극장"],
        "숙박비": ["산들펜션", "도심호텔"],
        "수수료": ["티켓예매수수료", "카드서비스수수료"],
        "기타": ["액자점", "수리센터"],
    },
    "test": {
        "식비": ["우리밥상", "따뜻한찻집"],
        "교통비": ["SRT", "한마음택시"],
        "대관료": ["시민강당", "행복회의실"],
        "물품비": ["생활문구", "디지털인쇄"],
        "활동비": ["미술전시관", "문화강좌"],
        "숙박비": ["별빛리조트", "도시게스트하우스"],
        "수수료": ["예약대행수수료", "카드관리수수료"],
        "기타": ["공방", "악기수리점"],
    },
}

# AI작성: 업종명이 직접 드러나지 않는 브랜드형·공간형 상호를 분할별로 따로 둡니다.
HARD_MERCHANTS_BY_SPLIT = {
    "train": {
        "식비": ["브릭키친", "오후식당", "두레상회"],
        "교통비": ["모빌리티원", "그린패스", "라인25"],
        "대관료": ["스페이스127", "커넥트룸", "마루홀"],
        "물품비": ["소담상점", "프로젝트M", "리빙랩"],
        "활동비": ["스테이지ON", "아트큐브", "모먼트랩"],
        "숙박비": ["스테이오후", "더하우스", "블루도어"],
        "수수료": ["티켓브릿지", "플랫폼정산", "링크패스"],
        "기타": ["동백상회", "한땀공방", "필름하우스"],
    },
    "validation": {
        "식비": ["테이블하나", "모락", "온기다이닝"],
        "교통비": ["웨이브모빌", "패스온", "로드플랜"],
        "대관료": ["라운드룸", "오픈마루", "다락스페이스"],
        "물품비": ["스튜디오팩", "베이직마켓", "포켓상점"],
        "활동비": ["플레이그라운드", "포커스스튜디오", "아트웨이브"],
        "숙박비": ["루프탑스테이", "온더힐", "마일드하우스"],
        "수수료": ["원클릭", "예약게이트", "페이플로우"],
        "기타": ["그린케어", "프레임룸", "꽃피는날"],
    },
    "test": {
        "식비": ["사계절", "정담", "소반"],
        "교통비": ["무브업", "블루라인", "이동의정석"],
        "대관료": ["워크라운지", "루프탑B", "커뮤니티25"],
        "물품비": ["더블유스토어", "하우스랩", "오브젝트마켓"],
        "활동비": ["클래스플러스", "플레이아지트", "컬처랩"],
        "숙박비": ["나인스테이", "도어스테이", "하루하우스"],
        "수수료": ["서비스플러스", "큐패스", "바로결제"],
        "기타": ["그림상점", "리페어룸", "소중한날"],
    },
}

# AI작성: 지나치게 쉬운 업종 단어는 피하면서도 사람이 문맥을 추정할 수 있는 상호를 추가합니다.
CONTEXT_MERCHANTS_BY_SPLIT = {
    "train": {
        "식비": ["바른한상", "온기국수", "마당식탁"],
        "교통비": ["도시이동", "마을패스", "그린라이드"],
        "대관료": ["모임마루", "한칸회의", "함께라운지"],
        "물품비": ["일상문구", "살림상점", "준비마켓"],
        "활동비": ["전시마당", "체험극장", "공연한마당"],
        "숙박비": ["달빛스테이", "여행하우스", "휴식객실"],
        "수수료": ["예약연결", "예매창구", "이용정산"],
        "기타": ["동네꽃방", "사진기록", "옷수선방"],
    },
    "validation": {
        "식비": ["정성국수", "하루한끼", "담소주방"],
        "교통비": ["한결라이드", "생활패스", "우리노선"],
        "대관료": ["공유마당", "다정한방", "모임한칸"],
        "물품비": ["생활준비", "작은문구", "행사상점"],
        "활동비": ["배움마당", "영화한편", "창작시간"],
        "숙박비": ["느린스테이", "여행쉼터", "밤하우스"],
        "수수료": ["티켓연결", "간편정산", "예약창구"],
        "기타": ["꽃한송이", "수선기록", "기념사진"],
    },
    "test": {
        "식비": ["소박한끼", "국수마당", "오늘식탁"],
        "교통비": ["동네라이드", "길찾기", "이동패스"],
        "대관료": ["마을라운지", "열린회의", "공간한칸"],
        "물품비": ["우리문구", "소품준비", "살림가게"],
        "활동비": ["문화마당", "전시한편", "놀이극장"],
        "숙박비": ["하루스테이", "여행의집", "쉼하우스"],
        "수수료": ["예매연결", "서비스정산", "이용창구"],
        "기타": ["꽃과기록", "수선한날", "사진이야기"],
    },
}

# AI작성: 취미 재료·도구·장비 구매를 물품비로 학습시키기 위한 분할별 상호 예시입니다.
HOBBY_MATERIAL_MERCHANTS_BY_SPLIT = {
    "train": ["붓과물감", "비즈온", "뜨개실마켓", "캠핑기어", "러닝슈즈", "필름카메라", "삼각대스토어", "악보마켓", "퍼즐팩"],
    "validation": ["캔버스랩", "가죽공예도구", "등산스틱샵", "렌즈스토리", "삼각대마켓", "원예가게", "보드게임샵", "베이킹틀"],
    "test": ["점토상점", "재봉도구", "하이킹기어", "삼각대팩", "씨앗마켓", "카드게임샵", "요리도구"],
}

# AI작성: 문맥 단서가 있는 합성 1,200건을 더해 총 3,700건이 되도록 분할합니다.
RECORD_COUNTS = {
    "식비": {"train": 304, "validation": 71, "test": 68},
    "교통비": {"train": 324, "validation": 71, "test": 68},
    "대관료": {"train": 324, "validation": 71, "test": 68},
    "물품비": {"train": 324, "validation": 69, "test": 70},
    "활동비": {"train": 286, "validation": 68, "test": 70},
    "숙박비": {"train": 304, "validation": 68, "test": 70},
    "수수료": {"train": 323, "validation": 69, "test": 70},
    "기타": {"train": 323, "validation": 68, "test": 71},
}

# AI작성: 금액은 보조 특징으로만 쓰기 위해 카테고리 간 범위를 일부 겹치게 설정합니다.
AMOUNT_RANGES = {
    "식비": (5_000, 180_000), "교통비": (1_500, 160_000), "대관료": (30_000, 350_000),
    "물품비": (3_000, 180_000), "활동비": (5_000, 200_000), "숙박비": (60_000, 500_000),
    "수수료": (500, 30_000), "기타": (5_000, 150_000),
}

# AI작성: 공개자료에서 확인한 실제 상호는 학습 분할에만 보강합니다.
# 실제 카드 결제 금액·일시는 제공되지 않아 항목별 합성 범위로만 만듭니다.
PUBLIC_CANDIDATE_FILES = (
    "expense_public_restaurant_candidates_v1.csv",
    "expense_public_lodging_candidates_v1.csv",
    "expense_public_culture_candidates_v1.csv",
)
ACTIVITY_CANDIDATE_FILE = "expense_osm_merchant_candidates_v1.csv"
HOLDOUT_FILE = "expense_actual_merchant_holdout_v1.csv"
SYNTHETIC_CORRECTION_FILES = (
    "expense_synthetic_user_corrections_v1_400.csv",
    "expense_synthetic_confusion_corrections_v1_400.csv",
)
# AI작성: 카드 명세서에 자주 보이는 법인·영문 법인·지점 표기 변형입니다.
CARD_STATEMENT_VARIANTS = (
    "{}",
    "(주){}",
    "{} CO., LTD",
    "{} 성수점",
)


def generate_records(seed: int = 20261104) -> list[dict[str, str | int]]:
    """가맹점 계열 분할을 보존한 분류 실험용 레코드를 만듭니다."""
    random_generator = random.Random(seed)
    records: list[dict[str, str | int]] = []
    start = datetime(2026, 1, 1, 9, 0)
    for split, categories in MERCHANTS_BY_SPLIT.items():
        for category, merchants in categories.items():
            minimum, maximum = AMOUNT_RANGES[category]
            for _ in range(RECORD_COUNTS[category][split]):
                # AI작성: 명세서 표기처럼 법인·지점·영문/숫자 표기를 섞어 단순 키워드 암기를 줄입니다.
                merchant_pool = (
                    merchants
                    + HARD_MERCHANTS_BY_SPLIT[split][category]
                    + CONTEXT_MERCHANTS_BY_SPLIT[split][category]
                )
                if category == "물품비":
                    merchant_pool += HOBBY_MATERIAL_MERCHANTS_BY_SPLIT[split]
                merchant_name = random_generator.choice(merchant_pool)
                display_style = random_generator.choice(("기본", "기본", "법인", "지점", "영문"))
                if display_style == "법인":
                    merchant_name = f"(주){merchant_name}"
                elif display_style == "지점":
                    merchant_name = f"{merchant_name} {random_generator.choice(('강남', '성수', '중앙', '서부'))}점"
                elif display_style == "영문":
                    merchant_name = f"{merchant_name} Co.,Ltd"
                transacted_at = start + timedelta(minutes=random_generator.randint(0, 525_599))
                amount = random_generator.randrange(minimum, maximum + 1, 100)
                records.append({
                    "split": split,
                    "merchant_name": merchant_name,
                    "amount": amount,
                    "transacted_at": transacted_at.strftime("%Y-%m-%d %H:%M"),
                    "confirmed_category": category,
                })
    random_generator.shuffle(records)
    return records


def public_merchant_records(seed: int = 20261104) -> list[dict[str, str | int]]:
    """검토한 공개 상호명 후보를 학습용 보강 레코드로 변환합니다."""
    random_generator = random.Random(seed + 1)
    start = datetime(2026, 1, 1, 9, 0)
    records: list[dict[str, str | int]] = []
    base_directory = Path(__file__).resolve().parent
    used_names: set[str] = set()

    def append_record(merchant_name: str, category: str) -> None:
        """AI작성: 공개 상호의 카드 명세서 표기 변형을 금액만 합성해 학습에 추가합니다."""
        minimum, maximum = AMOUNT_RANGES[category]
        for display_format in CARD_STATEMENT_VARIANTS:
            records.append({
                "split": "train",
                "merchant_name": display_format.format(merchant_name),
                "amount": random_generator.randrange(minimum, maximum + 1, 100),
                "transacted_at": (start + timedelta(minutes=random_generator.randint(0, 525_599))).strftime("%Y-%m-%d %H:%M"),
                "confirmed_category": category,
            })
        used_names.add(merchant_name)

    for file_name in PUBLIC_CANDIDATE_FILES:
        with (base_directory / file_name).open(encoding="utf-8-sig", newline="") as input_file:
            for candidate in csv.DictReader(input_file):
                append_record(candidate["merchant_name"], candidate["suggested_category"])

    # AI작성: 미출현 실제 상호 검사 파일은 학습에서 제외하고, 나머지 활동시설만 중복 없이 보강합니다.
    with (base_directory / HOLDOUT_FILE).open(encoding="utf-8-sig", newline="") as input_file:
        holdout_names = {candidate["merchant_name"] for candidate in csv.DictReader(input_file)}
    with (base_directory / ACTIVITY_CANDIDATE_FILE).open(encoding="utf-8-sig", newline="") as input_file:
        for candidate in csv.DictReader(input_file):
            merchant_name = candidate["merchant_name"]
            if (candidate["suggested_category"] == "활동비"
                    and merchant_name not in holdout_names
                    and merchant_name not in used_names):
                append_record(merchant_name, "활동비")
    return records


def simulated_correction_records() -> list[dict[str, str | int]]:
    """AI작성: 합성 사용자 수정 결과는 최종 분류만 사용해 학습 분할에 보강합니다."""
    base_directory = Path(__file__).resolve().parent
    records: list[dict[str, str | int]] = []
    for file_name in SYNTHETIC_CORRECTION_FILES:
        with (base_directory / file_name).open(encoding="utf-8-sig", newline="") as input_file:
            for correction in csv.DictReader(input_file):
                records.append({
                    "split": "train",
                    "merchant_name": correction["merchant_name"],
                    "amount": int(correction["amount"]),
                    "transacted_at": correction["transacted_at"],
                    "confirmed_category": correction["user_confirmed_category"],
                })
    return records


def write_csv(output_path: Path, records: list[dict[str, str | int]]) -> None:
    """합성 데이터는 실제 카드 내역과 섞지 않고 별도 CSV 파일로 저장합니다."""
    with output_path.open("w", newline="", encoding="utf-8-sig") as output_file:
        writer = csv.DictWriter(output_file, fieldnames=[
            "split", "merchant_name", "amount", "transacted_at", "confirmed_category",
        ])
        writer.writeheader()
        writer.writerows(records)


if __name__ == "__main__":
    # AI작성: 합성 3,622건에 실제 상호 표기 312건·사용자 수정 시뮬레이션 800건을 보강합니다.
    generated_records = generate_records() + public_merchant_records() + simulated_correction_records()
    random.Random(20261104).shuffle(generated_records)
    write_csv(Path(__file__).resolve().parent / "expense_synthetic_data_v12_4734.csv", generated_records)
    category_counts = Counter(record["confirmed_category"] for record in generated_records)
    split_counts = Counter(record["split"] for record in generated_records)
    print(f"총 {len(generated_records)}건")
    print(f"분할별 건수: {dict(split_counts)}")
    print(f"카테고리별 건수: {dict(category_counts)}")
