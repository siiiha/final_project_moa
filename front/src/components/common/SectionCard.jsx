import styled from 'styled-components';

const Card = styled.section`
  overflow: hidden;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Header = styled.header`
  padding: 14px 18px;
  background: ${({ theme }) => theme.colors.primarySoft};
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;

const Body = styled.div`
  padding: 18px;
`;

function SectionCard({ title, children, className, headingLevel = 'h2' }) {
  return (
    <Card className={className}>
      <Header>
        <Title as={headingLevel}>{title}</Title>
      </Header>
      <Body>{children}</Body>
    </Card>
  );
}

export default SectionCard;
