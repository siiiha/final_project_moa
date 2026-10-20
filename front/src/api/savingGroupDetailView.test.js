import assert from 'node:assert/strict';
import { test } from 'node:test';
import { createServer } from 'vite';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { ThemeProvider } from 'styled-components';
import theme from '../styles/theme.js';

// AI작성: 추가 패키지 없이 실제 상세 JSX를 렌더링하여 공개 화면과 역할별 메뉴를 검증한다.
test('실제 상세 화면은 API 제목만 표시하고 역할별 탭과 고정 가입 버튼을 렌더링한다', async () => {
  const server = await createServer({ configFile: false, server: { middlewareMode: true, watch: null },
    appType: 'custom', optimizeDeps: { noDiscovery: true },
    // AI작성: Node SSR의 CJS 기본 내보내기 차이만 보정하고 같은 styled 구현을 사용한다.
    plugins: [{ name: 'node-styled-interop', enforce: 'pre', transform(code, id) {
      if (id.includes('/src/') && id.endsWith('.jsx')) {
        return code.replace("import styled from 'styled-components'", "import { styled } from 'styled-components'")
          .replace(/import styled,\s*\{([^}]*)\} from 'styled-components'/g, "import { styled, $1 } from 'styled-components'");
      }
    } }],
    ssr: { external: ['react', 'react-dom', 'styled-components'] } });
  try {
    const { default: Detail } = await server.ssrLoadModule('/src/components/savingGroup/SavingGroupDetail.jsx');
    const group = { groupId: 42, groupName: '공개 모임', description: null, regionName: '서울',
      memberCount: 1, maxMembers: 8, duesAmount: null, duesDay: 31, status: '01', notices: [], myRole: null };
    const render = (value, activeMenu = 'notice') => renderToStaticMarkup(createElement(ThemeProvider,
      { theme }, createElement(Detail, { group: value, activeMenu, onMenuChange() {}, onJoinRequest() {}, onBack() {} })));
    const publicHtml = render(group);
    assert.match(publicHtml, /등록된 공지가 없습니다/);
    assert.match(publicHtml, /회비 미정/);
    assert.match(publicHtml, /납부일 확인 필요/);
    assert.match(publicHtml, /가입 신청하기/);
    assert.doesNotMatch(publicHtml, />납부현황</);
    assert.doesNotMatch(publicHtml, />설정</);
    assert.match(render(group, 'settings'), /등록된 공지가 없습니다/);
    for (const role of ['OWNER', 'MANAGER', 'MEMBER']) {
      const html = render({ ...group, myRole: role, duesAmount: 100000,
        notices: [{ noticeId: 1, title: '제목만 공개', content: '본문은 노출 금지' }] });
      assert.match(html, /제목만 공개/);
      assert.doesNotMatch(html, /본문은 노출 금지/);
      assert.match(html, /100,000원/);
      assert.match(html, />납부현황</);
      assert.match(html, /가입 신청하기/);
      assert.equal(/>신청 목록</.test(html), role === 'OWNER');
      assert.equal(/>설정</.test(html), role === 'OWNER');
      assert.equal(/>계원</.test(html), role === 'OWNER');
    }
  } finally { await server.close(); }
});
