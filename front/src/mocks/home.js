// AI작성: 메인페이지 서비스 카드에 기존 목록 페이지 경로를 연결합니다.
export const homeServiceCards = [
  {
    title: '계모임',
    description: '이웃과 함께 회비를 모으고\n투명하게 관리하세요.',
    actionLabel: '둘러보기',
    to: '/saving-groups',
    image: '/moa-card-saving.png',
  },
  {
    title: '소모임',
    description: '관심사가 같은 이웃과\n가볍게 만나보세요.',
    actionLabel: '둘러보기',
    to: '/small-groups',
    image: '/moa-card-small.png',
  },
  {
    title: '동네 게시판',
    description: '우리 동네 소식과\n생활 정보를 나눠보세요.',
    actionLabel: '둘러보기',
    to: '/post/list',
    image: '/moa-card-board.png',
  },
];
