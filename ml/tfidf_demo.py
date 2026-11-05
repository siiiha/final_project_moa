"""가맹점명 TF-IDF 변환의 작은 예제입니다."""

from sklearn.feature_extraction.text import TfidfVectorizer


# AI작성: 같은 단어가 여러 가맹점에 반복되면 점수는 낮아지고, 구분에 도움이 되는 단어는 높아집니다.
MERCHANT_NAMES = [
    "행복한식당",
    "동네카페",
    "서울역 KTX",
    "공영주차장",
    "OO호텔",
    "바다펜션",
]


def print_tfidf_scores() -> None:
    """가맹점명을 문자 단위 TF-IDF 숫자로 바꾼 결과를 출력합니다."""
    # AI작성: 한국어 가맹점명은 띄어쓰기·형태가 일정하지 않아 우선 문자 2~3글자 단위로 살펴봅니다.
    vectorizer = TfidfVectorizer(analyzer="char", ngram_range=(2, 3))
    matrix = vectorizer.fit_transform(MERCHANT_NAMES)
    terms = vectorizer.get_feature_names_out()

    for merchant_name, row in zip(MERCHANT_NAMES, matrix):
        top_indexes = row.toarray()[0].argsort()[-3:][::-1]
        top_scores = ", ".join(f"{terms[index]}={row[0, index]:.3f}" for index in top_indexes)
        print(f"{merchant_name}: {top_scores}")


if __name__ == "__main__":
    print_tfidf_scores()
