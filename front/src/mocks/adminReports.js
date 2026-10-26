export const adminReports = [
  { id: 15, reason: '욕설·비방', target: '동네소식의 댓글', reporter: '김하늘', reportedAt: '2026.10.05', status: '대기' },
  { id: 14, reason: '광고·홍보', target: '중고 가구 판매합니다', reporter: '이도윤', reportedAt: '2026.10.05', status: '처리 완료' },
  { id: 13, reason: '부적절한 콘텐츠', target: '주말 모임 참여자 모집', reporter: '박수진', reportedAt: '2026.10.04', status: '대기' },
  { id: 12, reason: '욕설·비방', target: '이웃모임 게시글의 댓글', reporter: '최유진', reportedAt: '2026.10.04', status: '처리 완료' },
  { id: 11, reason: '개인정보 노출', target: '동네 장터 거래 안내', reporter: '정민서', reportedAt: '2026.10.03', status: '대기' },
  { id: 10, reason: '광고·홍보', target: '무료 체험 이벤트 안내', reporter: '한지민', reportedAt: '2026.10.03', status: '처리 완료' },
  { id: 9, reason: '부적절한 콘텐츠', target: '오늘의 동네 소식', reporter: '윤서준', reportedAt: '2026.10.02', status: '대기' },
  { id: 8, reason: '욕설·비방', target: '계모임 회비 관련 댓글', reporter: '김영희', reportedAt: '2026.10.02', status: '처리 완료' },
  { id: 7, reason: '기타', target: '지역 행사 참여 후기', reporter: '이민수', reportedAt: '2026.10.01', status: '대기' },
  { id: 6, reason: '광고·홍보', target: '주말 아르바이트 모집', reporter: '박지훈', reportedAt: '2026.10.01', status: '처리 완료' },
  { id: 5, reason: '개인정보 노출', target: '소모임 가입 인사글', reporter: '최도윤', reportedAt: '2026.09.30', status: '대기' },
  { id: 4, reason: '욕설·비방', target: '동네 반려동물 모임', reporter: '정하늘', reportedAt: '2026.09.30', status: '처리 완료' },
  { id: 3, reason: '부적절한 콘텐츠', target: '계모임 순번 안내 글', reporter: '윤지아', reportedAt: '2026.09.29', status: '대기' },
  { id: 2, reason: '기타', target: '우리 동네 나눔 소식', reporter: '한서준', reportedAt: '2026.09.29', status: '처리 완료' },
  { id: 1, reason: '광고·홍보', target: '지역 업체 할인 안내', reporter: '김소연', reportedAt: '2026.09.28', status: '대기' },
];

const reportedMemberById = {
  15: { name: '동네소식', email: 'neighborhood@example.com' },
  14: { name: '중고마켓', email: 'market@example.com' },
  13: { name: '주말모임', email: 'weekend@example.com' },
  12: { name: '이웃모임', email: 'neighbor@example.com' },
  11: { name: '동네장터', email: 'marketplace@example.com' },
};

export const adminReportDetails = Object.fromEntries(
  adminReports.map((report) => {
    const reportedMember = reportedMemberById[report.id] || {
      name: '모아 회원',
      email: `member${report.id}@example.com`,
    };

    return [
      report.id,
      {
        ...reportedMember,
        targetType: report.target.includes('댓글') ? '댓글' : '게시글',
        reportContent: `${report.target} 콘텐츠가 ${report.reason} 사유에 해당하는지 확인이 필요하여 신고합니다.`,
        receivedAt: `${report.reportedAt} 14:20`,
      },
    ];
  })
);
