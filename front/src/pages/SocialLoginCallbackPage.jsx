import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import client from '../api/client';
import useAuthStore from '../store/authStore';

const Container = styled.main`
  max-width: 560px;
  margin: 64px auto;
  padding: 24px;
`;

// AI작성: 백엔드에 보관된 소셜 인증 결과를 받아 다음 화면으로 이동한다.
function SocialLoginCallbackPage() {
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);
  const clearToken = useAuthStore((state) => state.clearToken);
  const [errorMessage, setErrorMessage] = useState('');

  // AI작성: 개발 모드에서 effect가 반복 실행돼도 결과를 중복 요청하지 않는다.
  const requestRef = useRef(null);

  useEffect(() => {
    let active = true;

    if (!requestRef.current) {
      requestRef.current = client.get('/auth/kakao/result');
    }

    requestRef.current
      .then((response) => {
        if (!active) return;

        const body = response.data;
        const result = body?.data;

        if (body?.success !== true || !result) {
          throw new Error('소셜 로그인 결과를 확인할 수 없습니다.');
        }

        // AI작성: 인증 결과를 원래 모아 화면으로 전달한다.
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage(
        { type: 'MOA_KAKAO_RESULT', result },
        window.location.origin,
      );
      return;
    }

        if (result.signupRequired === true) {
          if (
            typeof result.signupToken !== 'string' ||
            !/^[A-Za-z0-9_-]{43}$/.test(result.signupToken)
          ) {
            throw new Error('추가 가입 정보를 확인할 수 없습니다.');
          }

          // AI작성: 이전 로그인 정보를 정리하고 가입 토큰을 현재 탭에 보관한다.
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
          result.signupRequired !== false ||
          typeof result.token !== 'string' ||
          !result.token.trim()
        ) {
          throw new Error('로그인 정보를 확인할 수 없습니다.');
        }

        // AI작성: 기존 회원은 모아 JWT를 저장하고 마이페이지로 이동한다.
        sessionStorage.removeItem('moa-social-signup');
        setToken(result.token);
        navigate('/member/mypage', { replace: true });
      })
      .catch((error) => {
        if (!active) return;

        // AI작성: 오류도 원래 모아 화면에서 안내한다.
        if (window.opener && !window.opener.closed) {
          window.opener.postMessage(
            {
              type: 'MOA_KAKAO_RESULT',
              error: '카카오 로그인에 실패했습니다. 다시 시도해주세요.',
        },
        window.location.origin,
      );
      return;
    } 

        setErrorMessage(
          error.response?.data?.error?.message ??
            error.message ??
            '소셜 로그인 처리에 실패했습니다.',
        );
      });

    return () => {
      active = false;
    };
  }, [navigate, setToken, clearToken]);

  return (
    <Container>
      {errorMessage ? (
        <>
          <p role="alert">{errorMessage}</p>
          <Link to="/member/login">로그인 화면으로 돌아가기</Link>
        </>
      ) : (
        <p role="status">카카오 로그인 결과를 확인하고 있습니다.</p>
      )}
    </Container>
  );
}

export default SocialLoginCallbackPage;