import { useState } from 'react';
import { FcGoogle } from 'react-icons/fc';
import { SiKakaotalk, SiNaver } from 'react-icons/si';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import PasswordInput from '../common/PasswordInput';
import TextInput from '../common/TextInput';
import { socialLoginProviders } from '../../mocks/auth';

const Card = styled.section`
  width: min(100%, 512px);
  padding: 40px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid rgba(227, 232, 230, 0.62);
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: 0 6px 18px rgba(29, 55, 48, 0.08);

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 28px 24px;
  }
`;

const Heading = styled.div`
  text-align: center;
`;

const Title = styled.h1`
  color: ${({ theme }) => theme.colors.text};
  font-size: 30px;
  font-weight: 800;
  letter-spacing: -1.7px;
`;

const Subtitle = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  letter-spacing: -0.4px;
`;

const Form = styled.form`
  margin-top: 29px;
  display: grid;
  gap: 17px;
`;

const LoginButton = styled(Button)`
  width: 100%;
  min-height: 52px;
  margin-top: 4px;
`;

const SignupPrompt = styled.p`
  margin-top: 16px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  text-align: center;
`;

const SignupButton = styled(Button)`
  width: 100%;
  min-height: 44px;
  margin-top: 9px;
`;

const Divider = styled.div`
  margin: 25px 0 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;

  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: ${({ theme }) => theme.colors.border};
  }
`;

const SocialActions = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SocialButton = styled.button`
  min-height: 40px;
  padding: 7px 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  color: ${({ $provider, theme }) => ($provider === 'naver' ? theme.colors.white : theme.colors.text)};
  background: ${({ $provider }) => {
    if ($provider === 'naver') return '#03c75a';
    if ($provider === 'kakao') return '#fee500';
    return '#ffffff';
  }};
  border: 1px solid ${({ $provider, theme }) => ($provider === 'google' ? theme.colors.border : 'transparent')};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 10px;
  font-weight: 700;

  &:hover {
    filter: brightness(0.97);
  }
`;

function ProviderIcon({ provider }) {
  if (provider === 'naver') return <SiNaver size={13} aria-hidden="true" />;
  if (provider === 'kakao') return <SiKakaotalk size={15} aria-hidden="true" />;
  return <FcGoogle size={15} aria-hidden="true" />;
}

function LoginForm() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 로그인 API 연동 후 인증 결과에 따라 화면을 이동합니다.
  };

  const handleSocialLogin = () => {
    // TODO: Spring Boot OAuth 연동 후 선택한 소셜 로그인 제공자 인증을 시작합니다.
  };

  return (
    <Card aria-labelledby="login-title">
      <Heading>
        <Title id="login-title">로그인</Title>
        <Subtitle>이웃과 함께하는 일상, 모아에서 시작하세요.</Subtitle>
      </Heading>
      <Form onSubmit={handleSubmit}>
        <TextInput
          id="email"
          label="이메일"
          type="email"
          placeholder="이메일을 입력해주세요"
          autoComplete="email"
          required
        />
        <PasswordInput
          id="password"
          label="비밀번호"
          placeholder="비밀번호를 입력해주세요"
          autoComplete="current-password"
          required
          isVisible={isPasswordVisible}
          onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
        />
        <LoginButton type="submit">로그인</LoginButton>
      </Form>
      <SignupPrompt>아직 회원이 아니신가요?</SignupPrompt>
      <SignupButton as={Link} to="/member/join" $variant="secondary">
        회원가입
      </SignupButton>
      <Divider>간편 로그인</Divider>
      <SocialActions>
        {socialLoginProviders.map((provider) => (
          <SocialButton
            key={provider.id}
            type="button"
            $provider={provider.id}
            onClick={handleSocialLogin}
            aria-label={provider.label}
          >
            <ProviderIcon provider={provider.icon} />
            {provider.label}
          </SocialButton>
        ))}
      </SocialActions>
    </Card>
  );
}

export default LoginForm;
