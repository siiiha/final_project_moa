import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import {
  createSmallGroup,
  createSmallGroupReview,
  getSmallGroupReviews,
  hasWrittenSmallGroupReview,
  SmallGroupReviewListLoadError,
  toSmallGroupReviewListItem,
  SmallGroupReviewCreateError,
  createSmallGroupVote,
  closeSmallGroupVote,
  deleteSmallGroupVote,
  getSmallGroupVoteDetail,
  getSmallGroupVoteStatus,
  getSmallGroupVotes,
  fetchSmallGroupRegions,
  SmallGroupCreationError,
  getSmallGroupDetail,
  SmallGroupDetailLoadError,
  SmallGroupJoinRequestError,
  SmallGroupVoteCreateError,
  SmallGroupVoteCloseError,
  SmallGroupVoteDeleteError,
  SmallGroupVoteDetailLoadError,
  SmallGroupVoteParticipationError,
  SmallGroupVoteStatusLoadError,
  SmallGroupVoteListLoadError,
  approveSmallGroupJoinRequest,
  rejectSmallGroupJoinRequest,
  getSmallGroupJoinRequests,
  getSmallGroupSettings,
  requestSmallGroupJoin,
  SmallGroupSettingsLoadError,
  updateSmallGroupSettings,
  SmallGroupSettingsUpdateError,
  toSmallGroupCard,
  toSmallGroupDetail,
  toSmallGroupVoteDetail,
  participateSmallGroupVote,
  toSmallGroupSettings,
} from './smallGroups.js';

// AI작성: 본인 여부는 서버 인증으로 판단하며 boolean false를 누락값으로 처리하지 않는다.
test('후기 작성 여부는 인증 전용 경로와 취소 신호를 사용하고 true·false를 보존한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;
  try {
    for (const written of [true, false]) {
      client.get = async (path, options) => {
        assert.equal(path, '/small-groups/7/reviews/me');
        assert.deepEqual(options, { signal });
        return { data: { success: true, data: written } };
      };
      assert.equal(await hasWrittenSmallGroupReview(7, { signal, memberId: 999 }), written);
    }
  } finally { client.get = originalGet; }
});

test('후기 작성 여부는 불명확한 응답을 미작성으로 처리하지 않는다', async () => {
  const originalGet = client.get;
  try {
    for (const data of [null, 'false', 0, {}]) {
      client.get = async () => ({ data: { success: true, data } });
      await assert.rejects(hasWrittenSmallGroupReview(7), (error) => error.code === 'INVALID_RESPONSE');
    }
    client.get = async () => { throw new Error('network'); };
    await assert.rejects(hasWrittenSmallGroupReview(7), (error) => error.code === 'SMALL_GROUP_REVIEW_STATUS_LOAD_FAILED');
    const canceled = Object.assign(new Error('canceled'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(hasWrittenSmallGroupReview(7), (error) => error === canceled);
    client.get = async () => ({ data: { success: false, error: { code: 'ACCESS_DENIED', message: '접근 제한' } } });
    await assert.rejects(hasWrittenSmallGroupReview(7), (error) => error.code === 'ACCESS_DENIED' && error.message === '접근 제한');
  } finally { client.get = originalGet; }
});

// AI작성: 페이지 요청·응답 변환·취소 전달을 검증하고 서버 정렬을 변경하지 않는다.
test('후기 목록은 페이지와 취소 신호만 보내고 공개 항목과 페이징을 반환한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;
  const item = { smallGroupReviewId: 50, writerNickname: '모아회원', content: '즐거웠어요',
    createdAt: '2026-11-01T12:00:00', rating: 0 };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7/reviews');
      assert.deepEqual(options, { params: { page: 2 }, signal });
      return { data: { success: true, data: [{ ...item, writerId: 999 }, { ...item, smallGroupReviewId: 49, rating: 4.5 }],
        meta: { page: 2, size: 8, total: 10, totalPages: 2 } } };
    };
    assert.deepEqual(await getSmallGroupReviews(7, { page: 2, signal, size: 100, keyword: '검색' }), {
      items: [item, { ...item, smallGroupReviewId: 49, rating: 4.5 }],
      currentPage: 2, pageSize: 8, totalElements: 10, totalPages: 2,
    });
  } finally { client.get = originalGet; }
});

test('후기 목록은 기본 페이지 1과 빈 목록을 처리하고 경로를 인코딩한다', async () => {
  const originalGet = client.get;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7%2F8/reviews');
      assert.equal(options.params.page, 1);
      return { data: { success: true, data: [], meta: { page: 1, size: 8, total: 0, totalPages: 0 } } };
    };
    assert.deepEqual(await getSmallGroupReviews('7/8'), {
      items: [], currentPage: 1, pageSize: 8, totalElements: 0, totalPages: 0,
    });
  } finally { client.get = originalGet; }
});

test('후기 목록은 실패 봉투와 HTTP 오류의 서버 코드·메시지를 보존한다', async () => {
  const originalGet = client.get;
  try {
    for (const code of ['ACCESS_DENIED', 'NOT_FOUND']) {
      for (const throws of [false, true]) {
        client.get = async () => {
          const data = { success: false, error: { code, message: '조회할 수 없습니다.' } };
          if (throws) { const error = new Error('HTTP error'); error.response = { data }; throw error; }
          return { data };
        };
        await assert.rejects(getSmallGroupReviews(7), (error) => error instanceof SmallGroupReviewListLoadError
          && error.code === code && error.message === '조회할 수 없습니다.');
      }
    }
  } finally { client.get = originalGet; }
});

test('후기 목록은 잘못된 목록·페이징 응답을 거절한다', async () => {
  const originalGet = client.get;
  const valid = { page: 1, size: 8, total: 0, totalPages: 0 };
  try {
    for (const data of [null, { success: true, data: null, meta: valid },
      { success: true, data: [], meta: null },
      ...[{ page: 0 }, { size: 10 }, { total: -1 }, { totalPages: '0' }]
        .map((patch) => ({ success: true, data: [], meta: { ...valid, ...patch } }))]) {
      client.get = async () => ({ data });
      await assert.rejects(getSmallGroupReviews(7), (error) => error.code === 'INVALID_RESPONSE');
    }
  } finally { client.get = originalGet; }
});

test('후기 항목은 별점 0·0.5·5와 문자열 ID를 보존하고 잘못된 필드를 거절한다', () => {
  const item = { smallGroupReviewId: '50', writerNickname: '모아회원', content: '좋아요',
    createdAt: '2026-11-01T12:00:00', rating: 0 };
  for (const rating of [0, 0.5, 5]) {
    assert.equal(toSmallGroupReviewListItem({ ...item, rating }).rating, rating);
  }
  for (const patch of [{ smallGroupReviewId: 0 }, { writerNickname: null }, { content: null },
    { createdAt: null }, { rating: '4.5' }, { rating: 0.1 }, { rating: 5.5 }]) {
    assert.throws(() => toSmallGroupReviewListItem({ ...item, ...patch }),
      (error) => error instanceof SmallGroupReviewListLoadError && error.code === 'INVALID_RESPONSE');
  }
});

test('후기 목록은 네트워크 오류를 변환하고 취소 오류는 그대로 전달한다', async () => {
  const originalGet = client.get;
  try {
    client.get = async () => { throw new Error('Network Error'); };
    await assert.rejects(getSmallGroupReviews(7), (error) => error instanceof SmallGroupReviewListLoadError
      && error.code === 'SMALL_GROUP_REVIEW_LIST_LOAD_FAILED');
    const canceled = Object.assign(new Error('canceled'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(getSmallGroupReviews(7), (error) => error === canceled);
  } finally { client.get = originalGet; }
});

// AI작성: 별점 0점도 보존하고 작성자 등 불필요한 값이 전송되지 않는지 검증한다.
test('소모임 후기 등록은 별점과 코멘트만 보내고 생성 ID를 반환한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async (path, body) => {
      assert.equal(path, '/small-groups/7/reviews');
      assert.deepEqual(body, { rating: 0, content: '즐거웠어요' });
      return { data: { success: true, data: 50, meta: null, error: null } };
    };
    assert.equal(await createSmallGroupReview(7, { rating: 0, content: '즐거웠어요', writerId: 999 }), 50);
  } finally {
    client.post = originalPost;
  }
});

// AI작성: 화면의 중복 완료 안내에 필요한 서버 코드와 메시지를 그대로 유지한다.
test('소모임 후기 등록은 서버 중복 오류를 보존한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async () => {
      const error = new Error('Conflict');
      error.response = { data: { error: {
        code: 'SMALL_GROUP_REVIEW_ALREADY_EXISTS', message: '이미 등록한 후기입니다.',
      } } };
      throw error;
    };
    await assert.rejects(createSmallGroupReview(7, { rating: 4.5, content: '좋아요' }), (error) => {
      assert.ok(error instanceof SmallGroupReviewCreateError);
      assert.equal(error.code, 'SMALL_GROUP_REVIEW_ALREADY_EXISTS');
      assert.equal(error.message, '이미 등록한 후기입니다.');
      return true;
    });
  } finally {
    client.post = originalPost;
  }
});

// AI작성: 성공 여부·생성 ID가 불명확하면 등록 성공으로 표시하지 않는다.
test('소모임 후기 등록은 잘못된 성공 응답을 거절한다', async () => {
  const originalPost = client.post;
  try {
    for (const data of [null, 0, -1, '50', {}, 0.5]) {
      client.post = async () => ({ data: { success: true, data } });
      await assert.rejects(createSmallGroupReview(7, { rating: 5, content: '좋아요' }),
        (error) => error.code === 'INVALID_RESPONSE');
    }
  } finally {
    client.post = originalPost;
  }
});

test('소모임 후기 등록은 실패 봉투의 서버 권한 오류를 보존한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async () => ({ data: { success: false, error: {
      code: 'SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED', message: '개설자는 작성할 수 없습니다.',
    } } });
    await assert.rejects(createSmallGroupReview(7, { rating: 5, content: '좋아요' }),
      (error) => error.code === 'SMALL_GROUP_REVIEW_OWNER_NOT_ALLOWED'
        && error.message === '개설자는 작성할 수 없습니다.');
  } finally {
    client.post = originalPost;
  }
});

test('소모임 후기 등록은 네트워크 실패를 안내 오류로 변환한다', async () => {
  const originalPost = client.post;
  try {
    client.post = async () => { throw new Error('Network Error'); };
    await assert.rejects(createSmallGroupReview(7, { rating: 5, content: '좋아요' }),
      (error) => error instanceof SmallGroupReviewCreateError && error.code === 'NETWORK_ERROR');
  } finally {
    client.post = originalPost;
  }
});

// AI작성: 종료 요청의 경로 인코딩·본문 없음·재요청 성공과 오류 보존을 기존 client 대역으로 검증한다.
test('소모임 투표 종료는 본문 없이 인코딩한 경로로 PATCH 요청하며 재요청도 성공한다', async () => {
  const originalPatch = client.patch;
  let calls = 0;
  try {
    client.patch = async (...args) => {
      assert.deepEqual(args, ['/small-groups/7%2F8/votes/11%3F12/close']);
      calls += 1;
      return { data: { success: true, data: null, meta: null, error: null } };
    };
    assert.equal(await closeSmallGroupVote('7/8', '11?12'), undefined);
    assert.equal(await closeSmallGroupVote('7/8', '11?12'), undefined);
    assert.equal(calls, 2);
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 종료는 HTTP 오류의 서버 코드와 메시지를 보존한다', async () => {
  const originalPatch = client.patch;
  try {
    for (const [status, code] of [
      [400, 'BAD_REQUEST'], [401, 'LOGIN_REQUIRED'], [403, 'SMALL_GROUP_VOTE_CLOSE_FORBIDDEN'],
      [404, 'SMALL_GROUP_NOT_FOUND'], [404, 'SMALL_GROUP_VOTE_NOT_FOUND'], [500, 'SMALL_GROUP_VOTE_CLOSE_FAILED'],
    ]) {
      const message = `종료 요청 오류 ${status}`;
      client.patch = async () => {
        throw { response: { status, data: { success: false, error: { code, message } } } };
      };
      await assert.rejects(closeSmallGroupVote(7, 11), (error) => error instanceof SmallGroupVoteCloseError
        && error.code === code && error.message === message);
    }
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 종료는 실패 응답 봉투의 코드와 메시지도 보존한다', async () => {
  const originalPatch = client.patch;
  try {
    client.patch = async () => ({ data: { success: false,
      error: { code: 'SMALL_GROUP_VOTE_CLOSE_FORBIDDEN', message: '종료 권한이 없습니다.' } } });
    await assert.rejects(closeSmallGroupVote(7, 11), (error) => error instanceof SmallGroupVoteCloseError
      && error.code === 'SMALL_GROUP_VOTE_CLOSE_FORBIDDEN' && error.message === '종료 권한이 없습니다.');
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 종료는 불명확한 성공 응답을 거절한다', async () => {
  const originalPatch = client.patch;
  try {
    for (const data of [null, {}, { success: false }, { success: 'true' }, { success: 1 }]) {
      client.patch = async () => ({ data });
      await assert.rejects(closeSmallGroupVote(7, 11), (error) => error instanceof SmallGroupVoteCloseError
        && error.code === 'SMALL_GROUP_VOTE_CLOSE_FAILED');
    }
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 종료는 네트워크 오류를 종료 전용 오류와 기본 안내로 변환한다', async () => {
  const originalPatch = client.patch;
  try {
    client.patch = async () => { throw new Error('Network Error'); };
    await assert.rejects(closeSmallGroupVote(7, 11), (error) => error instanceof SmallGroupVoteCloseError
      && error.code === 'SMALL_GROUP_VOTE_CLOSE_FAILED'
      && error.message === '투표를 종료하지 못했습니다. 잠시 후 다시 시도해 주세요.');
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 종료는 취소 오류와 기존 종료 오류 객체를 그대로 전달한다', async () => {
  const originalPatch = client.patch;
  try {
    for (const original of [Object.assign(new Error('cancelled'), { code: 'ERR_CANCELED' }),
      new SmallGroupVoteCloseError('SMALL_GROUP_VOTE_CLOSE_FAILED', '종료 오류')]) {
      client.patch = async () => { throw original; };
      await assert.rejects(closeSmallGroupVote(7, 11), (error) => error === original);
    }
  } finally { client.patch = originalPatch; }
});

test('소모임 투표 삭제는 두 식별자로 DELETE 요청을 전송한다', async () => {
  const originalDelete = client.delete;

  try {
    client.delete = async (path) => {
      assert.equal(path, '/small-groups/7/votes/11');
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await deleteSmallGroupVote(7, 11);
  } finally {
    client.delete = originalDelete;
  }
});

test('소모임 투표 삭제는 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalDelete = client.delete;

  try {
    client.delete = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_VOTE_DELETE_FORBIDDEN',
            message: '투표 작성자인 소모임 개설자만 투표를 삭제할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      deleteSmallGroupVote(7, 11),
      (error) => error instanceof SmallGroupVoteDeleteError
        && error.code === 'SMALL_GROUP_VOTE_DELETE_FORBIDDEN'
        && error.message === '투표 작성자인 소모임 개설자만 투표를 삭제할 수 있습니다.',
    );
  } finally {
    client.delete = originalDelete;
  }
});

test('소모임 투표 삭제는 이미 삭제됐거나 다른 소모임 투표의 NOT_FOUND 오류를 보존한다', async () => {
  const originalDelete = client.delete;

  try {
    client.delete = async () => {
      const error = new Error('Not Found');
      error.response = {
        data: {
          success: false,
          error: { code: 'SMALL_GROUP_VOTE_NOT_FOUND', message: '투표를 찾을 수 없습니다.' },
        },
      };
      throw error;
    };

    await assert.rejects(
      deleteSmallGroupVote(7, 11),
      (error) => error instanceof SmallGroupVoteDeleteError
        && error.code === 'SMALL_GROUP_VOTE_NOT_FOUND',
    );
  } finally {
    client.delete = originalDelete;
  }
});

test('소모임 투표 참여는 선택 보기 ID를 POST 본문으로 전송한다', async () => {
  const originalPost = client.post;

  try {
    client.post = async (path, payload) => {
      assert.equal(path, '/small-groups/7/votes/11/participations');
      assert.deepEqual(payload, { selectedOptionIds: [101, 102] });
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await participateSmallGroupVote(7, 11, [101, 102]);
  } finally {
    client.post = originalPost;
  }
});

test('소모임 투표 참여는 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalPost = client.post;

  try {
    client.post = async () => {
      const error = new Error('Closed');
      error.response = {
        data: {
          success: false,
          error: { code: 'SMALL_GROUP_VOTE_CLOSED', message: '마감된 투표에는 참여할 수 없습니다.' },
        },
      };
      throw error;
    };

    await assert.rejects(
      participateSmallGroupVote(7, 11, [101]),
      (error) => error instanceof SmallGroupVoteParticipationError
        && error.code === 'SMALL_GROUP_VOTE_CLOSED'
        && error.message === '마감된 투표에는 참여할 수 없습니다.',
    );
  } finally {
    client.post = originalPost;
  }
});

test('소모임 투표 현황은 참여 완료 회원 전용 GET 요청과 상세 응답 검증을 사용한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;
  const detail = voteDetail();

  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7/votes/11/status');
      assert.deepEqual(options, { signal });
      return { data: { success: true, data: detail, meta: null, error: null } };
    };

    assert.deepEqual(await getSmallGroupVoteStatus(7, 11, { signal }), detail);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 현황은 미참여 회원 오류 코드를 보존한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_VOTE_STATUS_FORBIDDEN',
            message: '투표를 완료한 소모임 회원만 투표 현황을 조회할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      getSmallGroupVoteStatus(7, 11),
      (error) => error instanceof SmallGroupVoteStatusLoadError
        && error.code === 'SMALL_GROUP_VOTE_STATUS_FORBIDDEN',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 상세는 두 ID와 취소 신호로 GET 요청하고 검증된 응답을 반환한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;
  const detail = voteDetail();

  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7/votes/11');
      assert.deepEqual(options, { signal });
      return { data: { success: true, data: detail, meta: null, error: null } };
    };

    assert.deepEqual(await getSmallGroupVoteDetail(7, 11, { signal }), detail);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 상세는 필수 응답 필드가 없으면 INVALID_RESPONSE로 실패한다', () => {
  assert.throws(
    () => toSmallGroupVoteDetail({ smallGroupVoteId: 11, title: '11월 모임 장소' }),
    (error) => error instanceof SmallGroupVoteDetailLoadError && error.code === 'INVALID_RESPONSE',
  );
});

test('소모임 투표 상세 조회는 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => {
      const error = new Error('Not Found');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_VOTE_NOT_FOUND',
            message: '투표를 찾을 수 없습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      getSmallGroupVoteDetail(7, 11),
      (error) => error instanceof SmallGroupVoteDetailLoadError
        && error.code === 'SMALL_GROUP_VOTE_NOT_FOUND'
        && error.message === '투표를 찾을 수 없습니다.',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 목록은 취소 신호와 함께 GET 요청하고 목록 항목만 반환한다', async () => {
  const originalGet = client.get;
  const signal = new AbortController().signal;
  const votes = [
    { smallGroupVoteId: 11, title: '11월 정기 모임 날짜', status: 'OPEN', endsAt: '2026-11-02T18:00:00' },
    { smallGroupVoteId: 12, title: '식사 장소', status: 'CLOSED', endsAt: null },
  ];

  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/7/votes');
      assert.deepEqual(options, { signal });
      return { data: { success: true, data: votes, meta: null, error: null } };
    };

    assert.deepEqual(await getSmallGroupVotes(7, { signal }), votes);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 목록의 빈 결과는 정상 응답으로 유지한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => ({ data: { success: true, data: [], meta: null, error: null } });

    assert.deepEqual(await getSmallGroupVotes(7), []);
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 목록은 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_VOTE_LIST_FORBIDDEN',
            message: '승인된 소모임 회원만 투표 목록을 조회할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      getSmallGroupVotes(7),
      (error) => error instanceof SmallGroupVoteListLoadError
        && error.code === 'SMALL_GROUP_VOTE_LIST_FORBIDDEN'
        && error.message === '승인된 소모임 회원만 투표 목록을 조회할 수 있습니다.',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 목록은 알 수 없는 상태값을 INVALID_RESPONSE로 처리한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => ({
      data: { success: true, data: [{ smallGroupVoteId: 11, title: '상태가 잘못된 응답', status: 'WAITING', endsAt: null }], meta: null, error: null },
    });

    await assert.rejects(
      getSmallGroupVotes(7),
      (error) => error instanceof SmallGroupVoteListLoadError && error.code === 'INVALID_RESPONSE',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 목록은 상세 식별자가 없으면 INVALID_RESPONSE로 처리한다', async () => {
  const originalGet = client.get;

  try {
    client.get = async () => ({
      data: { success: true, data: [{ title: '식사 장소', status: 'OPEN', endsAt: null }], meta: null, error: null },
    });

    await assert.rejects(
      getSmallGroupVotes(7),
      (error) => error instanceof SmallGroupVoteListLoadError && error.code === 'INVALID_RESPONSE',
    );
  } finally {
    client.get = originalGet;
  }
});

test('소모임 투표 생성은 소모임 ID와 입력값을 POST 본문으로 전달하고 생성 ID를 반환한다', async () => {
  const originalPost = client.post;
  const request = {
    title: '주말 산책 장소',
    options: ['한강 공원', '동네 둘레길'],
    multipleChoice: false,
    anonymous: true,
    endsAt: '2099-01-01T12:00',
  };

  try {
    client.post = async (path, payload) => {
      assert.equal(path, '/small-groups/7/votes');
      assert.deepEqual(payload, request);
      return {
        data: {
          success: true,
          data: { smallGroupVoteId: 101 },
          meta: null,
          error: null,
        },
      };
    };

    assert.deepEqual(await createSmallGroupVote(7, request), { smallGroupVoteId: 101 });
  } finally {
    client.post = originalPost;
  }
});

test('소모임 투표 생성 실패는 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalPost = client.post;

  try {
    client.post = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_VOTE_CREATE_FORBIDDEN',
            message: '승인된 소모임 개설자만 투표를 생성할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      createSmallGroupVote(7, {}),
      (error) => error instanceof SmallGroupVoteCreateError
        && error.code === 'SMALL_GROUP_VOTE_CREATE_FORBIDDEN'
        && error.message === '승인된 소모임 개설자만 투표를 생성할 수 있습니다.',
    );
  } finally {
    client.post = originalPost;
  }
});

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

test('소모임 가입 신청 승인은 두 ID 경로만 사용하고 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalPatch = client.patch;

  try {
    client.patch = async (...args) => {
      assert.deepEqual(args, ['/small-groups/7/join-requests/9007199254740993/approve']);
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await assert.doesNotReject(approveSmallGroupJoinRequest(7, '9007199254740993'));

    client.patch = async () => {
      const error = new Error('Forbidden');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'SMALL_GROUP_OWNER_REQUIRED',
            message: '소모임 개설자만 가입 신청을 승인할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      approveSmallGroupJoinRequest(7, '9007199254740993'),
      (error) => error instanceof SmallGroupJoinRequestError
        && error.code === 'SMALL_GROUP_OWNER_REQUIRED'
        && error.message === '소모임 개설자만 가입 신청을 승인할 수 있습니다.',
    );
  } finally {
    client.patch = originalPatch;
  }
});

test('소모임 가입 신청 거절은 두 ID 경로만 사용하고 서버 오류 코드와 메시지를 보존한다', async () => {
  const originalPatch = client.patch;

  try {
    client.patch = async (...args) => {
      assert.deepEqual(args, ['/small-groups/7/join-requests/9007199254740993/reject']);
      return { data: { success: true, data: null, meta: null, error: null } };
    };

    await assert.doesNotReject(rejectSmallGroupJoinRequest(7, '9007199254740993'));

    client.patch = async () => {
      const error = new Error('Conflict');
      error.response = {
        data: {
          success: false,
          error: {
            code: 'JOIN_REQUEST_NOT_PENDING',
            message: '대기 중인 가입 신청만 거절할 수 있습니다.',
          },
        },
      };
      throw error;
    };

    await assert.rejects(
      rejectSmallGroupJoinRequest(7, '9007199254740993'),
      (error) => error instanceof SmallGroupJoinRequestError
        && error.code === 'JOIN_REQUEST_NOT_PENDING'
        && error.message === '대기 중인 가입 신청만 거절할 수 있습니다.',
    );
  } finally {
    client.patch = originalPatch;
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

function voteDetail() {
  return {
    smallGroupVoteId: 11,
    title: '11월 모임 장소',
    creatorNickname: '모임장',
    multipleChoice: true,
    anonymous: false,
    createdAt: '2026-10-30T10:00:00',
    endsAt: '2026-10-31T18:00:00',
    status: 'OPEN',
    participantCount: 2,
    myParticipation: {
      participated: true,
      selectedOptionIds: [101],
    },
    options: [{
      smallGroupVoteOptionId: 101,
      content: '한강공원',
      displayOrder: 1,
      voteCount: 2,
      voterNicknames: ['걷기좋아', '모아회원'],
    }],
  };
}

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
