import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

// AI작성: 실제 JSX를 SSR 렌더링해 결과·상태·권한별 진입 버튼을 검증한다. 브라우저 클릭 검증과 구분한다.
test('추천 패널 상태·금액·권한별 진입점을 표시하고 저장·적용 버튼은 제공하지 않는다', async () => {
  const entry = 'virtual:turn-order-render';
  const vite = await createServer({ server: { middlewareMode: true }, ssr: { noExternal: ['styled-components'] },
    plugins: [{ name: 'turn-order-render', resolveId: (id) => id === entry ? `\0${entry}` : undefined,
      load: (id) => id === `\0${entry}` ? `
        import { createElement as h } from 'react';
        import { renderToStaticMarkup } from 'react-dom/server';
        import { ThemeProvider } from 'styled-components';
        import { MemoryRouter } from 'react-router-dom';
        import Detail from '/src/components/savingGroup/SavingGroupDetail.jsx';
        import theme from '/src/styles/theme.js';
        export { TurnOrderRecommendationView as View } from '/src/components/savingGroup/SavingGroupTurnOrderRecommendationPanel.jsx';
        export { default as Members } from '/src/components/savingGroup/SavingGroupMembersPanel.jsx';
        export const render = (C, props) => renderToStaticMarkup(h(ThemeProvider, { theme }, h(C, props)));
        // AI작성: 실제 상세 화면에서 유형이 계원 패널까지 전달되는지도 검증한다.
        export const renderDetail = (group) => renderToStaticMarkup(h(MemoryRouter, null,
          h(ThemeProvider, { theme }, h(Detail, { group, activeMenu: 'members', noticePagination: { page: 1, sort: 'LATEST' } }))));
      ` : undefined }] });
  try {
    const { render, renderDetail, View, Members } = await vite.ssrLoadModule(entry);
    assert.match(render(View, {}), /추천받기/);
    let html = render(View, { phase: 'loading' });
    assert.match(html, /추천 중…/); assert.match(html, /<button[^>]*disabled/);
    html = render(View, { phase: 'success', items: [{ memberId: '50', nickname: '테스트계원', recommendedTurnOrder: 1, expectedPayoutAmount: 3297 }] });
    assert.match(html, /테스트계원/); assert.match(html, /1번/); assert.match(html, /3,297원/);
    assert.match(html, /다시 추천받기/); assert.doesNotMatch(html, /<button[^>]*>[^<]*(저장|적용|확정)/);
    assert.match(render(View, { phase: 'success', items: [] }), /추천할 승인 모임원이 없습니다/);
    html = render(View, { phase: 'error', error: '권한 없음', items: [{ memberId: '50', nickname: '이전결과' }] });
    assert.match(html, /role="alert"/); assert.match(html, /권한 없음/); assert.doesNotMatch(html, /이전결과/);
    const props = { savingGroupId: '10', groupId: '20', myRole: 'OWNER', membershipStatus: 'APPROVED', status: '01', groupType: '순번계' };
    assert.match(render(Members, props), />순번 추천<\/button>/);
    const group = { ...props, groupName: '테스트 모임', memberCount: 3, notices: [] };
    assert.match(renderDetail(group), />순번 추천<\/button>/);
    assert.doesNotMatch(renderDetail({ ...group, groupType: '친목계' }), />순번 추천<\/button>/);
    for (const change of [{ myRole: 'MANAGER' }, { myRole: 'MEMBER' }, { membershipStatus: 'LEFT' },
      { membershipStatus: 'PENDING' }, { status: '03' }, { groupType: '친목계' }]) {
      assert.doesNotMatch(render(Members, { ...props, ...change }), />순번 추천<\/button>/);
    }
  } finally { await vite.close(); }
});
