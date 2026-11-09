import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

// AI작성: 실제 화면의 조회 상태와 서버 페이지 및 금액 표시를 확인한다.
test('거래내역 화면은 성공·빈 목록·조회 오류를 구분한다', async () => {
  const vite = await createServer({
    configFile: false, server: { middlewareMode: true, hmr: false }, appType: 'custom',
    ssr: { noExternal: ['styled-components'] }, oxc: { jsx: { runtime: 'automatic' } },
  });
  try {
    const { ThemeProvider } = await vite.ssrLoadModule('styled-components');
    const { default: theme } = await vite.ssrLoadModule('/src/styles/theme.js');
    const { SavingGroupTransactionHistoryView: View } = await vite.ssrLoadModule(
      '/src/components/savingGroup/SavingGroupTransactionHistoryPanel.jsx',
    );
    const render = (props) => renderToStaticMarkup(React.createElement(ThemeProvider, { theme },
      React.createElement(View, { page: 2, ...props })));
    assert.match(render({}), /role="status"/);
    assert.match(render({ failed: true }), /role="alert"/);
    assert.match(render({ result: { items: [], meta: { totalPages: 0 } } }), /아직 거래내역이 없어요/);
    const html = render({ result: {
      items: [{ transactedAt: '2026-11-08T12:00:00', transactionType: 'WITHDRAWAL', amount: 30000, balanceAfter: 0 }],
      meta: { totalPages: 2 },
    } });
    assert.match(html, /-30,000원/);
    assert.match(html, />0원</);
    assert.match(html, /2 \/ 2/);
  } finally {
    await vite.close();
  }
});
