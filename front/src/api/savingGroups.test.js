import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroups, getSavingGroupPages, savingGroupRegionOptions, toSavingGroupCard,
  getSavingGroupDetail, getSavingGroupMenus, formatSavingGroupAmount, requestSavingGroupJoin,
  approveSavingGroupJoin, rejectSavingGroupJoin, getSavingGroupJoinRequests } from './savingGroups.js';

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

// AI작성: 화면 표시 금액과 원본 값을 구분하고 null을 무료로 바꾸지 않는다.
test('금액 표시의 천 단위 구분·소수 유지·null·잘못된 값', () => {
  assert.equal(formatSavingGroupAmount('100000.00'), '100,000원');
  assert.equal(formatSavingGroupAmount('1234.50'), '1,234.5원');
  assert.equal(formatSavingGroupAmount(0), '0원');
  assert.equal(formatSavingGroupAmount(null), '회비 미정');
  assert.equal(formatSavingGroupAmount('invalid'), '회비 확인 필요');
  assert.equal(formatSavingGroupAmount(-1), '회비 확인 필요');
});

// AI작성: 큰 URL ID를 Number로 바꾸지 않고 취소 신호·원본 상세·오류를 유지한다.
test('상세 요청은 URL ID와 취소 신호를 보존하고 실패를 숨기지 않는다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  const detail = { groupId: '9223372036854775807', notices: [], myRole: null };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/9223372036854775807');
      assert.equal(options.signal, signal);
      return { data: { success: true, data: detail } };
    };
    assert.equal(await getSavingGroupDetail(detail.groupId, { signal }), detail);
    client.get = async () => ({ data: { success: false, error: { message: '상세 오류' } } });
    await assert.rejects(() => getSavingGroupDetail('42'), /상세 오류/);
    for (const status of [400, 403, 404, 500]) {
      const error = Object.assign(new Error('요청 실패'), { response: { status } });
      client.get = async () => { throw error; };
      await assert.rejects(() => getSavingGroupDetail('42'), (value) => value === error);
    }
  } finally { client.get = original; }
});

// AI작성: 미정과 이상 금액을 구분하고 소수를 반올림해 숨기지 않는다.
test('빈 값 대체 문구와 진행중 배지, 소수 회비 확인 표시', () => {
  const card = toSavingGroupCard({ description: ' ', memberCount: 0, status: '02' });
  assert.equal(card.description, '소개글이 없습니다.');
  assert.equal(card.members, '0명 / 정원 미정');
  assert.equal(card.monthlyDues, '회비 미정');
  assert.equal(card.nextDate, '미정');
  assert.equal(card.status, '진행중');
  assert.equal(toSavingGroupCard({ duesAmount: '123.50' }).monthlyDues, '회비 확인 필요');
});

// AI작성: 전체 5·6페이지의 묶음과 빈 결과·범위 초과를 확인한다.
test('페이지 번호는 최대 다섯 개이며 초과한 번호를 만들지 않는다', () => {
  assert.deepEqual(getSavingGroupPages(1, 5), [1, 2, 3, 4, 5]);
  assert.deepEqual(getSavingGroupPages(5, 6), [1, 2, 3, 4, 5]);
  assert.deepEqual(getSavingGroupPages(6, 6), [6]);
  assert.deepEqual(getSavingGroupPages(6, 10), [6, 7, 8, 9, 10]);
  assert.deepEqual(getSavingGroupPages(1, 0), []);
  assert.deepEqual(getSavingGroupPages(3, 2), []);
});

// AI작성: 실제 네트워크 호출 없이 파라미터 유지와 공통 응답 성공·실패를 확인한다.
test('검색·지역·페이지를 전달하고 API 실패를 정상 데이터로 처리하지 않는다', async () => {
  const original = client.get;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups');
      assert.deepEqual(options.params, { keyword: '산책', regionCode: '01', page: 2 });
      return { data: { success: true, data: [], meta: { page: 2, size: 6, total: 7, totalPages: 2 } } };
    };
    const result = await getSavingGroups({ keyword: '산책', regionCode: '01', page: 2 });
    assert.deepEqual(result, {
      items: [], totalElements: 7, totalPages: 2, currentPage: 2, pageSize: 6,
    });
    client.get = async () => ({ data: { success: false, error: { message: '요청 오류' } } });
    await assert.rejects(() => getSavingGroups({ page: 1 }), /요청 오류/);
    client.get = async () => { throw new Error('network failure'); };
    await assert.rejects(() => getSavingGroups({ page: 1 }), /network failure/);
  } finally { client.get = original; }
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

  try {
    const result = await requestSavingGroupJoin(12);

    assert.equal(result.success, true);
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});

// AI작성: 새 의존성 없이 기존 Axios adapter와 Node 테스트로 실제 목록·승인 계약을 검증한다.
async function withAdapter(adapter, action) {
  const original = client.defaults.adapter;
  client.defaults.adapter = adapter;
  try { await action(); } finally { client.defaults.adapter = original; }
}

function ok(config, data = null, meta = null) {
  return { data: { success: true, data, meta, error: null }, status: 200, headers: {}, config };
}

test('신청 목록은 서버 필터와 페이지를 전달하고 마스킹 이름·문자열 ID를 유지한다', async () => {
  const controller = new AbortController();
  await withAdapter(async (config) => {
    assert.equal(config.url, '/saving-groups/12/join-requests');
    assert.equal(config.params.status, 'PENDING');
    assert.equal(config.params.page, 2);
    assert.equal(config.signal, controller.signal);
    return ok(config, [{ crewMemberId: '9007199254740993', applicant: '가**', regionName: '서울',
      identityVerified: true, regionVerified: true, applicationMessage: null, membershipStatus: 'PENDING' }],
    { page: 2, size: 10, total: 11, totalPages: 2 });
  }, async () => {
    const result = await getSavingGroupJoinRequests('12', { status: 'PENDING', page: 2, signal: controller.signal });
    assert.equal(result.items[0].id, '9007199254740993');
    assert.equal(result.items[0].applicant, '가**');
    assert.equal(result.items[0].status, '대기');
    assert.equal(result.items[0].verification, '본인·지역 인증 완료');
    assert.equal(result.items[0].message, '신청 메시지가 없습니다.');
    assert.equal(result.meta.total, 11);
  });
});

test('승인은 원래 신청 ID를 경로에 넣고 요청 본문과 승인자를 보내지 않는다', async () => {
  await withAdapter(async (config) => {
    assert.equal(config.method, 'patch');
    assert.equal(config.url, '/saving-groups/12/join-requests/9007199254740993/approve');
    assert.equal(config.data, undefined);
    assert.equal(config.params, undefined);
    return ok(config);
  }, async () => assert.equal((await approveSavingGroupJoin('12', '9007199254740993')).success, true));
});

test('거절은 원래 신청 ID를 경로에 넣고 요청 본문과 처리자를 보내지 않는다', async () => {
  await withAdapter(async (config) => {
    assert.equal(config.method, 'patch');
    assert.equal(config.url, '/saving-groups/12/join-requests/9007199254740993/reject');
    assert.equal(config.data, undefined);
    assert.equal(config.params, undefined);
    return ok(config);
  }, async () => assert.equal((await rejectSavingGroupJoin('12', '9007199254740993')).success, true));
});

for (const [status, code] of [[401, 'UNAUTHORIZED'], [403, 'SAVING_GROUP_OWNER_REQUIRED'],
  [404, 'JOIN_REQUEST_NOT_FOUND'], [409, 'JOIN_REQUEST_NOT_PENDING'], [409, 'SAVING_GROUP_CAPACITY_FULL'],
  [409, 'SAVING_GROUP_JOIN_LIMIT_EXCEEDED']]) {
  test(`승인 실패 ${status} ${code}는 성공으로 바꾸지 않는다`, async () => {
    await withAdapter(async () => {
      const error = new Error('HTTP 요청 실패');
      error.response = { status, data: { success: false, error: { code, message: '승인 조건을 확인해주세요.' } } };
      throw error;
    }, async () => assert.rejects(approveSavingGroupJoin('12', '50'),
      (error) => error.code === code && error.message === '승인 조건을 확인해주세요.'));
  });
}

for (const [status, code] of [[401, 'UNAUTHORIZED'], [403, 'SAVING_GROUP_OWNER_REQUIRED'],
  [404, 'JOIN_REQUEST_NOT_FOUND'], [409, 'JOIN_REQUEST_NOT_PENDING'],
  [409, 'JOIN_REQUEST_REJECTION_FAILED']]) {
  test(`거절 실패 ${status} ${code}는 성공으로 바꾸지 않는다`, async () => {
    await withAdapter(async () => {
      const error = new Error('HTTP 요청 실패');
      error.response = { status, data: { success: false, error: { code, message: '거절 조건을 확인해주세요.' } } };
      throw error;
    }, async () => assert.rejects(rejectSavingGroupJoin('12', '50'),
      (error) => error.code === code && error.message === '거절 조건을 확인해주세요.'));
  });
}

test('인증 없는 목록 실패는 빈 목록이나 Mock으로 대체하지 않는다', async () => {
  await withAdapter(async (config) => ({ ...ok(config), data: { success: false,
    error: { code: 'UNAUTHORIZED', message: '로그인이 필요합니다.' } } }),
  async () => assert.rejects(getSavingGroupJoinRequests('12'), (error) => error.code === 'UNAUTHORIZED'));
});

test('조회 취소는 취소 상태를 유지한다', async () => {
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(getSavingGroupJoinRequests('12', { signal: controller.signal }), (error) => error.code === 'ERR_CANCELED');
});

test('잘못된 ID와 정밀도가 손실된 숫자는 네트워크 호출 전에 차단한다', async () => {
  let called = false;
  await withAdapter(async (config) => { called = true; return ok(config); }, async () => {
    for (const id of [null, 0, -1, 'abc', '../1', '9223372036854775808', 9007199254740992]) {
      await assert.rejects(approveSavingGroupJoin('12', id), (error) => error.code === 'BAD_REQUEST');
    }
  });
  assert.equal(called, false);
});

test('잘못된 목록 응답은 사용자용 오류로 처리한다', async () => {
  await withAdapter(async (config) => ok(config), async () => {
    await assert.rejects(getSavingGroupJoinRequests('12'), (error) => error.code === 'INVALID_RESPONSE');
  });
});

test('신청 0건은 정상 빈 목록이다', async () => {
  await withAdapter(async (config) => ok(config, [], { page: 1, size: 10, total: 0, totalPages: 0 }),
    async () => assert.deepEqual((await getSavingGroupJoinRequests('12')).items, []));
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
