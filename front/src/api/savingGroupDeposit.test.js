import test from 'node:test';
import assert from 'node:assert/strict';
import client from './client.js';
import { requestSavingGroupDeposit, confirmSavingGroupDeposit } from './savingGroups.js';

// AI작성: 실제 통신 없이 요청 범위·재전송 키·허용 응답 필드·권한 오류 보존을 검증한다.
const key = 'mock-request-key-0001';
const token = 'A'.repeat(43);
test('입금은 금액과 요청 키만 전송하고 내부 식별자는 응답에서 제거한다', async () => {
  const original = client.post;
  try {
    client.post = async (url, body) => {
      assert.equal(url, '/saving-groups/10/account/deposits');
      assert.deepEqual(body, { amount: 1000, requestKey: key });
      return { data: { success: true, data: { status: 'COMPLETED', amount: 1000, balance: 2000,
        transactionId: 'internal', accountNumber: 'must-not-propagate' } } };
    };
    const data = await requestSavingGroupDeposit('10', { amount: 1000, requestKey: key, memberId: 999, accountNumber: 'ignored' });
    assert.equal(data.status, 'COMPLETED'); assert.equal(data.balance, 2000);
    assert.equal('transactionId' in data, false); assert.equal('accountNumber' in data, false);
  } finally { client.post = original; }
});
test('중복 확인과 완료 재전송은 동일 요청 키를 사용한다', async () => {
  const original = client.post;
  try {
    client.post = async () => ({ data: { success: true, data: { status: 'PENDING_CONFIRMATION', amount: 1000,
      confirmationToken: token, confirmationExpiresAt: '2026-11-06T12:05:00' } } });
    const pending = await requestSavingGroupDeposit(10, { amount: 1000, requestKey: key });
    assert.equal(pending.confirmationToken, token); assert.equal(pending.balance, null);
    client.post = async (url, body) => {
      assert.equal(url, '/saving-groups/10/account/deposits/confirm');
      assert.deepEqual(body, { requestKey: key, confirmationToken: token });
      return { data: { success: true, data: { status: 'COMPLETED', amount: 1000, balance: 3000 } } };
    };
    for (let i = 0; i < 2; i++) assert.equal((await confirmSavingGroupDeposit(10, { requestKey: key, confirmationToken: token })).status, 'COMPLETED');
  } finally { client.post = original; }
});
test('잘못된 입력은 통신하지 않고 오류 응답은 보존한다', async () => {
  const original = client.post;
  try {
    client.post = async () => { assert.fail('유효하지 않은 입력으로 호출'); };
    for (const amount of [null, 0, -1, 1.1, 10000000000000]) {
      await assert.rejects(() => requestSavingGroupDeposit(10, { amount, requestKey: key }));
    }
    await assert.rejects(() => confirmSavingGroupDeposit(10, { requestKey: key, confirmationToken: 'bad' }));
    client.post = async () => { throw { response: { status: 403, data: { error: { code: 'SAVING_GROUP_ACCOUNT_FORBIDDEN', message: '승인 모임원만 가능합니다.' } } } }; };
    await assert.rejects(() => requestSavingGroupDeposit(10, { amount: 1000, requestKey: key }), e => e.status === 403 && e.code === 'SAVING_GROUP_ACCOUNT_FORBIDDEN');
    client.post = async () => ({ data: { success: true, data: { status: 'COMPLETED', amount: 1000, balance: 'invalid' } } });
    await assert.rejects(() => requestSavingGroupDeposit(10, { amount: 1000, requestKey: key }), e => e.code === 'INVALID_RESPONSE');
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.post = async () => { throw canceled; };
    await assert.rejects(() => requestSavingGroupDeposit(10, { amount: 1000, requestKey: key }), e => e === canceled);
  } finally { client.post = original; }
});

// AI작성: 응답 유실 이후 같은 키·금액으로 재조회할 수 있고 확인 실패의 서버 코드를 잃지 않는다.
test('통신 결과 불명확 시 같은 요청을 재전송하고 만료 오류를 구분한다', async () => {
  const original = client.post;
  const sent = [];
  try {
    client.post = async (url, body) => {
      sent.push({ url, body });
      if (sent.length === 1) throw new Error('응답 유실');
      return { data: { success: true, data: { status: 'COMPLETED', amount: 1000, balance: 1000 } } };
    };
    const request = { amount: 1000, requestKey: key };
    await assert.rejects(() => requestSavingGroupDeposit(10, request));
    assert.equal((await requestSavingGroupDeposit(10, request)).status, 'COMPLETED');
    assert.deepEqual(sent[0], sent[1]);
    client.post = async () => { throw { response: { status: 409, data: { error: {
      code: 'DEPOSIT_CONFIRMATION_EXPIRED', message: '확인 시간이 지났습니다.',
    } } } }; };
    await assert.rejects(() => confirmSavingGroupDeposit(10, { requestKey: key, confirmationToken: token }),
      e => e.status === 409 && e.code === 'DEPOSIT_CONFIRMATION_EXPIRED');
  } finally { client.post = original; }
});
