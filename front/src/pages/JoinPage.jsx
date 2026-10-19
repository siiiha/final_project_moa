import styled from 'styled-components';
import JoinForm from '../components/member/JoinForm';

const JoinMain = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 31px 0 46px;
`;

const Heading = styled.div`
  text-align: center;
`;

const Title = styled.h1`
  color: ${({ theme }) => theme.colors.text};
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -1.45px;
`;

const Subtitle = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
`;

const Content = styled.div`
  margin-top: 34px;
`;

function JoinPage() {
  return (
    <JoinMain>
      <Heading>
        <Title>회원가입</Title>
        <Subtitle>모아에서 함께할 이웃을 만나보세요.</Subtitle>
      </Heading>
      <Content>
        <JoinForm />
      </Content>
    </JoinMain>
  );
}

export default JoinPage;
