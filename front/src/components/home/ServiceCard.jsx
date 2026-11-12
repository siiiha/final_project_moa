import { Link } from 'react-router-dom';
import styled from 'styled-components';

// AI작성: 카드 전체를 기존 목록 페이지로 연결합니다.
const Card = styled(Link)`
  text-decoration: none;
  color: ${({ theme }) => theme.colors.text};
  min-height: 160px;
  padding: 24px;
    /* AI작성: 카드 내부에서 그림을 크게 배치하고 경계 밖은 숨깁니다. */
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
    background: linear-gradient(
    115deg,
    ${({ theme }) => theme.colors.white} 45%,
    ${({ theme }) => theme.colors.primarySoft} 100%
  );
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
    /* AI작성: 카드에 은은한 강조 효과를 적용합니다. */
  transition: border-color 180ms ease, box-shadow 180ms ease;

  @media (hover: hover) {
    &:hover {
      border-color: ${({ theme }) => theme.colors.primary};
      box-shadow: 0 6px 18px rgb(22 110 92 / 10%);
    }
  }

    &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
  }

  @media (prefers-reduced-motion: reduce) {
    transition: none;
  }

`;
const Title = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -1px;
`;
const Description = styled.p`
  margin-top: 8px;
  min-height: 50px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  letter-spacing: -0.45px;
  line-height: 1.6;
  white-space: pre-line;
`;
const MoreButton = styled.span`
  width: fit-content;
  margin-top: 13px;
  padding: 0;
  color: ${({ theme }) => theme.colors.primary};
  background: transparent;
  border: 0;
  font-size: 18px;
  font-weight: 700;
  &:hover {
    text-decoration: underline;
  }
`;

// AI작성: 안내 글은 그림보다 앞쪽의 왼쪽 영역에 표시합니다.
const CardContent = styled.div`
  position: relative;
  z-index: 1;
  width: 60%;
  min-width: 0;
`;

// AI작성: 그림을 카드 오른쪽 아래에 크게 배치합니다.
const Illustration = styled.img`
  position: absolute;
  right: -8px;
  bottom: -30px;
  width: 58%;
  height: auto;
  display: block;
  pointer-events: none;

  /* AI작성: 그림 왼쪽 경계를 부드럽게 연결합니다. */
  -webkit-mask-image: linear-gradient(
    to right, transparent, black 24%
  );
  mask-image: linear-gradient(
    to right, transparent, black 24%
  );
`;

// AI작성: 왼쪽에는 안내를, 오른쪽에는 서비스별 그림을 표시합니다.
function ServiceCard({ title, description, actionLabel, to, image }) {
  return (
    <Card to={to} aria-label={`${title} ${actionLabel}`}>
      <CardContent>
        <Title>{title}</Title>
        <Description>{description}</Description>
        <MoreButton>{actionLabel} ›</MoreButton>
      </CardContent>

      {image && (
        <Illustration
          src={image}
          alt=""
          width={1280}
          height={1280}
          loading="lazy"
          decoding="async"
        />
      )}
    </Card>
  );
}
export default ServiceCard;
