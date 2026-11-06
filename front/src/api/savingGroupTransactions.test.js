import test from 'node:test';
import assert from 'node:assert/strict';
import client from './client.js';
import { getSavingGroupTransactions } from './savingGroups.js';
// AI작성: 실제 통신 없이 쿼리·응답 공개 범위·오류·취소를 검증한다.
const row = { transactedAt: '2026-11-06T10:00:00', transactionType: 'WITHDRAWAL', amount: 10000, balanceAfter: null };
const envelope = (data = [row], total = data.length) => ({ data: { success: true, data,
  meta: { page: 1, size: 10, total, totalPages: Math.ceil(total / 10) } } });
test('조건과 취소 신호를 전달하고 공개 필드만 반환한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    client.get = async (url, options) => {
      assert.equal(url, '/saving-groups/9223372036854775807/account/transactions');
      assert.deepEqual(options.params, { page: 2, sort: 'OLDEST', transactionType: 'WITHDRAWAL', minAmount: 10000, maxAmount: 40000 });
      assert.equal(options.signal, signal);
      return envelope([{ ...row, accountNumber: 'private', description: 'private', sourceTransactionId: 'private' }]);
    };
    const result = await getSavingGroupTransactions('9223372036854775807', { page: 2, sort: 'OLDEST',
      transactionType: 'WITHDRAWAL', minAmount: 10000, maxAmount: 40000, signal });
    assert.deepEqual(result.items, [row]); assert.equal(result.meta.size, 10);
  } finally { client.get = original; }
});
test('기본 조건과 한쪽 금액·0원을 유지하고 빈 목록을 허용한다', async () => {
  const original = client.get;
  try {
    client.get = async (_url, { params }) => {
      assert.deepEqual(params, { page: 1, sort: 'LATEST' }); return envelope([]);
    };
    assert.deepEqual((await getSavingGroupTransactions(2)).items, []);
    client.get = async (_url, { params }) => {
      assert.equal(params.minAmount, 0); assert.equal('maxAmount' in params, false); return envelope();
    };
    await getSavingGroupTransactions(2, { minAmount: 0, maxAmount: '' });
  } finally { client.get = original; }
});
test('잘못된 응답과 서버 오류 및 취소를 구분한다', async () => {
  const original = client.get;
  try {
    for (const response of [envelope([{ ...row, amount: '10000' }]), envelope([{ ...row, transactionType: 'OTHER' }]),
      envelope([{ ...row, balanceAfter: undefined }]), { data: { success: true, data: [] } }]) {
      client.get = async () => response;
      await assert.rejects(() => getSavingGroupTransactions(2), e => e.code === 'INVALID_RESPONSE');
    }
    for (const status of [400,401,403,404,500]) {
      client.get = async () => { throw { response: { status, data: { error: { code: 'ERROR', message: '조회 불가' } } } }; };
      await assert.rejects(() => getSavingGroupTransactions(2), e => e.status === status && e.message === '조회 불가');
    }
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(() => getSavingGroupTransactions(2), e => e === canceled);
  } finally { client.get = original; }
});
