import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

// AI작성: 큰 글씨와 충분한 여백으로 인증 상태를 구분합니다.
const Panel = styled.section`
  padding: 28px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 20px;
  }
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const List = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const VerificationRow = styled.article`
  min-width: 0;
  min-height: 210px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 28px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};

  @media (max-width: 520px) {
    min-height: 190px;
    padding: 20px;
    gap: 22px;
  }
`;

const VerificationName = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  > span {
    min-height: 28px;
    padding: 5px 10px;
    font-size: 12px;
  }
`;

const Name = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 19px;
  font-weight: 800;
  letter-spacing: -0.5px;
`;

const Description = styled.p`
  margin-top: 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 14px;
  line-height: 1.7;
  word-break: keep-all;
  overflow-wrap: anywhere;
`;

const ActionButton = styled(Button)`
  width: 100%;
  min-height: 48px;
  padding: 12px 16px;
  font-size: 15px;
  border-radius: ${({ theme }) => theme.radius.md};

  &:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }

  &:disabled:active {
    transform: none;
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
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
