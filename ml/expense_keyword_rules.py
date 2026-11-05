"""계모임 카드 지출의 가맹점명 키워드 분류 기준입니다."""

from dataclasses import dataclass
from typing import Final


# AI작성: 자동 분류는 제안일 뿐이며, 확인 필요 건은 사용자가 반드시 판단해야 합니다.
NEEDS_REVIEW: Final = "확인 필요"
CONFIRMED: Final = "확정"


@dataclass(frozen=True)
class ExpenseClassification:
    """가맹점명 규칙의 분류 결과입니다."""

    category: str | None
    review_status: str
    reason: str


# AI작성: 모호한 가맹점은 다른 키워드보다 먼저 확인 필요로 처리해 잘못된 자동 분류를 막습니다.
AMBIGUOUS_KEYWORDS: Final[tuple[str, ...]] = (
    "네이버페이", "카카오페이", "토스페이", "payco", "쿠팡", "편의점",
)

# AI작성: 항목별 키워드는 가상 데이터 검토 결과를 바탕으로 시작하며 실제 카드 내역 검토 후 보완합니다.
CATEGORY_KEYWORDS: Final[tuple[tuple[str, tuple[str, ...]], ...]] = (
    ("수수료", ("수수료", "연회비")),
    ("숙박비", ("호텔", "모텔", "펜션", "리조트", "게스트하우스")),
    ("대관료", ("대관", "회의실", "강당", "세미나실")),
    # AI작성: 보드게임 카페·대여는 게임 이용료이므로 카페 식비보다 활동비를 우선 적용한다.
    ("활동비", ("보드게임 카페", "보드게임카페", "보드게임 대여", "보드게임대여",
              "문화센터", "체험", "박물관", "영화", "극장", "공연", "전시", "노래연습", "강좌")),
    # AI작성: 취미의 강좌·관람비는 활동비지만, 재료·도구·장비 구매는 물품비로 봅니다.
    ("물품비", (
        "다이소", "문구", "인쇄", "프린트", "사무용품",
        "미술재료", "공예재료", "뜨개용품", "원단", "비즈", "물감", "캔버스", "diy", "악기점",
        "등산용품", "하이킹용품", "등산화", "트레킹화", "러닝화", "스포츠용품", "운동용품",
        "붓", "점토", "레진", "가죽공예", "뜨개실", "재봉", "악보", "피크", "튜너", "기타줄",
        "캠핑용품", "캠핑장비", "등산스틱", "낚시용품", "카메라", "렌즈", "필름", "삼각대", "메모리카드",
        "보드게임샵", "보드게임몰", "퍼즐", "카드게임샵", "독서대", "다이어리", "노트", "문구", "씨앗", "화분", "비료", "원예",
        "베이킹도구", "베이킹틀", "쿠키틀", "요리도구",
    )),
    ("교통비", ("ktx", "srt", "택시", "교통", "버스", "지하철", "철도", "주차", "톨게이트")),
    ("식비", ("식당", "카페", "coffee", "음식", "맛집", "김밥", "치킨", "도시락")),
    ("기타", ("꽃집", "화훼")),
)


def classify_merchant_name(merchant_name: str | None) -> ExpenseClassification:
    """가맹점명만으로 보수적으로 분류하고, 판단 근거가 없으면 확인 필요를 반환합니다."""
    normalized_name = "" if merchant_name is None else merchant_name.strip().lower()
    if not normalized_name:
        return ExpenseClassification(None, NEEDS_REVIEW, "가맹점명이 없습니다.")

    for keyword in AMBIGUOUS_KEYWORDS:
        if keyword in normalized_name:
            return ExpenseClassification(None, NEEDS_REVIEW, f"'{keyword}'만으로 실제 구매 내용을 알 수 없습니다.")

    for category, keywords in CATEGORY_KEYWORDS:
        for keyword in keywords:
            if keyword in normalized_name:
                return ExpenseClassification(category, CONFIRMED, f"가맹점명에 '{keyword}' 키워드가 있습니다.")

    return ExpenseClassification(None, NEEDS_REVIEW, "등록된 분류 키워드를 찾지 못했습니다.")


if __name__ == "__main__":
    # AI작성: 가상 카드 지출 예시 중 대표·모호 사례가 보수적으로 분류되는지 빠르게 확인합니다.
    examples = {
        "행복한식당": "식비",
        "서울역 KTX": "교통비",
        "주민센터 회의실": "대관료",
        "OO호텔": "숙박비",
        "카드 연회비": "수수료",
        "동네 미술재료": "물품비",
        "즐거운 뜨개용품": "물품비",
        "봄날 공예체험": "활동비",
        "우리동네 보드게임 카페": "활동비",
        "보드게임대여": "활동비",
        "보드게임샵": "물품비",
        "네이버페이": None,
        "쿠팡": None,
        "편의점": None,
    }
    for merchant_name, expected_category in examples.items():
        result = classify_merchant_name(merchant_name)
        assert result.category == expected_category, (merchant_name, result)
    print("키워드 규칙 예시 검증 완료")
