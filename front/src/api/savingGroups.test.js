import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { getSavingGroups, getSavingGroupPages, savingGroupRegionOptions, toSavingGroupCard,
  getSavingGroupDetail, getSavingGroupMenus, formatSavingGroupAmount, requestSavingGroupJoin,
  approveSavingGroupJoin, rejectSavingGroupJoin, getSavingGroupJoinRequests, createSavingGroupApplication,
  createSavingGroupNotice, getSavingGroupNoticeDetail, getSavingGroupNoticePagination,
  readSavingGroupNoticeQuery, toSavingGroupNoticeSearch, updateSavingGroupNoticePinned } from './savingGroups.js';

// AI작성: 공통 groupId·공지 ID의 큰 정수 문자열과 취소 신호, 서버 응답 6개 항목을 보존한다.
test('공지 상세는 groupId와 noticeId로 요청하고 상세 데이터를 반환한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  const detail = { noticeId: '9223372036854775806', title: '공지 제목', content: '공지 내용',
    writerId: 30, writerNickname: '테스트닉네임', createdAt: '2026-11-04T10:00:00' };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/9223372036854775807/notices/9223372036854775806');
      assert.deepEqual(options, { signal });
      return { status: 200, data: { success: true, data: detail } };
    };
    assert.deepEqual(await getSavingGroupNoticeDetail('9223372036854775807', '9223372036854775806', { signal }), detail);
  } finally { client.get = original; }
});

// AI작성: HTTP 오류와 실패 봉투 모두 조회 오류 코드·안내 문구·상태를 유지한다.
test('공지 상세는 서버의 인증 권한 및 조회 오류를 보존한다', async () => {
  const original = client.get;
  try {
    for (const [status, code] of [[400, 'BAD_REQUEST'], [401, 'LOGIN_REQUIRED'], [401, 'UNAUTHORIZED'],
      [403, 'SAVING_GROUP_NOTICE_READ_FORBIDDEN'], [404, 'NOT_FOUND'],
      [404, 'SAVING_GROUP_NOTICE_NOT_FOUND'], [500, 'INTERNAL_ERROR']]) {
      for (const rejected of [false, true]) {
        client.get = async () => {
          const response = { status, data: { success: false, error: { code, message: '조회 실패' } } };
          if (rejected) throw { response };
          return response;
        };
        await assert.rejects(() => getSavingGroupNoticeDetail(20, 40),
          (error) => error.code === code && error.message === '조회 실패' && error.status === status);
      }
    }
  } finally { client.get = original; }
});

// AI작성: 본문 없는 성공 응답을 정상 상세로 전달하지 않고 통신 실패와 요청 취소도 구분한다.
test('공지 상세는 잘못된 응답과 통신 실패를 안내하고 취소 원본을 유지한다', async () => {
  const original = client.get;
  try {
    for (const response of [{}, { data: null }, { data: { success: false } }, { data: { success: 'true' } }]) {
      client.get = async () => response;
      await assert.rejects(() => getSavingGroupNoticeDetail(20, 40),
        (error) => error.code === 'SAVING_GROUP_NOTICE_READ_FAILED' && !error.message.includes('등록'));
    }
    for (const data of [undefined, null, [], '공지', 1]) {
      client.get = async () => ({ status: 200, data: { success: true, data } });
      await assert.rejects(() => getSavingGroupNoticeDetail(20, 40),
        (error) => error.code === 'INVALID_RESPONSE' && error.status === 200);
    }
    client.get = async () => { throw new Error('network failure'); };
    await assert.rejects(() => getSavingGroupNoticeDetail(20, 40),
      (error) => error.code === 'SAVING_GROUP_NOTICE_READ_FAILED' && error.status === undefined);
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(() => getSavingGroupNoticeDetail(20, 40), (error) => error === canceled);
  } finally { client.get = original; }
});

// AI작성: 두 경로 ID 중 하나라도 유효하지 않으면 네트워크 호출 전에 차단한다.
test('공지 상세는 잘못된 groupId 또는 noticeId로 요청하지 않는다', async () => {
  const original = client.get;
  let calls = 0;
  try {
    client.get = async () => { calls += 1; };
    for (const id of [null, undefined, 0, -1, 'abc', '20/notices', '01', '1.5',
      '9223372036854775808', Number.MAX_SAFE_INTEGER + 1]) {
      for (const [groupId, noticeId] of [[id, 40], [20, id]]) {
        await assert.rejects(() => getSavingGroupNoticeDetail(groupId, noticeId),
          (error) => error.code === 'BAD_REQUEST');
      }
    }
    assert.equal(calls, 0);
  } finally { client.get = original; }
});

// AI작성: 저장 대상 savingGroupId를 보존하고 작성자 등 추가 입력은 서버로 보내지 않는다.
test('공지 작성은 제목·내용만 JSON으로 전달하고 빈 성공 응답을 반환한다', async () => {
  const original = client.post;
  const signal = new AbortController().signal;
  try {
    client.post = async (path, body, options) => {
      assert.equal(path, '/saving-groups/9223372036854775807/notices');
      assert.deepEqual(body, { title: ' 공지 ', content: '공지 내용' });
      assert.equal(options.signal, signal);
      return { status: 200, data: { success: true, data: null } };
    };
    assert.equal(await createSavingGroupNotice('9223372036854775807',
      { title: ' 공지 ', content: '공지 내용', writerId: 99, groupId: 42 }, { signal }), null);
  } finally { client.post = original; }
});

// AI작성: 상태·권한·검증 오류를 성공 데이터로 바꾸지 않고 HTTP 오류와 실패 봉투 모두 보존한다.
test('공지 작성은 서버 오류 코드·문구·HTTP 상태를 보존한다', async () => {
  const original = client.post;
  try {
    for (const [status, code] of [[400, 'BAD_REQUEST'], [401, 'LOGIN_REQUIRED'],
      [403, 'SAVING_GROUP_NOTICE_WRITE_FORBIDDEN'], [404, 'SAVING_GROUP_NOT_FOUND'],
      [409, 'SAVING_GROUP_ACTIVITY_RESTRICTED'], [500, 'SAVING_GROUP_NOTICE_WRITE_FAILED']]) {
      for (const rejected of [false, true]) {
        client.post = async () => {
          const response = { status, data: { success: false, error: { code, message: '등록 실패' } } };
          if (rejected) throw { response };
          return response;
        };
        await assert.rejects(() => createSavingGroupNotice(10, { title: '공지', content: '내용' }),
          (error) => error.code === code && error.message === '등록 실패' && error.status === status);
      }
    }
  } finally { client.post = original; }
});

// AI작성: 통신 실패·잘못된 응답은 안내 오류로 바꾸고 사용자 요청 취소는 원본 그대로 전달한다.
test('공지 작성은 통신·응답 실패를 안내하고 취소를 보존한다', async () => {
  const original = client.post;
  try {
    for (const response of [{}, { data: null }, { data: { success: false } }, { data: { success: 'true' } }]) {
      client.post = async () => response;
      await assert.rejects(() => createSavingGroupNotice(10, { title: '공지', content: '내용' }),
        (error) => error.code === 'SAVING_GROUP_NOTICE_WRITE_FAILED');
    }
    client.post = async () => { throw new Error('network failure'); };
    await assert.rejects(() => createSavingGroupNotice(10, { title: '공지', content: '내용' }),
      (error) => error.code === 'SAVING_GROUP_NOTICE_WRITE_FAILED');
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.post = async () => { throw canceled; };
    await assert.rejects(() => createSavingGroupNotice(10, { title: '공지', content: '내용' }),
      (error) => error === canceled);
  } finally { client.post = original; }
});

// AI작성: 잘못된 경로 ID는 네트워크 호출 전에 차단한다.
test('공지 작성은 유효하지 않은 ID로 요청하지 않는다', async () => {
  const original = client.post;
  let calls = 0;
  try {
    client.post = async () => { calls += 1; };
    for (const id of [null, undefined, 0, -1, 'abc', '10/notice', '9223372036854775808', Number.MAX_SAFE_INTEGER + 1]) {
      await assert.rejects(() => createSavingGroupNotice(id, { title: '공지', content: '내용' }),
        (error) => error.code === 'BAD_REQUEST');
    }
    assert.equal(calls, 0);
  } finally { client.post = original; }
});

// AI작성: 구 단위·동 단위·세종 신청의 multipart 요청 계약을 네트워크 호출 없이 검증한다.
test('개설 요청은 최종 지역명 하나와 기존 입력·이미지를 보존한다', async () => {
  const original = client.post;
  try {
    for (const regionName of ['서울특별시 강남구', '서울특별시 강남구 역삼동', '부산광역시 해운대구', '세종특별자치시']) {
      for (const withImage of [false, true]) {
        const input = new FormData();
        const fields = { groupName: '테스트 모임', groupType: '친목계', regionName,
          maxMembers: '10', description: '지역 선택 검증', duesAmount: '30000', duesDay: '15' };
        Object.entries(fields).forEach(([key, value]) => input.set(key, value));
        if (withImage) input.set('groupImage', new File(['test image'], 'test.png', { type: 'image/png' }));
        client.post = async (path, body) => {
          assert.equal(path, '/saving-group');
          assert.ok(body instanceof FormData);
          assert.deepEqual(body.getAll('regionName'), [regionName]);
          Object.entries(fields).forEach(([key, value]) => assert.equal(body.get(key), value));
          assert.deepEqual([...body.keys()].sort(), [...Object.keys(fields), ...(withImage ? ['groupImage'] : [])].sort());
          if (withImage) {
            assert.equal(body.get('groupImage').name, 'test.png');
            assert.equal(await body.get('groupImage').text(), 'test image');
          }
          return { data: { success: true } };
        };
        await createSavingGroupApplication(input);
      }
    }
  } finally { client.post = original; }
});

// AI작성: 같은 입력 폼의 지역 변경 후 이전 지역명이 요청에 남지 않는지 확인한다.
test('지역 변경 후 개설 요청은 이전 지역명을 포함하지 않는다', async () => {
  const original = client.post;
  const input = new FormData();
  const sent = [];
  try {
    client.post = async (_path, body) => {
      sent.push(body.getAll('regionName'));
      return { data: { success: true } };
    };
    input.set('regionName', '서울특별시 강남구 역삼동');
    await createSavingGroupApplication(input);
    input.set('regionName', '부산광역시 해운대구');
    await createSavingGroupApplication(input);
    assert.deepEqual(sent, [['서울특별시 강남구 역삼동'], ['부산광역시 해운대구']]);
  } finally { client.post = original; }
});

// AI작성: 지역 관련 서버 검증 오류와 통신 실패를 성공으로 처리하지 않는다.
test('개설 요청은 서버 지역 오류 코드와 통신 실패를 전달한다', async () => {
  const original = client.post;
  try {
    client.post = async () => { throw { response: { data: { error: {
      code: 'REGION_NOT_FOUND', message: '활동 지역을 다시 선택해 주세요.' } } } }; };
    await assert.rejects(() => createSavingGroupApplication(new FormData()),
      (error) => error.code === 'REGION_NOT_FOUND' && error.message === '활동 지역을 다시 선택해 주세요.');
    client.post = async () => { throw new Error('network failure'); };
    await assert.rejects(() => createSavingGroupApplication(new FormData()),
      (error) => error.code === 'SAVING_GROUP_APPLICATION_FAILED');
  } finally { client.post = original; }
});

// AI작성: 추가 테스트 패키지 없이 카드 표시와 페이징·API 실패 계약을 검증한다.
test('지역 코드는 01~10 순서이며 카드는 기존 필드와 표시 형식을 유지한다', () => {
  assert.equal(savingGroupRegionOptions[0].name, '서울');
  assert.equal(savingGroupRegionOptions[9].code, '10');
  const card = toSavingGroupCard({ savingGroupId: 101, groupId: 42, groupName: '테스트', description: '소개',
    regionName: '서울', maxMembers: 8, memberCount: 5, duesAmount: '50000.00', duesDay: 25, status: '01' });
  assert.equal(card.members, '5 / 8명');
  assert.equal(card.id, 42);
  assert.equal(card.monthlyDues, '월 50,000원');
  assert.equal(card.nextDate, '매월 25일');
  assert.equal(card.status, '모집중');
  assert.equal(card.district, '서울');
});

// AI작성: 미가입·비승인은 공지만, 회계·계원은 같은 여섯 탭, 계주는 아홉 탭이다.
test('승인 역할별 메뉴와 미정의 역할의 권한 축소', () => {
  const common = ['notice', 'payment', 'calendar', 'dues', 'transactionHistory', 'payoutRequest'];
  for (const role of [null, undefined, '', 'PENDING', 'REJECTED', 'LEFT', 'ADMIN']) {
    assert.deepEqual(getSavingGroupMenus(role), ['notice']);
  }
  assert.deepEqual(getSavingGroupMenus('MANAGER'), common);
  assert.deepEqual(getSavingGroupMenus('MEMBER'), common);
  assert.deepEqual(getSavingGroupMenus('OWNER'), [...common, 'members', 'settings', 'joinRequests']);
});

// AI작성: 화면 표시 금액과 원본 값을 구분하고 null을 무료로 바꾸지 않는다.
test('금액 표시의 천 단위 구분·소수 유지·null·잘못된 값', () => {
  assert.equal(formatSavingGroupAmount('100000.00'), '100,000원');
  assert.equal(formatSavingGroupAmount('1234.50'), '1,234.5원');
  assert.equal(formatSavingGroupAmount(0), '0원');
  assert.equal(formatSavingGroupAmount(null), '회비 미정');
  assert.equal(formatSavingGroupAmount('invalid'), '회비 확인 필요');
  assert.equal(formatSavingGroupAmount(-1), '회비 확인 필요');
});

// AI작성: 구·동·세종·기존 광역 표시를 임의로 변환하지 않고 매 요청의 서버 값을 사용한다.
test('상세 재조회는 저장된 지역명을 보존하고 이전 응답을 재사용하지 않는다', async () => {
  const original = client.get;
  let calls = 0;
  try {
    for (const regionName of ['서울특별시 강남구', '서울특별시 강남구 역삼동', '세종특별자치시', '서울']) {
      client.get = async (path) => {
        assert.equal(path, '/saving-groups/42');
        calls += 1;
        return { data: { success: true, data: { groupId: '42', regionName, notices: [] } } };
      };
      const first = await getSavingGroupDetail('42');
      const reloaded = await getSavingGroupDetail('42');
      assert.equal(first.regionName, regionName);
      assert.equal(reloaded.regionName, regionName);
      assert.notEqual(first, reloaded);
    }
    assert.equal(calls, 8);
  } finally { client.get = original; }
});

// AI작성: 큰 URL ID와 취소 신호를 유지하고 상세 요청 실패를 검증한다.
test('상세 요청은 URL ID와 취소 신호를 보존하고 실패를 숨기지 않는다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  const detail = { groupId: '9223372036854775807', notices: [], myRole: null };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/9223372036854775807');
      assert.equal(options.signal, signal);
      return { data: { success: true, data: detail } };
    };
    assert.deepEqual(await getSavingGroupDetail(detail.groupId, { signal }), { ...detail, noticePageMeta: null });
    client.get = async () => ({ data: { success: false, error: { message: '상세 오류' } } });
    await assert.rejects(() => getSavingGroupDetail('42'), /상세 오류/);
    for (const status of [400, 403, 404, 500]) {
      const error = Object.assign(new Error('요청 실패'), { response: { status } });
      client.get = async () => { throw error; };
      await assert.rejects(() => getSavingGroupDetail('42'), (value) => value === error);
    }
  } finally { client.get = original; }
});

// AI작성: 미정과 이상 금액을 구분하고 소수를 반올림해 숨기지 않는다.
test('빈 값 대체 문구와 진행중 배지, 소수 회비 확인 표시', () => {
  const card = toSavingGroupCard({ description: ' ', memberCount: 0, status: '02' });
  assert.equal(card.description, '소개글이 없습니다.');
  assert.equal(card.members, '0명 / 정원 미정');
  assert.equal(card.monthlyDues, '회비 미정');
  assert.equal(card.nextDate, '미정');
  assert.equal(card.status, '진행중');
  assert.equal(toSavingGroupCard({ duesAmount: '123.50' }).monthlyDues, '회비 확인 필요');
});

// AI작성: 전체 5·6페이지의 묶음과 빈 결과·범위 초과를 확인한다.
test('페이지 번호는 최대 다섯 개이며 초과한 번호를 만들지 않는다', () => {
  assert.deepEqual(getSavingGroupPages(1, 5), [1, 2, 3, 4, 5]);
  assert.deepEqual(getSavingGroupPages(5, 6), [1, 2, 3, 4, 5]);
  assert.deepEqual(getSavingGroupPages(6, 6), [6]);
  assert.deepEqual(getSavingGroupPages(6, 10), [6, 7, 8, 9, 10]);
  assert.deepEqual(getSavingGroupPages(1, 0), []);
  assert.deepEqual(getSavingGroupPages(3, 2), []);
});

// AI작성: 실제 네트워크 호출 없이 파라미터 유지와 공통 응답 성공·실패를 확인한다.
test('검색·지역·페이지를 전달하고 API 실패를 정상 데이터로 처리하지 않는다', async () => {
  const original = client.get;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups');
      assert.deepEqual(options.params, { keyword: '산책', regionCode: '01', page: 2 });
      return { data: { success: true, data: [], meta: { page: 2, size: 6, total: 7, totalPages: 2 } } };
    };
    const result = await getSavingGroups({ keyword: '산책', regionCode: '01', page: 2 });
    assert.deepEqual(result, {
      items: [], totalElements: 7, totalPages: 2, currentPage: 2, pageSize: 6,
    });
    client.get = async () => ({ data: { success: false, error: { message: '요청 오류' } } });
    await assert.rejects(() => getSavingGroups({ page: 1 }), /요청 오류/);
    client.get = async () => { throw new Error('network failure'); };
    await assert.rejects(() => getSavingGroups({ page: 1 }), /network failure/);
  } finally { client.get = original; }
});

// AI작성: 선택한 단계까지만 전달하고 전체 조회·세종·취소 신호·기존 코드 요청을 구분한다.
test('시만 또는 시·구까지만 선택한 검색과 동 검색의 파라미터를 보존한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  const areas = [
    {},
    { provinceName: '서울특별시' },
    { provinceName: '서울특별시', districtName: '강남구' },
    { provinceName: '서울특별시', districtName: '강남구', townName: '역삼동' },
    { provinceName: '세종특별자치시', districtName: '', townName: '어진동' },
  ];
  try {
    for (const area of areas) {
      client.get = async (path, options) => {
        assert.equal(path, '/saving-groups');
        assert.deepEqual(options.params, { keyword: '산책', regionCode: undefined, ...area, page: 1 });
        assert.equal(options.signal, signal);
        return { data: { success: true, data: [], meta: { page: 1, size: 6, total: 0, totalPages: 0 } } };
      };
      await getSavingGroups({ keyword: '산책', ...area, page: 1, signal });
    }
  } finally { client.get = original; }
});

test('가입 신청 성공 응답을 그대로 반환한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async (config) => ({
    data: { success: true, data: null, meta: null, error: null },
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
  });

  try {
    const result = await requestSavingGroupJoin(12);

    assert.equal(result.success, true);
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});

// AI작성: 새 의존성 없이 기존 Axios adapter와 Node 테스트로 실제 목록·승인 계약을 검증한다.
async function withAdapter(adapter, action) {
  const original = client.defaults.adapter;
  client.defaults.adapter = adapter;
  try { await action(); } finally { client.defaults.adapter = original; }
}

function ok(config, data = null, meta = null) {
  return { data: { success: true, data, meta, error: null }, status: 200, headers: {}, config };
}

test('신청 목록은 서버 필터와 페이지를 전달하고 마스킹 이름·문자열 ID를 유지한다', async () => {
  const controller = new AbortController();
  await withAdapter(async (config) => {
    assert.equal(config.url, '/saving-groups/12/join-requests');
    assert.equal(config.params.status, 'PENDING');
    assert.equal(config.params.page, 2);
    assert.equal(config.signal, controller.signal);
    return ok(config, [{ crewMemberId: '9007199254740993', applicant: '가**', regionName: '서울',
      identityVerified: true, regionVerified: true, applicationMessage: null, membershipStatus: 'PENDING' }],
    { page: 2, size: 10, total: 11, totalPages: 2 });
  }, async () => {
    const result = await getSavingGroupJoinRequests('12', { status: 'PENDING', page: 2, signal: controller.signal });
    assert.equal(result.items[0].id, '9007199254740993');
    assert.equal(result.items[0].applicant, '가**');
    assert.equal(result.items[0].status, '대기');
    assert.equal(result.items[0].verification, '본인·지역 인증 완료');
    assert.equal(result.items[0].message, '신청 메시지가 없습니다.');
    assert.equal(result.meta.total, 11);
  });
});

test('승인은 원래 신청 ID를 경로에 넣고 요청 본문과 승인자를 보내지 않는다', async () => {
  await withAdapter(async (config) => {
    assert.equal(config.method, 'patch');
    assert.equal(config.url, '/saving-groups/12/join-requests/9007199254740993/approve');
    assert.equal(config.data, undefined);
    assert.equal(config.params, undefined);
    return ok(config);
  }, async () => assert.equal((await approveSavingGroupJoin('12', '9007199254740993')).success, true));
});

test('거절은 원래 신청 ID를 경로에 넣고 요청 본문과 처리자를 보내지 않는다', async () => {
  await withAdapter(async (config) => {
    assert.equal(config.method, 'patch');
    assert.equal(config.url, '/saving-groups/12/join-requests/9007199254740993/reject');
    assert.equal(config.data, undefined);
    assert.equal(config.params, undefined);
    return ok(config);
  }, async () => assert.equal((await rejectSavingGroupJoin('12', '9007199254740993')).success, true));
});

for (const [status, code] of [[401, 'UNAUTHORIZED'], [403, 'SAVING_GROUP_OWNER_REQUIRED'],
  [404, 'JOIN_REQUEST_NOT_FOUND'], [409, 'JOIN_REQUEST_NOT_PENDING'], [409, 'SAVING_GROUP_CAPACITY_FULL'],
  [409, 'SAVING_GROUP_JOIN_LIMIT_EXCEEDED']]) {
  test(`승인 실패 ${status} ${code}는 성공으로 바꾸지 않는다`, async () => {
    await withAdapter(async () => {
      const error = new Error('HTTP 요청 실패');
      error.response = { status, data: { success: false, error: { code, message: '승인 조건을 확인해주세요.' } } };
      throw error;
    }, async () => assert.rejects(approveSavingGroupJoin('12', '50'),
      (error) => error.code === code && error.message === '승인 조건을 확인해주세요.'));
  });
}

for (const [status, code] of [[401, 'UNAUTHORIZED'], [403, 'SAVING_GROUP_OWNER_REQUIRED'],
  [404, 'JOIN_REQUEST_NOT_FOUND'], [409, 'JOIN_REQUEST_NOT_PENDING'],
  [409, 'JOIN_REQUEST_REJECTION_FAILED']]) {
  test(`거절 실패 ${status} ${code}는 성공으로 바꾸지 않는다`, async () => {
    await withAdapter(async () => {
      const error = new Error('HTTP 요청 실패');
      error.response = { status, data: { success: false, error: { code, message: '거절 조건을 확인해주세요.' } } };
      throw error;
    }, async () => assert.rejects(rejectSavingGroupJoin('12', '50'),
      (error) => error.code === code && error.message === '거절 조건을 확인해주세요.'));
  });
}

test('인증 없는 목록 실패는 빈 목록이나 Mock으로 대체하지 않는다', async () => {
  await withAdapter(async (config) => ({ ...ok(config), data: { success: false,
    error: { code: 'UNAUTHORIZED', message: '로그인이 필요합니다.' } } }),
  async () => assert.rejects(getSavingGroupJoinRequests('12'), (error) => error.code === 'UNAUTHORIZED'));
});

test('조회 취소는 취소 상태를 유지한다', async () => {
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(getSavingGroupJoinRequests('12', { signal: controller.signal }), (error) => error.code === 'ERR_CANCELED');
});

test('잘못된 ID와 정밀도가 손실된 숫자는 네트워크 호출 전에 차단한다', async () => {
  let called = false;
  await withAdapter(async (config) => { called = true; return ok(config); }, async () => {
    for (const id of [null, 0, -1, 'abc', '../1', '9223372036854775808', 9007199254740992]) {
      await assert.rejects(approveSavingGroupJoin('12', id), (error) => error.code === 'BAD_REQUEST');
    }
  });
  assert.equal(called, false);
});

test('잘못된 목록 응답은 사용자용 오류로 처리한다', async () => {
  await withAdapter(async (config) => ok(config), async () => {
    await assert.rejects(getSavingGroupJoinRequests('12'), (error) => error.code === 'INVALID_RESPONSE');
  });
});

test('신청 0건은 정상 빈 목록이다', async () => {
  await withAdapter(async (config) => ok(config, [], { page: 1, size: 10, total: 0, totalPages: 0 }),
    async () => assert.deepEqual((await getSavingGroupJoinRequests('12')).items, []));
});

test('본인 인증 미완료 응답의 코드와 메시지를 보존한다', async () => {
  const originalAdapter = client.defaults.adapter;
  client.defaults.adapter = async () => {
    const error = new Error('Forbidden');
    error.response = {
      data: {
        success: false,
        error: {
          code: 'IDENTITY_VERIFICATION_REQUIRED',
          message: '본인 인증 완료 후 가입 신청할 수 있습니다.',
        },
      },
      status: 403,
    };
    throw error;
  };

  try {
    await assert.rejects(
      requestSavingGroupJoin(12),
      (error) => error.code === 'IDENTITY_VERIFICATION_REQUIRED'
        && error.message === '본인 인증 완료 후 가입 신청할 수 있습니다.',
    );
  } finally {
    client.defaults.adapter = originalAdapter;
  }
});

// AI작성: 기존 상세 API에 공지 조건과 취소 신호를 전달하고 상세 필드·페이징 정보를 함께 보존한다.
test('공지 페이지와 정렬을 전달하고 PageMeta를 반환한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  const meta = { page: 4, size: 4, total: 25, totalPages: 7 };
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/saving-groups/42');
      assert.deepEqual(options.params, { noticePage: 4, noticeSort: 'OLDEST' });
      assert.equal(options.signal, signal);
      return { data: { success: true, data: { groupId: '42', notices: [] }, meta } };
    };
    assert.deepEqual(await getSavingGroupDetail('42', { noticePage: 4, noticeSort: 'OLDEST', signal }),
      { groupId: '42', notices: [], noticePageMeta: meta });
    client.get = async (path, options) => {
      assert.deepEqual(options.params, { noticePage: 1, noticeSort: 'LATEST' });
      return { data: { success: true, data: { groupId: '42', notices: [] }, meta: { ...meta, page: 1 } } };
    };
    assert.equal((await getSavingGroupDetail('42')).noticePageMeta.page, 1);
  } finally { client.get = original; }
});

// AI작성: 사용자 지정 3개 묶음 이동과 빈 목록의 1페이지 표시를 검증한다.
test('공지 페이지는 세 개 묶음의 앞 끝과 뒤 시작으로 이동한다', () => {
  assert.deepEqual(getSavingGroupNoticePagination(1, 7), { pages: [1, 2, 3], previous: null, next: 4 });
  assert.deepEqual(getSavingGroupNoticePagination(3, 7), { pages: [1, 2, 3], previous: null, next: 4 });
  assert.deepEqual(getSavingGroupNoticePagination(4, 7), { pages: [4, 5, 6], previous: 3, next: 7 });
  assert.deepEqual(getSavingGroupNoticePagination(7, 7), { pages: [7], previous: 6, next: null });
  assert.deepEqual(getSavingGroupNoticePagination(1, 0), { pages: [1], previous: null, next: null });
  assert.deepEqual(getSavingGroupNoticePagination(1, 1), { pages: [1], previous: null, next: null });
});

// AI작성: 상세 복귀·새로고침용 URL 조건과 잘못된 직접 입력의 기본값을 확인한다.
test('공지 URL은 페이지와 정렬을 복원하고 서버 범위를 벗어난 입력은 기본값으로 처리한다', () => {
  const search = toSavingGroupNoticeSearch(6, 'OLDEST', 'keep=yes');
  assert.deepEqual(readSavingGroupNoticeQuery(search), { page: 6, sort: 'OLDEST' });
  assert.equal(new URLSearchParams(search).get('keep'), 'yes');
  assert.deepEqual(readSavingGroupNoticeQuery(''), { page: 1, sort: 'LATEST' });
  for (const page of ['0', '-1', '1.5', 'abc', '', '2147483648', '99999999999999999999']) {
    assert.deepEqual(readSavingGroupNoticeQuery(new URLSearchParams({ noticePage: page, noticeSort: 'UNKNOWN' })),
      { page: 1, sort: 'LATEST' });
  }
  assert.equal(readSavingGroupNoticeQuery('noticePage=2147483647').page, 2147483647);
  assert.deepEqual(readSavingGroupNoticeQuery(toSavingGroupNoticeSearch(1, 'LATEST', search)), { page: 1, sort: 'LATEST' });
});

// AI작성: 고정 변경은 큰 정수 ID와 명시 상태·취소 신호만 전달하고 오류를 그대로 구분한다.
test('고정 지정과 해제는 PATCH로 목표 상태만 전달한다', async () => {
  const original = client.patch;
  const signal = new AbortController().signal;
  try {
    for (const pinned of [true, false]) {
      client.patch = async (path, body, options) => {
        assert.equal(path, '/saving-groups/9223372036854775807/notices/9223372036854775806/pin');
        assert.deepEqual(body, { pinned }); assert.equal(options.signal, signal);
        return { data: { success: true, data: null } };
      };
      assert.equal(await updateSavingGroupNoticePinned('9223372036854775807', '9223372036854775806', pinned, { signal }), null);
    }
  } finally { client.patch = original; }
});

test('고정 변경의 권한·개수 제한·서버 실패와 취소를 보존한다', async () => {
  const original = client.patch;
  try {
    for (const [status, code] of [[401, 'UNAUTHORIZED'], [403, 'SAVING_GROUP_NOTICE_PIN_FORBIDDEN'],
      [404, 'SAVING_GROUP_NOTICE_NOT_FOUND'], [409, 'SAVING_GROUP_NOTICE_PIN_LIMIT_EXCEEDED'], [500, 'INTERNAL_ERROR']]) {
      client.patch = async () => { throw { response: { status, data: { error: { code, message: '변경 실패' } } } }; };
      await assert.rejects(() => updateSavingGroupNoticePinned(20, 40, true), e => e.status === status && e.code === code && e.message === '변경 실패');
    }
    client.patch = async () => ({ status: 200, data: { success: false, error: { code: 'FAIL', message: '실패' } } });
    await assert.rejects(() => updateSavingGroupNoticePinned(20, 40, true), e => e.code === 'FAIL');
    const canceled = Object.assign(new Error('취소'), { code: 'ERR_CANCELED' });
    client.patch = async () => { throw canceled; };
    await assert.rejects(() => updateSavingGroupNoticePinned(20, 40, false), e => e === canceled);
    client.patch = async () => { throw new Error('통신 실패'); };
    await assert.rejects(() => updateSavingGroupNoticePinned(20, 40, true), /목록을 새로 고침/);
  } finally { client.patch = original; }
});

test('잘못된 고정 ID와 상태는 전송 전에 차단한다', async () => {
  const original = client.patch;
  let calls = 0;
  client.patch = async () => { calls++; };
  try {
    for (const id of [null, 0, -1, 'abc', '9223372036854775808']) {
      await assert.rejects(() => updateSavingGroupNoticePinned(id, 40, true));
      await assert.rejects(() => updateSavingGroupNoticePinned(20, id, true));
    }
    for (const pinned of [null, undefined, 'true', 1]) await assert.rejects(() => updateSavingGroupNoticePinned(20, 40, pinned));
    assert.equal(calls, 0);
  } finally { client.patch = original; }
});
