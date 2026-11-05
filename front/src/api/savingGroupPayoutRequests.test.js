import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroupPayoutRequests } from './savingGroups.js';

// AI작성: 기존 인증 client를 대체한 내부 응답으로 요청 취소·오류·금액·BIGINT 보존을 검증한다.
test('계금 요청 목록은 서버 페이지를 사용하고 유형별 표시를 변환한다', async () => {
  const original = client.get;
  try {
    const signal = new AbortController().signal;
    client.get = async (url, options) => {
      assert.equal(url, '/saving-groups/10/payout-requests');
      assert.deepEqual(options, { params: { status: 'PENDING', page: 2 }, signal });
      return { data: { success: true, data: [
        { requestId: '9223372036854775807', requesterName: '테스트회원', roundNumber: 3, amount: '1200000', status: 'COMPLETED' },
        { requestId: '2', requesterName: '테스트회원', title: '다과', hasEvidence: false, amount: '1234.50', status: 'REJECTED' },
      ], meta: { page: 2, size: 10, total: 12, totalPages: 2 } } };
    };
    const result = await getSavingGroupPayoutRequests('10', { status: 'PENDING', page: 2, signal });
    assert.equal(result.items[0].id, '9223372036854775807');
    assert.equal(result.items[0].amount, '1,200,000원');
    assert.equal(result.items[0].status, '완료');
    assert.equal(result.items[1].amount, '1,234.5원');
    assert.equal(result.items[1].type, '지출');
    assert.equal(result.items[1].hasEvidence, false);
  } finally { client.get = original; }
});

test('빈 결과·실패·잘못된 응답·취소를 구분하며 Mock으로 대체하지 않는다', async () => {
  const original = client.get;
  try {
    client.get = async () => ({ data: { success: true, data: [], meta: { page: 1, size: 10, total: 0, totalPages: 0 } } });
    assert.deepEqual((await getSavingGroupPayoutRequests(10)).items, []);
    for (const status of [401, 403, 404, 409, 500]) {
      for (const rejected of [true, false]) {
        client.get = async () => {
          const response = { status, data: { success: false, error: { code: 'DENIED', message: '조회 불가' } } };
          if (rejected) throw { response };
          return response;
        };
        await assert.rejects(() => getSavingGroupPayoutRequests(10), e => e.code === 'DENIED' && e.status === status);
      }
    }
    client.get = async () => ({ data: { success: true, data: null } });
    await assert.rejects(() => getSavingGroupPayoutRequests(10), /응답/);
    const canceled = { code: 'ERR_CANCELED' };
    client.get = async () => { throw canceled; };
    await assert.rejects(() => getSavingGroupPayoutRequests(10), e => e === canceled);
    let calls = 0;
    client.get = async () => { calls += 1; };
    await assert.rejects(() => getSavingGroupPayoutRequests('invalid'));
    assert.equal(calls, 0);
  } finally { client.get = original; }
});
