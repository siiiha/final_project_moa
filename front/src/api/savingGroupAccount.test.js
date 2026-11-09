import test from 'node:test';
import assert from 'node:assert/strict';
import client from './client.js';
import { getSavingGroupAccount, approveSavingGroupCreationApplication } from './savingGroups.js';

// AI작성: 기존 Node 테스트 방식으로 실제 통신 없이 마스킹·오류·취소를 확인한다.
test('계좌 조회는 마스킹 필드와 취소 신호만 전달한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    client.get = async (url, options) => {
      assert.equal(url, '/saving-groups/9223372036854775807/account');
      assert.equal(options.signal, signal);
      return { data: { success: true, data: { issued: true, bankCode: '00',
        maskedAccountNumber: '********0000', balance: 0, testAccount: true, accountNumber: 'must-not-propagate' } } };
    };
    const data = await getSavingGroupAccount('9223372036854775807', { signal });
    assert.equal(data.balance, 0); assert.equal('accountNumber' in data, false);
  } finally { client.get = original; }
});

// AI작성: 원문 형태의 번호·잘못된 응답·서버 권한 오류가 성공 데이터로 전달되지 않는다.
test('계좌 조회는 미발급과 오류를 구분한다', async () => {
  const original = client.get;
  try {
    client.get = async () => ({ data: { success: true, data: { issued: false, balance: 0, testAccount: true } } });
    assert.equal((await getSavingGroupAccount(2)).issued, false);
    for (const data of [null, {}, { issued: true, balance: 0, testAccount: true, bankCode: '00', maskedAccountNumber: '000000000000' }]) {
      client.get = async () => ({ data: { success: true, data } });
      await assert.rejects(() => getSavingGroupAccount(2), e => e.code === 'INVALID_RESPONSE');
    }
    client.get = async () => { throw { response: { status: 403, data: { error: { code: 'SAVING_GROUP_ACCOUNT_FORBIDDEN', message: '권한 없음' } } } }; };
    await assert.rejects(() => getSavingGroupAccount(2), e => e.status === 403 && e.message === '권한 없음');
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(() => getSavingGroupAccount(2), e => e === canceled);
  } finally { client.get = original; }
});

// AI작성: 발급 오류를 보존하고 승인 요청을 자동 반복하지 않는다.
test('관리자 승인은 본문 없이 대기하며 발급 오류를 보존한다', async () => {
  const original = client.patch;
  let count = 0;
  try {
    client.patch = async (url, body, options) => {
      count++; assert.equal(body, undefined); assert.equal(options.timeout, 30000);
      throw { response: { data: { error: { code: 'ACCOUNT_ISSUE_UNKNOWN', message: '발급 확인 필요' } } } };
    };
    await assert.rejects(() => approveSavingGroupCreationApplication(1), e => e.code === 'ACCOUNT_ISSUE_UNKNOWN');
    assert.equal(count, 1);
  } finally { client.patch = original; }
});
