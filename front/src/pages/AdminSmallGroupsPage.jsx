import { useEffect, useState } from 'react';
import styled from 'styled-components';
import SmallGroupFilterBar from '../components/admin/SmallGroupFilterBar';
import SmallGroupTable from '../components/admin/SmallGroupTable';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import client from '../api/client';
import { smallGroupRegionOptions } from '../api/smallGroups';
import ConfirmModal from '../components/common/ConfirmModal';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;

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
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.article`
  min-height: 108px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
`;

const SummaryValue = styled.strong`
  display: block;
  margin-top: 13px;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.text)};
  font-size: 24px;
  letter-spacing: -1px;
`;

const SummaryDescription = styled.p`
  margin-top: 2px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

const ReasonInput = styled.textarea`
  width: 100%;
  min-height: 110px;
  padding: 11px;
  resize: none;
  color: ${({ theme }) => theme.colors.text};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font: inherit;
`;

const ErrorMessage = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.danger};
  font-size: 12px;
`;

function AdminSmallGroupsPage() {
  const [groups, setGroups] = useState([]);
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalCount, setTotalCount] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [actionTarget, setActionTarget] = useState(null);
  const [reason, setReason] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [notice, setNotice] = useState('');

  useEffect(() => {
    const controller = new AbortController();

    async function loadGroups() {
      setLoading(true);
      setErrorMessage('');
      try {
        const regionCode = smallGroupRegionOptions.find((item) => item.name === region)?.code;
        const response = await client.get('/admin/small-groups', {
          params: {
            keyword: submittedQuery || undefined,
            regionCode,
            page: currentPage,
          },
          signal: controller.signal,
        });

        if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
          throw new Error('소모임 목록 응답을 확인할 수 없습니다.');
        }

        // AI작성: 관리자 전용 목록 응답의 모임장·운영 상태를 화면 표시값으로 변환한다.
        setGroups(response.data.data.map((group) => ({
          id: group.smallGroupId,
          name: group.groupName,
          category: group.likeCategory || '-',
          leader: group.creatorNickname || '-',
          region: group.regionName,
          participants: group.memberCount ?? 0,
          status: group.status === 'ACTIVE' ? '활동중' : group.status === 'RESTRICTED' ? '정지' : '-',
          statusCode: group.status,
        })));
        setTotalCount(response.data.meta.total || 0);
        setTotalPages(response.data.meta.totalPages || 0);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setGroups([]);
          setTotalCount(0);
          setTotalPages(0);
          setErrorMessage(error.message || '소모임 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadGroups();
    return () => controller.abort();
  }, [currentPage, region, reloadKey, submittedQuery]);

  const activeCount = groups.filter((group) => group.statusCode === 'ACTIVE').length;
  const suspendedCount = groups.filter((group) => group.statusCode === 'RESTRICTED').length;

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleStatusAction = (group) => {
    setReason('');
    setActionError('');
    setActionTarget(group);
  };

  const handleActionConfirm = async () => {
    if (!actionTarget || submitting) return;
    if (!reason.trim()) {
      setActionError('처리 사유를 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setActionError('');
    try {
      const actionPath = actionTarget.statusCode === 'RESTRICTED'
        ? 'restriction/release'
        : 'restriction';
      const response = await client.patch(
        `/admin/small-groups/${encodeURIComponent(actionTarget.id)}/${actionPath}`,
        { reason: reason.trim() },
      );
      if (!response.data?.success) {
        throw new Error('소모임 이용 상태 변경 응답을 확인할 수 없습니다.');
      }
      setNotice(actionTarget.statusCode === 'RESTRICTED'
        ? '소모임 이용 제한을 해제했습니다.'
        : '소모임 이용을 제한했습니다.');
      setActionTarget(null);
      setReloadKey((current) => current + 1);
    } catch (error) {
      setActionError(error.response?.data?.error?.message || error.message || '소모임 이용 상태를 변경하지 못했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Page>
      <Heading>
        <h1>소모임 관리</h1>
        <p>서비스에서 운영 중인 소모임의 활동 현황과 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="groups" />
        <main>
          <SummaryGrid aria-label="소모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 소모임</SummaryLabel>
              <SummaryValue>{totalCount}개</SummaryValue>
              <SummaryDescription>검색 조건에 해당하는 소모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>활동 중</SummaryLabel>
              <SummaryValue>{activeCount}개</SummaryValue>
              <SummaryDescription>정상 운영 중인 소모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>이용 제한</SummaryLabel>
              <SummaryValue $danger>{suspendedCount}개</SummaryValue>
              <SummaryDescription>현재 정지된 소모임</SummaryDescription>
            </SummaryCard>
          </SummaryGrid>
          <SmallGroupTable
            groups={groups}
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
            onStatusAction={handleStatusAction}
            loading={loading}
            errorMessage={errorMessage}
            submitting={submitting}
            filter={
              <SmallGroupFilterBar
                status={status}
                region={region}
                query={query}
                onStatusChange={(value) => {
                  setStatus(value);
                  setCurrentPage(1);
                }}
                onRegionChange={(value) => {
                  setRegion(value);
                  setCurrentPage(1);
                }}
                onQueryChange={setQuery}
                onSearch={handleSearch}
              />
            }
          />
        </main>
      </Grid>
      {actionTarget && (
        <ConfirmModal
          title={actionTarget.statusCode === 'RESTRICTED' ? '소모임 제한 해제' : '소모임 이용 제한'}
          description={`${actionTarget.name}의 처리 사유를 입력해주세요.`}
          confirmLabel={submitting ? '처리 중...' : actionTarget.statusCode === 'RESTRICTED' ? '제한 해제' : '이용 제한'}
          confirmVariant={actionTarget.statusCode === 'RESTRICTED' ? 'primary' : 'danger'}
          cancelFirst
          isBusy={submitting}
          onConfirm={handleActionConfirm}
          onCancel={() => !submitting && setActionTarget(null)}
        >
          <ReasonInput
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            maxLength={500}
            disabled={submitting}
            placeholder="처리 사유를 입력해주세요"
            aria-label="소모임 처리 사유"
          />
          {actionError && <ErrorMessage role="alert">{actionError}</ErrorMessage>}
        </ConfirmModal>
      )}
      {notice && (
        <ConfirmModal
          title="처리 완료"
          description={notice}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setNotice('')}
          onCancel={() => setNotice('')}
        />
      )}
    </Page>
  );
}

export default AdminSmallGroupsPage;
