import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { createSmallGroup, SmallGroupCreationError } from './smallGroups.js';

// AI작성: 화면 연결 전 실제 네트워크 없이 요청 본문·성공 데이터·서버 오류 보존을 검증한다.
test('소모임 생성은 임시 회원 ID와 요청 값을 API 본문에 전달하고 생성 결과를 반환한다', async () => {
  const originalPost = client.post;
  const request = {
    groupName: '동네 책모임',
    regionName: '서울 마포구',
    likeCategory: '독서',
    description: '함께 책을 읽고 이야기합니다.',
    maxMembers: 12,
    meetDate: '2099-01-01',
    location: '마포구 도서관',
  };
  const created = { smallGroupId: 200, groupId: 100, ...request, memberRole: 'OWNER' };

  try {
    client.post = async (path, payload) => {
      assert.equal(path, '/smallgroups');
      assert.deepEqual(payload, { ...request, memberId: 1 });
      return { data: { success: true, data: created, meta: null, error: null } };
    };

    assert.equal(await createSmallGroup(1, request), created);
  } finally {
    client.post = originalPost;
  }
});

test('소모임 생성 실패는 서버의 최대 생성 수 오류 코드와 메시지를 보존한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async () => {
      const error = new Error('Conflict');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_CREATION_LIMIT_EXCEEDED',
            message: '소모임은 최대 5개까지 만들 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      createSmallGroup(1, {}),
      (error) => error instanceof SmallGroupCreationError
        && error.code === 'SMALL_GROUP_CREATION_LIMIT_EXCEEDED'
        && error.message === '소모임은 최대 5개까지 만들 수 있습니다.',
    );
  } finally {
    client.post = originalPost;
  }
});
