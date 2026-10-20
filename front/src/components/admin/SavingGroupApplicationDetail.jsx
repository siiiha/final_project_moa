import { FiCalendar, FiCheckCircle, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.article`
  min-height: 108px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
`;

const SummaryValue = styled.strong`
  display: block;
  margin-top: 13px;
  color: ${({ $status, theme }) => {
    if ($status === '반려') return theme.colors.danger;
    if ($status === '대기') return theme.colors.warning;
    return theme.colors.text;
  }};
  font-size: 23px;
  letter-spacing: -1px;
`;

const SummaryDescription = styled.p`
  margin-top: 2px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

const DetailGrid = styled.section`
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Card = styled.section`
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const CardTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const InformationList = styled.dl`
  margin-top: 14px;
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 10px 14px;
  font-size: 12px;

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-weight: 700;
  }

  dd {
    min-width: 0;
    color: ${({ theme }) => theme.colors.secondaryText};
    overflow-wrap: anywhere;
  }
`;

const Description = styled.p`
  margin-top: 14px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.7;
  white-space: pre-line;
`;

const Purpose = styled.div`
  margin-top: 14px;
  padding: 12px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 11px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    line-height: 1.6;
  }
`;

const ReviewCard = styled(Card)`
  margin-top: 14px;
`;

const ReviewDescription = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Actions = styled.div`
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
`;

const ActionButton = styled(Button)`
  min-height: 38px;
  padding: 8px 13px;
  font-size: 12px;
`;

const BackActions = styled.div`
  margin-top: 14px;
`;

function SavingGroupApplicationDetail({ application, detail, status, onDeleteApprove, onReject, onBack }) {
  return (
    <>
      <SummaryGrid aria-label="계모임 삭제 신청 요약">
        <SummaryCard>
          <SummaryLabel>
            <FiCheckCircle size={14} aria-hidden="true" /> 신청 상태
          </SummaryLabel>
          <SummaryValue $status={status}>{status}</SummaryValue>
          <SummaryDescription>{detail.type}</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiUsers size={14} aria-hidden="true" /> 현재 계원
          </SummaryLabel>
          <SummaryValue>{detail.currentMembers}명</SummaryValue>
          <SummaryDescription>삭제 신청 시점의 계원 수</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiCalendar size={14} aria-hidden="true" /> 미정산 금액
          </SummaryLabel>
          <SummaryValue>{detail.outstandingAmount}</SummaryValue>
          <SummaryDescription>삭제 전 확인이 필요한 금액</SummaryDescription>
        </SummaryCard>
      </SummaryGrid>

      <DetailGrid>
        <Card>
          <CardTitle>삭제 신청 정보</CardTitle>
          <InformationList>
            <dt>계모임명</dt>
            <dd>{application.name}</dd>
            <dt>신청자</dt>
            <dd>{application.applicant}</dd>
            <dt>희망 지역</dt>
            <dd>{application.region}</dd>
            <dt>신청일</dt>
            <dd>{application.appliedAt}</dd>
            <dt>신청 유형</dt>
            <dd>{detail.type}</dd>
          </InformationList>
        </Card>
        <Card>
          <CardTitle>삭제 사유</CardTitle>
          <Description>{detail.deletionReason}</Description>
          <Purpose>
            <strong>정산 확인</strong>
            <p>{detail.settlementDescription}</p>
          </Purpose>
        </Card>
      </DetailGrid>

      <ReviewCard>
        <CardTitle>삭제 심사 처리</CardTitle>
        <ReviewDescription>계원·정산 상태를 확인한 뒤 삭제 승인 또는 반려 처리를 진행해주세요.</ReviewDescription>
        <Actions>
          <ActionButton type="button" onClick={onDeleteApprove}>
            삭제 승인
          </ActionButton>
          <ActionButton type="button" $variant="danger" onClick={onReject}>
            반려
          </ActionButton>
        </Actions>
      </ReviewCard>

      <BackActions>
        <ActionButton type="button" $variant="secondary" onClick={onBack}>
          목록으로
        </ActionButton>
      </BackActions>
    </>
  );
}

export default SavingGroupApplicationDetail;
