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
  /* AI작성: 안내 문구를 제거한 카드 내용량에 맞춰 불필요한 세로 여백을 줄인다. */
  min-height: 104px;
  padding: 16px;
  display: grid;
  /* AI작성: 항목명과 집계 수치가 한 묶음으로 읽히도록 간격을 좁힌다. */
  align-content: start;
  gap: 6px;
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
  /* AI작성: 관리가 필요한 회원 통계는 위험 색상으로 빠르게 식별하게 한다. */
  color: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.danger : theme.colors.secondaryText)};
  /* AI작성: 회원 현황 항목명을 더 뚜렷하게 보여 주도록 크기를 키운다. */
  font-size: 15px;
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
  margin-top: 0;
  color: ${({ $tone, theme }) => ($tone === 'danger' ? theme.colors.danger : theme.colors.text)};
  /* AI작성: 핵심 집계 수치를 빠르게 확인할 수 있도록 강조한다. */
  font-size: 29px;
  letter-spacing: -1px;
`;

function AdminDashboard({ stats }) {
  return (
    <SummaryGrid aria-label="회원 현황 요약">
      {stats.map((stat) => {
        const Icon = stat.tone === 'danger' ? FiAlertCircle : stat.id === 'new' ? FiTrendingUp : FiUsers;

        return (
          <SummaryCard key={stat.id}>
            <CardTop $tone={stat.tone}>
              {stat.label}
              <IconBox $tone={stat.tone}>
                <Icon size={15} aria-hidden="true" />
              </IconBox>
            </CardTop>
            <div>
              <Value $tone={stat.tone}>{stat.value}</Value>
            </div>
          </SummaryCard>
        );
      })}
    </SummaryGrid>
  );
}

export default AdminDashboard;
