import { Outlet, useLocation } from 'react-router-dom';
import styled from 'styled-components';
import Header from './Header';

const Main = styled.main`
  min-height: ${({ $isSocialFlow }) =>
    $isSocialFlow ? '100vh' : 'calc(100vh - 56px)'};
`;

function PageLayout() {
  const { pathname, search } = useLocation();
  const searchParams = new URLSearchParams(search);

  // AI작성: 소셜 인증 결과·추가 가입 화면에서는 상단 메뉴를 숨긴다.
  const isSocialFlow =
    pathname === '/member/social/callback' ||
    (pathname === '/member/join' &&
      searchParams.get('social') === '1');

  return (
    <>
      {!isSocialFlow && <Header />}
      <Main $isSocialFlow={isSocialFlow}>
        <Outlet />
      </Main>
    </>
  );
}

export default PageLayout;