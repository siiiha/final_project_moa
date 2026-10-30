import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const Card = styled.section`
  padding: 20px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const TitleRow = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 19px;
  font-weight: 800;
`;

const StatusBadge = styled(Badge)`
  color: ${({ $pending, theme }) => ($pending ? theme.colors.warning : theme.colors.primary)};
  background: ${({ $pending, theme }) => ($pending ? theme.colors.warningSoft : theme.colors.primarySoft)};
`;

const Summary = styled.dl`
  margin: 18px 0 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  border-top: 1px solid ${({ theme }) => theme.colors.border};
  border-left: 1px solid ${({ theme }) => theme.colors.border};
  div { padding: 12px; border-right: 1px solid ${({ theme }) => theme.colors.border}; border-bottom: 1px solid ${({ theme }) => theme.colors.border}; }
  dt { color: ${({ theme }) => theme.colors.muted}; font-size: 11px; font-weight: 700; }
  dd { margin: 6px 0 0; color: ${({ theme }) => theme.colors.secondaryText}; font-size: 13px; }
`;

const SectionTitle = styled.h3`
  margin-top: 26px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const SubmissionList = styled.div`
  margin-top: 12px;
  display: grid;
  gap: 10px;
`;

const Submission = styled.article`
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  strong { color: ${({ theme }) => theme.colors.text}; font-size: 13px; }
  p { margin: 8px 0; color: ${({ theme }) => theme.colors.secondaryText}; font-size: 13px; line-height: 1.55; white-space: pre-wrap; }
  small { color: ${({ theme }) => theme.colors.muted}; font-size: 11px; }
`;

const ProcessForm = styled.form`
  margin-top: 26px;
  padding-top: 20px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const Field = styled.label`
  margin-top: 14px;
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
  select, textarea { width: 100%; padding: 10px 12px; color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white}; border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: ${({ theme }) => theme.radius.sm}; font: inherit; }
  textarea { min-height: 92px; resize: vertical; }
`;

// AI작성: 승인 처리에 필요한 선택 항목을 한 줄에 배치해 관리자가 값을 빠르게 비교할 수 있게 한다.
const ProcessFields = styled.div`
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;

  ${Field} {
    margin-top: 14px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Actions = styled.div`
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
`;

// AI작성: 신고 사건 정보와 처리 입력 영역을 분리해 관리자에게 처리 대상과 결과를 명확히 안내한다.
function AdminReportDetail({ report, action, restrictionScope, restrictionType, processReason, submitting, onActionChange, onRestrictionScopeChange, onRestrictionTypeChange, onProcessReasonChange, onSubmit, onBack }) {
  const pending = report.statusCode === 'PENDING';
  return (
    <Card>
      <TitleRow><Title>신고 사건 #{report.id}</Title><StatusBadge $pending={pending}>{report.status}</StatusBadge></TitleRow>
      <Summary>
        <div><dt>신고 유형</dt><dd>{report.type}</dd></div><div><dt>신고 대상</dt><dd>{report.targetMember}</dd></div>
        <div><dt>신고 수</dt><dd>{report.reporterCount}건</dd></div><div><dt>접수일</dt><dd>{report.createdAt}</dd></div>
        {!pending && <div><dt>처리일</dt><dd>{report.processedAt}</dd></div>}
        {report.restrictionScope && <div><dt>제한 영역</dt><dd>{report.restrictionScope}</dd></div>}
        {report.processReason && <div><dt>처리 사유</dt><dd>{report.processReason}</dd></div>}
      </Summary>
      <SectionTitle>개별 신고 내용</SectionTitle>
      <SubmissionList>{report.submissions.map((submission) => <Submission key={submission.id}><strong>{submission.reporter}</strong><p>{submission.reason}</p><small>{submission.reportedAt}{submission.hasEvidence ? ' · 증빙 첨부' : ''}</small></Submission>)}</SubmissionList>
      {pending && <ProcessForm onSubmit={onSubmit}>
        <SectionTitle>신고 처리</SectionTitle>
        <ProcessFields>
          <Field>처리 결과<select value={action} onChange={(event) => onActionChange(event.target.value)} disabled={submitting}><option value="CONFIRMED">승인</option><option value="REJECTED">반려</option></select></Field>
          {/* AI작성: 신고 승인 시 관리자가 실제로 제한할 기능 범위를 명시적으로 선택한다. */}
          {action === 'CONFIRMED' && <Field>제한 영역<select value={restrictionScope} onChange={(event) => onRestrictionScopeChange(event.target.value)} disabled={submitting}><option value="CONTENT">게시글·댓글</option><option value="CHAT">채팅</option><option value="REPORT">신고</option></select></Field>}
          {action === 'CONFIRMED' && <Field>이용 제한<select value={restrictionType} onChange={(event) => onRestrictionTypeChange(event.target.value)} disabled={submitting}><option value="7_DAYS">7일</option><option value="30_DAYS">30일</option><option value="PERMANENT">영구</option></select></Field>}
        </ProcessFields>
        <Field>처리 사유<textarea value={processReason} onChange={(event) => onProcessReasonChange(event.target.value)} maxLength={500} required disabled={submitting} /></Field>
        <Actions>
          <Button type="button" $variant="secondary" onClick={onBack}>목록으로</Button>
          <Button type="submit" disabled={submitting}>{submitting ? '처리 중...' : '처리 저장'}</Button>
        </Actions>
      </ProcessForm>}
      {!pending && <Actions><Button type="button" $variant="secondary" onClick={onBack}>목록으로</Button></Actions>}
    </Card>
  );
}
export default AdminReportDetail;
