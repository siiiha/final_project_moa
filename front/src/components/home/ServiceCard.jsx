import styled from 'styled-components';

const Card = styled.article`
  min-height: 260px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
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
const MoreButton = styled.button`
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
function ServiceCard({ title, description, actionLabel }) {
  const handleFeatureClick = () => {
    /* TODO: Spring Boot API 연동 후 해당 서비스의 목록 또는 안내 화면으로 이동합니다. */
  };
  return (
    <Card>
      <Title>{title}</Title>
      <Description>{description}</Description>
      <MoreButton type="button" onClick={handleFeatureClick} aria-label={`${title} ${actionLabel}`}>
        {actionLabel} ›
      </MoreButton>
    </Card>
  );
}
export default ServiceCard;
