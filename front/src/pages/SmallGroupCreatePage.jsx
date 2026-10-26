import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { createSmallGroup } from '../api/smallGroups';
import SmallGroupCreateForm from '../components/smallGroup/SmallGroupCreateForm';
import { smallGroupInterests, smallGroupRegions } from '../mocks/smallGroups';

// AI작성: TODO: JWT 연동 후 임시 회원 ID를 제거하고 토큰의 로그인 회원 식별값을 사용한다.
const TEMP_MEMBER_ID = 1;

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 24px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Heading = styled.div`
  margin-top: 8px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -1.2px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }
`;

const Content = styled.main`
  width: min(100%, 1040px);
  margin: 18px auto 0;
`;

const Notice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
`;

const ErrorNotice = styled(Notice)`
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
`;

function SmallGroupCreatePage() {
  const navigate = useNavigate();
  const [createdName, setCreatedName] = useState('');
  const [submissionError, setSubmissionError] = useState('');

  // AI작성: 기존 폼 name을 백엔드 소모임 생성 DTO 필드명으로 변환한다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setCreatedName('');
    setSubmissionError('');

    try {
      const created = await createSmallGroup(TEMP_MEMBER_ID, {
        groupName: formData.get('name'),
        regionName: formData.get('region'),
        likeCategory: formData.get('interest'),
        description: formData.get('description'),
        maxMembers: Number(formData.get('capacity')),
        meetDate: formData.get('meetingDate'),
        location: formData.get('place'),
      });
      setCreatedName(created.groupName);
    } catch (error) {
      setSubmissionError(error.message || '소모임 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록 &gt; 소모임 만들기</Breadcrumb>
      <Heading>
        <h1>소모임 만들기</h1>
        <p>공통의 관심사를 이웃과 함께하는 모임을 만들어보세요.</p>
      </Heading>
      <Content>
        <SmallGroupCreateForm
          regions={smallGroupRegions}
          interests={smallGroupInterests}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/small-groups')}
        />
        {createdName && <Notice role="status">{createdName} 소모임을 만들었습니다.</Notice>}
        {submissionError && <ErrorNotice role="alert">{submissionError}</ErrorNotice>}
      </Content>
    </Page>
  );
}

export default SmallGroupCreatePage;
