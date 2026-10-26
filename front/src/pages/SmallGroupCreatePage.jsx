import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import SmallGroupCreateForm from '../components/smallGroup/SmallGroupCreateForm';
import { smallGroupInterests, smallGroupRegions } from '../mocks/smallGroups';

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

function SmallGroupCreatePage() {
  const navigate = useNavigate();
  const [createdName, setCreatedName] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    // TODO: Spring Boot 소모임 생성 API에 폼 입력값을 전송하고 생성 결과를 처리합니다.
    setCreatedName(formData.get('name'));
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
        {createdName && <Notice role="status">{createdName} 소모임 만들기를 준비했습니다.</Notice>}
      </Content>
    </Page>
  );
}

export default SmallGroupCreatePage;
