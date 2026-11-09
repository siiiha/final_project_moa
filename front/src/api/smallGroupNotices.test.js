import assert from 'node:assert/strict';
import test from 'node:test';
import client from './client.js';
import { createSmallGroupNotice, updateSmallGroupNotice, deleteSmallGroupNotice, getSmallGroupNoticeDetail,
  updateSmallGroupNoticePinned, getSmallGroupDetail, toSmallGroupDetail, readSmallGroupNoticeQuery,
  getSmallGroupNoticePagination } from './smallGroups.js';

// AI작성: 서버 계약·권한 오류·고정 목록 본문 제외·페이지 복원 경계를 확인한다.
test('작성·수정은 제목·본문만 보내고 삭제는 두 식별자만 전달한다', async () => {
  const original = { post: client.post, put: client.put, delete: client.delete };
  const calls = [];
  try {
    client.post = async (...args) => { calls.push(args); return { data: { success: true, data: null } }; };
    client.put = async (...args) => { calls.push(args); return { data: { success: true, data: 9 } }; };
    client.delete = async (...args) => { calls.push(args); return { data: { success: true, data: null } }; };
    await createSmallGroupNotice(10, { title: '제목', content: '본문', writerId: 999 });
    assert.equal(await updateSmallGroupNotice(10, 9, { title: '수정', content: '본문', writerId: 999 }), 9);
    await deleteSmallGroupNotice(10, 9);
    assert.equal(calls[0][0], '/small-groups/10/notices');
    assert.deepEqual(calls[0][1], { title: '제목', content: '본문' });
    assert.equal(calls[1][0], '/small-groups/10/notices/9');
    assert.deepEqual(calls[1][1], { title: '수정', content: '본문' });
    assert.equal(calls[2][0], '/small-groups/10/notices/9');
  } finally { Object.assign(client, original); }
});

test('고정 개수 제한과 삭제 권한 오류의 상태·코드를 보존한다', async () => {
  const original = { patch: client.patch, delete: client.delete };
  try {
    client.patch = async () => ({ status: 409, data: { success: false, error: { code: 'SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED', message: '최대 2개' } } });
    await assert.rejects(updateSmallGroupNoticePinned(10, 9, true), (error) => error.status === 409 && error.code === 'SMALL_GROUP_NOTICE_PIN_LIMIT_EXCEEDED');
    client.delete = async () => { throw { response: { status: 403, data: { error: { code: 'SMALL_GROUP_NOTICE_AUTHOR_FORBIDDEN', message: '작성자만 삭제 가능' } } } }; };
    await assert.rejects(deleteSmallGroupNotice(10, 9), (error) => error.status === 403 && error.code === 'SMALL_GROUP_NOTICE_AUTHOR_FORBIDDEN');
  } finally { Object.assign(client, original); }
});

test('상세 조회는 취소 신호를 전달하고 잘못된 본문 응답을 거절한다', async () => {
  const original = client.get; const controller = new AbortController();
  try {
    client.get = async (path, options) => {
      assert.equal(path, '/small-groups/10/notices/9'); assert.equal(options.signal, controller.signal);
      return { data: { success: true, data: { noticeId: 9, title: '제목' } } };
    };
    await assert.rejects(getSmallGroupNoticeDetail(10, 9, { signal: controller.signal }), (error) => error.code === 'INVALID_RESPONSE');
  } finally { client.get = original; }
});

test('일반·고정 목록은 본문을 제외하고 정렬 조건과 페이지 메타를 유지한다', async () => {
  const original = client.get;
  const notice = { noticeId: 9, title: '제목', writerNickname: '모임장', createdAt: '2026-11-04T12:00:00', content: '비공개 본문' };
  try {
    client.get = async (path, options) => {
      assert.deepEqual(options.params, { noticePage: 4, noticeSort: 'OLDEST' });
      return { data: { success: true, data: { notices: [], pinnedNotices: [notice] }, meta: { page: 4, totalPages: 5 } } };
    };
    const result = await getSmallGroupDetail(10, { noticePage: 4, noticeSort: 'OLDEST' });
    assert.equal(result.noticePageMeta.page, 4); assert.equal(result.pinnedNotices[0].content, undefined);
    assert.deepEqual(toSmallGroupDetail({ notices: [], pinnedNotices: [] }).pinnedNotices, []);
  } finally { client.get = original; }
});

test('공지 페이지 URL과 세 페이지 묶음의 경계를 보정한다', () => {
  assert.deepEqual(readSmallGroupNoticeQuery('?noticePage=2147483648&noticeSort=bad'), { page: 1, sort: 'LATEST' });
  assert.deepEqual(readSmallGroupNoticeQuery('?noticePage=4&noticeSort=OLDEST'), { page: 4, sort: 'OLDEST' });
  assert.deepEqual(getSmallGroupNoticePagination(4, 7), { pages: [4, 5, 6], previous: 3, next: 7 });
});
