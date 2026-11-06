import test from 'node:test';
import assert from 'node:assert/strict';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';

// AI작성: 기존 SSR 방식으로 테스트 안내·마스킹·입력 잠금·추가 확인 화면을 검증한다.
test('입금 화면은 원문 없이 테스트 안내와 중복 확인·불확실 결과 상태를 표시한다', async () => {
  const vite = await createServer({ configFile: false, server: { middlewareMode: true, hmr: false },
    appType: 'custom', ssr: { noExternal: ['styled-components'] }, oxc: { jsx: { runtime: 'automatic' } } });
  try {
    const { ThemeProvider } = await vite.ssrLoadModule('styled-components');
    const { SavingGroupDepositView: View } = await vite.ssrLoadModule('/src/components/savingGroup/SavingGroupDepositForm.jsx');
    const { default: theme } = await vite.ssrLoadModule('/src/styles/theme.js');
    const render = props => renderToStaticMarkup(React.createElement(ThemeProvider, { theme }, React.createElement(View, {
      account: { maskedAccountNumber: '********0000' }, amount: '1000', onSubmit() {}, onAmountChange() {}, ...props,
    })));
    assert.match(render({}), /실제 돈은 이동하지 않습니다/);
    assert.match(render({}), /회비 납부/); assert.match(render({}), /role="dialog"/);
    assert.match(render({}), />입금</); assert.match(render({}), />취소</);
    assert.match(render({}), /1,000/); assert.match(render({}), /\*{8}0000/);
    assert.match(render({ busy: true }), /<input[^>]*disabled/);
    const pending = { amount: 1000, message: '최근 5분 이내 동일한 계좌로 동일한 금액을 보낸 적이 있습니다.' };
    assert.match(render({ pending }), /role="dialog"/);
    assert.match(render({ pending }), /한 번 더 등록/); assert.match(render({ pending }), />취소</);
    const unknown = render({ pending, uncertain: true, error: '응답 유실' });
    assert.match(unknown, /같은 확인 요청 결과 다시 확인/); assert.match(unknown, /응답 유실/);
    assert.doesNotMatch(unknown, />취소</);
    assert.match(render({ message: '테스트 입금이 등록되었습니다.' }), /role="status"/);
    // AI작성: 확인 대기 중 본문 변경·재제출을 막고 처리중에는 확인·취소 버튼도 함께 잠근다.
    const waiting = render({ pending });
    assert.doesNotMatch(waiting, /<input/);
    assert.equal((waiting.match(/role="dialog"/g) || []).length, 1);
    const sending = render({ pending, busy: true });
    assert.match(sending, /<button[^>]*disabled[^>]*>처리중입니다/);
    assert.match(sending, /<button[^>]*disabled[^>]*>취소/);
    const initialUnknown = render({ uncertain: true, error: '결과 확인 필요' });
    assert.match(initialUnknown, /같은 입금 요청 결과 다시 확인/);
    assert.match(initialUnknown, /<input[^>]*disabled/);
    assert.match(initialUnknown, /role="dialog"/);
    assert.match(initialUnknown, /입금 결과 확인 필요/);
    const { SavingGroupDepositResultModal: Result } = await vite.ssrLoadModule('/src/components/savingGroup/SavingGroupDepositForm.jsx');
    const complete = renderToStaticMarkup(React.createElement(ThemeProvider, { theme }, React.createElement(Result, {
      result: { amount: 15000, balance: 20000, message: '테스트 입금이 등록되었습니다.' }, onClose() {},
    })));
    assert.match(complete, /테스트 입금 완료/); assert.match(complete, /15,000원/); assert.match(complete, /20,000원/);
    assert.match(complete, /실제 돈은 이동하지 않았습니다/); assert.match(complete, />확인</);
  } finally { await vite.close(); }
});
