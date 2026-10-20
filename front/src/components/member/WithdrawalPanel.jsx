import { FiAlertTriangle, FiCheckCircle } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const Panel = styled.section`
  padding: 30px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 22px 18px;
  }
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.9px;
`;

const Description = styled.p`
  margin-top: 9px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.6;
`;

const StatusList = styled.dl`
  margin-top: 24px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
`;

const StatusItem = styled.div`
  min-height: 92px;
  padding: 17px;
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const StatusLabel = styled.dt`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const StatusValue = styled.dd`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.8px;
`;

const AvailableNotice = styled.p`
  margin-top: 14px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.5;
`;

const DangerArea = styled.div`
  margin-top: 26px;
  padding: 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  background: ${({ theme }) => theme.colors.dangerSoft};
  border: 1px solid #f5d7d4;
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: 520px) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

const DangerTitle = styled.h3`
  color: ${({ theme }) => theme.colors.danger};
  font-size: 15px;
  font-weight: 800;
`;

const DangerDescription = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.6;
`;

function WithdrawalPanel({ status, onRequestWithdrawal }) {
  return (
    <Panel>
      <Title>{status.title}</Title>
      <Description>{status.description}</Description>
      <StatusList>
        {status.checks.map((check) => (
          <StatusItem key={check.label}>
            <StatusLabel>{check.label}</StatusLabel>
            <StatusValue>{check.value}</StatusValue>
          </StatusItem>
        ))}
      </StatusList>
      <AvailableNotice>
        <FiCheckCircle size={16} aria-hidden="true" />
        현재 탈퇴를 제한하는 계모임이나 미정산 금액이 없습니다.
      </AvailableNotice>
      <DangerArea>
        <div>
          <DangerTitle>회원탈퇴</DangerTitle>
          <DangerDescription>안내사항 확인과 비밀번호 인증 후 탈퇴할 수 있습니다.</DangerDescription>
        </div>
        <Button type="button" $variant="danger" $size="small" onClick={onRequestWithdrawal}>
          <FiAlertTriangle size={14} aria-hidden="true" />
          회원탈퇴
        </Button>
      </DangerArea>
    </Panel>
  );
}

export default WithdrawalPanel;
