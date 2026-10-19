import { FiBell, FiMapPin, FiShield, FiUser } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';

const Aside = styled.aside`
  padding-right: 18px;
  border-right: 1px solid ${({ theme }) => theme.colors.border};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding-right: 0;
    border-right: 0;
  }
`;

const MenuList = styled.nav`
  display: grid;
  gap: 4px;
`;

const MenuButton = styled.button`
  width: 100%;
  min-height: 42px;
  padding: 9px 11px;
  display: flex;
  align-items: center;
  gap: 9px;
  color: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primarySoft : 'transparent')};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
  font-weight: ${({ $active }) => ($active ? 800 : 600)};
  text-align: left;

  &:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
    color: ${({ theme }) => theme.colors.primary};
  }
`;

const Divider = styled.div`
  height: 1px;
  margin: 18px 0;
  background: ${({ theme }) => theme.colors.border};
`;

const WithdrawButton = styled.button`
  width: 100%;
  min-height: 40px;
  color: ${({ theme }) => theme.colors.white};
  background: ${({ $active, theme }) => ($active ? theme.colors.dangerDark : theme.colors.danger)};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

const iconMap = { overview: FiUser, verify: FiShield, alerts: FiBell, location: FiMapPin };
const menuRoutes = {
  overview: '/member/mypage',
  verify: '/member/verify',
  alerts: '/member/notification',
};

function MemberSidebar({ items, activeItem, onSelect, onWithdraw, isWithdrawActive = false }) {
  const navigate = useNavigate();

  const handleMenuSelect = (item) => {
    const route = menuRoutes[item.id];

    if (!route) {
      return;
    }

    onSelect(item.id);
    navigate(route);
  };

  return (
    <Aside>
      <MenuList aria-label="마이페이지 메뉴">
        {items.map((item) => {
          const Icon = iconMap[item.icon] || FiUser;
          return (
            <MenuButton
              key={item.id}
              type="button"
              $active={activeItem === item.id}
              onClick={() => handleMenuSelect(item)}
            >
              <Icon size={16} aria-hidden="true" />
              {item.label}
            </MenuButton>
          );
        })}
      </MenuList>
      <Divider />
      <WithdrawButton type="button" $active={isWithdrawActive} onClick={onWithdraw}>
        회원탈퇴
      </WithdrawButton>
    </Aside>
  );
}

export default MemberSidebar;
