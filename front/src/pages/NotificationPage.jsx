import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import NotificationList from '../components/member/NotificationList';
import { memberMenuItems, notificationItems } from '../mocks/mypage';

const NOTIFICATIONS_PER_PAGE = 5;

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 216px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

function NotificationPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('alerts');
  const [notifications, setNotifications] = useState(notificationItems);
  const [expandedIds, setExpandedIds] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);

  const totalPages = Math.max(Math.ceil(notifications.length / NOTIFICATIONS_PER_PAGE), 1);
  const visibleNotifications = notifications.slice(
    (currentPage - 1) * NOTIFICATIONS_PER_PAGE,
    currentPage * NOTIFICATIONS_PER_PAGE
  );

  const handleToggle = (notificationId) => {
    setExpandedIds((ids) =>
      ids.includes(notificationId) ? ids.filter((id) => id !== notificationId) : [...ids, notificationId]
    );
  };

  const handleDelete = (notificationId) => {
    // TODO: Spring Boot 알림 삭제 API 요청이 성공한 뒤 목록 상태를 갱신합니다.
    const remainingItems = notifications.filter((item) => item.id !== notificationId);
    const remainingPages = Math.max(Math.ceil(remainingItems.length / NOTIFICATIONS_PER_PAGE), 1);

    setNotifications(remainingItems);
    setCurrentPage((page) => Math.min(page, remainingPages));
    setExpandedIds((ids) => ids.filter((id) => id !== notificationId));
  };

  return (
    <Page>
      <Heading>
        <h1>알림 목록</h1>
        <p>모임 소식과 내 활동의 새로운 소식을 확인하세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          <NotificationList
            notifications={visibleNotifications}
            expandedIds={expandedIds}
            currentPage={currentPage}
            totalPages={totalPages}
            onToggle={handleToggle}
            onDelete={handleDelete}
            onPageChange={setCurrentPage}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default NotificationPage;
