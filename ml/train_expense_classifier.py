"""합성 카드 지출 데이터로 두 가지 1차 분류 모델을 학습하고 저장합니다."""

import csv
from pathlib import Path

import joblib
import numpy as np
from scipy.sparse import csr_matrix, hstack
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import accuracy_score, f1_score
from sklearn.preprocessing import StandardScaler


DATA_PATH = Path(__file__).resolve().parent / "expense_synthetic_data_v15_5689.csv"
MODEL_DIRECTORY = Path(__file__).resolve().parent / "models"
LABELS = ("식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타")


def load_records() -> list[dict[str, str]]:
    with DATA_PATH.open(encoding="utf-8-sig", newline="") as input_file:
        return list(csv.DictReader(input_file))


def merchant_names(records: list[dict[str, str]]) -> list[str]:
    return [record["merchant_name"] for record in records]


def labels(records: list[dict[str, str]]) -> list[str]:
    return [record["confirmed_category"] for record in records]


def log_amounts(records: list[dict[str, str]]) -> np.ndarray:
    return np.log1p(np.array([float(record["amount"]) for record in records])).reshape(-1, 1)


def fit_bundle(records: list[dict[str, str]], include_amount: bool) -> dict:
    """AI작성: 금액 포함 여부만 다르게 해 같은 TF-IDF·로지스틱 회귀 조건에서 비교합니다."""
    vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 3))
    name_features = vectorizer.fit_transform(merchant_names(records))
    amount_scaler = None
    features = name_features

    if include_amount:
        amount_scaler = StandardScaler()
        amount_features = csr_matrix(amount_scaler.fit_transform(log_amounts(records)))
        features = hstack([name_features, amount_features], format="csr")

    classifier = LogisticRegression(max_iter=1_000, random_state=20261104)
    classifier.fit(features, labels(records))
    return {
        "model_version": "expense-tfidf-logistic-v15-5689",
        "feature_mode": "merchant_name_and_amount" if include_amount else "merchant_name",
        "labels": LABELS,
        "vectorizer": vectorizer,
        "amount_scaler": amount_scaler,
        "classifier": classifier,
    }


def predict(bundle: dict, records: list[dict[str, str]]) -> list[str]:
    name_features = bundle["vectorizer"].transform(merchant_names(records))
    features = name_features
    if bundle["amount_scaler"] is not None:
        amount_features = csr_matrix(bundle["amount_scaler"].transform(log_amounts(records)))
        features = hstack([name_features, amount_features], format="csr")
    return bundle["classifier"].predict(features).tolist()


def metrics(bundle: dict, records: list[dict[str, str]]) -> dict[str, float]:
    actual = labels(records)
    predicted = predict(bundle, records)
    return {
        "accuracy": accuracy_score(actual, predicted),
        "macro_f1": f1_score(actual, predicted, labels=LABELS, average="macro", zero_division=0),
    }


def print_metrics(title: str, values: dict[str, float]) -> None:
    print(f"{title}: 정확도={values['accuracy']:.3f}, Macro F1={values['macro_f1']:.3f}")


def train_and_save(model_file_name: str, include_amount: bool, train_records: list[dict[str, str]],
                   validation_records: list[dict[str, str]], test_records: list[dict[str, str]]) -> None:
    validation_bundle = fit_bundle(train_records, include_amount)
    print_metrics(f"{model_file_name} 검증", metrics(validation_bundle, validation_records))

    # AI작성: 최종 검사는 모델 선택·학습에 쓰지 않고, 학습+검증 데이터만 합쳐 저장 모델을 다시 학습합니다.
    saved_bundle = fit_bundle(train_records + validation_records, include_amount)
    saved_bundle["training_record_count"] = len(train_records) + len(validation_records)
    saved_bundle["validation_record_count"] = len(validation_records)
    saved_bundle["test_record_count"] = len(test_records)
    print_metrics(f"{model_file_name} 최종 검사", metrics(saved_bundle, test_records))
    joblib.dump(saved_bundle, MODEL_DIRECTORY / model_file_name)


def main() -> None:
    records = load_records()
    train_records = [record for record in records if record["split"] == "train"]
    validation_records = [record for record in records if record["split"] == "validation"]
    test_records = [record for record in records if record["split"] == "test"]
    if not train_records or not validation_records or not test_records:
        raise ValueError("학습·검증·최종 검사 데이터가 모두 필요합니다.")

    MODEL_DIRECTORY.mkdir(exist_ok=True)
    print(f"학습={len(train_records)}건, 검증={len(validation_records)}건, 최종 검사={len(test_records)}건")
    train_and_save("expense_tfidf_name_v15_5689.joblib", False, train_records, validation_records, test_records)
    train_and_save("expense_tfidf_name_amount_v15_5689.joblib", True, train_records, validation_records, test_records)
    print(f"저장 위치: {MODEL_DIRECTORY}")


if __name__ == "__main__":
    main()
