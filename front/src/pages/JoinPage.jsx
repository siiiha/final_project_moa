import styled from 'styled-components';
import JoinForm from '../components/member/JoinForm';
import { Link, useSearchParams } from 'react-router-dom';

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
  const [searchParams] = useSearchParams();
  const isSocialSignup = searchParams.get('social') === '1';

  let signupToken = '';

  // AI작성: 같은 탭에 보관된 소셜 가입 정보를 확인한다.
  if (isSocialSignup) {
    try {
      const saved = JSON.parse(
        sessionStorage.getItem('moa-social-signup') ?? 'null',
      );

      if (
        typeof saved?.signupToken === 'string' &&
        /^[A-Za-z0-9_-]{43}$/.test(saved.signupToken) &&
        typeof saved.expiresAt === 'number' &&
        saved.expiresAt > Date.now()
      ) {
        signupToken = saved.signupToken;
      }
    } catch {
      signupToken = '';
    }
  }

  if (isSocialSignup && !signupToken) {
    return (
      <JoinMain>
        <Heading>
          <Title>카카오 로그인을 다시 진행해주세요</Title>
          <Subtitle>가입 정보가 없거나 유효시간이 지났습니다.</Subtitle>
          <Link to="/member/login">로그인 화면으로 이동</Link>
        </Heading>
      </JoinMain>
    );
  }

  return (
    <JoinMain>
      <Heading>
        <Title>{isSocialSignup ? '소셜 회원가입' : '회원가입'}</Title>
        <Subtitle>
          {isSocialSignup
            ? '가입에 필요한 추가 정보와 약관 동의를 완료해주세요.'
            : '모아에서 함께할 이웃을 만나보세요.'}
        </Subtitle>
      </Heading>
      <Content>
        <JoinForm
          key={isSocialSignup ? 'social' : 'general'}
          isSocialSignup={isSocialSignup}
          signupToken={signupToken}
        />
      </Content>
    </JoinMain>
  );
}

export default JoinPage;
