import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroupMembers, updateSavingGroupMemberRole } from './savingGroups.js';

// AI작성: client 대역으로 큰 ID·계모임 ID·본문·메타 계약과 실패·취소를 검증한다.
test('계원 목록은 savingGroupId와 페이지를 전달하고 서버 이름을 그대로 유지한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/10/members');
      assert.deepEqual(options, { params: { page: 2 }, signal });
      return { data: { success: true, data: [{ memberId: '9223372036854775807', name: '테스트계원',
        email: 't***@example.com', role: 'MEMBER', joinedAt: '2026-11-05T10:00:00' }],
      meta: { page: 2, size: 10, total: 11, totalPages: 2 } } };
    };
    const result = await getSavingGroupMembers(10, { page: 2, signal });
    assert.equal(result.items[0].id, '9223372036854775807');
    assert.equal(result.items[0].name, '테스트계원');
    assert.equal(result.items[0].role, 'MEMBER');
    assert.equal(result.items[0].joinedAt, '2026.11.05.');
  } finally { client.get = original; }
});

test('빈 결과는 유지하고 잘못된 메타·역할·목록은 거절한다', async () => {
  const original = client.get;
  const empty = { success: true, data: [], meta: { page: 1, size: 10, total: 0, totalPages: 0 } };
  try {
    client.get = async () => ({ data: empty });
    assert.deepEqual((await getSavingGroupMembers(10)).items, []);
    for (const data of [{ ...empty, data: null }, { ...empty, meta: null },
      { ...empty, meta: { ...empty.meta, total: 21, totalPages: 3 } },
      { ...empty, data: [{ memberId: '50', role: 'OWNER' }] }]) {
      client.get = async () => ({ data });
      await assert.rejects(() => getSavingGroupMembers(10), (e) => e.code === 'INVALID_RESPONSE');
    }
  } finally { client.get = original; }
});

test('역할 변경은 MEMBER·MANAGER만 보내고 큰 회원 ID와 취소 신호를 보존한다', async () => {
  const original = client.patch;
  const signal = new AbortController().signal;
  try {
    for (const role of ['MEMBER', 'MANAGER']) {
      client.patch = async (path, body, options) => {
        assert.equal(path, '/saving-groups/10/members/9223372036854775807/role');
        assert.deepEqual(body, { role }); assert.deepEqual(options, { signal });
        return { data: { success: true, data: null } };
      };
      assert.equal(await updateSavingGroupMemberRole(10, '9223372036854775807', role, { signal }), null);
    }
  } finally { client.patch = original; }
});

test('목록·변경 모두 서버 오류 코드·상태와 취소를 보존한다', async () => {
  const originals = { get: client.get, patch: client.patch };
  try {
    for (const [method, call] of [['get', () => getSavingGroupMembers(10)],
      ['patch', () => updateSavingGroupMemberRole(10, 50, 'MANAGER')]]) {
      for (const status of [401, 403, 404, 409, 500]) {
        for (const reject of [true, false]) {
          client[method] = async () => {
            const response = { status, data: { success: false, error: { code: 'POLICY_ERROR', message: '변경 불가' } } };
            if (reject) throw { response };
            return response;
          };
          await assert.rejects(call, (e) => e.status === status && e.code === 'POLICY_ERROR' && e.message === '변경 불가');
        }
      }
      const canceled = { code: 'ERR_CANCELED' };
      client[method] = async () => { throw canceled; };
      await assert.rejects(call, (e) => e === canceled);
      client[method] = async () => { throw new Error('network'); };
      await assert.rejects(call, (e) => e.code === 'SAVING_GROUP_MEMBER_REQUEST_FAILED');
    }
  } finally { Object.assign(client, originals); }
});

test('잘못된 ID·페이지·역할은 요청 전에 차단한다', async () => {
  const originals = { get: client.get, patch: client.patch };
  let calls = 0;
  try {
    client.get = client.patch = async () => { calls += 1; };
    for (const id of [0, -1, 'abc', null, '9223372036854775808', Number.MAX_SAFE_INTEGER + 1]) {
      await assert.rejects(() => getSavingGroupMembers(id));
      await assert.rejects(() => updateSavingGroupMemberRole(10, id, 'MEMBER'));
    }
    for (const page of [0, -1, 1.5, '2']) await assert.rejects(() => getSavingGroupMembers(10, { page }));
    for (const role of ['OWNER', 'ADMIN', null, 'manager']) await assert.rejects(() => updateSavingGroupMemberRole(10, 50, role));
    assert.equal(calls, 0);
  } finally { Object.assign(client, originals); }
});

// AI작성: 검색어는 원문 기준으로 전송하고 빈 검색어는 생략하며 마스킹된 응답을 그대로 유지한다.
test('계원 검색은 정리한 이름·원문 이메일·특수문자와 페이지를 전달한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    for (const keyword of ['테스트계원', 'test@example.com', '%_!', "x' OR 1=1", '가'.repeat(20)]) {
      client.get = async (path, options) => {
        assert.equal(path, '/saving-groups/10/members');
        assert.deepEqual(options, { params: { keyword, page: 2 }, signal });
        return { data: { success: true, data: [{ memberId: '50', name: '테스트계원', email: 't***@example.com', role: 'MEMBER' }],
          meta: { page: 2, size: 10, total: 11, totalPages: 2 } } };
      };
      const result = await getSavingGroupMembers(10, { keyword: ` ${keyword} `, page: 2, signal });
      assert.equal(result.items[0].email, 't***@example.com');
    }
  } finally { client.get = original; }
});

test('검색 해제는 keyword를 생략하고 잘못된 검색어는 요청 전에 거절한다', async () => {
  const original = client.get;
  let calls = 0;
  try {
    client.get = async (path, options) => {
      calls += 1; assert.deepEqual(options.params, { page: 1 });
      return { data: { success: true, data: [], meta: { page: 1, size: 10, total: 0, totalPages: 0 } } };
    };
    for (const keyword of [undefined, null, '', ' ', '　']) await getSavingGroupMembers(10, { keyword });
    assert.equal(calls, 5);
    for (const keyword of ['가'.repeat(21), 123, {}, []]) await assert.rejects(() => getSavingGroupMembers(10, { keyword }));
    assert.equal(calls, 5);
  } finally { client.get = original; }
});
