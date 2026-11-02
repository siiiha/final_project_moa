import styled from 'styled-components';

const Menu = styled.nav`
  margin-top: 16px;
  display: flex;
  gap: 4px;
  overflow-x: auto;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
`;

const MenuButton = styled.button`
  min-width: max-content;
  min-height: 34px;
  padding: 7px 13px;
  position: relative;
  color: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primarySoft : 'transparent')};
  border: 0;
  border-radius: ${({ theme }) => `${theme.radius.sm} ${theme.radius.sm} 0 0`};
  font-size: 12px;
  font-weight: ${({ $active }) => ($active ? 800 : 600)};
  white-space: nowrap;

  &::after {
    width: 100%;
    height: 2px;
    position: absolute;
    right: 0;
    bottom: -1px;
    left: 0;
    content: '';
    background: ${({ $active, theme }) => ($active ? theme.colors.primary : 'transparent')};
  }

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  &:focus-visible {
    outline: 3px solid rgba(23, 107, 91, 0.22);
    outline-offset: -3px;
  }
`;

const commonMenuItems = [
  { id: 'notice', label: '공지' },
  { id: 'directions', label: '길찾기' },
  { id: 'poll', label: '투표' },
  { id: 'review', label: '후기' },
];

const ownerMenuItems = [
  { id: 'settings', label: '설정' },
  { id: 'joinRequests', label: '신청 목록' },
];

// AI작성: 비회원도 공지·후기를 조회하며 MANAGER는 기존 접근 차단 정책에 따라 탭을 제공하지 않는다.
export function getSmallGroupMenuItems(myRole) {
  if (myRole === 'MANAGER') return [];
  if (myRole === 'OWNER') return [...commonMenuItems, ...ownerMenuItems];
  return myRole === 'MEMBER' ? commonMenuItems : commonMenuItems.filter((item) => ['notice', 'review'].includes(item.id));
}

function SmallGroupMenuTabs({ myRole, activeMenu, onMenuChange }) {
  const menuItems = getSmallGroupMenuItems(myRole);

  return (
    <Menu aria-label="소모임 메뉴">
      {menuItems.map((item) => (
        <MenuButton
          key={item.id}
          type="button"
          $active={activeMenu === item.id}
          aria-current={activeMenu === item.id ? 'page' : undefined}
          onClick={() => onMenuChange(item.id)}
        >
          {item.label}
        </MenuButton>
      ))}
    </Menu>
  );
}

export default SmallGroupMenuTabs;
