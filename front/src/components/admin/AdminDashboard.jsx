import { FiAlertCircle, FiTrendingUp, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const SummaryCard = styled.article`
  min-height: 132px;
  padding: 16px;
  display: grid;
  align-content: space-between;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const CardTop = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
`;

const IconBox = styled.span`
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: ${({ $tone, theme }) => {
    if ($tone === 'danger') return theme.colors.danger;
    if ($tone === 'neutral') return theme.colors.secondaryText;
    return theme.colors.primary;
  }};
  background: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.dangerSoft : theme.colors.primarySoft)};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const Value = styled.strong`
  margin-top: 12px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 23px;
  letter-spacing: -1px;
`;

const Description = styled.p`
  margin-top: 3px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

function AdminDashboard({ stats }) {
  return (
    <SummaryGrid aria-label="회원 현황 요약">
      {stats.map((stat) => {
        const Icon = stat.tone === 'danger' ? FiAlertCircle : stat.id === 'new' ? FiTrendingUp : FiUsers;

        return (
          <SummaryCard key={stat.id}>
            <CardTop>
              {stat.label}
              <IconBox $tone={stat.tone}>
                <Icon size={15} aria-hidden="true" />
              </IconBox>
            </CardTop>
            <div>
              <Value>{stat.value}</Value>
              <Description>{stat.description}</Description>
            </div>
          </SummaryCard>
        );
      })}
    </SummaryGrid>
  );
}

export default AdminDashboard;
