import test from 'node:test';
import assert from 'node:assert/strict';
import client from './client.js';
import { getSavingGroupPaymentStatus } from './savingGroups.js';

// AI작성: 선택 월, 페이지, 취소 신호를 전달하고 공통 응답을 읽는지 확인한다.
test('월별 납부 현황 요청에 조회 월과 페이지를 전달한다', async (t) => {
  const signal = new AbortController().signal;
  const month = '2026-11';
  const items = [{ memberName: '테스트', duesAmount: 12000, depositedAmount: 2000, remainingAmount: 10000, paid: false }];
  const meta = { page: 2, size: 10, total: 11, totalPages: 2 };
  t.mock.method(client, 'get', async (path, options) => {
    assert.equal(path, '/saving-groups/2/dues/payment-status');
    assert.deepEqual(options, { params: { month, page: 2 }, signal });
    return { data: { success: true, data: items, meta } };
  });
  assert.deepEqual(await getSavingGroupPaymentStatus(2, { month, page: 2, signal }), { items, meta });
});
