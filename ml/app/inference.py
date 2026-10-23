# AI작성: 기존 stage5 추론 로직을 재사용하며 모델 경로만 독립 서비스 기준으로 변경한다.
from __future__ import annotations

import hashlib
import json
import math
import os
import platform
from pathlib import Path
from typing import Any

import joblib
import numpy as np
import sklearn

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MODEL = ROOT / "artifacts/model.joblib"
DEFAULT_METADATA = ROOT / "artifacts/metadata.json"
MANIFEST_PATH = Path(__file__).resolve().parents[1] / "model-manifest.json"
SCHEMA_VERSION = "moa-anomaly-features-v1"
SERVICE_VERSION = "stage5-local-inference-v1"
SCORE_DEFINITION = "-IsolationForest.score_samples(X); higher means more anomalous; not a probability"


class PreparationError(RuntimeError):
    pass


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _read_json(path: Path) -> Any:
    with path.open(encoding="utf-8-sig") as stream:
        return json.load(stream)


def build_manifest(model_path: Path, metadata_path: Path, metadata: dict[str, Any]) -> dict[str, Any]:
    return {
        "service_version": SERVICE_VERSION,
        "model_version": f"sha256:{sha256(model_path)}",
        "feature_schema_version": SCHEMA_VERSION,
        "metadata_sha256": sha256(metadata_path),
        "stage4_model_sha256": metadata.get("model_sha256"),
        "source": "stage4 saved artifact; the original artifact did not define a model version",
    }


class Scorer:
    def __init__(self, model_path: Path | None = None, metadata_path: Path | None = None):
        self.model_path = model_path or Path(os.environ.get("MOA_MODEL_PATH", DEFAULT_MODEL)).resolve()
        self.metadata_path = metadata_path or Path(os.environ.get("MOA_METADATA_PATH", DEFAULT_METADATA)).resolve()
        self.model = None
        self.metadata: dict[str, Any] | None = None
        self.manifest: dict[str, Any] | None = None
        self.ready = False
        self.error: str | None = None
        try:
            self._load()
        except Exception as exc:
            self.error = f"{type(exc).__name__}: {exc}"

    def _load(self) -> None:
        if not self.model_path.is_file() or not self.metadata_path.is_file():
            raise PreparationError("model.joblib 또는 metadata.json 파일을 찾을 수 없습니다.")
        metadata = _read_json(self.metadata_path)
        if not isinstance(metadata, dict):
            raise PreparationError("metadata.json 형식이 객체가 아닙니다.")
        expected_hash = metadata.get("model_sha256")
        actual_hash = sha256(self.model_path)
        if not expected_hash or expected_hash != actual_hash:
            raise PreparationError("모델 SHA-256이 metadata.json과 일치하지 않습니다.")
        versions = {
            "python_version": platform.python_version(),
            "numpy_version": np.__version__,
            "scikit_learn_version": sklearn.__version__,
            "joblib_version": joblib.__version__,
        }
        mismatches = [f"{key}: artifact={metadata.get(key)!r}, runtime={value!r}"
                      for key, value in versions.items() if metadata.get(key) != value]
        if mismatches:
            raise PreparationError("저장 모델의 실행 환경과 다릅니다: " + "; ".join(mismatches))
        names = metadata.get("feature_allowlist")
        retained = metadata.get("retained_features")
        medians = metadata.get("imputation_medians_fit_on_train_only")
        order = metadata.get("transformed_feature_order")
        if not isinstance(names, list) or not names or len(names) != len(set(names)):
            raise PreparationError("feature_allowlist가 비었거나 중복입니다.")
        if not isinstance(retained, list) or not retained or not set(retained) <= set(names):
            raise PreparationError("retained_features가 유효하지 않습니다.")
        expected_order = [x for name in retained for x in (name, f"{name}__is_missing")]
        if order != expected_order or not isinstance(medians, dict) or set(medians) != set(retained):
            raise PreparationError("전처리 열 순서 또는 학습 중앙값 명세가 일치하지 않습니다.")
        cfg = metadata.get("thresholds_fit_on_validation_only", {}).get("isolation_forest", {}).get("0.05", {})
        if cfg.get("comparison") != ">" or not isinstance(cfg.get("threshold"), (int, float)):
            raise PreparationError("validation 5% threshold 명세가 유효하지 않습니다.")
        if metadata.get("algorithm") != "sklearn.ensemble.IsolationForest":
            raise PreparationError("지원하지 않는 저장 모델입니다.")
        model = joblib.load(self.model_path)
        if not hasattr(model, "score_samples") or getattr(model, "n_features_in_", None) != len(order):
            raise PreparationError("모델 입력 열 수가 metadata와 일치하지 않습니다.")
        manifest = build_manifest(self.model_path, self.metadata_path, metadata)
        # AI작성: 서비스 시작 시 저장소 파일을 덮어쓰지 않고 버전 정보를 메모리에 유지한다.
        self.model, self.metadata, self.manifest, self.ready = model, metadata, manifest, True

    def score(self, features: dict[str, Any], quality_flags: list[str]) -> float:
        if not self.ready or self.metadata is None or self.model is None:
            raise PreparationError(self.error or "추론기가 준비되지 않았습니다.")
        names = self.metadata["feature_allowlist"]
        retained = self.metadata["retained_features"]
        medians = self.metadata["imputation_medians_fit_on_train_only"]
        masked = set(quality_flags).intersection(self.metadata["balance_ratio_mask_flags"])
        values: dict[str, float] = {}
        for name in names:
            value = features[name]
            if name == "withdrawal_to_balance_before" and masked:
                value = None
            if value is None:
                values[name] = math.nan
            else:
                try:
                    number = float(value)
                except (TypeError, ValueError, OverflowError) as exc:
                    raise ValueError(f"features.{name} 숫자로 변환할 수 없습니다.") from exc
                if not math.isfinite(number):
                    raise ValueError(f"features.{name} 유한한 숫자여야 합니다.")
                values[name] = number
        columns: list[np.ndarray] = []
        for name in names:
            if name not in retained:
                continue
            missing = not math.isfinite(values[name])
            columns.append(np.asarray([float(medians[name]) if missing else values[name]], dtype=np.float64))
            columns.append(np.asarray([1.0 if missing else 0.0], dtype=np.float64))
        matrix = np.column_stack(columns).astype(np.float64, copy=False)
        if matrix.shape[1] != len(self.metadata["transformed_feature_order"]) or not np.isfinite(matrix).all():
            raise PreparationError("요청 전처리 결과가 저장 모델 명세와 다릅니다.")
        score = float(-self.model.score_samples(matrix)[0])
        if not math.isfinite(score):
            raise PreparationError("모델이 유한하지 않은 점수를 반환했습니다.")
        return score

    @property
    def threshold(self) -> float:
        assert self.metadata is not None
        return float(self.metadata["thresholds_fit_on_validation_only"]["isolation_forest"]["0.05"]["threshold"])


scorer = Scorer()
