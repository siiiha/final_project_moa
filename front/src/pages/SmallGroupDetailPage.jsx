import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import SmallGroupDetail from '../components/smallGroup/SmallGroupDetail';
import { defaultSmallGroupDetail, smallGroupDetails, smallGroupInterests, smallGroups } from '../mocks/smallGroups';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 24px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Content = styled.main`
  margin-top: 12px;
`;

const EmptyCard = styled.section`
  padding: 42px 20px;
  text-align: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};

  p {
    margin-bottom: 16px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 13px;
  }
`;

function SmallGroupDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [activeMenu, setActiveMenu] = useState('notice');
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  // TODO: Spring Boot 소모임 상세 조회 API로 mock 데이터를 대체합니다.
  const group = smallGroups.find((item) => item.id === Number(id));
  const detail = smallGroupDetails[id] || defaultSmallGroupDetail;

  const handleSettingsSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 소모임 정보 수정 API에 변경한 폼 값을 전송합니다.
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 소모임 삭제 API에 현재 소모임 ID를 전송하고 목록 화면으로 이동합니다.
    setIsDeleteModalOpen(false);
  };

  if (!group) {
    return (
      <Page>
        <EmptyCard>
          <p>존재하지 않는 소모임입니다.</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/small-groups')}>
            소모임 목록으로
          </Button>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록 &gt; 소모임 메인</Breadcrumb>
      <Content>
        <SmallGroupDetail
          group={group}
          detail={detail}
          interests={smallGroupInterests}
          activeMenu={activeMenu}
          onMenuChange={setActiveMenu}
          onSettingsSubmit={handleSettingsSubmit}
          onSettingsCancel={() => setActiveMenu('notice')}
          onDelete={() => setIsDeleteModalOpen(true)}
        />
      </Content>
      {isDeleteModalOpen && (
        <ConfirmModal
          title="소모임 삭제"
          description={`${group.name} 소모임을 삭제하시겠어요?`}
          confirmLabel="소모임 삭제"
          confirmVariant="danger"
          cancelFirst
          onConfirm={handleDeleteConfirm}
          onCancel={() => setIsDeleteModalOpen(false)}
        />
      )}
    </Page>
  );
}

export default SmallGroupDetailPage;
