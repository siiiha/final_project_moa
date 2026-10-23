# AI작성: 기존 과거 이력 기반 특성 생성기를 그대로 재사용한다.
#!/usr/bin/env python3
"""거래 JSONL에서 온라인 과거 기반 특성을 생성한다 (표준 라이브러리 전용)."""
from __future__ import annotations

import argparse
import json
import math
from collections import Counter, defaultdict
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Any

KST = timezone(timedelta(hours=9), "KST")
FEATURE_NAMES = [
    "amount_log1p", "is_withdrawal", "hour_sin", "hour_cos",
    "seconds_since_previous", "withdrawal_count_prev_1h",
    "withdrawal_count_prev_24h", "withdrawal_sum_prev_24h",
    "amount_to_same_direction_median_30d", "withdrawal_to_balance_before",
]
WINDOW_1H = timedelta(hours=1)
WINDOW_24H = timedelta(hours=24)
WINDOW_30D = timedelta(days=30)


def parse_time(value: str) -> datetime:
    dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if dt.tzinfo is None:
        raise ValueError("transacted_at에 UTC offset이 필요합니다")
    return dt.astimezone(KST)


def dec(value: Any) -> Decimal | None:
    if value is None:
        return None
    try:
        return Decimal(str(value))
    except InvalidOperation as exc:
        raise ValueError(f"유효하지 않은 금액: {value!r}") from exc


def num(value: Decimal | float | int | None) -> float | int | None:
    if value is None:
        return None
    return int(value) if isinstance(value, int) else float(value)


def median(values: list[Decimal]) -> Decimal:
    vals = sorted(values)
    n = len(vals)
    return vals[n // 2] if n % 2 else (vals[n // 2 - 1] + vals[n // 2]) / Decimal(2)


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    rows = []
    with path.open(encoding="utf-8") as f:
        for line_no, line in enumerate(f, 1):
            if line.strip():
                try:
                    rows.append(json.loads(line))
                except json.JSONDecodeError as exc:
                    raise ValueError(f"{path}:{line_no}: JSON 오류: {exc}") from exc
    return rows


def load_splits(path: Path) -> dict[str, dict[str, date]]:
    raw = json.loads(path.read_text(encoding="utf-8"))
    return {name: {"from": date.fromisoformat(v["from"]), "through": date.fromisoformat(v["through"])}
            for name, v in raw.items() if name in {"train", "validation", "test"}}


def assign_split(dt: datetime, splits: dict[str, dict[str, date]]) -> str:
    for name, bounds in splits.items():
        if bounds["from"] <= dt.date() <= bounds["through"]:
            return name
    return "unassigned"


def build_features(transactions: list[dict[str, Any]], openings: list[dict[str, Any]] | None = None,
                   splits: dict[str, dict[str, date]] | None = None) -> list[dict[str, Any]]:
    """계좌별 시간순으로 계산. 같은 시각은 완전한 source_sequence가 있을 때만 선후 처리."""
    openings = openings or []
    splits = splits or {}
    opening_by_account = {str(o["account_id"]): o for o in openings}
    by_account: dict[str, list[tuple[int, dict[str, Any], datetime, Decimal, str]]] = defaultdict(list)
    for input_idx, tx in enumerate(transactions):
        required = ("account_id", "transaction_type", "amount", "transacted_at")
        missing = [k for k in required if k not in tx]
        if missing:
            raise ValueError(f"필수 필드 누락: {missing}")
        kind = tx["transaction_type"]
        if kind not in ("DEPOSIT", "WITHDRAWAL"):
            raise ValueError(f"지원하지 않는 transaction_type: {kind}")
        amount = dec(tx["amount"])
        if amount is None or amount <= 0:
            raise ValueError("amount는 양수여야 합니다")
        dt = parse_time(tx["transacted_at"])
        by_account[str(tx["account_id"])].append((input_idx, tx, dt, amount, kind))

    output: list[dict[str, Any]] = []
    for account_id, items in by_account.items():
        # 순번이 주어진 동시각 행은 순번대로. 없는 순번은 뒤에 두되 해당 동시각 묶음에서
        # 서로의 선행 거래로 취급하지 않는다. 입력 순서와 source ID는 발생 순서로 쓰지 않는다.
        items.sort(key=lambda x: (x[2], x[1].get("source_sequence") is None,
                                  x[1].get("source_sequence") if x[1].get("source_sequence") is not None else 0,
                                  json.dumps(x[1], sort_keys=True, ensure_ascii=False)))
        history: list[tuple[datetime, str, Decimal]] = []
        previous_dt: datetime | None = None
        opening = opening_by_account.get(account_id)
        opening_dt = parse_time(opening["opening_at"]) if opening and opening.get("opening_at") else None
        ledger_balance = dec(opening.get("opening_balance")) if opening else None
        i = 0
        while i < len(items):
            j = i + 1
            while j < len(items) and items[j][2] == items[i][2]:
                j += 1
            same_time = items[i:j]
            seqs = [x[1].get("source_sequence") for x in same_time]
            tie_unordered = len(same_time) > 1 and (any(s is None for s in seqs) or len(set(seqs)) != len(seqs))
            ambiguous_group: list[tuple[int, dict[str, Any], datetime, Decimal, str]] = []
            group_history = list(history)
            group_previous_dt = previous_dt
            for item in same_time:
                _, tx, dt, amount, kind = item
                # 하나라도 순서가 불명확한 동시각 묶음은 해당 묶음 전체를 서로의 과거로 보지 않는다.
                active_history = group_history if tie_unordered else history
                active_previous_dt = group_previous_dt if tie_unordered else previous_dt
                # 창은 [t-W,t): 같은 시각 선행 원천 순번 행도 제외한다.
                prev_1h = [(t, k, a) for t, k, a in active_history if dt - WINDOW_1H <= t < dt]
                prev_24h = [(t, k, a) for t, k, a in active_history if dt - WINDOW_24H <= t < dt]
                prev_30d = [(t, k, a) for t, k, a in active_history if dt - WINDOW_30D <= t < dt and k == kind]
                median_sample = [a for _, _, a in prev_30d]
                balance_after = dec(tx.get("balance_after"))
                balance_before = None
                quality: list[str] = []
                if balance_after is not None:
                    balance_before = balance_after - amount if kind == "DEPOSIT" else balance_after + amount
                else:
                    quality.append("bank_balance_missing")
                if tie_unordered:
                    quality.append("same_timestamp_order_unknown")
                if tx.get("source_sequence") is None:
                    quality.append("source_sequence_missing")
                # 직전 은행 잔액은 거래 후 잔액에서 역산. 기초잔액은 장부 정합성 참조로만 사용.
                if ledger_balance is not None and dt >= (opening_dt or dt):
                    if balance_before is not None and ledger_balance != balance_before:
                        quality.append("ledger_balance_mismatch")
                    expected_after = ledger_balance + amount if kind == "DEPOSIT" else ledger_balance - amount
                    if balance_after is not None and expected_after != balance_after:
                        quality.append("bank_balance_arithmetic_mismatch")
                    ledger_balance = balance_after if balance_after is not None else expected_after
                elif ledger_balance is None and balance_after is not None:
                    ledger_balance = balance_after
                span_start = opening_dt if opening_dt and opening_dt <= dt else (active_history[0][0] if active_history else dt)
                seconds_since = (dt - active_previous_dt).total_seconds() if active_previous_dt else None
                minute = dt.hour * 3600 + dt.minute * 60 + dt.second + dt.microsecond / 1_000_000
                phase = 2 * math.pi * minute / 86400
                ratio = amount / median(median_sample) if len(median_sample) >= 5 and median(median_sample) > 0 else None
                if len(median_sample) < 5:
                    quality.append("same_direction_history_under_5")
                bal_ratio = amount / balance_before if kind == "WITHDRAWAL" and balance_before is not None and balance_before > 0 else None
                if kind == "WITHDRAWAL" and balance_before is not None and balance_before <= 0:
                    quality.append("balance_before_nonpositive")
                if kind == "WITHDRAWAL" and balance_before is None:
                    quality.append("withdrawal_balance_unavailable")
                if balance_before is not None and balance_before < amount and kind == "WITHDRAWAL":
                    quality.append("withdrawal_exceeds_balance_before")
                row = {
                    "account_id": tx["account_id"],
                    "source_transaction_id": tx.get("source_transaction_id"),
                    "source_sequence": tx.get("source_sequence"),
                    "source_provider": tx.get("source_provider"),
                    "transacted_at": dt.isoformat(),
                    "original_transacted_at": tx["transacted_at"],
                    "transaction_type": kind,
                    "amount": str(amount),
                    "description": tx.get("description"),
                    "split": assign_split(dt, splits),
                    "features": {
                        "amount_log1p": math.log1p(float(amount)),
                        "is_withdrawal": int(kind == "WITHDRAWAL"),
                        "hour_sin": math.sin(phase), "hour_cos": math.cos(phase),
                        "seconds_since_previous": seconds_since,
                        "withdrawal_count_prev_1h": sum(k == "WITHDRAWAL" for _, k, _ in prev_1h),
                        "withdrawal_count_prev_24h": sum(k == "WITHDRAWAL" for _, k, _ in prev_24h),
                        "withdrawal_sum_prev_24h": str(sum((a for _, k, a in prev_24h if k == "WITHDRAWAL"), Decimal(0))),
                        "amount_to_same_direction_median_30d": num(ratio),
                        "withdrawal_to_balance_before": num(bal_ratio),
                    },
                    "context": {
                        "prior_transaction_count": len(active_history),
                        "same_direction_count_prev_30d": len(median_sample),
                        "history_span_days": (dt - span_start).total_seconds() / 86400,
                        "history_span_start": span_start.isoformat(),
                        "balance_before": str(balance_before) if balance_before is not None else None,
                        "balance_after": str(balance_after) if balance_after is not None else None,
                        "cold_start": len(median_sample) < 5,
                        "quality_flags": sorted(set(quality)),
                    },
                }
                output.append(row)
                if tie_unordered:
                    ambiguous_group.append(item)
                else:
                    history.append((dt, kind, amount))
                    previous_dt = dt
            # 순서 미상 동시각 거래는 그 시각 이후의 과거에는 포함하지만 서로의 선행값으로는 쓰지 않는다.
            for _, tx, dt, amount, kind in ambiguous_group:
                history.append((dt, kind, amount))
                previous_dt = max(previous_dt, dt) if previous_dt else dt
            i = j
    output.sort(key=lambda r: (r["transacted_at"], str(r["account_id"]), r.get("source_sequence") is None,
                               r.get("source_sequence") or 0, r.get("source_transaction_id") or ""))
    return output


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as f:
        for row in rows:
            f.write(json.dumps(row, ensure_ascii=False, separators=(",", ":")) + "\n")


def main() -> None:
    ap = argparse.ArgumentParser(description="MOA 과거 기반 거래 특성 생성기")
    ap.add_argument("--transactions", type=Path, required=True)
    ap.add_argument("--opening-balances", type=Path)
    ap.add_argument("--splits", type=Path)
    ap.add_argument("--output", type=Path, required=True)
    args = ap.parse_args()
    txs = load_jsonl(args.transactions)
    openings = json.loads(args.opening_balances.read_text(encoding="utf-8")) if args.opening_balances else []
    splits = load_splits(args.splits) if args.splits else {}
    rows = build_features(txs, openings, splits)
    out = args.output
    write_jsonl(out / "features.jsonl", rows)
    for split in ("train", "validation", "test", "unassigned"):
        write_jsonl(out / f"{split}.jsonl", [r for r in rows if r["split"] == split])
    (out / "feature_names.json").write_text(json.dumps(FEATURE_NAMES, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    missing = Counter()
    for row in rows:
        for name in FEATURE_NAMES:
            if row["features"].get(name) is None:
                missing[name] += 1
    reasons = Counter(flag for row in rows for flag in row["context"]["quality_flags"])
    periods = Counter(r["transacted_at"][:7] for r in rows)
    summary = ["# 특성 생성 요약", "", f"- 입력 거래: {len(txs)}건", f"- 출력 행: {len(rows)}건", f"- 계좌: {len({str(r['account_id']) for r in rows})}개", f"- 전체 특성 null 수: {sum(missing.values())}건", f"- cold_start: {sum(r['context']['cold_start'] for r in rows)}건", "", "## 월별 건수", ""]
    summary += [f"- {m}: {periods[m]}건" for m in sorted(periods)]
    summary += ["", "## 특성별 결측 수", ""] + [f"- {n}: {missing[n]}건" for n in FEATURE_NAMES]
    summary += ["", "## 품질 플래그 건수", ""] + ([f"- {k}: {v}건" for k, v in sorted(reasons.items())] or ["- 없음"])
    summary += ["", "라벨 파일은 열거나 결합하지 않았습니다. 특성 값은 이상 확률이나 탐지 성능이 아닙니다.", ""]
    (out / "summary.md").write_text("\n".join(summary), encoding="utf-8")


if __name__ == "__main__":
    main()
