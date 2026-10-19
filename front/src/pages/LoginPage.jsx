import styled from 'styled-components';
import LoginForm from '../components/member/LoginForm';

const LoginMain = styled.div`
  min-height: calc(100vh - 56px);
  padding: 84px 16px 48px;
  display: flex;
  justify-content: center;
  align-items: flex-start;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding-top: 42px;
  }
`;

function LoginPage() {
  return (
    <LoginMain>
      <LoginForm />
    </LoginMain>
  );
}

export default LoginPage;
