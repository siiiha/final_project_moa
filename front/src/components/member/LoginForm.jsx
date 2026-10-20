import { useState } from 'react';
import { FcGoogle } from 'react-icons/fc';
import { SiKakaotalk, SiNaver } from 'react-icons/si';
import { Link, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import PasswordInput from '../common/PasswordInput';
import TextInput from '../common/TextInput';
import { socialLoginProviders } from '../../mocks/auth';
import { loginMember } from '../../api/member';
import useAuthStore from '../../store/authStore';

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
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);

  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  // 로그인 성공 시 JWT를 저장하고 마이페이지로 이동
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (isSubmitting) return;

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      const token = await loginMember(email.trim(), password);

      if (typeof token !== 'string' || !token.trim()) {
        throw new Error('로그인 응답을 확인할 수 없습니다.');
      }

      setToken(token);
      setPassword('');
      navigate('/member/mypage', { replace: true });
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message ??
          '로그인에 실패했습니다. 입력값과 서버 연결을 확인해주세요.',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSocialLogin = () => {
    // 소셜 로그인 연동 후 구현
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
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          disabled={isSubmitting}
        />

        <PasswordInput
          id="password"
          label="비밀번호"
          placeholder="비밀번호를 입력해주세요"
          autoComplete="current-password"
          required
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          disabled={isSubmitting}
          isVisible={isPasswordVisible}
          onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
        />

        {errorMessage && <p role="alert">{errorMessage}</p>}

        <LoginButton type="submit" disabled={isSubmitting}>
          {isSubmitting ? '로그인 중…' : '로그인'}
        </LoginButton>
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
