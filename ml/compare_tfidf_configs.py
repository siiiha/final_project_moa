"""TF-IDF 문자 범위와 로지스틱 회귀 설정을 검증 데이터로 비교합니다."""

import csv
from pathlib import Path

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score


DATA_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v14_5645.csv"
LABELS = ("식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타")
CONFIGURATIONS = (
    ((2, 3), 1.0),
    ((2, 4), 1.0),
    ((2, 5), 1.0),
    ((3, 5), 1.0),
    ((2, 4), 0.5),
    ((2, 4), 2.0),
)


def load_records() -> list[dict[str, str]]:
    with DATA_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def score(train_records: list[dict[str, str]], target_records: list[dict[str, str]], ngram_range: tuple[int, int], c_value: float) -> tuple[float, float]:
    """AI작성: 가맹점명만 사용해 문자 단위 TF-IDF 설정의 차이만 비교한다."""
    vectorizer = TfidfVectorizer(analyzer="char", ngram_range=ngram_range)
    classifier = LogisticRegression(max_iter=1_000, random_state=20261105, C=c_value)
    train_names = [row["merchant_name"] for row in train_records]
    classifier.fit(vectorizer.fit_transform(train_names), [row["confirmed_category"] for row in train_records])
    expected = [row["confirmed_category"] for row in target_records]
    predicted = classifier.predict(vectorizer.transform([row["merchant_name"] for row in target_records]))
    return accuracy_score(expected, predicted), f1_score(expected, predicted, labels=LABELS, average="macro")


def main() -> None:
    records = load_records()
    train_records = [row for row in records if row["split"] == "train"]
    validation_records = [row for row in records if row["split"] == "validation"]
    test_records = [row for row in records if row["split"] == "test"]

    scores = []
    for ngram_range, c_value in CONFIGURATIONS:
        accuracy, macro_f1 = score(train_records, validation_records, ngram_range, c_value)
        scores.append((macro_f1, accuracy, ngram_range, c_value))
        print(f"검증 ngram={ngram_range}, C={c_value}: 정확도={accuracy:.3f}, Macro F1={macro_f1:.3f}")

    _, _, best_ngram_range, best_c_value = max(scores)
    final_accuracy, final_macro_f1 = score(
        train_records + validation_records, test_records, best_ngram_range, best_c_value
    )
    print(f"선택 설정 ngram={best_ngram_range}, C={best_c_value}")
    print(f"최종 검사: 정확도={final_accuracy:.3f}, Macro F1={final_macro_f1:.3f}")


if __name__ == "__main__":
    main()
