import { FiClipboard, FiFileText, FiFlag, FiGrid, FiHeart, FiMessageSquare, FiUsers } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { adminRoutes } from '../../adminRoutes';

const Aside = styled.aside`
  padding-right: 18px;
  border-right: 1px solid ${({ theme }) => theme.colors.border};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 0 0 14px;
    border-right: 0;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  }
`;

const MenuList = styled.nav`
  display: grid;
  gap: 4px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
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
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-height: 38px;
    padding: 7px 8px;
    gap: 5px;
    font-size: 11px;
  }
`;

const iconMap = {
  dashboard: FiGrid,
  members: FiUsers,
  gatherings: FiHeart,
  review: FiClipboard,
  groups: FiUsers,
  posts: FiFileText,
  comments: FiMessageSquare,
  reports: FiFlag,
};

function AdminSidebar({ items, activeItem }) {
  const navigate = useNavigate();
  return (
    <Aside>
      <MenuList aria-label="관리자 메뉴">
        {items.map((item) => {
          const Icon = iconMap[item.icon] || FiGrid;

          return (
            <MenuButton
              key={item.id}
              type="button"
              $active={activeItem === item.id}
              aria-current={activeItem === item.id ? 'page' : undefined}
              onClick={() => adminRoutes[item.id] && navigate(adminRoutes[item.id])}
            >
              <Icon size={16} aria-hidden="true" />
              {item.label}
            </MenuButton>
          );
        })}
      </MenuList>
    </Aside>
  );
}

export default AdminSidebar;
