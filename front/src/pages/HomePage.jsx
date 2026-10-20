import styled from 'styled-components';
import LandingHero from '../components/home/LandingHero';
import ServiceCard from '../components/home/ServiceCard';
import { homeServiceCards } from '../mocks/home';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 48px 0 22px;
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding-top: 24px;
  }
`;
const Services = styled.section`
  margin-top: 38px;
`;
const SectionTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -1.2px;
`;
const SectionDescription = styled.p`
  margin-top: 5px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 13px;
  letter-spacing: -0.4px;
`;
const CardGrid = styled.div`
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
    gap: 12px;
  }
`;
const Footer = styled.footer`
  margin-top: 13px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  div {
    display: flex;
    gap: 7px;
  }
  a:hover {
    color: ${({ theme }) => theme.colors.primary};
  }
  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    align-items: flex-start;
    flex-direction: column;
  }
`;
function HomePage() {
  return (
    <Page>
      <LandingHero />
      <Services aria-labelledby="service-heading">
        <SectionTitle id="service-heading">모아에서 무엇을 할 수 있나요?</SectionTitle>
        <SectionDescription>필요한 서비스를 골라 편하게 이용할 수 있어요.</SectionDescription>
        <CardGrid>
          {homeServiceCards.map((service) => (
            <ServiceCard key={service.title} {...service} />
          ))}
        </CardGrid>
      </Services>
      <Footer>
        <span>학습 · 개발 · 일상 자료를 이곳에 기록하며 만들어갑니다.</span>
        <div>
          <a href="#current-gatherings">현재 54개 모임</a>
          <a href="#members">회원 보기</a>
          <a href="#admin">관리자 전용</a>
        </div>
      </Footer>
    </Page>
  );
}
export default HomePage;
