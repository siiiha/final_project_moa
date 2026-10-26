import { useState } from 'react';
import { FiCalendar, FiCheckCircle, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';
import Textarea from '../common/Textarea';

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
  color: ${({ $status, theme }) => ($status === 'REJECTED' ? theme.colors.danger : $status === 'PENDING' ? theme.colors.warning : theme.colors.text)};
  font-size: 22px;
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
const ReviewCard = styled(Card)`
  margin-top: 14px;
`;
const ReviewDescription = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;
const RejectLabel = styled.label`
  margin-top: 14px;
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
  textarea {
    min-height: 92px;
  }
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

function formatAmount(amount) {
  const value = Number(amount);
  return Number.isFinite(value) ? `${value.toLocaleString('ko-KR')}원` : '금액 확인 필요';
}

function SavingGroupApplicationDetail({ application, isProcessing, onApprove, onReject, onBack }) {
  const [rejectReason, setRejectReason] = useState('');
  const pending = application.status === 'PENDING';

  return (
    <>
      <SummaryGrid aria-label="계모임 개설 신청 요약">
        <SummaryCard>
          <SummaryLabel>
            <FiCheckCircle size={14} aria-hidden="true" /> 신청 상태
          </SummaryLabel>
          <SummaryValue $status={application.status}>
            {{ PENDING: '대기', APPROVED: '승인', REJECTED: '반려' }[application.status]}
          </SummaryValue>
          <SummaryDescription>개설 심사</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiUsers size={14} aria-hidden="true" /> 모집 인원
          </SummaryLabel>
          <SummaryValue>{application.maxMembers}명</SummaryValue>
          <SummaryDescription>신청한 계모임 정원</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiCalendar size={14} aria-hidden="true" /> 월 회비
          </SummaryLabel>
          <SummaryValue>{formatAmount(application.duesAmount)}</SummaryValue>
          <SummaryDescription>매월 {application.duesDay}일 납부</SummaryDescription>
        </SummaryCard>
      </SummaryGrid>

      <DetailGrid>
        <Card>
          <CardTitle>개설 신청 정보</CardTitle>
          <InformationList>
            <dt>계모임명</dt>
            <dd>{application.groupName}</dd>
            <dt>신청자</dt>
            <dd>{application.applicantName}</dd>
            <dt>활동 지역</dt>
            <dd>{application.regionName}</dd>
            <dt>계모임 종류</dt>
            <dd>{application.groupType}</dd>
            <dt>신청일</dt>
            <dd>
              {application.appliedAt ? new Date(application.appliedAt).toLocaleDateString('ko-KR') : '날짜 확인 필요'}
            </dd>
          </InformationList>
        </Card>
        <Card>
          <CardTitle>계모임 소개</CardTitle>
          <Description>{application.description}</Description>
          {application.groupImagePath && <ReviewDescription>대표 이미지가 첨부되었습니다.</ReviewDescription>}
        </Card>
      </DetailGrid>

      <ReviewCard>
        <CardTitle>개설 심사 처리</CardTitle>
        {pending ? (
          <>
            <ReviewDescription>신청 내용을 확인한 뒤 승인하거나 반려 사유를 입력해 반려 처리하세요.</ReviewDescription>
            <RejectLabel>
              반려 사유
              <Textarea
                value={rejectReason}
                maxLength="255"
                onChange={(event) => setRejectReason(event.target.value)}
                placeholder="반려 사유를 입력하세요"
              />
            </RejectLabel>
            <Actions>
              <ActionButton type="button" onClick={onApprove} disabled={isProcessing}>
                승인
              </ActionButton>
              <ActionButton
                type="button"
                $variant="danger"
                onClick={() => onReject(rejectReason.trim())}
                disabled={isProcessing || !rejectReason.trim()}
              >
                반려
              </ActionButton>
            </Actions>
          </>
        ) : (
          <>
            <ReviewDescription>처리 완료된 신청입니다.</ReviewDescription>
            {application.rejectReason && <Description>반려 사유: {application.rejectReason}</Description>}
          </>
        )}
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
