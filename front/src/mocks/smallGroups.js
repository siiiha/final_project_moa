export const smallGroupRegions = ['전체', '서울 마포구', '서울 성동구', '서울 광진구', '서울 용산구'];

export const smallGroupInterests = ['전체', '운동·러닝', '문화·취미', '사진·여행', '독서'];

export const smallGroups = [
  {
    id: 1,
    name: '걷기 좋은 날',
    region: '서울 마포구',
    interest: '운동·러닝',
    participants: '18/20명',
    description: '마포구와 한강 주변을 함께 걸으며 건강한 일상을 만들어요.',
    createdAt: '2026-10-14',
  },
  {
    id: 2,
    name: '동네 책모임',
    region: '서울 마포구',
    interest: '문화·취미',
    participants: '9/12명',
    description: '매주 목요일 서로의 생각을 나누는 동네 책모임입니다.',
    createdAt: '2026-10-11',
  },
  {
    id: 3,
    name: '주말 사진 산책',
    region: '서울 마포구',
    interest: '사진·여행',
    participants: '12/15명',
    description: '카메라와 휴대폰으로 일상의 순간을 함께 기록해요.',
    createdAt: '2026-10-08',
  },
  {
    id: 4,
    name: '함께하는 등산',
    region: '서울 마포구',
    interest: '운동·러닝',
    participants: '6/10명',
    description: '가까운 산과 둘레길을 함께 오르며 안전하게 즐겨요.',
    createdAt: '2026-10-03',
  },
  {
    id: 5,
    name: '퇴근 후 드로잉',
    region: '서울 마포구',
    interest: '문화·취미',
    participants: '7/10명',
    description: '가벼운 드로잉으로 하루를 마무리하는 저녁 모임입니다.',
    createdAt: '2026-09-29',
  },
  {
    id: 6,
    name: '주말 브런치 탐방',
    region: '서울 마포구',
    interest: '사진·여행',
    participants: '11/14명',
    description: '동네의 새로운 브런치 가게를 함께 찾아가요.',
    createdAt: '2026-09-25',
  },
  {
    id: 7,
    name: '아침 독서 습관',
    region: '서울 마포구',
    interest: '독서',
    participants: '8/10명',
    description: '출근 전 30분, 각자 읽은 책을 나누는 모임입니다.',
    createdAt: '2026-09-20',
  },
  {
    id: 8,
    name: '동네 플로깅 크루',
    region: '서울 마포구',
    interest: '운동·러닝',
    participants: '15/20명',
    description: '걷고 달리며 우리 동네를 더 깨끗하게 만들어요.',
    createdAt: '2026-09-15',
  },
  {
    id: 9,
    name: '영화 한 편의 여유',
    region: '서울 마포구',
    interest: '문화·취미',
    participants: '10/12명',
    description: '주말마다 영화를 보고 자유롭게 감상을 나눠요.',
    createdAt: '2026-09-10',
  },
  {
    id: 10,
    name: '한강 자전거 라이딩',
    region: '서울 마포구',
    interest: '운동·러닝',
    participants: '13/16명',
    description: '한강 자전거길을 안전하게 함께 달리는 모임입니다.',
    createdAt: '2026-09-04',
  },
];

export const smallGroupDetails = {
  1: {
    status: '모집 중',
    nextMeeting: '매주 토요일 오전 9시',
    meetingPlace: '마포구 망원 한강공원',
    notices: [
      { id: 1, title: '첫 모임 안내', author: '모아이웃', date: '10.04' },
      { id: 2, title: '참여 준비물 안내', author: '운영진', date: '10.03' },
      { id: 3, title: '비가 오는 날 일정 안내', author: '함께걷기', date: '10.01' },
    ],
  },
};

export const defaultSmallGroupDetail = {
  status: '모집 중',
  nextMeeting: '일정 조율 중',
  meetingPlace: '모임에서 추후 안내',
  notices: [
    { id: 1, title: '소모임 공지사항', author: '운영진', date: '오늘' },
  ],
};
