export const memberSummary = {
  name: '모아이웃',
  email: 'moa@example.com',
  badges: ['본인 인증 완료', '지역 인증 완료'],
  stats: [
    { label: '계정 등급', value: '우수 회원' },
    { label: '가입한 모임', value: '12회' },
    { label: '참여 모임', value: '5개' },
  ],
};

export const memberMenuItems = [
  { id: 'overview', label: '마이페이지', icon: 'overview' },
  { id: 'verify', label: '인증관리', icon: 'verify' },
  { id: 'alerts', label: '알림 목록', icon: 'alerts' },
];

export const participatingMeetings = [
  { id: 1, title: '성동 이웃 친목계', description: '이웃과 좋은 정보를 나누는 친목 모임', meta: '12명 · 서울 성동구' },
  {
    id: 2,
    title: '함께 모으는 소비계',
    description: '매달 함께 모아 소비계에 돈이 되는 모임',
    meta: '8명 · 서울 성동구',
  },
  { id: 3, title: '드로잉 산책', description: '건강한 동기와 이웃과 걷는 나눔 모임', meta: '15명 · 서울 성동구' },
];

export const quickLinks = [
  { id: 'verify', title: '인증관리 ›', description: '본인 인증 상태를 확인하고 관리하세요.', icon: 'verify' },
  {
    id: 'location',
    title: '지역인증 ›',
    description: '내 활동 지역을 인증하고 가까운 이웃을 만나세요.',
    icon: 'location',
  },
  { id: 'alerts', title: '내 알림 ›', description: '새로운 소식과 모임 알림을 확인하세요.', icon: 'alerts' },
];

export const withdrawalStatus = {
  title: '탈퇴 전 확인사항',
  description: '회원탈퇴를 진행하면 계정과 서비스 이용 정보가 삭제되며 일부 정보는 복구할 수 없습니다.',
  checks: [
    { label: '참여 계모임', value: '0개' },
    { label: '미정산 금액', value: '0원' },
    { label: '진행 상태', value: '탈퇴 가능' },
  ],
};

export const verificationItems = [
  {
    id: 'identity',
    title: '본인 인증',
    status: '인증 완료',
    description: '본인 명의의 휴대전화를 인증합니다.',
    actionLabel: '본인 인증 안내',
  },
  {
    id: 'region',
    title: '지역 인증',
    status: '인증 완료',
    description: '활동 지역 · 서울 성동구',
    actionLabel: '지역 인증 관리',
  },
];

export const notificationItems = [
  {
    id: 1,
    title: '성현 첫계가 가입이 승인되었습니다.',
    summary: '이제 모임의 알림과 회비 내역을 확인할 수 있어요.',
    date: '2026.10.05 · 새 알림',
    detail: '성현 첫계의 계원으로 가입되었습니다. 모임 페이지에서 다음 회비 납부일과 모임 일정을 확인해보세요.',
  },
  {
    id: 2,
    title: '10월 회비 납부일을 확인해주세요.',
    summary: '매월 10일 · 월 회비 30,000원',
    date: '2026.10.05 · 새 알림',
    detail: '이번 달 회비 납부일은 10월 10일입니다. 납부 현황 메뉴에서 회비 납부 상태를 확인할 수 있습니다.',
  },
  {
    id: 3,
    title: '내 게시글에 새로운 댓글이 달렸습니다.',
    summary: '함께걷기님이 댓글을 남겼습니다.',
    date: '2026.10.05 · 새 알림',
    detail:
      '내가 작성한 게시글에 새로운 댓글이 등록되었습니다. 게시글 상세 화면에서 댓글 내용을 확인하고 답글을 남겨보세요.',
  },
];
