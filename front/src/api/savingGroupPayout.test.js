import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { createSavingGroupPayout, getSavingGroupPayoutOptions } from './savingGroups.js';

// AI작성: 큰 ID·빈 순번계 폼·친목계 증빙·오류·취소 전달을 실제 금융 호출 없이 검증한다.
test('계금 요청 multipart와 인증 사용자 입력 제외', async () => {
  const original = client.post;
  try {
    client.post = async (path, body) => {
      assert.equal(path, '/saving-groups/9223372036854775807/payout-requests');
      assert.deepEqual([...body.keys()], []);
      return { data: { success: true, data: { status: 'PENDING' } } };
    };
    assert.equal((await createSavingGroupPayout('9223372036854775807')).status, 'PENDING');
    client.post = async (path, body) => {
      assert.equal(body.get('amount'), '1000'); assert.equal(body.get('title'), '식비');
      assert.equal(await body.get('receipt').text(), 'test'); assert.equal(body.has('memberId'), false);
      return { data: { success: true, data: { requestId: 2 } } };
    };
    await createSavingGroupPayout(1, { amount: '1000', title: '식비', receipt: new Blob(['test'], { type: 'image/png' }), memberId: 99 });
    for (const status of [401, 403, 404, 409, 500]) {
      client.post = async () => { throw { response: { status, data: { error: { code: 'TEST', message: '요청 실패' } } } }; };
      await assert.rejects(() => createSavingGroupPayout(1), e => e.status === status && e.code === 'TEST');
    }
    client.post = async () => { throw { code: 'ERR_CANCELED' }; };
    await assert.rejects(() => createSavingGroupPayout(1), e => e.code === 'ERR_CANCELED');
  } finally { client.post = original; }
});

test('요청 가능 정보의 경로·취소 신호와 잘못된 ID 차단', async () => {
  const original = client.get;
  try {
    const signal = new AbortController().signal;
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/1/payout-requests/options'); assert.equal(options.signal, signal);
      return { data: { success: true, data: { groupType: '순번계' } } };
    };
    assert.equal((await getSavingGroupPayoutOptions(1, { signal })).groupType, '순번계');
    client.get = async () => { assert.fail('잘못된 ID로 호출하면 안 됨'); };
    for (const id of [0, -1, 'abc', null]) await assert.rejects(() => getSavingGroupPayoutOptions(id));
  } finally { client.get = original; }
});
