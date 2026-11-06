import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

// AI작성: 실제 지정 화면을 SSR로 렌더링하여 지급 완료 제외·회차 입력·적용월·저장 제한을 검증한다.
test('순번 지정 화면은 다음 달 적용과 지급 완료 제외 및 입력 상태를 표시한다', async () => {
  const entry = 'virtual:assignment-render';
  const vite = await createServer({ server: { middlewareMode: true }, ssr: { noExternal: ['styled-components'] },
    plugins: [{ name: 'assignment-render', resolveId: id => id === entry ? `\0${entry}` : undefined,
      load: id => id === `\0${entry}` ? `
        import { createElement as h } from 'react';
        import { renderToStaticMarkup } from 'react-dom/server';
        import { ThemeProvider } from 'styled-components';
        import theme from '/src/styles/theme.js';
        export { TurnOrderAssignmentView as View } from '/src/components/savingGroup/SavingGroupTurnOrderAssignmentPanel.jsx';
        export { default as Members } from '/src/components/savingGroup/SavingGroupMembersPanel.jsx';
        export const render = (C, props) => renderToStaticMarkup(h(ThemeProvider, { theme }, h(C, props)));
      ` : undefined }] });
  try {
    const { render, View, Members } = await vite.ssrLoadModule(entry);
    const data = { effectiveMonth: '2026-12-01', startRoundNumber: 3, scheduled: true, needsReassignment: true,
      members: [{ memberId: '30', nickname: '테스트계주', role: 'OWNER', paid: false, currentSequenceNumber: 2 },
        { memberId: '40', nickname: '완료회원', role: 'MEMBER', paid: true, currentSequenceNumber: 1 }] };
    let html = render(View, { data, drafts: { 30: '3' } });
    assert.match(html, /2026년 12월부터 적용/); assert.match(html, /이번 달 순번은 유지/);
    assert.match(html, /지급 완료 · 재배정 제외/); assert.equal((html.match(/<select/g) || []).length, 1);
    assert.match(html, /3회차/); assert.match(html, /모임원 구성이 변경/);
    assert.doesNotMatch(html, /<button[^>]*disabled[^>]*>다음 달 순번 저장/);
    html = render(View, { data, drafts: {} }); assert.match(html, /<button[^>]*disabled[^>]*>다음 달 순번 저장/);
    html = render(View, { data, drafts: { 30: '3' }, saving: true }); assert.match(html, /저장 중…/); assert.match(html, /<select[^>]*disabled/);
    assert.match(render(View, { loading: true }), /불러오고 있습니다/);
    assert.match(render(View, { error: '조회 실패' }), /role="alert"/);
    assert.match(render(View, { data: { ...data, members: [] } }), /미지급 승인 회원이 없습니다/);
    const props = { savingGroupId: '10', groupId: '20', groupType: '순번계', status: '02', myRole: 'OWNER', membershipStatus: 'APPROVED' };
    assert.match(render(Members, props), />순번 지정<\/button>/);
    for (const change of [{ myRole: 'MANAGER' }, { myRole: 'MEMBER' }, { membershipStatus: 'LEFT' }, { groupType: '친목계' }])
      assert.doesNotMatch(render(Members, { ...props, ...change }), />순번 지정<\/button>/);
  } finally { await vite.close(); }
});
