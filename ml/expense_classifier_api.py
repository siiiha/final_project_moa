"""Spring 서버가 호출하는 카드 지출 분류 내부 API입니다."""

from decimal import Decimal

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from predict_expense_category import predict_expense_category


app = FastAPI(title="MOA 카드 지출 분류 API", version="1.0.0")


class ExpenseClassificationRequest(BaseModel):
    """AI작성: 분류에 필요한 가맹점명과 참고용 금액만 받는다."""

    merchant_name: str = Field(min_length=1, max_length=255)
    amount: Decimal | None = Field(default=None, ge=0)


@app.get("/health")
def health_check() -> dict[str, str]:
    """Spring 서버가 분류 API 실행 상태를 확인한다."""
    return {"status": "ok"}


@app.post("/classifications")
def classify_expense(request: ExpenseClassificationRequest) -> dict:
    """AI작성: 자동 추천만 반환하며 지출 분류를 확정하거나 저장하지 않는다."""
    try:
        result = predict_expense_category(request.merchant_name)
    except ValueError as exception:
        raise HTTPException(status_code=400, detail=str(exception)) from exception

    # AI작성: 금액은 현재 모델의 확정 근거가 아니므로 요청값을 그대로 분류 결과에만 함께 돌려준다.
    result["amount"] = request.amount
    return result
