import { Outlet } from 'react-router-dom';
import styled from 'styled-components';
import Header from './Header';
import useAuthStore from '../../store/authStore';

const Main = styled.main`
  min-height: calc(100vh - 56px);
`;

// 헤더는 로그인 상태를 직접 확인하고, 본문에 현재 페이지를 표시
function PageLayout() {

  return (
    <>
      <Header />
      <Main>
        <Outlet />
      </Main>
    </>
  );
}
export default PageLayout;
