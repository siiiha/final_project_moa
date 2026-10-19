import { FiAlertCircle, FiCalendar, FiFileText, FiUser } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';
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
  color: ${({ theme }) => theme.colors.text};
  font-size: 21px;
  letter-spacing: -1px;
`;

const SummaryDescription = styled.p`
  margin-top: 4px;
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

const ReportContentCard = styled(Card)`
  margin-top: 14px;
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
  grid-template-columns: 86px minmax(0, 1fr);
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

const ContentBox = styled.div`
  margin-top: 14px;
  padding: 14px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.7;
`;

const StatusActions = styled.div`
  margin-top: 14px;
  display: flex;
  align-items: end;
  justify-content: flex-end;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const StatusField = styled.label`
  display: grid;
  gap: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const StatusSelect = styled.select`
  min-width: 124px;
  height: 38px;
  padding: 0 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const StatusButton = styled(Button)`
  min-height: 38px;
  padding: 8px 13px;
  font-size: 12px;
`;

const StatusBadge = styled(Badge)`
  color: ${({ $pending, theme }) => ($pending ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $pending, theme }) => ($pending ? theme.colors.warningSoft : theme.colors.primarySoft)};
`;

const BackArea = styled.div`
  margin-top: 14px;
`;

function AdminReportDetail({ report, detail, status, selectedStatus, onStatusSelect, onSaveStatus, onBack }) {
  return (
    <>
      <SummaryGrid aria-label="회원 신고 요약">
        <SummaryCard>
          <SummaryLabel>
            <FiAlertCircle size={14} aria-hidden="true" /> 처리 상태
          </SummaryLabel>
          <SummaryValue>
            <StatusBadge $pending={status === '대기'}>{status}</StatusBadge>
          </SummaryValue>
          <SummaryDescription>신고 접수 후 현재 처리 상태</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiFileText size={14} aria-hidden="true" /> 신고 사유
          </SummaryLabel>
          <SummaryValue>{report.reason}</SummaryValue>
          <SummaryDescription>{detail.targetType} 신고</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>
            <FiCalendar size={14} aria-hidden="true" /> 신고 접수일
          </SummaryLabel>
          <SummaryValue>{report.reportedAt}</SummaryValue>
          <SummaryDescription>{detail.receivedAt.slice(-5)} 접수</SummaryDescription>
        </SummaryCard>
      </SummaryGrid>

      <DetailGrid>
        <Card>
          <CardTitle>신고 정보</CardTitle>
          <InformationList>
            <dt>신고자</dt>
            <dd>{report.reporter}</dd>
            <dt>신고일</dt>
            <dd>{detail.receivedAt}</dd>
            <dt>신고 사유</dt>
            <dd>{report.reason}</dd>
            <dt>신고 대상 유형</dt>
            <dd>{detail.targetType}</dd>
          </InformationList>
        </Card>
        <Card>
          <CardTitle>신고 대상</CardTitle>
          <InformationList>
            <dt>대상 콘텐츠</dt>
            <dd>{report.target}</dd>
            <dt>
              <FiUser size={13} aria-hidden="true" /> 작성자
            </dt>
            <dd>{detail.name}</dd>
            <dt>이메일</dt>
            <dd>{detail.email}</dd>
          </InformationList>
        </Card>
      </DetailGrid>

      <ReportContentCard aria-labelledby="report-content-title">
        <CardTitle id="report-content-title">신고 내용</CardTitle>
        <ContentBox>{detail.reportContent}</ContentBox>
        <StatusActions>
          <StatusField>
            처리 상태
            <StatusSelect value={selectedStatus} onChange={(event) => onStatusSelect(event.target.value)}>
              <option value="대기">대기</option>
              <option value="처리 완료">처리 완료</option>
            </StatusSelect>
          </StatusField>
          <StatusButton type="button" onClick={onSaveStatus}>
            상태 변경
          </StatusButton>
        </StatusActions>
      </ReportContentCard>

      <BackArea>
        <Button type="button" $variant="secondary" onClick={onBack}>
          목록으로
        </Button>
      </BackArea>
    </>
  );
}

export default AdminReportDetail;
