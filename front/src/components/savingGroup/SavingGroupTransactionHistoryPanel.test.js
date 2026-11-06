import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
// AI작성: 실제 JSX를 렌더링해 공개 항목·금액·빈 결과·오류와 페이지 출력을 검증한다.
test('거래내역 화면의 상태·금액·필터·페이지를 렌더링한다', async () => {
  const vite = await createServer({ configFile: false, server: { middlewareMode: true, hmr: false }, appType: 'custom',
    ssr: { noExternal: ['styled-components'] }, oxc: { jsx: { runtime: 'automatic' } } });
  try {
    const { ThemeProvider } = await vite.ssrLoadModule('styled-components');
    const { SavingGroupTransactionHistoryView: View } = await vite.ssrLoadModule('/src/components/savingGroup/SavingGroupTransactionHistoryPanel.jsx');
    const { default: theme } = await vite.ssrLoadModule('/src/styles/theme.js');
    const render = (state, validationMessage = '') => renderToStaticMarkup(React.createElement(ThemeProvider, { theme },
      React.createElement(View, { state, validationMessage,
        draftFilters: { sort: 'OLDEST', transactionType: 'WITHDRAWAL', minAmount: '10000', maxAmount: '40000' },
        onFilterChange() {}, onSearch() {}, onPageChange() {}, onRetry() {} })));
    assert.match(render({ status: 'loading' }), /거래내역을 불러오는 중/);
    const error = render({ status: 'error', message: '계모임 또는 공용 계좌를 찾을 수 없습니다.' });
    assert.match(error, /role="alert"/); assert.match(error, /거래내역 다시 조회/); assert.doesNotMatch(error, /<tbody>/);
    const success = { status: 'success', items: [
      { transactedAt: '2026-11-06T10:00:00', transactionType: 'WITHDRAWAL', amount: 10000, balanceAfter: null },
      { transactedAt: '2026-11-05T09:00:00', transactionType: 'DEPOSIT', amount: 40000, balanceAfter: 50000, depositRequesterName: '김**' },
    ], meta: { page: 2, size: 10, total: 61, totalPages: 7 }, account: { balance: 50000 } };
    const html = render(success);
    assert.match(html, /입금 요청자/); assert.match(html, /김\*\*/); assert.match(html, /—/);
    assert.ok(html.includes('-10,000원')); assert.ok(html.includes('+40,000원')); assert.match(html, /확인 불가/);
    assert.ok(html.includes('현재 내부 장부 잔액: 50,000원')); assert.match(html, /aria-current="page"/);
    assert.ok(html.includes('최소 금액 (원)')); assert.ok(html.includes('최대 금액 (원)'));
    assert.match(html, /과거순/); assert.match(html, /2026-11-06 10:00:00/);
    assert.doesNotMatch(html, /내용<|다운로드|조회 입금|조회 출금/);
    assert.match(render({ ...success, items: [], meta: { page: 1, totalPages: 0 } }), /조회 조건에 맞는 거래내역이 없습니다/);
    assert.match(render(success, '최소 금액은 최대 금액보다 클 수 없습니다.'), /최소 금액은 최대 금액보다 클 수 없습니다/);
    assert.match(render({ ...success, account: null, balanceError: true }), /현재 잔액을 불러오지 못했습니다/);
  } finally { await vite.close(); }
});
