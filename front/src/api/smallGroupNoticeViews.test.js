import assert from 'node:assert/strict';
import test, { after } from 'node:test';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import theme from '../styles/theme.js';

// AI작성: 실제 JSX를 Vite SSR로 읽어 권한별 조작 노출과 일반 텍스트 렌더링을 검증한다.
const server = await createServer({ configFile: false, plugins: [react()],
  ssr: { noExternal: ['styled-components'] }, server: { middlewareMode: true }, appType: 'custom', optimizeDeps: { noDiscovery: true, include: [] } });
after(() => server.close());
const { ThemeProvider } = await server.ssrLoadModule('styled-components');
const { default: Panel } = await server.ssrLoadModule('/src/components/smallGroup/SmallGroupNoticesPanel.jsx');
const { default: Detail } = await server.ssrLoadModule('/src/components/smallGroup/SmallGroupNoticeDetail.jsx');
const notice = { noticeId: 9, title: '모임 안내', writerNickname: '모임장', createdAt: '2026-11-04T12:00:00' };
function renderPanel(role, status) {
  return renderToStaticMarkup(createElement(ThemeProvider, { theme }, createElement(Panel, {
    group: { smallGroupId: 10, myRole: role, membershipStatus: status, notices: [notice], pinnedNotices: [{ ...notice, noticeId: 8, title: '고정 안내' }] },
    heading: createElement('h2', null, '공지'), pageMeta: { page: 1, totalPages: 2 }, sort: 'LATEST',
    onRefresh: async () => {}, onQueryChange() {}, onDetail() {},
  })));
}
test('소모임장에게 작성·핀 버튼과 고정·일반 공지 영역을 제공한다', () => {
  const html = renderPanel('OWNER', 'APPROVED');
  assert.match(html, /공지 작성/); assert.match(html, /고정 공지 목록/); assert.match(html, /일반 공지 목록/);
  assert.match(html, /aria-label="고정 안내 고정 해제"/); assert.match(html, /aria-label="모임 안내 고정"/);
});
test('승인 모임원은 상세 제목 버튼만 사용하며 작성·핀 버튼은 보이지 않는다', () => {
  const html = renderPanel('MEMBER', 'APPROVED');
  assert.doesNotMatch(html, /공지 작성/); assert.doesNotMatch(html, /aria-pressed="false" aria-label="모임 안내 고정"/);
  assert.doesNotMatch(html, /aria-label="모임 안내 고정"/); assert.match(html, /<button[^>]*>모임 안내<\/button>/);
});
test('비회원에게 제목 요약만 표시하고 본문은 HTML로 해석하지 않는다', () => {
  const html = renderPanel(null, null);
  assert.doesNotMatch(html, /공지 작성/); assert.doesNotMatch(html, /<button[^>]*>모임 안내<\/button>/);
  const body = renderToStaticMarkup(createElement(ThemeProvider, { theme }, createElement(Detail, {
    notice: { ...notice, content: '<script>alert(1)</script>\n모임 안내' },
  })));
  assert.doesNotMatch(body, /<script>/); assert.match(body, /&lt;script&gt;/);
});
