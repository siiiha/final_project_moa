import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import SavingGroupSettingsForm from '../components/savingGroup/SavingGroupSettingsForm';
import { savingGroups } from '../mocks/savingGroups';

const Page = styled.div`
  width: 100%;
  max-width: 1040px;
  margin: 0 auto;
  padding: 40px 24px 64px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 28px 16px 48px;
  }
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
`;

const Heading = styled.div`
  margin-top: 10px;
`;

const Title = styled.h1`
  color: ${({ theme }) => theme.colors.text};
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -1.3px;
`;

const Subtitle = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 14px;
`;

const Content = styled.div`
  margin-top: 28px;
`;

const EmptyCard = styled.section`
  padding: 44px 24px;
  text-align: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const EmptyTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
`;

const EmptyText = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
`;

const BackButton = styled(Button)`
  margin-top: 20px;
`;

function SavingGroupSettingsPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const group = savingGroups.find((item) => item.id === Number(id));

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 서버에 계모임 설정 수정 API 요청을 연결합니다.
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 서버에 계모임 삭제 요청 API를 연결합니다.
    setIsDeleteModalOpen(false);
  };

  if (!group) {
    return (
      <Page>
        <EmptyCard>
          <EmptyTitle>계모임 정보를 찾을 수 없습니다.</EmptyTitle>
          <EmptyText>계모임 목록에서 다시 선택해주세요.</EmptyText>
          <BackButton type="button" onClick={() => navigate('/saving-groups')}>
            계모임 목록으로 이동
          </BackButton>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Breadcrumb>계모임 · {group.name} · 설정</Breadcrumb>
      <Heading>
        <Title>계모임 설정</Title>
        <Subtitle>계모임의 기본 정보와 회비 납부 기준을 관리하세요.</Subtitle>
      </Heading>
      <Content>
        <SavingGroupSettingsForm
          group={group}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/saving-groups/${group.id}`)}
          onDeleteRequest={() => setIsDeleteModalOpen(true)}
        />
      </Content>
      {isDeleteModalOpen && (
        <ConfirmModal
          title="계모임 삭제 요청"
          description={`${group.name}의 삭제를 요청하시겠습니까? 요청 내용은 운영진 확인 후 처리됩니다.`}
          confirmLabel="삭제 요청"
          confirmVariant="danger"
          onConfirm={handleDeleteConfirm}
          onCancel={() => setIsDeleteModalOpen(false)}
        />
      )}
    </Page>
  );
}

export default SavingGroupSettingsPage;
