"""가맹점명 정규화가 지출 분류 성능에 주는 영향을 같은 검사 데이터에서 비교합니다."""

import csv
import re
import unicodedata
from pathlib import Path

from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score


DATA_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v14_5645.csv"
LABELS = ("식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타")
BRANCH_SUFFIX_PATTERN = re.compile(r"\s+(강남|성수|중앙|서부)\s*점$", re.IGNORECASE)
CORPORATE_PATTERN = re.compile(r"\(주\)|㈜|주식회사|\bco\s*[,\.]?\s*ltd\b", re.IGNORECASE)


def normalize_merchant_name(value: str) -> str:
    """AI작성: 법인·지점 표기 차이만 정리하고 상호의 핵심 단어는 보존한다."""
    normalized = unicodedata.normalize("NFKC", value).casefold().strip()
    normalized = CORPORATE_PATTERN.sub(" ", normalized)
    normalized = BRANCH_SUFFIX_PATTERN.sub("", normalized)
    return " ".join(normalized.split())


def load_records() -> list[dict[str, str]]:
    with DATA_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def evaluate(train_records: list[dict[str, str]], test_records: list[dict[str, str]], normalize: bool) -> tuple[float, float]:
    transform = normalize_merchant_name if normalize else lambda value: value
    vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 3))
    train_names = [transform(row["merchant_name"]) for row in train_records]
    test_names = [transform(row["merchant_name"]) for row in test_records]
    classifier = LogisticRegression(max_iter=1_000, random_state=20261105)
    classifier.fit(vectorizer.fit_transform(train_names), [row["confirmed_category"] for row in train_records])
    predicted = classifier.predict(vectorizer.transform(test_names))
    expected = [row["confirmed_category"] for row in test_records]
    return accuracy_score(expected, predicted), f1_score(expected, predicted, labels=LABELS, average="macro")


def main() -> None:
    records = load_records()
    train_records = [row for row in records if row["split"] in {"train", "validation"}]
    test_records = [row for row in records if row["split"] == "test"]
    normalized_train_names = {normalize_merchant_name(row["merchant_name"]) for row in train_records}
    normalized_test_names = {normalize_merchant_name(row["merchant_name"]) for row in test_records}
    overlap = normalized_train_names & normalized_test_names
    print(f"정규화 후 학습·검사 상호 중복: {len(overlap)}개")

    raw_accuracy, raw_f1 = evaluate(train_records, test_records, False)
    normalized_accuracy, normalized_f1 = evaluate(train_records, test_records, True)
    print(f"원본 가맹점명: 정확도={raw_accuracy:.3f}, Macro F1={raw_f1:.3f}")
    print(f"정규화 가맹점명: 정확도={normalized_accuracy:.3f}, Macro F1={normalized_f1:.3f}")


if __name__ == "__main__":
    main()
