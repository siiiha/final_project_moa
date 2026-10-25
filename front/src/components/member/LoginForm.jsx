import { useEffect, useRef, useState } from 'react';
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
  const clearToken = useAuthStore((state) => state.clearToken);

  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const popupRef = useRef(null);
const popupTimerRef = useRef(null);

// AI작성: 우리가 연 카카오 인증 창의 메시지만 처리한다.
useEffect(() => {
  const closePopup = () => {
    window.clearInterval(popupTimerRef.current);
    popupTimerRef.current = null;

    if (popupRef.current && !popupRef.current.closed) {
      popupRef.current.close();
    }

    popupRef.current = null;
  };

  const handlePopupMessage = (event) => {
    if (
      event.origin !== window.location.origin ||
      !popupRef.current ||
      event.source !== popupRef.current ||
      event.data?.type !== 'MOA_KAKAO_RESULT'
    ) {
      return;
    }

    closePopup();
    setIsSubmitting(false);

    if (typeof event.data.error === 'string') {
      setErrorMessage(event.data.error);
      return;
    }

    const result = event.data.result;

    if (
      result?.signupRequired === true &&
      typeof result.signupToken === 'string' &&
      /^[A-Za-z0-9_-]{43}$/.test(result.signupToken)
    ) {
      clearToken();
      sessionStorage.setItem(
        'moa-social-signup',
        JSON.stringify({
          signupToken: result.signupToken,
          expiresAt: Date.now() + 10 * 60 * 1000,
        }),
      );

      navigate('/member/join?social=1', { replace: true });
      return;
    }

    if (
      result?.signupRequired === false &&
      typeof result.token === 'string' &&
      result.token.trim()
    ) {
      sessionStorage.removeItem('moa-social-signup');
      setToken(result.token);
      navigate('/member/mypage', { replace: true });
      return;
    }

    setErrorMessage('카카오 로그인 결과를 확인할 수 없습니다.');
  };

  window.addEventListener('message', handlePopupMessage);

  return () => {
    window.removeEventListener('message', handlePopupMessage);
    closePopup();
  };
}, [navigate, setToken, clearToken]);

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

  // AI작성: 원래 모아 화면을 유지하면서 카카오 인증 창을 연다.
const handleSocialLogin = (providerId) => {
  if (isSubmitting || popupRef.current) return;

  if (providerId !== 'kakao') {
    setErrorMessage('해당 소셜 로그인은 준비 중입니다.');
    return;
  }

  setErrorMessage('');

  const width = 480;
  const height = 720;
  const left = Math.max(
    0,
    window.screenX + (window.outerWidth - width) / 2,
  );
  const top = Math.max(
    0,
    window.screenY + (window.outerHeight - height) / 2,
  );

  const popup = window.open(
    '/api/auth/kakao/login',
    `moa-kakao-${crypto.randomUUID()}`,
    `popup=yes,width=${width},height=${height},left=${left},top=${top}`,
  );

  if (!popup) {
    setErrorMessage('팝업이 차단됐습니다. 팝업을 허용한 뒤 다시 눌러주세요.');
    return;
  }

  popupRef.current = popup;
  setIsSubmitting(true);

  // AI작성: 사용자가 인증 창을 직접 닫으면 다시 시도할 수 있게 한다.
  popupTimerRef.current = window.setInterval(() => {
    if (!popup.closed) return;

    window.clearInterval(popupTimerRef.current);
    popupTimerRef.current = null;
    popupRef.current = null;
    setIsSubmitting(false);
    setErrorMessage('카카오 로그인 창이 닫혔습니다. 다시 시도해주세요.');
  }, 500);
};

  // AI작성: 로그인 입력과 소셜 로그인 버튼을 표시한다.
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
            onClick={() => handleSocialLogin(provider.id)}
            disabled={isSubmitting}
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
