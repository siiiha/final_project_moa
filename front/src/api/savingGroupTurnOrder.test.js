import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroupTurnOrderRecommendations as getRecommendations } from './savingGroups.js';

// AI작성: 인증 client 재사용·큰 ID·응답 검증·오류·취소를 실제 HTTP 대신 client 대역으로 검증한다.
const row = { memberId: '9223372036854775807', nickname: '테스트계원', recommendedTurnOrder: 1, expectedPayoutAmount: 3297 };
test('추천 조회는 GET과 취소 신호를 전달하고 허용 필드만 반환한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/10/turn-orders/recommendations'); assert.deepEqual(options, { signal });
      return { data: { success: true, data: [{ ...row, accountNumber: '미반환' }] } };
    };
    assert.deepEqual(await getRecommendations(10, { signal }), [row]);
    client.get = async () => ({ data: { success: true, data: [] } });
    assert.deepEqual(await getRecommendations(10), []);
  } finally { client.get = original; }
});
test('중복 회원·순번, 누락 순번, 잘못된 금액·닉네임·목록을 거절한다', async () => {
  const original = client.get;
  try {
    for (const data of [null, [row, row], [{ ...row, recommendedTurnOrder: 2 }],
      [{ ...row, expectedPayoutAmount: -1 }], [{ ...row, expectedPayoutAmount: 1.5 }],
      [{ ...row, nickname: '' }], [{ ...row, memberId: 9223372036854775807 }],
      [row, { ...row, memberId: '50' }]]) {
      client.get = async () => ({ data: { success: true, data } });
      await assert.rejects(() => getRecommendations(10), (e) => e.code === 'INVALID_RESPONSE');
    }
    client.get = async () => ({ data: { success: true, data: [
      { ...row, recommendedTurnOrder: 2 }, { ...row, memberId: '50' }] } });
    assert.deepEqual((await getRecommendations(10)).map((item) => item.recommendedTurnOrder), [1, 2]);
  } finally { client.get = original; }
});
test('잘못된 ID는 요청 전에 거절하고 서버 오류·취소를 유지한다', async () => {
  const original = client.get;
  try {
    client.get = async () => { throw new Error('호출되면 안 됨'); };
    for (const id of [0, -1, 'abc', '9223372036854775808']) await assert.rejects(() => getRecommendations(id));
    for (const status of [401, 403, 404, 409, 500]) {
      client.get = async () => { throw { response: { status, data: { success: false, error: { code: 'POLICY_ERROR', message: '조회 불가' } } } }; };
      await assert.rejects(() => getRecommendations(10), (e) => e.code === 'POLICY_ERROR' && e.status === status);
    }
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(() => getRecommendations(10), (e) => e === canceled);
  } finally { client.get = original; }
});
