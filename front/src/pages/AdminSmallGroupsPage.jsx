import { useEffect, useState } from 'react';
import styled from 'styled-components';
import SmallGroupFilterBar from '../components/admin/SmallGroupFilterBar';
import SmallGroupTable from '../components/admin/SmallGroupTable';
import AdminPageLayout from '../components/layout/AdminPageLayout';
import client from '../api/client';
import { formatAdminDate } from '../api/adminFormatters';
import { getMemberRegions } from '../api/member';
import ConfirmModal from '../components/common/ConfirmModal';

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.article`
  /* AI작성: 안내 문구 제거 후 남는 세로 여백을 줄인다. */
  min-height: 104px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  /* AI작성: 요약 항목명을 더 쉽게 확인할 수 있도록 키운다. */
  font-size: 15px;
  font-weight: 700;
`;

const SummaryValue = styled.strong`
  display: block;
  margin-top: 6px;
  color: ${({ $danger, theme }) => ($danger ? theme.colors.danger : theme.colors.text)};
  /* AI작성: 핵심 집계 수치를 강조한다. */
  font-size: 29px;
  letter-spacing: -1px;
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
  const [regions, setRegions] = useState([]);
  const [province, setProvince] = useState('');
  const [district, setDistrict] = useState('');
  const [town, setTown] = useState('');
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [summary, setSummary] = useState({ totalSmallGroups: 0, activeSmallGroups: 0, restrictedSmallGroups: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [actionTarget, setActionTarget] = useState(null);
  const [reason, setReason] = useState('');
  const [actionError, setActionError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [notice, setNotice] = useState('');

  // AI작성: 소모임 제한·해제 성공 안내는 확인 버튼을 누르지 않아도 3초 후 자동으로 닫는다.
  useEffect(() => {
    if (!notice) return undefined;
    const timer = window.setTimeout(() => setNotice(''), 3000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    getMemberRegions().then((items) => {
      if (Array.isArray(items)) setRegions(items);
    }).catch(() => setRegions([]));
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    client.get('/admin/small-groups/summary', { signal: controller.signal }).then((response) => {
      if (!response.data?.success || !response.data.data) {
        throw new Error('소모임 현황 응답을 확인할 수 없습니다.');
      }
      setSummary(response.data.data);
    }).catch((error) => {
      if (error.code !== 'ERR_CANCELED') {
        setSummary({ totalSmallGroups: 0, activeSmallGroups: 0, restrictedSmallGroups: 0 });
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
        const response = await client.get('/admin/small-groups', {
          params: {
            keyword: submittedQuery || undefined,
            provinceName: province || undefined,
            districtName: district || undefined,
            townName: town || undefined,
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
          // AI작성: 소모임 생성 시각을 관리자 목록의 개설일 표시 형식으로 변환한다.
          openedAt: formatAdminDate(group.createdAt, { fallback: '-', compact: false }),
          status: group.status === 'ACTIVE' ? '활동중' : group.status === 'RESTRICTED' ? '정지' : '-',
          statusCode: group.status,
        })));
        setTotalPages(response.data.meta.totalPages || 0);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setGroups([]);
          setTotalPages(0);
          setErrorMessage(error.message || '소모임 목록을 불러오지 못했습니다.');
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadGroups();
    return () => controller.abort();
  }, [currentPage, district, province, reloadKey, submittedQuery, town]);

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
    <>
      <AdminPageLayout
        title="소모임 관리"
        description="서비스에서 운영 중인 소모임의 활동 현황과 이용 상태를 관리하세요."
        activeItem="groups"
      >
          <SummaryGrid aria-label="소모임 현황 요약">
            <SummaryCard>
              <SummaryLabel>전체 소모임</SummaryLabel>
              <SummaryValue>{summary.totalSmallGroups.toLocaleString()}개</SummaryValue>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>활동 중</SummaryLabel>
              <SummaryValue>{summary.activeSmallGroups.toLocaleString()}개</SummaryValue>
            </SummaryCard>
            <SummaryCard>
              <SummaryLabel>이용 제한</SummaryLabel>
              <SummaryValue $danger>{summary.restrictedSmallGroups.toLocaleString()}개</SummaryValue>
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
      </AdminPageLayout>
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
    </>
  );
}

export default AdminSmallGroupsPage;
