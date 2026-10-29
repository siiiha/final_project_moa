import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import {
  createSmallGroup,
  fetchSmallGroupRegions,
  SmallGroupCreationError,
  getSmallGroupDetail,
  SmallGroupDetailLoadError,
  SmallGroupJoinRequestError,
  getSmallGroupJoinRequests,
  getSmallGroupSettings,
  requestSmallGroupJoin,
  SmallGroupSettingsLoadError,
  updateSmallGroupSettings,
  SmallGroupSettingsUpdateError,
  toSmallGroupCard,
  toSmallGroupDetail,
  toSmallGroupSettings,
} from './smallGroups.js';

test('소모임 가입 신청 목록은 상태·페이지·취소 신호를 API에 전달하고 화면 필드로 변환한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;

  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7/join-requests');
      assert.deepEqual(options.params, { status: 'PENDING', page: 2 });
      assert.equal(options.signal, signal);
      return {
        data: {
          success: true,
          data: [{
            crewMemberId: '9007199254740993',
            applicant: '가**',
            regionName: '서울',
            applicationMessage: '함께하고 싶습니다.',
            membershipStatus: 'PENDING',
          }],
          meta: { page: 2, size: 10, total: 11, totalPages: 2 },
          error: null,
        },
      };
    };

    assert.deepEqual(await getSmallGroupJoinRequests(7, { status: 'PENDING', page: 2, signal }), {
      items: [{
        id: '9007199254740993',
        applicant: '가**',
        regionInterest: '서울',
        message: '함께하고 싶습니다.',
        status: '대기',
      }],
      meta: { page: 2, size: 10, total: 11, totalPages: 2 },
    });
  } finally {
    client.get = originalGet;
  }
});

test('소모임 가입 신청 목록의 빈 결과는 정상 응답으로 유지하고 서버 오류는 보존한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => ({
      data: { success: true, data: [], meta: { page: 1, size: 10, total: 0, totalPages: 0 }, error: null },
    });
    assert.deepEqual((await getSmallGroupJoinRequests(7)).items, []);

    client.get = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: { code: 'SMALL_GROUP_OWNER_REQUIRED', message: '소모임 개설자만 가입 신청을 조회할 수 있습니다.' },
        },
      };
      throw error;
    };
    await assert.rejects(
      getSmallGroupJoinRequests(7),
      (error) => error instanceof SmallGroupJoinRequestError
        && error.code === 'SMALL_GROUP_OWNER_REQUIRED'
        && error.message === '소모임 개설자만 가입 신청을 조회할 수 있습니다.',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 가입 신청 목록은 잘못된 ID와 잘못된 성공 응답을 사용자용 오류로 처리한다', async () => {
  const originalGet = client.get;
  let called = false;

  try {
    client.get = async () => {
      called = true;
      return { data: { success: true, data: {}, meta: null, error: null } };
    };

    await assert.rejects(
      getSmallGroupJoinRequests('잘못된-ID'),
      (error) => error instanceof SmallGroupJoinRequestError && error.code === 'BAD_REQUEST',
    );
    assert.equal(called, false);

    await assert.rejects(
      getSmallGroupJoinRequests(7),
      (error) => error instanceof SmallGroupJoinRequestError && error.code === 'INVALID_RESPONSE',
    );
  } finally {
    client.get = originalGet;
  }
});

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

test('소모임 가입 신청은 ID 경로만 사용해 본문 없이 POST 요청한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async (...args) => {
      assert.deepEqual(args, ['/small-groups/7/join-requests']);
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await assert.doesNotReject(requestSmallGroupJoin(7));
  } finally {
    client.post = originalPost;
  }
});

test('소모임 가입 신청은 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async () => {
      const error = new Error('소모임 정원이 모두 찼습니다.');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_CAPACITY_FULL',
            message: '소모임 정원이 모두 찼습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      requestSmallGroupJoin(7),
      (error) => error instanceof SmallGroupJoinRequestError
        && error.code === 'SMALL_GROUP_CAPACITY_FULL'
        && error.message === '소모임 정원이 모두 찼습니다.',
    );
  } finally {
    client.post = originalPost;
  }
});

test('소모임 설정 조회는 OWNER 전용 경로와 서버 응답을 그대로 사용한다', async () => {
  const originalGet = client.get;
  const settings = {
    groupName: '동네 걷기',
    likeCategory: '운동',
    description: '함께 걷는 소모임입니다.',
    maxMembers: 10,
    meetDate: '2026-11-01',
    location: '한강 공원',
    approvedMemberCount: 2,
  };

  try {
    client.get = async (path) => {
      assert.equal(path, '/small-groups/7/settings');
      return { data: { success: true, data: settings, meta: null, error: null } };
    };

    assert.deepEqual(await getSmallGroupSettings(7), settings);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 설정 조회는 필수 응답 필드가 없으면 INVALID_RESPONSE로 실패한다', () => {
  assert.throws(
    () => toSmallGroupSettings({ groupName: '동네 걷기', maxMembers: 10 }),
    (error) => error instanceof SmallGroupSettingsLoadError && error.code === 'INVALID_RESPONSE',
  );
});

test('소모임 설정 조회는 서버 권한 오류 코드와 메시지를 보존한다', async () => {
  const originalGet = client.get;
  try {
    client.get = async () => {
      const error = new Error('개설자만 설정을 조회할 수 있습니다.');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_SETTINGS_FORBIDDEN',
            message: '개설자만 설정을 조회할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      getSmallGroupSettings(7),
      (error) => error instanceof SmallGroupSettingsLoadError
        && error.code === 'SMALL_GROUP_SETTINGS_FORBIDDEN'
        && error.message === '개설자만 설정을 조회할 수 있습니다.',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 설정 수정은 변경된 필드만 OWNER 전용 PATCH 본문으로 전달한다', async () => {
  const originalPatch = client.patch;
  const changes = {
    groupName: '주말 걷기',
    maxMembers: 8,
  };

  try {
    client.patch = async (path, payload) => {
      assert.equal(path, '/small-groups/7');
      assert.deepEqual(payload, changes);
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await assert.doesNotReject(updateSmallGroupSettings(7, changes));
  } finally {
    client.patch = originalPatch;
  }
});

test('소모임 설정 수정은 성공 봉투가 아니면 INVALID_RESPONSE로 실패한다', async () => {
  const originalPatch = client.patch;
  try {
    client.patch = async () => ({ data: { success: false, data: null, meta: null, error: null } });

    await assert.rejects(
      updateSmallGroupSettings(7, { groupName: '주말 걷기' }),
      (error) => error instanceof SmallGroupSettingsUpdateError && error.code === 'INVALID_RESPONSE',
    );
  } finally {
    client.patch = originalPatch;
  }
});

test('소모임 설정 수정은 서버 정원 축소 오류 코드와 메시지를 보존한다', async () => {
  const originalPatch = client.patch;
  try {
    client.patch = async () => {
      const error = new Error('승인된 참여자 수보다 작게 설정할 수 없습니다.');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_CAPACITY_BELOW_MEMBERS',
            message: '승인된 참여자 수보다 작게 설정할 수 없습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      updateSmallGroupSettings(7, { maxMembers: 2 }),
      (error) => error instanceof SmallGroupSettingsUpdateError
        && error.code === 'SMALL_GROUP_CAPACITY_BELOW_MEMBERS'
        && error.message === '승인된 참여자 수보다 작게 설정할 수 없습니다.',
    );
  } finally {
    client.patch = originalPatch;
  }
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
