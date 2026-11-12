import { Link } from 'react-router-dom';
import styled from 'styled-components';

// AI작성: 카드 전체를 기존 목록 페이지로 연결합니다.
const Card = styled(Link)`
  text-decoration: none;
  color: ${({ theme }) => theme.colors.text};
  min-height: 180px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: ${({ theme }) => theme.colors.white};
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
  min-height: 42px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  letter-spacing: -0.45px;
  line-height: 1.6;
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
// AI작성: 서비스 카드에서 전달받은 목록 페이지로 이동합니다.
function ServiceCard({ title, description, actionLabel, to }) {
  return (
    <Card to={to} aria-label={`${title} ${actionLabel}`}>
      <Title>{title}</Title>
      <Description>{description}</Description>
      <MoreButton>
        {actionLabel} ›
      </MoreButton>
    </Card>
  );
}
export default ServiceCard;
