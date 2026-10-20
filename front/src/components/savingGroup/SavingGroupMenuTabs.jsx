import styled from 'styled-components';
import { getSavingGroupMenus } from '../../api/savingGroups.js';

const Menu = styled.nav`
  margin-top: 18px;
  display: flex;
  gap: 5px;
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

const menuItems = [
  { id: 'notice', label: '공지' },
  { id: 'members', label: '계원' },
  { id: 'payment', label: '납부현황' },
  { id: 'calendar', label: '캘린더' },
  { id: 'dues', label: '대시보드' },
  { id: 'transactionHistory', label: '거래 내역' },
  { id: 'payoutRequest', label: '계금 요청' },
  { id: 'settings', label: '설정' },
  { id: 'joinRequests', label: '신청 목록' },
];

// AI작성: 역할에 맞는 탭만 렌더링한다. 실제 업무 API의 권한 검증은 서버에서 별도로 수행한다.
function SavingGroupMenuTabs({ activeMenu, onMenuChange, myRole = null }) {
  return (
    <Menu aria-label="계모임 메뉴">
      {menuItems.filter((item) => getSavingGroupMenus(myRole).includes(item.id)).map((item) => (
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

export default SavingGroupMenuTabs;
