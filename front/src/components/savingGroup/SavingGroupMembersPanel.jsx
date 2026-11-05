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

// AI작성: Mock을 섞지 않고 현재 모임·페이지의 서버 결과만 표시하며 검색은 후속 작업까지 유지한다.
function SavingGroupMembersPanel({ savingGroupId }) {
  const [currentPage, setCurrentPage] = useState(1);
  const [isRoleEditing, setIsRoleEditing] = useState(false);
  const [result, setResult] = useState(null);
  const [refresh, setRefresh] = useState(0);
  const [drafts, setDrafts] = useState({});
  const [pendingId, setPendingId] = useState(null);
  const [notice, setNotice] = useState('');
  const active = useRef(false);
  const submitting = useRef(false);
  const queryKey = `${savingGroupId}:${currentPage}:${refresh}`;
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
    getSavingGroupMembers(savingGroupId, { page: currentPage, signal: controller.signal })
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
  }, [savingGroupId, currentPage, queryKey]);

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
        <p>승인된 계원을 최대 20명까지 조회합니다. 회계는 한 명만 지정할 수 있습니다.</p>
        <p>회계를 교체하려면 기존 회계를 계원으로 변경한 뒤 새 회계를 지정해주세요.</p>
      </Intro>
      <SearchForm onSubmit={(event) => event.preventDefault()}>
        <TextInput
          id="saving-group-member-search"
          label="계원 검색"
          type="search"
          compact
          disabled
          placeholder="이름 또는 이메일을 입력하세요"
          helpText="검색 기능은 준비 중입니다."
          action={<SearchButton type="submit" disabled>검색</SearchButton>}
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
