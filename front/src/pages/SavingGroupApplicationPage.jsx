import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import SavingGroupApplicationForm from '../components/savingGroup/SavingGroupApplicationForm';
import { savingGroupRegions } from '../mocks/savingGroups';

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

function SavingGroupApplicationPage() {
  const navigate = useNavigate();

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 계모임 개설 신청 API에 입력값과 선택한 이미지 파일을 전송합니다.
  };

  return (
    <Page>
      <Breadcrumb>계모임 &gt; 계모임 목록 &gt; 계모임 개설 신청</Breadcrumb>
      <Heading>
        <h1>계모임 개설 신청</h1>
        <p>우리 동네 이웃과 함께할 새로운 계모임을 만들어보세요.</p>
      </Heading>
      <Content>
        <SavingGroupApplicationForm
          regions={savingGroupRegions}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/saving-groups')}
        />
      </Content>
    </Page>
  );
}

export default SavingGroupApplicationPage;
