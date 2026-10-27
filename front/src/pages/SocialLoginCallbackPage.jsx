import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import styled from 'styled-components';
import client from '../api/client';
import useAuthStore from '../store/authStore';

const Container = styled.main`
  max-width: 560px;
  margin: 64px auto;
  padding: 24px;
`;

// AI작성: 제공사별 인증 결과를 받아 팝업 또는 현재 화면에서 처리한다.
function SocialLoginCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const provider = searchParams.get('provider') ?? 'kakao';

  const setToken = useAuthStore((state) => state.setToken);
  const clearToken = useAuthStore((state) => state.clearToken);
  const [errorMessage, setErrorMessage] = useState('');

  // AI작성: 개발 모드에서 결과를 중복 요청하지 않도록 보관한다.
  const requestRef = useRef(null);

  useEffect(() => {
    let active = true;

    if (
      !requestRef.current ||
      requestRef.current.provider !== provider
    ) {
      const promise = ['kakao', 'naver'].includes(provider)
        ? client.get(`/auth/${provider}/result`)
        : Promise.reject(new Error('지원하지 않는 로그인 제공사입니다.'));

      requestRef.current = { provider, promise };
    }

    requestRef.current.promise
      .then((response) => {
        if (!active) return;

        const body = response.data;
        const result = body?.data;

        const validSignup =
          result?.signupRequired === true &&
          typeof result.signupToken === 'string' &&
          /^[A-Za-z0-9_-]{43}$/.test(result.signupToken);

        const validLogin =
          result?.signupRequired === false &&
          typeof result.token === 'string' &&
          Boolean(result.token.trim());

        if (body?.success !== true || (!validSignup && !validLogin)) {
          throw new Error('소셜 로그인 결과를 확인할 수 없습니다.');
        }

        // AI작성: 팝업이면 원래 모아 화면에 결과를 전달한다.
        if (window.opener && !window.opener.closed) {
          window.opener.postMessage(
            {
              type: 'MOA_SOCIAL_RESULT',
              result,
            },
            window.location.origin,
          );
          return;
        }

        // AI작성: 일반 창에서 진행한 신규 사용자는 추가 가입으로 이동한다.
        if (validSignup) {
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

        sessionStorage.removeItem('moa-social-signup');
        setToken(result.token);
        navigate('/', { replace: true });
      })
      .catch((error) => {
        if (!active) return;

        const message =
          error.response?.data?.error?.message ??
          error.message ??
          '소셜 로그인 처리에 실패했습니다.';

        // AI작성: 팝업의 오류도 원래 모아 화면에서 안내한다.
        if (window.opener && !window.opener.closed) {
          window.opener.postMessage(
            {
              type: 'MOA_SOCIAL_RESULT',
              error: message,
            },
            window.location.origin,
          );
          return;
        }

        setErrorMessage(message);
      });

    return () => {
      active = false;
    };
  }, [provider, navigate, setToken, clearToken]);

  return (
    <Container>
      {errorMessage ? (
        <>
          <p role="alert">{errorMessage}</p>
          <Link to="/member/login">로그인 화면으로 돌아가기</Link>
        </>
      ) : (
        <p role="status">소셜 로그인 결과를 확인하고 있습니다.</p>
      )}
    </Container>
  );
}

export default SocialLoginCallbackPage;