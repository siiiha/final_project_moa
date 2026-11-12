import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import AdminDashboard from '../components/admin/AdminDashboard';
import AdminFilterBar from '../components/admin/AdminFilterBar';
import AdminMemberTable from '../components/admin/AdminMemberTable';
import AdminPageLayout from '../components/layout/AdminPageLayout';
import { getAdminMembers } from '../api/adminMembers';
import { getAdminDashboardSummary } from '../api/adminDashboard';
import { getMemberRegions } from '../api/member';

const PAGE_SIZE = 10;

function AdminMembersPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams, setSearchParams] = useSearchParams();
  // AI작성: 상세 화면 복귀와 새로고침 뒤에도 목록 검색 조건을 URL 쿼리에서 복원한다.
  const initialPage = Number(searchParams.get('page'));
  const [status, setStatus] = useState(searchParams.get('status') || '전체');
  const [regions, setRegions] = useState([]);
  const [province, setProvince] = useState(searchParams.get('province') || '');
  const [district, setDistrict] = useState(searchParams.get('district') || '');
  const [town, setTown] = useState(searchParams.get('town') || '');
  const [restrictionScope, setRestrictionScope] = useState(searchParams.get('restrictionScope') || '');
  const [query, setQuery] = useState(searchParams.get('keyword') || '');
  const [submittedQuery, setSubmittedQuery] = useState(searchParams.get('keyword') || '');
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [currentPage, setCurrentPage] = useState(Number.isInteger(initialPage) && initialPage > 0 ? initialPage : 1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [dashboardStats, setDashboardStats] = useState([]);

  useEffect(() => {
    // AI작성: 개발 모드 StrictMode의 첫 정리 실행에서는 지역 조회 요청 자체가 발생하지 않도록 한 틱 뒤 호출한다.
    const requestTimer = window.setTimeout(() => {
      getMemberRegions().then((items) => {
        if (Array.isArray(items)) setRegions(items);
      }).catch(() => setRegions([]));
    }, 0);

    return () => window.clearTimeout(requestTimer);
  }, []);

  useEffect(() => {
    // AI작성: 목록 상태를 주소에 반영해 상세 화면에서 돌아온 뒤에도 동일한 검색 결과를 보여준다.
    const nextSearchParams = new URLSearchParams();
    if (status !== '전체') nextSearchParams.set('status', status);
    if (province) nextSearchParams.set('province', province);
    if (district) nextSearchParams.set('district', district);
    if (town) nextSearchParams.set('town', town);
    if (restrictionScope) nextSearchParams.set('restrictionScope', restrictionScope);
    if (submittedQuery) nextSearchParams.set('keyword', submittedQuery);
    if (currentPage > 1) nextSearchParams.set('page', String(currentPage));

    if (nextSearchParams.toString() !== searchParams.toString()) {
      setSearchParams(nextSearchParams, { replace: true });
    }
  }, [currentPage, district, province, restrictionScope, searchParams, setSearchParams, status, submittedQuery, town]);

  useEffect(() => {
    let controller;
    // AI작성: StrictMode의 확인용 첫 실행이 정리되면 타이머도 취소해 중복 통계 조회를 막는다.
    const requestTimer = window.setTimeout(() => {
      controller = new AbortController();
      getAdminDashboardSummary({ signal: controller.signal }).then((summary) => {
        // AI작성: API 집계값을 기존 대시보드 카드 표시 형식으로 변환한다.
        setDashboardStats([
          { id: 'total', label: '전체 회원', value: `${summary.totalMembers.toLocaleString()}명`, description: '탈퇴하지 않은 회원 기준', tone: 'primary' },
          { id: 'active', label: '활성 회원', value: `${summary.activeMembers.toLocaleString()}명`, description: '현재 ACTIVE 계정 기준', tone: 'success' },
          { id: 'new', label: '신규 가입', value: `${summary.newMembersThisMonth.toLocaleString()}명`, description: '이번 달 가입 회원', tone: 'neutral' },
          { id: 'suspended', label: '관리 필요 회원', value: `${summary.managedMembers.toLocaleString()}명`, description: '휴면 및 제재 계정 기준', tone: 'danger' },
        ]);
      }).catch((error) => {
        if (error.code !== 'ERR_CANCELED') setDashboardStats([]);
      });
    }, 0);

    return () => {
      window.clearTimeout(requestTimer);
      controller?.abort();
    };
  }, []);

  useEffect(() => {
    let controller;
    async function loadMembers() {
      setLoading(true);
      setErrorMessage('');
      try {
        // AI작성: 계정 이용 정지와 ACTIVE 계정의 기능 제재를 서로 다른 검색 조건으로 전달한다.
        const statusConditionMap = {
          정상: { accountStatus: 'ACTIVE', hasActiveRestriction: false },
          제재: { accountStatus: 'ACTIVE', hasActiveRestriction: true },
          '이용 정지': { accountStatus: 'SUSPENDED' },
          탈퇴: { accountStatus: 'INACTIVE' },
        };
        const statusCondition = statusConditionMap[status] || {};
        const result = await getAdminMembers({
          keyword: submittedQuery,
          provinceName: province,
          districtName: district,
          townName: town,
          accountStatus: statusCondition.accountStatus || '',
          hasActiveRestriction: statusCondition.hasActiveRestriction,
          restrictionScope,
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setMembers(result.items);
        setPageMeta(result.meta);
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setMembers([]);
          setErrorMessage(error.message);
          setPageMeta({ total: 0, totalPages: 0 });
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    // AI작성: StrictMode의 확인용 첫 실행이 정리되면 타이머도 취소해 중복 목록 조회를 막는다.
    const requestTimer = window.setTimeout(() => {
      controller = new AbortController();
      loadMembers();
    }, 0);

    return () => {
      window.clearTimeout(requestTimer);
      controller?.abort();
    };
  }, [currentPage, district, province, restrictionScope, status, submittedQuery, town]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleViewMember = (memberId) => {
    // AI작성: 상세 화면이 목록 조건을 알 수 있도록 현재 목록 주소를 복귀 경로로 전달한다.
    navigate(`/admin/members/${memberId}`, { state: { returnTo: `${location.pathname}${location.search}` } });
  };

  return (
    <AdminPageLayout
      title="관리자 대시보드"
      description="서비스 회원 현황을 확인하고 회원 정보를 관리하세요."
      activeItem="members"
    >
          <AdminDashboard stats={dashboardStats} />
          <AdminMemberTable
            members={members}
            loading={loading}
            errorMessage={errorMessage}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            filter={
              <AdminFilterBar
                status={status}
                province={province}
                district={district}
                town={town}
                regions={regions}
                restrictionScope={restrictionScope}
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
                onRestrictionScopeChange={(value) => {
                  setRestrictionScope(value);
                  setCurrentPage(1);
                }}
                onQueryChange={setQuery}
                onSearch={handleSearch}
              />
            }
            onViewMember={handleViewMember}
          />
    </AdminPageLayout>
  );
}

export default AdminMembersPage;
