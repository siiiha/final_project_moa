import { Outlet, useLocation } from 'react-router-dom';
import styled from 'styled-components';
import Header from './Header';

const Main = styled.main`
  min-height: calc(100vh - 56px);
`;
function PageLayout() {
  const { pathname } = useLocation();
  const isLoggedIn =
    pathname.startsWith('/member/mypage') ||
    pathname === '/member/verify' ||
    pathname === '/member/notification' ||
    pathname.startsWith('/saving-groups') ||
    pathname.startsWith('/small-groups') ||
    pathname.startsWith('/post') ||
    pathname.startsWith('/admin');

  return (
    <>
      <Header isLoggedIn={isLoggedIn} />
      <Main>
        <Outlet />
      </Main>
    </>
  );
}
export default PageLayout;
