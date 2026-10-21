import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import SavingGroupSettingsForm from '../components/savingGroup/SavingGroupSettingsForm';
import { getSavingGroupSettings, updateSavingGroupSettings } from '../api/savingGroups';

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

const RetryButton = styled(Button)`
  margin-top: 12px;
`;

function SavingGroupSettingsPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [saveErrorMessage, setSaveErrorMessage] = useState('');
  const [retry, setRetry] = useState(0);
  const requestKey = `${id}:${retry}`;
  const [result, setResult] = useState({ requestKey: null, settings: null, errorMessage: '' });
  const isLoading = result.requestKey !== requestKey;
  const { settings, errorMessage } = result;

  useEffect(() => {
    const controller = new AbortController();
    let isActive = true;

    getSavingGroupSettings(id, { signal: controller.signal })
      .then((data) => {
        if (isActive) setResult({ requestKey, settings: data, errorMessage: '' });
      })
      .catch((error) => {
        if (isActive && error.code !== 'ERR_CANCELED') {
          setResult({ requestKey, settings: null, errorMessage: error.message });
        }
      });

    return () => {
      isActive = false;
      controller.abort();
    };
  }, [id, requestKey]);

  const handleSubmit = async (changedFields) => {
    if (isSaving) return;

    setIsSaving(true);
    setSaveErrorMessage('');
    try {
      await updateSavingGroupSettings(id, changedFields);
      navigate(`/saving-groups/${id}`);
    } catch (error) {
      setSaveErrorMessage(error.message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 서버에 계모임 삭제 요청 API를 연결합니다.
    setIsDeleteModalOpen(false);
  };

  if (isLoading) {
    return (
      <Page>
        <EmptyCard role="status">계모임 설정 정보를 불러오는 중입니다.</EmptyCard>
      </Page>
    );
  }

  if (errorMessage) {
    return (
      <Page>
        <EmptyCard role="alert">
          <EmptyTitle>계모임 설정을 열 수 없습니다.</EmptyTitle>
          <EmptyText>{errorMessage}</EmptyText>
          <RetryButton type="button" onClick={() => setRetry((value) => value + 1)}>
            다시 시도
          </RetryButton>
          <BackButton type="button" $variant="secondary" onClick={() => navigate('/saving-groups')}>
            계모임 목록으로 이동
          </BackButton>
        </EmptyCard>
      </Page>
    );
  }

  if (!settings?.canAccessSettings || settings.current == null) {
    return (
      <Page>
        <EmptyCard>
          <EmptyTitle>표시할 계모임 설정이 없습니다.</EmptyTitle>
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
      <Breadcrumb>계모임 · {settings.current.groupName} · 설정</Breadcrumb>
      <Heading>
        <Title>계모임 설정</Title>
        <Subtitle>계모임의 기본 정보와 회비 납부 기준을 관리하세요.</Subtitle>
      </Heading>
      <Content>
        <SavingGroupSettingsForm
          key={JSON.stringify(settings.current)}
          current={settings.current}
          pending={settings.pending}
          canEditSettings={settings.canEditSettings}
          status={settings.status}
          isSaving={isSaving}
          submitErrorMessage={saveErrorMessage}
          onSubmit={handleSubmit}
          onCancel={() => navigate(`/saving-groups/${settings.id}`)}
          onDeleteRequest={() => setIsDeleteModalOpen(true)}
        />
      </Content>
      {isDeleteModalOpen && (
        <ConfirmModal
          title="계모임 삭제 요청"
          description={`${settings.current.groupName}의 삭제를 요청하시겠습니까? 요청 내용은 운영진 확인 후 처리됩니다.`}
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
