import assert from 'node:assert/strict';
import { test } from 'node:test';
import client from './client.js';
import { requestSavingGroupJoin } from './savingGroups.js';
import { getSavingGroups, getSavingGroupPages, savingGroupRegionOptions, toSavingGroupCard,
  getSavingGroupDetail, getSavingGroupMenus, formatSavingGroupAmount } from './savingGroups.js';

// AI작성: 추가 테스트 패키지 없이 카드 표시와 페이징·API 실패 계약을 검증한다.
test('지역 코드는 01~10 순서이며 카드는 기존 필드와 표시 형식을 유지한다', () => {
  assert.equal(savingGroupRegionOptions[0].name, '서울');
  assert.equal(savingGroupRegionOptions[9].code, '10');
  const card = toSavingGroupCard({ savingGroupId: 101, groupId: 42, groupName: '테스트', description: '소개',
    regionName: '서울', maxMembers: 8, memberCount: 5, duesAmount: '50000.00', duesDay: 25, status: '01' });
  assert.equal(card.members, '5 / 8명');
  assert.equal(card.id, 42);
  assert.equal(card.monthlyDues, '월 50,000원');
  assert.equal(card.nextDate, '매월 25일');
  assert.equal(card.status, '모집중');
  assert.equal(card.district, '서울');
});

// AI작성: 미가입·비승인은 공지만, 회계·계원은 같은 여섯 탭, 계주는 아홉 탭이다.
test('승인 역할별 메뉴와 미정의 역할의 권한 축소', () => {
  const common = ['notice', 'payment', 'calendar', 'dues', 'transactionHistory', 'payoutRequest'];
  for (const role of [null, undefined, '', 'PENDING', 'REJECTED', 'LEFT', 'ADMIN']) {
    assert.deepEqual(getSavingGroupMenus(role), ['notice']);
  }
  assert.deepEqual(getSavingGroupMenus('MANAGER'), common);
  assert.deepEqual(getSavingGroupMenus('MEMBER'), common);
  assert.deepEqual(getSavingGroupMenus('OWNER'), [...common, 'members', 'settings', 'joinRequests']);
});

test('가입 신청 성공 응답을 그대로 반환한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async (config) => ({
    data: { success: true, data: null, meta: null, error: null },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  });
// AI작성: 화면 표시 금액과 원본 값을 구분하고 null을 무료로 바꾸지 않는다.
test('금액 표시의 천 단위 구분·소수 유지·null·잘못된 값', () => {
  assert.equal(formatSavingGroupAmount('100000.00'), '100,000원');
  assert.equal(formatSavingGroupAmount('1234.50'), '1,234.5원');
  assert.equal(formatSavingGroupAmount(0), '0원');
  assert.equal(formatSavingGroupAmount(null), '회비 미정');
  assert.equal(formatSavingGroupAmount('invalid'), '회비 확인 필요');
  assert.equal(formatSavingGroupAmount(-1), '회비 확인 필요');
});

  try {
    const result = await requestSavingGroupJoin(12);

    assert.equal(result.success, true);
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});

test('본인 인증 미완료 응답의 코드와 메시지를 보존한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async () => {
    const error = new Error('Forbidden');
    error.response = {
      data: {
        success: false,
        error: {
          code: 'IDENTITY_VERIFICATION_REQUIRED',
          message: '본인 인증 완료 후 가입 신청할 수 있습니다.',
        },
      },
      status: 403,
    };
    throw error;
  };

  try {
    await assert.rejects(
      requestSavingGroupJoin(12),
      (error) => error.code === 'IDENTITY_VERIFICATION_REQUIRED'
        && error.message === '본인 인증 완료 후 가입 신청할 수 있습니다.',
    );
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});
