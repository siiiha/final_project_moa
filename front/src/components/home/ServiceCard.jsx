import { Link } from 'react-router-dom';
import styled from 'styled-components';

const Card = styled.article`
  min-height: 180px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
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
const MoreButton = styled(Link)`
  width: fit-content;
  margin-top: 13px;
  padding: 0;
  color: ${({ theme }) => theme.colors.primary};
  background: transparent;
  border: 0;
  font-size: 12px;
  font-weight: 700;
  &:hover {
    text-decoration: underline;
  }
`;
// AI작성: 서비스 카드에서 전달받은 목록 페이지로 이동합니다.
function ServiceCard({ title, description, actionLabel, to }) {
  return (
    <Card>
      <Title>{title}</Title>
      <Description>{description}</Description>
      <MoreButton to={to} aria-label={`${title} ${actionLabel}`}>
        {actionLabel} ›
      </MoreButton>
    </Card>
  );
}
export default ServiceCard;
