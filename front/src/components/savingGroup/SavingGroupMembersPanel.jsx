import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import TextInput from '../common/TextInput';
import { getSavingGroupMembers, updateSavingGroupMemberRole } from '../../api/savingGroups';
import SavingGroupMemberList from './SavingGroupMemberList';

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
function SavingGroupMembersPanel({ savingGroupId }) {
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isRoleEditing, setIsRoleEditing] = useState(false);
  const [result, setResult] = useState(null);
  const [refresh, setRefresh] = useState(0);
  const [drafts, setDrafts] = useState({});
  const [pendingId, setPendingId] = useState(null);
  const [notice, setNotice] = useState('');
  const active = useRef(false);
  const submitting = useRef(false);
  const queryKey = JSON.stringify([savingGroupId, submittedKeyword, currentPage, refresh]);
  const currentResult = result?.key === queryKey ? result : null;
  const loading = !currentResult;
  const members = currentResult?.items || [];
  const totalPages = currentResult?.meta?.totalPages || 0;
  const error = currentResult?.error || '';
  const blocked = loading || Boolean(error) || pendingId !== null;

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

  return (
    <Panel>
      <Intro>
        <h2>계원 관리</h2>
        <p>승인된 계원의 이름 또는 이메일로 검색할 수 있습니다. 회계는 한 명만 지정할 수 있습니다.</p>
        <p>회계를 교체하려면 기존 회계를 계원으로 변경한 뒤 새 회계를 지정해주세요.</p>
      </Intro>
      <SearchForm onSubmit={handleSearch}>
        <TextInput
          id="saving-group-member-search"
          label="계원 검색"
          type="search"
          compact
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          maxLength={20}
          disabled={pendingId !== null}
          placeholder="이름 또는 이메일을 입력하세요"
          helpText="검색어는 20자까지 입력할 수 있습니다. 비우고 검색하면 전체 계원이 표시됩니다."
          action={<SearchButton type="submit" disabled={pendingId !== null}>검색</SearchButton>}
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
      />}
    </Panel>
  );
}

export default SavingGroupMembersPanel;
