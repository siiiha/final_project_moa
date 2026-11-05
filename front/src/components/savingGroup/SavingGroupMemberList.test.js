import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

// AI작성: 실제 JSX를 SSR로 렌더링해 역할 편집·최대 두 페이지·저장 중 상태·검색 화면을 검증한다.
// 브라우저 클릭 테스트 및 실제 JWT/DB 연동과 구분한다.
test('계원 관리 목록·역할 편집·저장 중·빈 상태 렌더링', async () => {
  const entry = 'virtual:moa-members-render';
  const vite = await createServer({ server: { middlewareMode: true }, ssr: { noExternal: ['styled-components'] },
    plugins: [{ name: 'moa-members-render-test', resolveId: (id) => id === entry ? `\0${entry}` : undefined,
      load: (id) => id === `\0${entry}` ? `
        import { createElement as h } from 'react';
        import { renderToStaticMarkup } from 'react-dom/server';
        import { ThemeProvider } from 'styled-components';
        import theme from '/src/styles/theme.js';
        export { default as List } from '/src/components/savingGroup/SavingGroupMemberList.jsx';
        export { default as Panel } from '/src/components/savingGroup/SavingGroupMembersPanel.jsx';
        export const render = (Component, props) => renderToStaticMarkup(h(ThemeProvider, { theme }, h(Component, props)));
      ` : undefined }] });
  try {
    const { render, List, Panel } = await vite.ssrLoadModule(entry);
    const props = { members: [{ id: '50', name: '테스트계원', email: 't***@example.com', role: 'MANAGER', joinedAt: '2026.11.05.' }],
      currentPage: 1, totalPages: 2, totalCount: 11, isRoleEditing: false, drafts: {}, pendingId: null };
    let html = render(List, props);
    assert.match(html, /테스트계원/); assert.match(html, /t\*\*\*@example.com/); assert.match(html, /회계/);
    assert.match(html, /2026.11.05./); assert.match(html, /계원 11명/);
    assert.equal((html.match(/aria-current="page"/g) || []).length, 1);
    assert.doesNotMatch(html, />3<|value="OWNER"/);
    html = render(List, { ...props, isRoleEditing: true, drafts: { 50: 'MEMBER' } });
    assert.match(html, /value="MEMBER" selected/); assert.match(html, /테스트계원 역할 저장/); assert.match(html, /편집 종료/);
    html = render(List, { ...props, isRoleEditing: true, pendingId: '50', drafts: { 50: 'MEMBER' } });
    assert.match(html, /저장 중…/); assert.match(html, /<select[^>]*disabled/);
    assert.doesNotMatch(html, /<button(?![^>]*disabled)[^>]*>/);
    html = render(List, { ...props, members: [], totalPages: 0, totalCount: 0 });
    assert.match(html, /승인된 계원이 없습니다/); assert.doesNotMatch(html, /계원 목록 페이지 이동/);
    html = render(List, { ...props, members: [], totalPages: 0, totalCount: 0, hasSearch: true });
    assert.match(html, /검색 조건에 맞는 계원이 없습니다/); assert.match(html, /검색 결과 0명/);
    assert.doesNotMatch(html, /승인된 계원이 없습니다/);
    html = render(List, { ...props, hasSearch: true });
    assert.match(html, /검색 결과 11명/); assert.match(html, /t\*\*\*@example.com/);
    // AI작성: 권한 없음·회계 허용·OWNER 제외·편집/요청 중 차단을 렌더링으로 검증한다.
    assert.doesNotMatch(render(List, props), /계원 추방/);
    html = render(List, { ...props, canExpel: true });
    assert.match(html, /테스트계원 계원 추방/);
    assert.doesNotMatch(render(List, { ...props, canExpel: true, members: [{ ...props.members[0], role: 'OWNER' }] }), /계원 추방/);
    html = render(List, { ...props, canExpel: true, expellingId: '50' });
    assert.doesNotMatch(html, /<button(?![^>]*disabled)[^>]*>/);
    html = render(List, { ...props, canExpel: true, isRoleEditing: true });
    assert.match(html, /disabled="" aria-label="테스트계원 계원 추방"/);
    html = render(Panel, { savingGroupId: '10' });
    assert.match(html, /계원 목록을 불러오고 있습니다/); assert.match(html, /비우고 검색하면 전체 계원/);
    assert.match(html, /type="search"[^>]*maxLength="20"/);
    assert.doesNotMatch(html, /type="search"[^>]*disabled|검색 기능은 준비 중|example.com|김영희/);
  } finally { await vite.close(); }
});
