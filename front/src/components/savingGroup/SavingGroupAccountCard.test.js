import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

// AI작성: 브라우저 자동화 없이 실제 JSX 컴포넌트를 로드해 상태별 출력과 마스킹·안내를 검증한다.
test('계좌 카드는 정상·미발급·오류·로딩을 구분해 렌더링한다', async () => {
  const vite = await createServer({ configFile: false, server: { middlewareMode: true, hmr: false },
    appType: 'custom', ssr: { noExternal: ['styled-components'] }, oxc: { jsx: { runtime: 'automatic' } } });
  try {
    const { ThemeProvider } = await vite.ssrLoadModule('styled-components');
    const { default: Card } = await vite.ssrLoadModule('/src/components/savingGroup/SavingGroupAccountCard.jsx');
    const { default: theme } = await vite.ssrLoadModule('/src/styles/theme.js');
    const render = (state, props = {}) => renderToStaticMarkup(React.createElement(ThemeProvider, { theme },
      React.createElement(Card, { state, onRetry() {}, ...props })));
    const normal = render({ status: 'success', data: { issued: true, bankCode: '20',
      maskedAccountNumber: '********0000', balance: 1234, testAccount: true } });
    assert.match(normal, /우리은행/);
    assert.match(normal, /\*{8}0000/);
    assert.match(normal, /1,234원/);
    assert.match(normal, /서비스 내부 장부 잔액/);
    assert.match(normal, /실제 입금할 수 없습니다/);
    assert.doesNotMatch(normal, /X000000000000/);
    assert.match(render({ status: 'success', data: { issued: false } }), /아직 발급된 테스트 계좌가 없습니다/);
    const error = render({ status: 'error', message: '테스트 조회 실패' });
    assert.match(error, /role="alert"/); assert.match(error, /계좌 정보 다시 조회/);
    assert.match(render(null), /계좌 정보를 불러오는 중입니다/);
    // AI작성: 입금 화면은 발급된 조회 성공 계좌에서만 제공하고 성공 안내는 재조회 오류에도 보존한다.
    const child = React.createElement('button', null, '테스트 입금 등록');
    assert.match(render({ status: 'success', data: { issued: true, bankCode: '20', maskedAccountNumber: '********0000', balance: 0 } }, { children: child }), /테스트 입금 등록/);
    assert.doesNotMatch(render({ status: 'success', data: { issued: false } }, { children: child }), /테스트 입금 등록/);
    assert.doesNotMatch(render({ status: 'error', message: '잔액 조회 실패' }, { children: child }), /테스트 입금 등록/);
    assert.match(render({ status: 'error', message: '잔액 조회 실패' }, { depositMessage: '테스트 입금이 등록되었습니다.' }), /테스트 입금이 등록되었습니다/);
    // AI작성: 발급·조회 성공·입금 권한이 모두 있는 경우에만 회비 납부 버튼을 제공한다.
    const issued = { status: 'success', data: { issued: true, bankCode: '20', maskedAccountNumber: '********0000', balance: 0 } };
    assert.match(render(issued, { canDeposit: true, onDeposit() {} }), />회비 납부</);
    assert.doesNotMatch(render(issued, { canDeposit: false }), />회비 납부</);
    assert.doesNotMatch(render({ status: 'success', data: { issued: false } }, { canDeposit: true }), />회비 납부</);
  } finally { await vite.close(); }
});
