import client from './client.js';

function toAdminReportError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminReportError';
  failure.code = responseError?.code || 'ADMIN_REPORT_REQUEST_FAILED';
  return failure;
}

function formatDate(value) {
  if (!value) return '날짜 확인 필요';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '날짜 확인 필요';
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit',
  }).format(date).replace(/\.\s?/g, '.').replace(/\.$/, '');
}

function formatReportType(value) {
  return {
    BOARD_POST: '게시글', BOARD_COMMENT: '댓글', BOARD_MEMBER: '회원',
    MEETING: '모임', MEETING_MEMBER: '모임 회원',
  }[value] || '유형 확인 필요';
}

function formatReportStatus(value) {
  return { PENDING: '대기', CONFIRMED: '승인', REJECTED: '반려' }[value] || '상태 확인 필요';
}

function mapReport(item) {
  return {
    id: item.caseId,
    type: formatReportType(item.reportType),
    typeCode: item.reportType,
    targetId: item.targetId,
    targetMember: item.reportedMemberNickname || '대상 확인 필요',
    reporterCount: item.reporterCount ?? 0,
    status: formatReportStatus(item.status),
    statusCode: item.status,
    processReason: item.processReason || '',
    createdAt: formatDate(item.createdAt),
    processedAt: item.processedAt ? formatDate(item.processedAt) : '-',
    restrictedUntil: item.restrictedUntil ? formatDate(item.restrictedUntil) : '-',
  };
}

// AI작성: 관리자 신고 목록 API 응답을 목록 화면의 표시 형식으로 변환한다.
export async function getAdminReports({ status, reportType, searchType, keyword, page = 1, size = 10, signal } = {}) {
  try {
    const response = await client.get('/admin/reports', {
      params: {
        status: status || undefined,
        reportType: reportType || undefined,
        searchType: searchType || undefined,
        keyword: keyword || undefined,
        page,
        size,
      },
      signal,
    });
    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new Error('신고 목록 응답을 확인할 수 없습니다.');
    }
    return { items: response.data.data.map(mapReport), meta: response.data.meta };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminReportError(error, '신고 목록을 불러오지 못했습니다.');
  }
}

// AI작성: 신고 사건 상세와 사건에 포함된 개별 신고 내용을 함께 변환한다.
export async function getAdminReport(caseId, signal) {
  try {
    const response = await client.get(`/admin/reports/${encodeURIComponent(caseId)}`, { signal });
    if (!response.data?.success || !response.data.data?.reportCase || !Array.isArray(response.data.data.submissions)) {
      throw new Error('신고 상세 응답을 확인할 수 없습니다.');
    }
    const report = mapReport(response.data.data.reportCase);
    return {
      report: {
        ...report,
        processId: response.data.data.submissions[0]?.reportId || null,
        submissions: response.data.data.submissions.map((submission) => ({
          id: submission.reportId,
          reporter: submission.reporterNickname || '신고자 확인 필요',
          reason: submission.reason || '신고 사유 없음',
          reportedAt: formatDate(submission.reportedAt),
          hasEvidence: Boolean(submission.evidenceFiles),
        })),
      },
    };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminReportError(error, '신고 상세 정보를 불러오지 못했습니다.');
  }
}

// AI작성: 개별 신고 번호로 사건 처리와 대상 회원 제한을 한 트랜잭션으로 요청한다.
export async function processAdminReport(reportId, request) {
  try {
    const response = await client.patch(`/admin/reports/${encodeURIComponent(reportId)}/process`, request);
    if (!response.data?.success) throw new Error('신고 처리 응답을 확인할 수 없습니다.');
  } catch (error) {
    throw toAdminReportError(error, '신고를 처리하지 못했습니다.');
  }
}
