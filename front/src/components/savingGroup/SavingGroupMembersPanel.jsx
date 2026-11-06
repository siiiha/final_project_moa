import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import TextInput from '../common/TextInput';
import { expelSavingGroupMember, getSavingGroupMembers, updateSavingGroupMemberRole } from '../../api/savingGroups';
import SavingGroupMemberList from './SavingGroupMemberList';
// AI작성: 추천 패널은 계원 관리 내부에서만 열며 별도 탭·저장·적용 동작은 추가하지 않는다.
import SavingGroupTurnOrderRecommendationPanel from './SavingGroupTurnOrderRecommendationPanel';

const Panel = styled.section`
  display: grid;
  gap: 16px;
`;

const Intro = styled.div`
  h2 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 20px;
    font-weight: 800;
    letter-spacing: -0.9px;
  }

  p {
    margin-top: 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
  }
`;

const SearchForm = styled.form`
  display: grid;
  grid-template-columns: minmax(0, 1fr);
`;

const SearchButton = styled(Button)`
  min-width: 100px;
  min-height: 42px;
  padding: 8px 17px;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-width: 76px;
    padding: 8px 12px;
  }
`;

// AI작성: 입력 중 검색어와 적용한 검색어를 분리하고 현재 모임·검색·페이지의 서버 결과만 표시한다.
function SavingGroupMembersPanel({ savingGroupId, groupId, myRole, membershipStatus, status, groupType }) {
  // AI작성: 서버 검증과 별도로 승인 계주·순번계·운영 상태에서만 추천 진입점을 노출한다.
  const [recommendationOpen, setRecommendationOpen] = useState(false);
  const canRecommend = groupType === '순번계' && myRole === 'OWNER'
    && membershipStatus === 'APPROVED' && ['01', '02'].includes(status);
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isRoleEditing, setIsRoleEditing] = useState(false);
  const [result, setResult] = useState(null);
  const [refresh, setRefresh] = useState(0);
  const [drafts, setDrafts] = useState({});
  const [pendingId, setPendingId] = useState(null);
  // AI작성: 추방과 역할 저장은 같은 요청 잠금을 공유하고 오류는 확인창에 유지한다.
  const [expellingId, setExpellingId] = useState(null);
  const [expulsionError, setExpulsionError] = useState('');
  const canExpel = myRole === 'OWNER' && membershipStatus === 'APPROVED' && status === '01';
  const [notice, setNotice] = useState('');
  const active = useRef(false);
  const submitting = useRef(false);
  const queryKey = JSON.stringify([savingGroupId, submittedKeyword, currentPage, refresh]);
  const currentResult = result?.key === queryKey ? result : null;
  const loading = !currentResult;
  const members = currentResult?.items || [];
  const totalPages = currentResult?.meta?.totalPages || 0;
  const error = currentResult?.error || '';
  const blocked = loading || Boolean(error) || pendingId !== null || expellingId !== null;

  useEffect(() => {
    active.current = true;
    return () => { active.current = false; };
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    getSavingGroupMembers(savingGroupId, { keyword: submittedKeyword, page: currentPage, signal: controller.signal })
      .then((data) => {
        if (controller.signal.aborted) return;
        setResult({ key: queryKey, ...data });
        // AI작성: 외부 탈퇴 등으로 마지막 페이지가 사라지면 남은 페이지를 다시 조회한다.
        if (currentPage > Math.max(1, data.meta.totalPages)) setCurrentPage(Math.max(1, data.meta.totalPages));
      })
      .catch((failure) => {
        if (!controller.signal.aborted) setResult({ key: queryKey, items: [], error: failure.message });
      });
    return () => controller.abort();
  }, [savingGroupId, submittedKeyword, currentPage, queryKey]);

  // AI작성: 검색 버튼·Enter에서만 적용하고 검색 전 페이지와 미저장 역할 편집값은 초기화한다.
  const handleSearch = (event) => {
    event.preventDefault();
    if (submitting.current) return;
    const normalizedKeyword = keyword.trim();
    if (normalizedKeyword.length > 20) { setNotice('검색어는 20자 이내로 입력해 주세요.'); return; }
    setSubmittedKeyword(normalizedKeyword); setCurrentPage(1);
    setDrafts({}); setIsRoleEditing(false); setNotice('');
    // AI작성: 같은 검색어로 다시 검색해도 서버 상태를 재조회하며 이전 GET은 effect 정리에서 취소한다.
    setRefresh((value) => value + 1);
  };

  const handlePageChange = (page) => {
    if (submitting.current || blocked) return;
    setDrafts({}); setNotice(''); setIsRoleEditing(false);
    setCurrentPage(Math.min(Math.max(page, 1), Math.max(totalPages, 1)));
  };

  // AI작성: 회원별로 한 건씩 저장하여 여러 PATCH의 부분 성공을 일괄 성공으로 표시하지 않는다.
  const handleRoleSave = async (memberId) => {
    const member = members.find((item) => item.id === memberId);
    const role = drafts[memberId];
    if (submitting.current || blocked || !member || !role || role === member.role) return;
    submitting.current = true; setPendingId(memberId); setNotice('');
    try {
      await updateSavingGroupMemberRole(savingGroupId, memberId, role);
      if (active.current) setNotice('역할을 저장했습니다.');
    } catch (failure) {
      if (active.current) setNotice(failure.message);
    } finally {
      submitting.current = false;
      if (active.current) {
        // AI작성: 성공·충돌·응답 유실 모두 서버 목록을 다시 읽고 이전 편집값을 폐기한다.
        setPendingId(null); setDrafts({}); setRefresh((value) => value + 1);
      }
    }
  };

  // AI작성: MEMBER·MANAGER만 직접 추방하며 실패 시 기존 목록과 확인창을 유지한다.
  const handleExpel = async (memberId) => {
    const member = members.find((item) => item.id === memberId);
    if (submitting.current || blocked || !canExpel || !member || !['MEMBER', 'MANAGER'].includes(member.role)) return false;
    submitting.current = true; setExpellingId(memberId); setExpulsionError(''); setNotice('');
    try {
      await expelSavingGroupMember(groupId, memberId);
      if (!active.current) return false;
      setNotice('계원을 추방했습니다.'); setDrafts({}); setIsRoleEditing(false);
      // AI작성: 현재 검색·페이지로 재조회하고 빈 마지막 페이지는 기존 GET 보정으로 이전 페이지를 읽는다.
      setRefresh((value) => value + 1);
      return true;
    } catch (failure) {
      if (active.current) setExpulsionError(failure.message);
      return false;
    } finally {
      submitting.current = false;
      if (active.current) setExpellingId(null);
    }
  };

  return (
    <Panel>
      <Intro>
        <h2>계원 관리</h2>
        <p>승인된 계원의 이름 또는 이메일로 검색할 수 있습니다. 회계는 한 명만 지정할 수 있습니다.</p>
        <p>회계를 교체하려면 기존 회계를 계원으로 변경한 뒤 새 회계를 지정해주세요.</p>
      </Intro>
      {/* AI작성: 검색·페이지와 무관하게 전체 승인 모임원을 추천하며 실제 계원 변경 후 결과를 초기화한다. */}
      {canRecommend && <div>
        <Button type="button" aria-expanded={recommendationOpen}
          disabled={pendingId !== null || expellingId !== null}
          onClick={() => setRecommendationOpen((value) => !value)}>
          {recommendationOpen ? '순번 추천 닫기' : '순번 추천'}
        </Button>
        {recommendationOpen && <SavingGroupTurnOrderRecommendationPanel
          key={JSON.stringify([savingGroupId, refresh])} savingGroupId={savingGroupId}
          disabled={pendingId !== null || expellingId !== null} />}
      </div>}
      <SearchForm onSubmit={handleSearch}>
        <TextInput
          id="saving-group-member-search"
          label="계원 검색"
          type="search"
          compact
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          maxLength={20}
          disabled={pendingId !== null || expellingId !== null}
          placeholder="이름 또는 이메일을 입력하세요"
          helpText="검색어는 20자까지 입력할 수 있습니다. 비우고 검색하면 전체 계원이 표시됩니다."
          action={<SearchButton type="submit" disabled={pendingId !== null || expellingId !== null}>검색</SearchButton>}
        />
      </SearchForm>
      {notice && <p role="status">{notice}</p>}
      {loading ? <p role="status">계원 목록을 불러오고 있습니다.</p> : error ? (
        <div role="alert"><p>{error}</p><Button type="button" onClick={() => setRefresh((value) => value + 1)}>다시 조회</Button></div>
      ) : <SavingGroupMemberList
        members={members}
        currentPage={currentPage}
        totalPages={totalPages}
        totalCount={currentResult.meta.total}
        hasSearch={Boolean(submittedKeyword)}
        onPageChange={handlePageChange}
        isRoleEditing={isRoleEditing}
        onStartRoleEdit={() => { if (!submitting.current) setIsRoleEditing(true); }}
        onCancelRoleEdit={() => { if (!submitting.current) { setIsRoleEditing(false); setDrafts({}); } }}
        drafts={drafts}
        pendingId={pendingId}
        onRoleChange={(id, role) => { if (!submitting.current) setDrafts((current) => ({ ...current, [id]: role })); }}
        onSaveRole={handleRoleSave}
        canExpel={canExpel}
        onExpel={handleExpel}
        onExpelOpen={() => setExpulsionError('')}
        expellingId={expellingId}
        expulsionError={expulsionError}
      />}
    </Panel>
  );
}

export default SavingGroupMembersPanel;
