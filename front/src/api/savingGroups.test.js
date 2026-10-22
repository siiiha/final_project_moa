import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { approveSavingGroupJoin, getSavingGroupJoinRequests, requestSavingGroupJoin } from './savingGroups.js';

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
