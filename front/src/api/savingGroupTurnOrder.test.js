import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroupTurnOrderRecommendations as getRecommendations } from './savingGroups.js';
import { getSavingGroupTurnOrders, updateSavingGroupTurnOrders } from './savingGroups.js';

// AI작성: 예약 조회·저장의 큰 회원 ID와 공통 오류·취소 계약을 검증한다.
test('순번 지정 API는 전체 목록·적용월을 보존하고 잘못된 응답을 거절한다', async () => {
  const originalGet = client.get; const originalPut = client.put;
  const signal = new AbortController().signal;
  const data = { effectiveMonth: '2026-12-01', startRoundNumber: 3, currentRoundNumber: 2, scheduled: true, needsReassignment: false,
    members: [{ memberId: '9223372036854775807', nickname: '테스트계주', role: 'OWNER', paid: false, currentSequenceNumber: 3, plannedSequenceNumber: 4, fixedReason: null }] };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/10/turn-orders'); assert.equal(options.signal, signal);
      return { data: { success: true, data } };
    };
    assert.deepEqual(await getSavingGroupTurnOrders(10, { signal }), data);
    const orders = [{ memberId: data.members[0].memberId, sequenceNumber: 3 }];
    client.put = async (path, body, options) => {
      assert.equal(path, '/saving-groups/10/turn-orders'); assert.deepEqual(body, { turnOrders: orders, currentRoundNumber: 2 }); assert.equal(options.signal, signal);
      return { data: { success: true, data } };
    };
    assert.deepEqual(await updateSavingGroupTurnOrders(10, orders, { signal, currentRoundNumber: 2 }), data);
    for (const invalid of [null, { ...data, members: [data.members[0], data.members[0]] }, { ...data, startRoundNumber: 0 }]) {
      client.get = async () => ({ data: { success: true, data: invalid } });
      await assert.rejects(() => getSavingGroupTurnOrders(10));
    }
    client.put = async () => { throw { response: { status: 409, data: { error: { code: 'CONFLICT', message: '모임원 변경' } } } }; };
    await assert.rejects(() => updateSavingGroupTurnOrders(10, orders), error => error.code === 'CONFLICT' && error.status === 409);
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.put = async () => { throw canceled; };
    await assert.rejects(() => updateSavingGroupTurnOrders(10, orders), error => error === canceled);
  } finally { client.get = originalGet; client.put = originalPut; }
});

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
      [{ ...row, nickname: '' }], [{ ...row, memberId: Number.MAX_SAFE_INTEGER + 1 }],
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
