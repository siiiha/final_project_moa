"""카드 가맹점명의 지출 항목 후보와 검토 상태를 반환합니다."""

import sys
from pathlib import Path

import joblib

from expense_keyword_rules import classify_merchant_name


MODEL_PATH = Path(__file__).resolve().parent / "models" / "expense_tfidf_name_v14_5645.joblib"
# AI작성: 30% 기준은 합성 최종 검사에서 확인 필요 비율을 줄이면서 자동 추천 정확도를 유지했다.
REVIEW_THRESHOLD = 0.30
# AI작성: 결제대행 표기만으로는 실제 구매처·지출 성격을 알 수 없어 모델 분류를 보류합니다.
PAYMENT_INTERMEDIARY_TERMS = (
    "네이버페이", "카카오페이", "토스페이", "페이코", "PAYCO",
    "KG이니시스", "다날", "스마트로",
)


def predict_expense_category(merchant_name: str) -> dict[str, str | float | bool | None]:
    """가맹점명으로 지출 항목 후보·확신도·사용자 검토 상태를 반환합니다."""
    normalized_name = merchant_name.strip()
    if not normalized_name:
        raise ValueError("가맹점명은 비워둘 수 없습니다.")

    if any(term.casefold() in normalized_name.casefold() for term in PAYMENT_INTERMEDIARY_TERMS):
        return {
            "merchant_name": normalized_name,
            "suggested_category": None,
            "confidence": None,
            "review_status": "확인 필요",
            "review_reason": "결제대행 표기만으로 실제 구매처를 알 수 없습니다.",
            "prediction_source": "payment_intermediary_rule",
            "is_confirmed": False,
        }

    # AI작성: 명확한 키워드는 우선 적용해, ML이 불필요하게 확실한 분류를 뒤집지 않도록 합니다.
    rule_result = classify_merchant_name(normalized_name)
    if rule_result.category is not None:
        return {
            "merchant_name": normalized_name,
            "suggested_category": rule_result.category,
            "confidence": None,
            "review_status": "검토 대기",
            "review_reason": rule_result.reason,
            "prediction_source": "keyword_rule",
            "is_confirmed": False,
        }

    model_bundle = joblib.load(MODEL_PATH)
    name_features = model_bundle["vectorizer"].transform([normalized_name])
    probabilities = model_bundle["classifier"].predict_proba(name_features)[0]
    best_index = probabilities.argmax()
    confidence = float(probabilities[best_index])

    # AI작성: 낮은 확신도는 카테고리 오류보다 사용자의 재확인이 우선입니다.
    review_status = "검토 대기" if confidence >= REVIEW_THRESHOLD else "확인 필요"
    return {
        "merchant_name": normalized_name,
        "suggested_category": str(model_bundle["classifier"].classes_[best_index]),
        "confidence": round(confidence, 4),
        "review_status": review_status,
        "review_reason": "확신도가 기준보다 낮습니다." if review_status == "확인 필요" else "사용자 확인 후 확정합니다.",
        "prediction_source": "ml_model",
        "is_confirmed": False,
    }


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit('사용법: py -3 ml/predict_expense_category.py "가맹점명"')
    print(predict_expense_category(sys.argv[1]))
