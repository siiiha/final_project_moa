import test from 'node:test';
import assert from 'node:assert/strict';
import client from './client.js';
import { getSavingGroupTransactions } from './savingGroups.js';

// AI작성: 인증 클라이언트의 요청 형식과 허용된 거래 필드만 전달되는지 확인한다.
test('공용 계좌 거래내역 조회와 서버 페이지 전달', async (t) => {
  const signal = new AbortController().signal;
  // AI작성: 기존 잔액과 새 분류 필드가 동일한 응답에서 전달되는지 확인한다.
  const row = {
    transactedAt: '2026-11-08T12:00:00', transactionType: 'WITHDRAWAL', amount: 30000, balanceAfter: 0,
    purposeType: 'EXPENSE', category: 'FOOD', classificationStatus: 'REVIEW_PENDING',
  };
  const meta = { page: 2, size: 10, total: 11, totalPages: 2 };
  t.mock.method(client, 'get', async (path, options) => {
    assert.equal(path, '/saving-groups/2/account/transactions');
    assert.deepEqual(options, { params: { page: 2 }, signal });
    return { data: { success: true, data: [{ ...row, description: '내부 메모' }], meta } };
  });
  assert.deepEqual(await getSavingGroupTransactions(2, { page: 2, signal }), { items: [row], meta });
});

test('거래내역 조회 실패는 호출자에게 전달한다', async (t) => {
  const error = Object.assign(new Error('조회 실패'), { response: { status: 404 } });
  t.mock.method(client, 'get', async () => { throw error; });
  await assert.rejects(() => getSavingGroupTransactions(2), (result) => result === error);
});
