// AI작성: 카드사 실제 엑셀 양식이 확정되기 전, 분류 검토 화면 동작을 확인하기 위한 가상 데이터입니다.
export const cardExpenseClassificationMock = [
  { id: 1, usedAt: '2026-11-03 12:18', merchantName: '사강식당', amount: 96000, approvalStatus: '승인', suggestedCategory: '식비', confidenceScore: 0.98, classificationStatus: '검토 대기', classificationSource: '키워드 규칙' },
  { id: 2, usedAt: '2026-11-03 16:40', merchantName: '시민회관 대관', amount: 180000, approvalStatus: '승인', suggestedCategory: '대관료', confidenceScore: 0.96, classificationStatus: '검토 대기', classificationSource: '키워드 규칙' },
  { id: 3, usedAt: '2026-11-02 10:15', merchantName: '대덕문화센터', amount: 5000, approvalStatus: '승인', suggestedCategory: '활동비', confidenceScore: 0.61, classificationStatus: '검토 대기', classificationSource: '머신러닝' },
  { id: 4, usedAt: '2026-11-01 14:05', merchantName: '네이버페이', amount: 38500, approvalStatus: '승인', suggestedCategory: '', confidenceScore: null, classificationStatus: '확인 필요', classificationSource: '결제대행사' },
  { id: 5, usedAt: '2026-10-31 19:20', merchantName: '보드게임 카페', amount: 24000, approvalStatus: '승인', suggestedCategory: '', confidenceScore: 0.25, classificationStatus: '확인 필요', classificationSource: '머신러닝' },
  { id: 6, usedAt: '2026-10-30 09:10', merchantName: '고속버스', amount: 62400, approvalStatus: '승인', suggestedCategory: '교통비', confidenceScore: 0.97, classificationStatus: '검토 대기', classificationSource: '키워드 규칙' },
  { id: 7, usedAt: '2026-10-29 13:30', merchantName: '아웃도어플러스', amount: 132000, approvalStatus: '승인', suggestedCategory: '물품비', confidenceScore: 0.89, classificationStatus: '검토 대기', classificationSource: '머신러닝' },
  { id: 8, usedAt: '2026-10-28 11:00', merchantName: '해오름펜션', amount: 320000, approvalStatus: '취소', suggestedCategory: '', confidenceScore: null, classificationStatus: '처리 보류', classificationSource: '취소 건' },
];

export const expenseCategories = ['식비', '교통비', '대관료', '물품비', '활동비', '숙박비', '수수료', '기타'];
