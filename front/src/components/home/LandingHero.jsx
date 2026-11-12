import { Link } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import useAuthStore from '../../store/authStore';

const Hero = styled.section`
  min-height: 376px;
  padding: 52px 46px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  background: ${({ theme }) => theme.colors.heroBackground};
  border-radius: ${({ theme }) => theme.radius.lg};
    /* AI작성: 작은 화면에서는 글과 사진을 세로로 배치합니다. */
  @media (max-width: 1050px) {
    min-height: auto;
    padding: 32px 24px;
    flex-direction: column;
    align-items: stretch;
    gap: 24px;
  }
`;
const Content = styled.div`
  position: relative;
  z-index: 1;
  max-width: 495px;
`;
const Eyebrow = styled.p`
  color: ${({ theme }) => theme.colors.primary};
  font-size: 14px;
  font-weight: 700;
  letter-spacing: -0.35px;
`;
const Title = styled.h1`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.text};
  font-size: clamp(30px, 3vw, 42px);
  font-weight: 800;
  letter-spacing: -2.5px;
  line-height: 1.25;
`;
const Description = styled.p`
  max-width: 440px;
  margin-top: 18px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 14px;
  letter-spacing: -0.55px;
  line-height: 1.7;
`;
const Actions = styled.div`
  margin-top: 28px;
  display: flex;
  gap: 10px;
`;
// AI작성: 사진 가장자리에 민트색 그라데이션을 겹쳐 배경과 연결합니다.
const Visual = styled.div`
  position: absolute;
  top: 0;
  right: 0;
  width: 54%;
  height: 100%;
  pointer-events: none;

  img {
    display: block;
    width: 100%;
    height: 100%;
    object-fit: contain;
    object-position: center;
  }

  &::after {
    content: '';
    position: absolute;
    inset: 0;
    background:
      linear-gradient(
        90deg,
        ${({ theme }) => theme.colors.heroBackground} 0%,
        transparent 24%,
        transparent 88%,
        ${({ theme }) => theme.colors.heroBackground} 100%
      ),
      linear-gradient(
        180deg,
        ${({ theme }) => theme.colors.heroBackground} 0%,
        transparent 16%,
        transparent 84%,
        ${({ theme }) => theme.colors.heroBackground} 100%
      );
  }

  /* AI작성: 좁은 화면에서는 사진을 글 아래에 배치합니다. */
  @media (max-width: 1050px) {
    position: relative;
    top: auto;
    right: auto;
    width: 100%;
    height: clamp(180px, 35vw, 280px);
  }
`;

function LandingHero() {
  // 헤더와 같은 로그인 상태로 홈 버튼 표시 
  const token = useAuthStore((state) => state.token);
  const isLoggedIn = Boolean(token);
  return (
    <Hero>
      <Content>
        <Eyebrow>이웃과 함께하는 일상</Eyebrow>
        <Title>
          우리 동네 좋은 이웃을
          <br />
          모아에서 만나보세요
        </Title>
        <Description>계모임부터 취미 소모임, 생활 정보까지 믿을 수 있는 이웃과 함께하세요.</Description>
        <Actions>
          <Button
            as={Link}
            to={isLoggedIn ? '/saving-groups' : '/member/join'}
          >
            {isLoggedIn ? '모임 둘러보기' : '모아 시작하기'}
          </Button>

          <Button
            as={Link}
            to={isLoggedIn ? '/member/mypage' : '/member/login'}
            $variant="secondary"
          >
            {isLoggedIn ? '마이페이지' : '로그인'}
          </Button>
        </Actions>
      </Content>
      {/* AI작성: 이웃 모임 사진을 배너 배경과 자연스럽게 연결합니다. */}
      <Visual aria-hidden="true">
        <img
          src="/moa-neighbors-illustration.png"
          alt=""
          width={1536}
          height={1024}
          decoding="async"
        />
      </Visual>
    </Hero>
  );
}
export default LandingHero;
