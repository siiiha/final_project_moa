// AI작성: 계금 지급은 일반 지출과 성격이 달라 거래 목적과 지출 분류를 분리한다.
export const savingGroupTransactions = [
  { id: 1, date: '2026-10-03', time: '09:20', type: '입금', purposeType: '회비', description: '김영희 회비', merchantName: '김영희', category: '회비', amount: 30000, balance: 4820000 },
  { id: 2, date: '2026-10-02', time: '16:30', type: '출금', purposeType: '일반 지출', description: '성수아트홀 체크카드', merchantName: '성수아트홀', category: '대관료', amount: -180000, balance: 4790000 },
  { id: 3, date: '2026-09-28', time: '12:10', type: '출금', purposeType: '일반 지출', description: '오늘의다과 체크카드', merchantName: '오늘의다과', category: '식비', amount: -100000, balance: 4970000 },
  { id: 4, date: '2026-09-25', time: '10:18', type: '입금', purposeType: '회비', description: '이정호 회비', merchantName: '이정호', category: '회비', amount: 30000, balance: 5070000 },
  { id: 5, date: '2026-09-23', time: '18:05', type: '출금', purposeType: '일반 지출', description: '프린트카페 체크카드', merchantName: '프린트카페', category: '물품비', amount: -24000, balance: 5040000 },
  { id: 6, date: '2026-09-20', time: '14:40', type: '입금', purposeType: '회비', description: '박민수 회비', merchantName: '박민수', category: '회비', amount: 30000, balance: 5064000 },
  { id: 7, date: '2026-09-18', time: '11:26', type: '출금', purposeType: '일반 지출', description: '공용 계좌 이체 수수료', merchantName: '은행 이체 수수료', category: '수수료', amount: -500, balance: 5034000 },
  { id: 8, date: '2026-09-15', time: '09:30', type: '입금', purposeType: '회비', description: '윤서연 회비', merchantName: '윤서연', category: '회비', amount: 30000, balance: 5034500 },
  { id: 9, date: '2026-09-12', time: '17:12', type: '출금', purposeType: '일반 지출', description: '동네밥상 체크카드', merchantName: '동네밥상', category: '식비', amount: -96000, balance: 5004500 },
  { id: 10, date: '2026-09-10', time: '08:55', type: '입금', purposeType: '회비', description: '한서준 회비', merchantName: '한서준', category: '회비', amount: 30000, balance: 5100500 },
  { id: 11, date: '2026-09-06', time: '13:08', type: '출금', purposeType: '일반 지출', description: '다이소 체크카드', merchantName: '다이소', category: '물품비', amount: -42000, balance: 5070500 },
  { id: 12, date: '2026-09-03', time: '10:02', type: '입금', purposeType: '회비', description: '문지아 회비', merchantName: '문지아', category: '회비', amount: 30000, balance: 5112500 },
  { id: 13, date: '2026-09-01', time: '15:30', type: '출금', purposeType: '계금 지급', description: '최민호 계금 지급', merchantName: '최민호', category: '계금 지급', amount: -360000, balance: 5082500 },
];
