import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import {
  createSmallGroup,
  fetchSmallGroupRegions,
  SmallGroupCreationError,
  getSmallGroupDetail,
  SmallGroupDetailLoadError,
  toSmallGroupCard,
  toSmallGroupDetail,
} from './smallGroups.js';

// AI작성: 화면 연결 전 실제 네트워크 없이 요청 본문·성공 데이터·서버 오류 보존을 검증한다.
test('소모임 생성은 입력값만 API 본문에 전달하고 생성 결과를 반환한다', async () => {
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
      assert.equal(path, '/small-groups');
      assert.deepEqual(payload, request);
      return { data: { success: true, data: created, meta: null, error: null } };
    };

    assert.equal(await createSmallGroup(request), created);
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
      createSmallGroup({}),
      (error) => error instanceof SmallGroupCreationError
        && error.code === 'SMALL_GROUP_CREATION_LIMIT_EXCEEDED'
        && error.message === '소모임은 최대 5개까지 만들 수 있습니다.',
    );
  } finally {
    client.post = originalPost;
  }
});

test('활동 지역 목록을 조회해 DB 응답 데이터를 반환한다', async () => {
  const originalGet = client.get;
  const regions = [
    { regionId: 1, regionName: '경기도' },
    { regionId: 2, regionName: '강원특별자치도' },
  ];

  try {
    client.get = async (path) => {
      assert.equal(path, '/small-groups/regions');
      return { data: { success: true, data: regions, meta: null, error: null } };
    };

    assert.deepEqual(await fetchSmallGroupRegions(), regions);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 목록 카드는 상세 URL에 사용할 smallGroupId를 보존한다', () => {
  const card = toSmallGroupCard({
    smallGroupId: 7,
    groupId: 42,
    groupName: '동네 걷기',
    regionName: '서울',
    likeCategory: '운동',
    maxMembers: 10,
    memberCount: 4,
    description: '함께 걷는 소모임입니다.',
  });

  assert.equal(card.id, 7);
});

test('소모임 상세는 smallGroupId로 공개 API를 호출하고 최근 공지 요약만 반환한다', async () => {
  const originalGet = client.get;
  const detail = {
    smallGroupId: 7,
    groupName: '동네 걷기',
    myRole: null,
    notices: [{
      noticeId: 3,
      title: '첫 모임 안내',
      writerNickname: '운영진',
      createdAt: '2026-10-28T10:00:00',
      content: '공지 본문은 상세 화면의 최근 공지 영역에 필요하지 않습니다.',
    }],
  };

  try {
    client.get = async (path) => {
      assert.equal(path, '/small-groups/7');
      return { data: { success: true, data: detail, meta: null, error: null } };
    };

    assert.deepEqual(await getSmallGroupDetail(7), {
      smallGroupId: 7,
      groupName: '동네 걷기',
      myRole: null,
      notices: [{
        noticeId: 3,
        title: '첫 모임 안내',
        writerNickname: '운영진',
        createdAt: '2026-10-28T10:00:00',
      }],
    });
  } finally {
    client.get = originalGet;
  }
});

test('소모임 상세는 공지가 없으면 빈 배열을 유지한다', () => {
  const detail = toSmallGroupDetail({ smallGroupId: 7, notices: [] });

  assert.deepEqual(detail.notices, []);
});

test('소모임 상세는 최근 공지 요약 필드가 없으면 INVALID_RESPONSE로 실패한다', () => {
  assert.throws(
    () => toSmallGroupDetail({ smallGroupId: 7, notices: [{ noticeId: 3, title: '첫 모임 안내' }] }),
    (error) => error instanceof SmallGroupDetailLoadError && error.code === 'INVALID_RESPONSE',
  );
});

test('소모임 상세 조회는 401 오류 코드와 메시지를 보존한다', async () => {
  await assertDetailLoadError('UNAUTHORIZED', '로그인이 필요합니다.');
});

test('소모임 상세 조회는 403 오류 코드와 메시지를 보존한다', async () => {
  await assertDetailLoadError('ACCESS_DENIED', '접근 권한이 없습니다.');
});

test('소모임 상세 조회는 404 오류 코드와 메시지를 보존한다', async () => {
  await assertDetailLoadError('NOT_FOUND', '조회할 수 있는 소모임이 없습니다.');
});

async function assertDetailLoadError(code, message) {
  const originalGet = client.get;
  try {
    client.get = async () => {
      const error = new Error(message);
      error.response = { data: { success: false, error: { code, message } } };
      throw error;
    };

    await assert.rejects(
      getSmallGroupDetail(7),
      (error) => error instanceof SmallGroupDetailLoadError
        && error.code === code
        && error.message === message,
    );
  } finally {
    client.get = originalGet;
  }
}
