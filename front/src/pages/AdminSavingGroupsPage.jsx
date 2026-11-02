import { useEffect, useState } from 'react';
import styled from 'styled-components';
import AdminSidebar from '../components/layout/AdminSidebar';
import SavingGroupFilterBar from '../components/admin/SavingGroupFilterBar';
import SavingGroupTable from '../components/admin/SavingGroupTable';
import { adminMenuItems } from '../mocks/admin';
import ConfirmModal from '../components/common/ConfirmModal';
import {
  getAdminSavingGroups,
  getAdminSavingGroupSummary,
  releaseAdminSavingGroupRestriction,
  restrictAdminSavingGroup,
} from '../api/adminSavingGroups';
import { getMemberRegions } from '../api/member';

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

function AdminSavingGroupsPage() {
  const [groups, setGroups] = useState([]);
  const [status, setStatus] = useState('전체');
  const [regions, setRegions] = useState([]);
  const [province, setProvince] = useState('');
  const [district, setDistrict] = useState('');
  const [town, setTown] = useState('');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [summary, setSummary] = useState({ totalSavingGroups: 0, activeSavingGroups: 0, restrictedSavingGroups: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [actionTarget, setActionTarget] = useState(null);
  const [reason, setReason] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [notice, setNotice] = useState('');

  useEffect(() => {
    getMemberRegions().then((items) => {
      if (Array.isArray(items)) setRegions(items);
    }).catch(() => setRegions([]));
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    getAdminSavingGroupSummary({ signal: controller.signal }).then(setSummary).catch((error) => {
      if (error.code !== 'ERR_CANCELED') {
        setSummary({ totalSavingGroups: 0, activeSavingGroups: 0, restrictedSavingGroups: 0 });
      }
    });
    return () => controller.abort();
  }, [reloadKey]);

  useEffect(() => {
    const controller = new AbortController();
    async function loadGroups() {
      setLoading(true);
      setErrorMessage('');
      try {
        const statusMap = { 심사중: '00', 모집중: '01', 활동중: '02', 정지: '03', 종료: '04' };
        const result = await getAdminSavingGroups({
          keyword: submittedQuery,
          provinceName: province,
          districtName: district,
          townName: town,
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
  }, [currentPage, district, province, reloadKey, status, submittedQuery, town]);

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

  return (
    <Page>
      <Heading>
        <h1>계모임 관리</h1>
        <p>서비스에서 운영 중인 계모임의 활동 현황과 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="gatherings" />
        <main>
          <SummaryGrid aria-label="계모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 계모임</SummaryLabel>
              <SummaryValue>{summary.totalSavingGroups.toLocaleString()}개</SummaryValue>
              <SummaryDescription>서비스에 등록된 계모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>활동 중</SummaryLabel>
              <SummaryValue>{summary.activeSavingGroups.toLocaleString()}개</SummaryValue>
              <SummaryDescription>정상 운영 중인 계모임</SummaryDescription>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>이용 제한</SummaryLabel>
              <SummaryValue $danger>{summary.restrictedSavingGroups.toLocaleString()}개</SummaryValue>
              <SummaryDescription>현재 관리자 제한 중인 계모임</SummaryDescription>
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
                province={province}
                district={district}
                town={town}
                regions={regions}
                query={query}
                onStatusChange={(value) => {
                  setStatus(value);
                  setCurrentPage(1);
                }}
                onProvinceChange={(value) => {
                  setProvince(value);
                  setDistrict('');
                  setTown('');
                  setCurrentPage(1);
                }}
                onDistrictChange={(value) => {
                  setDistrict(value);
                  setTown('');
                  setCurrentPage(1);
                }}
                onTownChange={(value) => {
                  setTown(value);
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
