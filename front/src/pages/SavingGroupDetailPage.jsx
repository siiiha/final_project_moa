import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupDetail from '../components/savingGroup/SavingGroupDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import { getSavingGroupDetail } from '../api/savingGroups.js';

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

// AI작성: URL 변경 시 이전 데이터를 숨기고 취소된 요청이 새 화면을 덮어쓰지 못하게 한다.
function SavingGroupDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [menu, setMenu] = useState({ id, value: 'notice' });
  const [joinModalId, setJoinModalId] = useState(null);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;
    getSavingGroupDetail(id, { signal: controller.signal })
      .then((group) => {
        if (active) setResult({ id, retry, group });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ id, retry, error });
      });
    return () => { active = false; controller.abort(); };
  }, [id, retry]);

  const current = result?.id === id && result?.retry === retry ? result : null;
  const errorStatus = current?.error?.response?.status;
  const errorMessage = errorStatus === 404 ? '조회할 수 있는 계모임이 없습니다.'
    : errorStatus === 403 ? '이 계모임에 접근할 수 없습니다.'
      : errorStatus === 401 ? '인증 정보를 확인해 주세요.'
        : errorStatus === 400 ? '계모임 주소가 올바르지 않습니다.'
          : '계모임 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';

  return (
    <Page>
      <Breadcrumb>계모임 &gt; 계모임 목록 &gt; 계모임 메인</Breadcrumb>
      <Content>
        {!current ? <EmptyCard role="status">계모임 정보를 불러오는 중입니다.</EmptyCard>
          : current.error ? (
            <EmptyCard role="alert">
              <p>{errorMessage}</p>
              {(!errorStatus || errorStatus >= 500) && (
                <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
              )}
              <Button type="button" $variant="secondary" onClick={() => navigate('/saving-groups')}>계모임 목록으로</Button>
            </EmptyCard>
          ) : (
            <SavingGroupDetail key={id} group={current.group}
              activeMenu={menu.id === id ? menu.value : 'notice'}
              onMenuChange={(value) => setMenu({ id, value })}
              onJoinRequest={() => setJoinModalId(id)}
              onBack={() => navigate('/saving-groups')} />
          )}
      </Content>
      {joinModalId === id && current?.group && (
        <ConfirmModal title="가입 신청 안내"
          description="가입 신청 기능은 준비 중입니다."
          confirmLabel="확인" showCancel={false}
          onConfirm={() => setJoinModalId(null)}
          onCancel={() => setJoinModalId(null)} />
      )}
    </Page>
  );
}

export default SavingGroupDetailPage;