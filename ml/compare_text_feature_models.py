"""가맹점명 문자·단어 TF-IDF 조합을 검증 데이터에서 비교합니다."""

import csv
from pathlib import Path

from scipy.sparse import hstack
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score


DATA_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v14_5645.csv"
LABELS = ("식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타")


def load_records() -> list[dict[str, str]]:
    with DATA_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def make_features(train_names: list[str], target_names: list[str], mode: str):
    """AI작성: 기존 문자 특징과 가맹점명 단어 특징의 조합만 비교한다."""
    char_vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 3))
    word_vectorizer = TfidfVectorizer(analyzer="word", ngram_range=(1, 2), token_pattern=r"(?u)\b\w+\b")
    if mode == "char":
        return char_vectorizer.fit_transform(train_names), char_vectorizer.transform(target_names)
    if mode == "word":
        return word_vectorizer.fit_transform(train_names), word_vectorizer.transform(target_names)
    return (
        hstack([char_vectorizer.fit_transform(train_names), word_vectorizer.fit_transform(train_names)]),
        hstack([char_vectorizer.transform(target_names), word_vectorizer.transform(target_names)]),
    )


def score(train_records: list[dict[str, str]], target_records: list[dict[str, str]], mode: str) -> tuple[float, float]:
    train_names = [row["merchant_name"] for row in train_records]
    target_names = [row["merchant_name"] for row in target_records]
    train_features, target_features = make_features(train_names, target_names, mode)
    classifier = LogisticRegression(max_iter=1_000, random_state=20261105)
    classifier.fit(train_features, [row["confirmed_category"] for row in train_records])
    expected = [row["confirmed_category"] for row in target_records]
    predicted = classifier.predict(target_features)
    return accuracy_score(expected, predicted), f1_score(expected, predicted, labels=LABELS, average="macro")


def main() -> None:
    records = load_records()
    train_records = [row for row in records if row["split"] == "train"]
    validation_records = [row for row in records if row["split"] == "validation"]
    test_records = [row for row in records if row["split"] == "test"]
    validation_scores = []

    for mode in ("char", "word", "char_word"):
        accuracy, macro_f1 = score(train_records, validation_records, mode)
        validation_scores.append((macro_f1, accuracy, mode))
        print(f"검증 {mode}: 정확도={accuracy:.3f}, Macro F1={macro_f1:.3f}")

    _, _, best_mode = max(validation_scores)
    final_accuracy, final_macro_f1 = score(train_records + validation_records, test_records, best_mode)
    print(f"선택 특징={best_mode}")
    print(f"최종 검사: 정확도={final_accuracy:.3f}, Macro F1={final_macro_f1:.3f}")


if __name__ == "__main__":
    main()
