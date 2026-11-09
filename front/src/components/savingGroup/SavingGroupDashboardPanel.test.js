import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

// AI작성: 대시보드 표시와 전체 페이지 미납 합산을 실제 모듈에서 검증한다.
test('회비 대시보드는 조회 상태와 계주 전용 미납 합계를 구분한다', async () => {
  const vite = await createServer({
    configFile: false, server: { middlewareMode: true, hmr: false }, appType: 'custom',
    ssr: { noExternal: ['styled-components'] }, oxc: { jsx: { runtime: 'automatic' } },
  });
  try {
    const { ThemeProvider } = await vite.ssrLoadModule('styled-components');
    const { default: theme } = await vite.ssrLoadModule('/src/styles/theme.js');
    const { SavingGroupDashboardView: View, loadOutstanding } = await vite.ssrLoadModule(
      '/src/components/savingGroup/SavingGroupDashboardPanel.jsx',
    );
    const render = (props) => renderToStaticMarkup(React.createElement(ThemeProvider, { theme },
      React.createElement(View, props)));
    assert.match(render({}), /role="status"/);
    assert.match(render({ balance: { value: null } }), /거래내역이 없습니다/);
    const failure = render({ balance: { failed: true } });
    assert.match(failure, /role="alert"/);
    assert.doesNotMatch(failure, />0원</);
    assert.match(render({ balance: { value: 0 } }), />0원</);
    assert.doesNotMatch(render({ outstanding: { value: 31000 } }), /미납|31,000원/);
    assert.match(render({ canViewOutstanding: true, outstanding: { value: null } }), /회비 기준 확인/);

    const signal = new AbortController().signal;
    const total = await loadOutstanding(7, '2026-11', signal, async (groupId, options) => {
      assert.equal(groupId, 7);
      assert.equal(options.month, '2026-11');
      assert.equal(options.signal, signal);
      return {
        items: options.page === 1
          ? [{ duesAmount: 50000, depositedAmount: 20000, remainingAmount: 30000 }]
          : [{ remainingAmount: 1000 }],
        meta: { totalPages: 2 },
      };
    });
    assert.equal(total, 31000);
    assert.match(render({ canViewOutstanding: true, outstanding: { value: total } }), /31,000원/);
    assert.equal(await loadOutstanding(7, '2026-11', signal, async () => ({
      items: [{ remainingAmount: null }], meta: { totalPages: 1 },
    })), null);

    const controller = new AbortController();
    await assert.rejects(loadOutstanding(7, '2026-11', controller.signal, async () => {
      controller.abort();
      return { items: [], meta: { totalPages: 2 } };
    }), { name: 'AbortError' });
  } finally {
    await vite.close();
  }
});
