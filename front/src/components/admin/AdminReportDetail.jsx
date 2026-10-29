import Button from '../common/Button';

// AI작성: 백엔드 신고 사건과 개별 신고 내역을 그대로 표시하고 대기 사건만 처리한다.
function AdminReportDetail({ report, action, restrictionType, processReason, submitting, onActionChange, onRestrictionTypeChange, onProcessReasonChange, onSubmit, onBack }) {
  const pending = report.statusCode === 'PENDING';
  return <section>
    <h2>신고 사건 #{report.id}</h2>
    <p>유형: {report.type} · 대상: {report.targetMember} · 신고 수: {report.reporterCount}건</p>
    <p>상태: {report.status} · 접수일: {report.createdAt}</p>
    {report.processReason && <p>처리 사유: {report.processReason}</p>}
    <h3>개별 신고 내용</h3>
    {report.submissions.map((submission) => <article key={submission.id}><strong>{submission.reporter}</strong><p>{submission.reason}</p><small>{submission.reportedAt}{submission.hasEvidence ? ' · 증빙 첨부' : ''}</small></article>)}
    {pending && <form onSubmit={onSubmit}>
      <label>처리 결과 <select value={action} onChange={(event) => onActionChange(event.target.value)} disabled={submitting}><option value="CONFIRMED">승인</option><option value="REJECTED">반려</option></select></label>
      {action === 'CONFIRMED' && <label>이용 제한 <select value={restrictionType} onChange={(event) => onRestrictionTypeChange(event.target.value)} disabled={submitting}><option value="7_DAYS">7일</option><option value="30_DAYS">30일</option><option value="PERMANENT">영구</option></select></label>}
      <label>처리 사유 <input value={processReason} onChange={(event) => onProcessReasonChange(event.target.value)} maxLength={500} required disabled={submitting} /></label>
      <Button type="submit" disabled={submitting}>{submitting ? '처리 중...' : '처리 저장'}</Button>
    </form>}
    <Button type="button" $variant="secondary" onClick={onBack}>목록으로</Button>
  </section>;
}
export default AdminReportDetail;
