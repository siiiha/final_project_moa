import { useMemo, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import TextInput from '../common/TextInput';
import { savingGroupMembers } from '../../mocks/savingGroupMembers';
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

const PAGE_SIZE = 10;

function SavingGroupMembersPanel() {
  const [members, setMembers] = useState(savingGroupMembers);
  const [keyword, setKeyword] = useState('');
  const [submittedKeyword, setSubmittedKeyword] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isRoleEditing, setIsRoleEditing] = useState(false);

  const filteredMembers = useMemo(() => {
    const normalizedKeyword = submittedKeyword.trim().toLowerCase();
    if (!normalizedKeyword) {
      return members;
    }

    return members.filter((member) => (
      member.name.toLowerCase().includes(normalizedKeyword) || member.email.toLowerCase().includes(normalizedKeyword)
    ));
  }, [members, submittedKeyword]);

  const totalPages = Math.max(1, Math.ceil(filteredMembers.length / PAGE_SIZE));
  const visibleMembers = filteredMembers.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);

  const handleSearch = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 서버의 계모임 계원 목록 조회 API에 계모임 ID와 검색어를 전달합니다.
    setSubmittedKeyword(keyword);
    setCurrentPage(1);
  };

  const handlePageChange = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
  };

  const handleRoleChange = (memberId, role) => {
    setMembers((currentMembers) => currentMembers.map((member) => (
      member.id === memberId ? { ...member, role } : member
    )));
  };

  const handleRoleSave = () => {
    // TODO: Spring Boot 서버에 계원 역할 변경 API 요청을 연결합니다.
    setIsRoleEditing(false);
  };

  return (
    <Panel>
      <Intro>
        <h2>계원 관리</h2>
        <p>계원 정보를 조회하고 역할을 관리할 수 있습니다.</p>
      </Intro>
      <SearchForm onSubmit={handleSearch}>
        <TextInput
          id="saving-group-member-search"
          label="계원 검색"
          type="search"
          compact
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="이름 또는 이메일을 입력하세요"
          action={<SearchButton type="submit">검색</SearchButton>}
        />
      </SearchForm>
      <SavingGroupMemberList
        members={visibleMembers}
        currentPage={currentPage}
        totalPages={totalPages}
        totalCount={filteredMembers.length}
        onPageChange={handlePageChange}
        isRoleEditing={isRoleEditing}
        onStartRoleEdit={() => setIsRoleEditing(true)}
        onRoleChange={handleRoleChange}
        onSaveRoles={handleRoleSave}
      />
    </Panel>
  );
}

export default SavingGroupMembersPanel;
