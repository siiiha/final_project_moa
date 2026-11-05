import assert from 'node:assert/strict';
import test from 'node:test';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { createServer } from 'vite';
import theme from '../../styles/theme.js';

// AI작성: 기존 Vite·React SSR로 화면을 렌더링하며 추가 테스트 패키지나 실제 금융 연결을 사용하지 않는다.
test('유형별 공개 필드와 로딩·실패·빈 상태를 표시하며 처리 버튼은 제공하지 않는다', async () => {
  const server = await createServer({ server: { middlewareMode: true }, appType: 'custom', ssr: { noExternal: ['styled-components'] } });
  try {
    const { ThemeProvider } = await server.ssrLoadModule('styled-components');
    const { SavingGroupPayoutRequestContent: Content } = await server.ssrLoadModule('/src/components/savingGroup/SavingGroupPayoutApprovalList.jsx');
    const render = props => renderToStaticMarkup(React.createElement(ThemeProvider, { theme }, React.createElement(Content, {
      onStatusChange() {}, onPageChange() {}, onRetry() {}, ...props,
    })));
    assert.match(render({ isLoading: true }), /불러오는 중/);
    assert.match(render({ errorMessage: '조회 권한이 없습니다.' }), /다시 시도/);
    assert.doesNotMatch(render({ errorMessage: '실패', items: [{ id: 'stale', applicant: '이전 결과' }] }), /이전 결과/);
    assert.match(render({ page: 2 }), /첫 페이지로/);
    const social = render({ groupType: '친목계', items: [{ id: '1', applicant: '테스트회원', type: '지출', amount: '1,000원', status: '대기', title: '다과', hasEvidence: false }], meta: { total: 1, totalPages: 1 } });
    assert.match(social, /테스트회원/); assert.match(social, /첨부 없음/); assert.match(social, /다과/);
    assert.doesNotMatch(social, /COMPLETED|검토자|내역 확인·처리|증빙 원본/);
    const rotation = render({ groupType: '순번계', items: [{ id: '2', applicant: '테스트회원', type: '계금', amount: '1,000원', status: '승인', roundNumber: 3, reviewedAt: '2026-11-05T10:00:00' }], page: 2, meta: { total: 12, totalPages: 2 } });
    assert.match(rotation, /3회차/); assert.match(rotation, /이름 비공개/); assert.match(rotation, /2026-11-05 10:00/);
    assert.match(rotation, /COMPLETED/); assert.match(rotation, /aria-current="page"/);
    assert.doesNotMatch(rotation, /지출 제목|내역 확인·처리/);
  } finally { await server.close(); }
});
