import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { searchSmallGroupPlaces, SmallGroupPlaceSearchError } from './smallGroups.js';

// AI작성: 실제 API·키·위치 없이 기존 공통 client를 가짜 응답으로 교체하고 매 테스트 후 복원한다.
function envelope() {
  return { success: true, data: { items: [{ placeId: 'test-1', placeName: '가상 장소',
    roadAddress: '가상 도로', address: '가상 주소', category: '가상 분류', latitude: 0, longitude: 0,
    placeUrl: 'https://place.map.kakao.com/1', distanceMeters: 42 }],
  page: 1, size: 15, totalCount: 1, pageableCount: 1, end: true } };
}

test('장소 검색은 기본값과 취소 신호를 보내고 회원 ID와 미승인 조건을 제외한다', async () => {
  const original = client.get;
  const signal = new AbortController().signal;
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/places');
      assert.deepEqual(options, { params: { smallGroupId: 10, query: '가상', page: 1, size: 15, sort: 'accuracy' }, signal });
      const result = envelope();
      result.data.items[0].phone = 'excluded';
      return { data: result };
    };
    const result = await searchSmallGroupPlaces(10, { query: '가상', signal, memberId: 999, category: 'ignored' });
    assert.equal(result.items[0].distanceMeters, null);
    assert.equal('phone' in result.items[0], false);
    assert.equal(result.end, true);
  } finally { client.get = original; }
});

test('위치·반경·거리순·페이지를 명시한 경우에만 해당 값을 전달한다', async () => {
  const original = client.get;
  try {
    client.get = async (_path, { params }) => {
      assert.deepEqual(params, { smallGroupId: 10, query: '가상', page: 2, size: 5,
        sort: 'distance', latitude: 0, longitude: 0, radius: 0 });
      const result = envelope();
      result.data.page = 2;
      result.data.size = 5;
      return { data: result };
    };
    const result = await searchSmallGroupPlaces(10, { query: '가상', page: 2, size: 5,
      latitude: 0, longitude: 0, radius: 0, sort: 'distance' });
    assert.equal(result.items[0].distanceMeters, 42);
    assert.equal(result.page, 2);
  } finally { client.get = original; }
});

test('빈 결과와 접근·외부 오류의 코드와 메시지를 보존한다', async () => {
  const original = client.get;
  try {
    for (const code of ['SMALL_GROUP_PLACE_NOT_FOUND', 'SMALL_GROUP_PLACE_FORBIDDEN', 'LOGIN_REQUIRED', 'KAKAO_PLACE_REQUEST_FAILED']) {
      for (const httpError of [false, true]) {
        client.get = async () => {
          const data = { success: false, error: { code, message: '가상 오류' } };
          if (httpError) { const error = new Error(); error.response = { data }; throw error; }
          return { data };
        };
        await assert.rejects(searchSmallGroupPlaces(10, { query: '가상' }),
          (error) => error instanceof SmallGroupPlaceSearchError && error.code === code && error.message === '가상 오류');
      }
    }
  } finally { client.get = original; }
});

test('잘못된 응답과 빈 성공 목록·잘못된 링크·좌표를 거절한다', async () => {
  const original = client.get;
  try {
    const malformed = [null, {}, { ...envelope().data, items: [] }, { ...envelope().data, page: 0 },
      { ...envelope().data, end: 'true' }, { ...envelope().data, size: 16 }];
    for (const changes of [{ latitude: NaN }, { longitude: 181 }, { placeUrl: 'javascript:alert(1)' },
      { distanceMeters: -1 }, { category: {} }]) {
      malformed.push({ ...envelope().data, items: [{ ...envelope().data.items[0], ...changes }] });
    }
    for (const data of malformed) {
      client.get = async () => ({ data: { success: true, data } });
      await assert.rejects(searchSmallGroupPlaces(10, { query: '가상' }), (error) => error.code === 'INVALID_RESPONSE');
    }
  } finally { client.get = original; }
});

test('취소 오류는 그대로 전달하고 네트워크 오류에는 요청·좌표를 포함하지 않는다', async () => {
  const original = client.get;
  try {
    const canceled = Object.assign(new Error('canceled'), { code: 'ERR_CANCELED' });
    client.get = async () => { throw canceled; };
    await assert.rejects(searchSmallGroupPlaces(10), (error) => error === canceled);
    client.get = async () => { throw new Error('private-request-data'); };
    await assert.rejects(searchSmallGroupPlaces(10),
      (error) => error.code === 'SMALL_GROUP_PLACE_SEARCH_FAILED' && !error.message.includes('private'));
  } finally { client.get = original; }
});
