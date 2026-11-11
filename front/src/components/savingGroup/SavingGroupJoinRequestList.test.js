import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

// AI작성: PC 표와 모바일 카드가 공개 합의된 신청자 정보만 렌더링하는지 검증한다.
test('계모임은 이름과 닉네임, 소모임은 닉네임만 PC·모바일에 표시한다', async () => {
  const entry = 'virtual:join-request-display';
  const server = await createServer({
    server: { middlewareMode: true }, ssr: { noExternal: ['styled-components'] },
    plugins: [{ name: 'join-request-display',
      resolveId: id => id === entry ? `\0${entry}` : undefined,
      load: id => id === `\0${entry}` ? `
        import { createElement as h } from 'react';
        import { renderToStaticMarkup } from 'react-dom/server';
        import { ThemeProvider } from 'styled-components';
        import theme from '/src/styles/theme.js';
        export { default as Saving } from '/src/components/savingGroup/SavingGroupJoinRequestList.jsx';
        export { default as Small } from '/src/components/smallGroup/SmallGroupJoinRequestList.jsx';
        export const render = (C, props) => renderToStaticMarkup(h(ThemeProvider, { theme }, h(C, props)));
      ` : undefined }],
  });
  try {
    const { render, Saving, Small } = await server.ssrLoadModule(entry);
    const props = { activeFilter: '전체', selectedIds: [], currentPage: 1, totalPages: 1, totalCount: 1 };
    const request = { id: '1', applicant: '테스트이름', nickname: '테스트닉네임', status: '대기', message: '신청합니다.' };
    const saving = render(Saving, { ...props, requests: [request] });
    assert.equal(saving.match(/테스트닉네임/g)?.length, 2);
    assert.match(saving, /테스트이름/);
    const small = render(Small, { ...props, requests: [{ ...request, applicant: '테스트닉네임' }] });
    assert.equal(small.match(/테스트닉네임/g)?.length, 2);
    assert.doesNotMatch(small, /테스트이름/);
  } finally {
    await server.close();
  }
});
