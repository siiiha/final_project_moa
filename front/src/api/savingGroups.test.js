import assert from 'node:assert/strict';
import { test } from 'node:test';
import client from './client.js';
import { getSavingGroups, getSavingGroupPages, savingGroupRegionOptions, toSavingGroupCard } from './savingGroups.js';

// AI작성: 추가 테스트 패키지 없이 카드 표시와 페이징·API 실패 계약을 검증한다.
test('지역 코드는 01~10 순서이며 카드는 기존 필드와 표시 형식을 유지한다', () => {
  assert.equal(savingGroupRegionOptions[0].name, '서울');
  assert.equal(savingGroupRegionOptions[9].code, '10');
  const card = toSavingGroupCard({ savingGroupId: 1, groupName: '테스트', description: '소개',
    regionName: '서울', maxMembers: 8, memberCount: 5, duesAmount: '50000.00', duesDay: 25, status: '01' });
  assert.equal(card.members, '5 / 8명');
  assert.equal(card.monthlyDues, '월 50,000원');
  assert.equal(card.nextDate, '매월 25일');
  assert.equal(card.status, '모집중');
  assert.equal(card.district, '서울');
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
      return { data: { success: true, data: { items: [], totalElements: 7, totalPages: 2 } } };
    };
    assert.equal((await getSavingGroups({ keyword: '산책', regionCode: '01', page: 2 })).totalPages, 2);
    client.get = async () => ({ data: { success: false, error: { message: '요청 오류' } } });
    await assert.rejects(() => getSavingGroups({ page: 1 }), /요청 오류/);
    client.get = async () => { throw new Error('network failure'); };
    await assert.rejects(() => getSavingGroups({ page: 1 }), /network failure/);
  } finally { client.get = original; }
});
