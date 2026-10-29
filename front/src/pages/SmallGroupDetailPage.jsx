import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import SmallGroupDetail from '../components/smallGroup/SmallGroupDetail';
import {
  fetchSmallGroupCategories,
  getSmallGroupDetail,
  getSmallGroupSettings,
  updateSmallGroupSettings,
} from '../api/smallGroups';

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

// AI작성: 공개 상세 API 응답을 기존 상세 컴포넌트의 표시 필드로만 변환한다.
function toSmallGroupDetailView(data) {
  const memberCount = Number.isFinite(Number(data.memberCount)) ? Number(data.memberCount) : 0;
  return {
    name: data.groupName,
    region: data.regionName,
    interest: data.likeCategory?.trim() || '관심사 미정',
    participants: data.maxMembers == null
      ? `${memberCount}명 / 정원 미정`
      : `${memberCount} / ${data.maxMembers}명`,
    description: data.description?.trim() || '소개글이 없습니다.',
    creatorNickname: data.creatorNickname,
    createdAt: data.createdAt,
    meetDate: data.meetDate,
    location: data.location,
    notices: data.notices,
  };
}

function SmallGroupDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [result, setResult] = useState(null);
  const [retry, setRetry] = useState(0);
  const [menu, setMenu] = useState({ id, value: 'notice' });
  const [settingsResult, setSettingsResult] = useState(null);
  const [settingsRetry, setSettingsRetry] = useState(0);
  const [isSettingsSaving, setIsSettingsSaving] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);

  // AI작성: URL 변경·화면 이탈 시 이전 요청을 취소해 다른 소모임의 결과가 표시되지 않게 한다.
  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    getSmallGroupDetail(id, { signal: controller.signal })
      .then((group) => {
        if (active) setResult({ id, retry, group });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setResult({ id, retry, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [id, retry]);

  const current = result?.id === id && result?.retry === retry ? result : null;
  const isSettingsSelected = current?.group?.myRole === 'OWNER'
    && menu.id === id && menu.value === 'settings';

  // AI작성: OWNER가 설정 탭을 열 때만 설정값과 서버 기준 관심사 목록을 함께 조회한다.
  useEffect(() => {
    if (!isSettingsSelected) return undefined;

    const controller = new AbortController();
    let active = true;
    setSettingsResult({ id, retry: settingsRetry, loading: true });

    Promise.all([
      getSmallGroupSettings(id, { signal: controller.signal }),
      fetchSmallGroupCategories({ signal: controller.signal }),
    ])
      .then(([settings, categories]) => {
        if (active) setSettingsResult({ id, retry: settingsRetry, settings, categories });
      })
      .catch((error) => {
        if (active && !controller.signal.aborted) setSettingsResult({ id, retry: settingsRetry, error });
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [id, isSettingsSelected, settingsRetry]);

  // AI작성: 폼이 비교·검증한 변경 필드만 저장하고 성공 후 공개 상세와 설정값을 모두 최신 상태로 다시 읽는다.
  const handleSettingsSubmit = async (changes) => {
    setIsSettingsSaving(true);
    try {
      await updateSmallGroupSettings(id, changes);
      setRetry((value) => value + 1);
      setSettingsRetry((value) => value + 1);
      // AI작성: 저장 성공한 경우에만 최신 상세 정보를 보여줄 수 있도록 공지 탭으로 돌아간다.
      setMenu({ id, value: 'notice' });
    } finally {
      setIsSettingsSaving(false);
    }
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 소모임 삭제 API에 현재 소모임 ID를 전송하고 목록 화면으로 이동합니다.
    setIsDeleteModalOpen(false);
  };

  const errorCode = current?.error?.code;
  const errorMessage = errorCode === 'NOT_FOUND' ? '조회할 수 있는 소모임이 없습니다.'
    : errorCode === 'ACCESS_DENIED' ? '이 소모임에 접근할 수 없습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(errorCode) ? '로그인이 필요합니다.'
        : current?.error?.message || '소모임 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
  const group = current?.group ? toSmallGroupDetailView(current.group) : null;
  const currentSettings = settingsResult?.id === id && settingsResult?.retry === settingsRetry
    ? settingsResult : null;
  const settingsErrorCode = currentSettings?.error?.code;
  const settingsErrorMessage = settingsErrorCode === 'SMALL_GROUP_SETTINGS_FORBIDDEN'
    ? '소모임 개설자만 설정을 조회할 수 있습니다.'
    : settingsErrorCode === 'SMALL_GROUP_NOT_FOUND'
      ? '조회할 수 있는 소모임이 없습니다.'
      : ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(settingsErrorCode)
        ? '로그인이 필요합니다.'
        : currentSettings?.error?.message || '소모임 설정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록 &gt; 소모임 메인</Breadcrumb>
      <Content>
        {!current ? <EmptyCard role="status">소모임 정보를 불러오는 중입니다.</EmptyCard>
          : current.error ? (
            <EmptyCard role="alert">
              <p>{errorMessage}</p>
              {!['NOT_FOUND', 'ACCESS_DENIED', 'UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(errorCode) && (
                <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 시도</Button>
              )}
              <Button type="button" $variant="secondary" onClick={() => navigate('/small-groups')}>
                소모임 목록으로
              </Button>
            </EmptyCard>
          ) : (
            <SmallGroupDetail
              key={id}
              group={group}
              myRole={current.group.myRole}
              activeMenu={menu.id === id ? menu.value : 'notice'}
              settings={currentSettings?.settings}
              settingsCategories={currentSettings?.categories || []}
              isSettingsLoading={isSettingsSelected && (!currentSettings || currentSettings.loading)}
              isSettingsSaving={isSettingsSaving}
              settingsErrorMessage={currentSettings?.error ? settingsErrorMessage : null}
              onMenuChange={(value) => setMenu({ id, value })}
              onSettingsSubmit={handleSettingsSubmit}
              onSettingsCancel={() => setMenu({ id, value: 'notice' })}
              onRetrySettings={() => setSettingsRetry((value) => value + 1)}
              onDelete={() => setIsDeleteModalOpen(true)}
            />
          )}
      </Content>
      {isDeleteModalOpen && group && (
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
