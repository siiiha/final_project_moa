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
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    min-height: 410px;
    padding: 42px 30px;
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
const Visual = styled.div`
  position: absolute;
  right: 48px;
  top: 50%;
  width: 360px;
  height: 280px;
  transform: translateY(-50%);
  pointer-events: none;
  opacity: 0.72;
  &::before,
  &::after {
    content: '';
    position: absolute;
    border-radius: 50%;
    background: ${({ theme }) => theme.colors.primarySoft};
  }
  &::before {
    width: 230px;
    height: 230px;
    right: 8px;
    top: 22px;
  }
  &::after {
    width: 144px;
    height: 144px;
    left: 8px;
    bottom: 8px;
    background: rgba(202, 231, 220, 0.62);
  }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    right: -116px;
    opacity: 0.55;
  }
`;
const Leaf = styled.span`
  position: absolute;
  right: 100px;
  top: 60px;
  width: 105px;
  height: 164px;
  border-radius: 100% 0 100% 0;
  background: ${({ theme }) => theme.colors.primary};
  opacity: 0.1;
  transform: rotate(-24deg);
`;
const MiniCard = styled.span`
  position: absolute;
  right: 0;
  bottom: 38px;
  width: 180px;
  height: 102px;
  border: 1px solid rgba(255, 255, 255, 0.82);
  border-radius: ${({ theme }) => theme.radius.md};
  background: rgba(255, 255, 255, 0.62);
  box-shadow: ${({ theme }) => theme.shadow.card};
  &::before,
  &::after {
    content: '';
    position: absolute;
    left: 18px;
    height: 8px;
    border-radius: ${({ theme }) => theme.radius.pill};
    background: ${({ theme }) => theme.colors.primary};
    opacity: 0.18;
  }
  &::before {
    top: 25px;
    width: 76px;
  }
  &::after {
    top: 45px;
    width: 118px;
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
      <Visual aria-label="이웃 모임을 상징하는 대표 이미지" role="img">
        <Leaf />
        <MiniCard />
      </Visual>
    </Hero>
  );
}
export default LandingHero;
