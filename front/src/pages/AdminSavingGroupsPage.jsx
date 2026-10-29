import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminSidebar from '../components/layout/AdminSidebar';
import SavingGroupFilterBar from '../components/admin/SavingGroupFilterBar';
import SavingGroupTable from '../components/admin/SavingGroupTable';
import { adminMenuItems } from '../mocks/admin';
import ConfirmModal from '../components/common/ConfirmModal';
import {
  getAdminSavingGroups,
  releaseAdminSavingGroupRestriction,
  restrictAdminSavingGroup,
} from '../api/adminSavingGroups';
import { savingGroupRegionOptions } from '../api/savingGroups';

const PAGE_SIZE = 8;

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
  resize: vertical;
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

function AdminSavingGroupsPage() {
  const navigate = useNavigate();
  const [groups, setGroups] = useState([]);
  const [status, setStatus] = useState('전체');
  const [region, setRegion] = useState('전체');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
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
        const statusMap = { 심사중: '00', 모집중: '01', 활동중: '02', 정지: '03', 종료: '04' };
        // AI작성: 필터의 지역명과 API가 요구하는 지역 코드를 공통 지역 목록으로 연결한다.
        const regionCode = savingGroupRegionOptions.find((item) => item.name === region)?.code || '';
        const result = await getAdminSavingGroups({
          keyword: submittedQuery,
          regionCode,
          status: statusMap[status] || '',
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setGroups(result.items);
        setPageMeta(result.meta);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setGroups([]);
          setPageMeta({ total: 0, totalPages: 0 });
          setErrorMessage(error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    loadGroups();
    return () => controller.abort();
  }, [currentPage, region, reloadKey, status, submittedQuery]);

  const activeCount = groups.filter((group) => group.status === '활동중').length;
  const suspendedCount = groups.filter((group) => group.status === '정지').length;

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
      if (actionTarget.adminRestricted) {
        await releaseAdminSavingGroupRestriction(actionTarget.id, reason.trim());
      } else {
        await restrictAdminSavingGroup(actionTarget.id, reason.trim());
      }
      setNotice(actionTarget.adminRestricted ? '계모임 이용 제한을 해제했습니다.' : '계모임 이용을 제한했습니다.');
      setActionTarget(null);
      setReloadKey((current) => current + 1);
    } catch (error) {
      setActionError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  const handleMenuSelect = (menuId) => {
    if (menuId === 'members') {
      navigate('/admin/members');
      return;
    }

    if (menuId === 'gathering-review') {
      navigate('/admin/saving-groups/applications');
      return;
    }

    if (menuId === 'groups') {
      navigate('/admin/small-groups');
      return;
    }

    if (menuId === 'posts') {
      navigate('/admin/posts');
      return;
    }

    if (menuId === 'comments') {
      navigate('/admin/comments');
      return;
    }

    if (menuId === 'reports') {
      navigate('/admin/reposts');
      return;
    }

    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  return (
    <Page>
      <Heading>
        <h1>계모임 관리</h1>
        <p>서비스에서 운영 중인 계모임의 활동 현황과 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gatherings" onSelect={handleMenuSelect} />
        <main>
          <SummaryGrid aria-label="계모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 계모임</SummaryLabel>
              <SummaryValue>{pageMeta.total}개</SummaryValue>
              <SummaryDescription>검색 조건에 해당하는 계모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>활동 중</SummaryLabel>
              <SummaryValue>{activeCount}개</SummaryValue>
              <SummaryDescription>현재 페이지 기준</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>이용 제한</SummaryLabel>
              <SummaryValue $danger>{suspendedCount}개</SummaryValue>
              <SummaryDescription>현재 페이지 기준 관리자 제한</SummaryDescription>
            </SummaryCard>
          </SummaryGrid>
          <SavingGroupTable
            groups={groups}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            onStatusAction={handleStatusAction}
            loading={loading}
            errorMessage={errorMessage}
            submitting={submitting}
            filter={
              <SavingGroupFilterBar
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
          title={actionTarget.adminRestricted ? '계모임 제한 해제' : '계모임 이용 제한'}
          description={`${actionTarget.name}의 처리 사유를 입력해주세요.`}
          confirmLabel={submitting ? '처리 중...' : actionTarget.adminRestricted ? '제한 해제' : '이용 제한'}
          confirmVariant={actionTarget.adminRestricted ? 'primary' : 'danger'}
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
            aria-label="계모임 처리 사유"
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

export default AdminSavingGroupsPage;
