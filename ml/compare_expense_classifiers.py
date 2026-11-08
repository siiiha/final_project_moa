"""카드 지출 분류의 키워드 규칙과 TF-IDF 모델을 같은 검사 데이터에서 비교합니다."""

import csv
from pathlib import Path

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.pipeline import FeatureUnion, FunctionTransformer, Pipeline

from expense_keyword_rules import classify_merchant_name


DATA_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v1.csv"
LABELS = ("식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타")


def load_records(path: Path) -> list[dict[str, str]]:
    """합성 CSV의 필수 열을 확인하고 레코드를 읽습니다."""
    with path.open(encoding="utf-8-sig", newline="") as input_file:
        records = list(csv.DictReader(input_file))
    required_columns = {"split", "merchant_name", "amount", "confirmed_category"}
    if not records or not required_columns.issubset(records[0]):
        raise ValueError("합성 데이터의 필수 열을 확인할 수 없습니다.")
    return records


def merchant_names(records: list[dict[str, str]]) -> list[str]:
    return [record["merchant_name"] for record in records]


def log_amounts(records: list[dict[str, str]]) -> np.ndarray:
    """AI작성: 금액은 단독 결정 근거가 아니므로 로그 변환한 보조 수치 한 칸으로만 전달합니다."""
    return np.log1p(np.array([float(record["amount"]) for record in records])).reshape(-1, 1)


def make_name_model() -> Pipeline:
    return Pipeline([
        # AI작성: CSV 레코드 전체가 아닌 가맹점명 문자열만 TF-IDF 변환기에 전달합니다.
        ("merchant_name", FunctionTransformer(merchant_names, validate=False)),
        ("tfidf", TfidfVectorizer(analyzer="char", ngram_range=(2, 3))),
        ("classifier", LogisticRegression(max_iter=1_000, random_state=20261104)),
    ])


def make_name_and_amount_model() -> Pipeline:
    # AI작성: 가맹점명 TF-IDF와 금액을 결합하되 금액이 분류를 단독 결정하지 않도록 같은 입력 묶음으로 학습합니다.
    features = FeatureUnion([
        ("merchant_name", Pipeline([
            ("select", FunctionTransformer(merchant_names, validate=False)),
            ("tfidf", TfidfVectorizer(analyzer="char", ngram_range=(2, 3))),
        ])),
        ("amount", FunctionTransformer(log_amounts, validate=False)),
    ])
    return Pipeline([
        ("features", features),
        ("classifier", LogisticRegression(max_iter=1_000, random_state=20261104)),
    ])


def evaluate_predictions(expected: list[str], predicted: list[str]) -> dict[str, float]:
    return {
        "accuracy": accuracy_score(expected, predicted),
        "macro_f1": f1_score(expected, predicted, labels=LABELS, average="macro", zero_division=0),
    }


def evaluate_keyword_rule(records: list[dict[str, str]]) -> dict[str, float]:
    """확인 필요는 틀린 자동 분류가 아니라 검토로 넘긴 비율로 별도 표시합니다."""
    confirmed_records = []
    automatic_predictions = []
    for record in records:
        prediction = classify_merchant_name(record["merchant_name"])
        if prediction.category is not None:
            confirmed_records.append(record)
            automatic_predictions.append(prediction.category)

    expected = [record["confirmed_category"] for record in confirmed_records]
    metrics = evaluate_predictions(expected, automatic_predictions) if expected else {"accuracy": 0.0, "macro_f1": 0.0}
    metrics["needs_review_rate"] = 1 - (len(confirmed_records) / len(records))
    metrics["false_automatic_rate"] = (
        sum(expected_label != predicted_label for expected_label, predicted_label in zip(expected, automatic_predictions)) / len(records)
    )
    return metrics


def evaluate_model(model: Pipeline, train_records: list[dict[str, str]], test_records: list[dict[str, str]]) -> dict[str, float]:
    expected = [record["confirmed_category"] for record in test_records]
    model.fit(train_records, [record["confirmed_category"] for record in train_records])
    predicted = model.predict(test_records).tolist()
    return evaluate_predictions(expected, predicted)


def print_metrics(title: str, metrics: dict[str, float]) -> None:
    values = [f"정확도={metrics['accuracy']:.3f}", f"Macro F1={metrics['macro_f1']:.3f}"]
    if "needs_review_rate" in metrics:
        values.extend([
            f"확인 필요 비율={metrics['needs_review_rate']:.3f}",
            f"틀린 자동 분류 비율={metrics['false_automatic_rate']:.3f}",
        ])
    print(f"{title}: {', '.join(values)}")


def main() -> None:
    records = load_records(DATA_PATH)
    train_records = [record for record in records if record["split"] == "train"]
    test_records = [record for record in records if record["split"] == "test"]
    train_merchants = {record["merchant_name"] for record in train_records}
    test_merchants = {record["merchant_name"] for record in test_records}
    if train_merchants & test_merchants:
        raise ValueError("학습·최종 검사 데이터에 같은 가맹점명이 있습니다.")

    print(f"학습 {len(train_records)}건 / 최종 검사 {len(test_records)}건 / 미등장 가맹점 {len(test_merchants)}개")
    print_metrics("키워드 규칙", evaluate_keyword_rule(test_records))
    print_metrics("TF-IDF 가맹점명", evaluate_model(make_name_model(), train_records, test_records))
    print_metrics("TF-IDF 가맹점명+금액", evaluate_model(make_name_and_amount_model(), train_records, test_records))


if __name__ == "__main__":
    main()
