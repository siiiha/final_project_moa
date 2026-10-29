import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const Panel = styled.section`
  padding: 22px 14px 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 19px 12px 12px;
  }
`;

const Title = styled.h2`
  padding: 0 6px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;

const List = styled.div`
  margin-top: 14px;
  display: grid;
  gap: 8px;
`;

const VerificationRow = styled.article`
  min-height: 72px;
  padding: 14px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: 520px) {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
`;

const VerificationName = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

const Name = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.55px;
`;

const Description = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const ActionButton = styled(Button)`
  min-width: 106px;

  @media (max-width: 520px) {
    width: 100%;
  }
`;

function VerificationStatusList({ items, onAction }) {
  return (
    <Panel>
      <Title>내 인증 상태</Title>
      <List>
        {items.map((item) => (
          <VerificationRow key={item.id}>
            <div>
              <VerificationName>
                <Name>{item.title}</Name>
                <Badge>{item.status}</Badge>
              </VerificationName>
              <Description>{item.description}</Description>
            </div>
            <ActionButton
              type="button"
              $variant="secondary"
              $size="small"
              disabled={Boolean(item.disabled)}
              onClick={() => onAction(item.id)}
            >
              {item.actionLabel}
            </ActionButton>
          </VerificationRow>
        ))}
      </List>
    </Panel>
  );
}

export default VerificationStatusList;
