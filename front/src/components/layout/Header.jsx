import { useState } from 'react';
import { FiBell } from 'react-icons/fi';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import { logoutMember } from '../../api/member';
import useAuthStore from '../../store/authStore';

const Shell = styled.header`
  height: 56px;
  background: ${({ theme }) => theme.colors.white};
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
`;
const Inner = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  gap: 42px;
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    gap: 18px;
  }
`;
const Brand = styled(Link)`
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;
const Mark = styled.span`
  position: relative;
  width: 17px;
  height: 20px;
  display: inline-block;
  &::before,
  &::after {
    content: '';
    position: absolute;
    width: 9px;
    height: 14px;
    background: ${({ theme }) => theme.colors.primary};
    border-radius: 9px 1px 9px 1px;
  }
  &::before {
    left: 1px;
    top: 1px;
    transform: rotate(-37deg);
  }
  &::after {
    right: 0;
    bottom: 0;
    opacity: 0.72;
    transform: rotate(32deg) scale(0.73);
  }
`;
const Navigation = styled.nav`
  display: flex;
  align-items: center;
  gap: 34px;
  height: 100%;
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: none;
  }
`;
const NavigationLink = styled(Link)`
  padding: 6px 10px;
  color: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.text)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primarySoft : 'transparent')};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
  font-weight: ${({ $active }) => ($active ? 800 : 600)};

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
  }
`;
const Actions = styled.div`
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;
  > a:not(${Button}) {
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    font-weight: 600;
  }
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    gap: 9px;
    > a:not(${Button}) {
      display: none;
    }
  }
`;
const SignupButton = styled(Button)`
  min-height: 30px;
  padding: 6px 11px;
  font-size: 11px;
`;

const LoggedInActions = styled.div`
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 16px;

  a,
  button {
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    font-weight: 600;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    gap: 9px;
    > a:not(:first-child) {
      display: none;
    }
  }
`;

const NotificationLink = styled(Link)`
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
`;

const NotificationCount = styled.span`
  width: 15px;
  height: 15px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.white};
  background: #d94b45;
  border-radius: 50%;
  font-size: 9px;
`;

const LogoutButton = styled.button`
  padding: 0;
  background: transparent;
  border: 0;
`;

function Header() {
  const { pathname } = useLocation();
  const navigate = useNavigate();

  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const isLoggedIn = Boolean(token);

  // 서버 로그아웃 성공 후 브라우저의 JWT를 제거
  const handleLogout = async () => {
    if (!token || isLoggingOut) return;

    setIsLoggingOut(true);

    try {
      const result = await logoutMember(token);

      if (!result.success) {
        throw new Error('로그아웃에 실패했습니다.');
      }

      clearToken();
      navigate('/member/login', { replace: true });
    } catch (error) {
      // 만료되거나 이미 무효인 JWT도 브라우저에서 제거
      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      window.alert(
        error.response?.data?.error?.message ??
          '로그아웃에 실패했습니다. 잠시 후 다시 시도해주세요.',
      );
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <Shell>
      <Inner>
        <Brand to="/" aria-label="모아 홈">
          <Mark aria-hidden="true" />
          모아
        </Brand>

        <Navigation aria-label="주요 메뉴">
          <NavigationLink
            to="/saving-groups"
            $active={pathname.startsWith('/saving-groups')}
          >
            계모임
          </NavigationLink>
          <NavigationLink
            to="/small-groups"
            $active={pathname.startsWith('/small-groups')}
          >
            소모임
          </NavigationLink>
          <NavigationLink to="/post/list" $active={pathname.startsWith('/post')}>
            게시판
          </NavigationLink>
        </Navigation>

        {isLoggedIn ? (
          <LoggedInActions>
            <NotificationLink to="/member/notification" aria-label="알림">
              <FiBell size={15} aria-hidden="true" /> 알림
            </NotificationLink>
            <Link to="/member/mypage">마이페이지</Link>
            <LogoutButton
              type="button"
              onClick={handleLogout}
              disabled={isLoggingOut}
            >
              {isLoggingOut ? '로그아웃 중…' : '로그아웃'}
            </LogoutButton>
          </LoggedInActions>
        ) : (
          <Actions>
            <Link to="/member/login">로그인</Link>
            <SignupButton
              as={Link}
              to="/member/join"
              $variant="secondary"
              $size="small"
            >
              회원가입
            </SignupButton>
          </Actions>
        )}
      </Inner>
    </Shell>
  );
}

export default Header;
