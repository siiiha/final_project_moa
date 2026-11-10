import client from './client.js';

// AI작성: 현재 순번과 다음 달 예약안을 같은 인증 client로 조회·저장한다.
function toTurnOrderAssignment(data) {
  if (!data || !/^\d{4}-\d{2}-01$/.test(data.effectiveMonth) || !Number.isSafeInteger(data.startRoundNumber)
    || data.startRoundNumber < 1 || typeof data.scheduled !== 'boolean' || typeof data.needsReassignment !== 'boolean'
    || !Array.isArray(data.members)) throw new Error('순번 지정 응답을 확인하지 못했습니다.');
  const ids = new Set();
  const members = data.members.map((row) => {
    const memberId = savingGroupPathId(row.memberId);
    if (ids.has(memberId) || typeof row.nickname !== 'string' || !['OWNER', 'MANAGER', 'MEMBER'].includes(row.role)
      || typeof row.paid !== 'boolean' || [row.currentSequenceNumber, row.plannedSequenceNumber].some(
        value => value != null && (!Number.isSafeInteger(value) || value < 1))) throw new Error('순번 지정 응답을 확인하지 못했습니다.');
    ids.add(memberId);
    return { memberId, nickname: row.nickname, role: row.role, paid: row.paid,
      currentSequenceNumber: row.currentSequenceNumber ?? null, plannedSequenceNumber: row.plannedSequenceNumber ?? null };
  });
  return { effectiveMonth: data.effectiveMonth, startRoundNumber: data.startRoundNumber,
    scheduled: data.scheduled, needsReassignment: data.needsReassignment, members };
}

export async function getSavingGroupTurnOrders(savingGroupId, { signal } = {}) {
  const result = await savingGroupMemberResponse(
    () => client.get(`/saving-groups/${savingGroupPathId(savingGroupId)}/turn-orders`, { signal }),
    '순번을 불러오지 못했습니다.');
  return toTurnOrderAssignment(result.data);
}

export async function updateSavingGroupTurnOrders(savingGroupId, turnOrders, { signal } = {}) {
  const result = await savingGroupMemberResponse(
    () => client.put(`/saving-groups/${savingGroupPathId(savingGroupId)}/turn-orders`, { turnOrders }, { signal }),
    '순번 저장 결과를 확인하지 못했습니다. 다시 조회해주세요.');
  return toTurnOrderAssignment(result.data);
}

// AI작성: 전체 모의 추천 결과만 조회하며 저장·적용 API는 호출하지 않는다.
export async function getSavingGroupTurnOrderRecommendations(savingGroupId, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const result = await savingGroupMemberResponse(
    () => client.get(`/saving-groups/${id}/turn-orders/recommendations`, { signal }),
    '순번 추천을 불러오지 못했습니다. 다시 시도해주세요.'
  );
  const rows = result.data;
  const ids = new Set();
  const orders = new Set();
  const invalid = () => { const error = new Error('순번 추천 응답을 확인하지 못했습니다. 다시 시도해주세요.');
    error.code = 'INVALID_RESPONSE'; return error; };
  if (!Array.isArray(rows)) throw invalid();
  const items = rows.map((row) => {
    let memberId;
    try { memberId = savingGroupPathId(row?.memberId); } catch { throw invalid(); }
    if (ids.has(memberId) || !Number.isSafeInteger(row.recommendedTurnOrder)
      || row.recommendedTurnOrder < 1 || row.recommendedTurnOrder > rows.length
      || orders.has(row.recommendedTurnOrder) || typeof row.nickname !== 'string' || !row.nickname.trim()
      || !Number.isSafeInteger(row.expectedPayoutAmount) || row.expectedPayoutAmount < 1
      || row.expectedPayoutAmount > 9999999999999) throw invalid();
    ids.add(memberId); orders.add(row.recommendedTurnOrder);
    return { memberId, nickname: row.nickname, recommendedTurnOrder: row.recommendedTurnOrder,
      expectedPayoutAmount: row.expectedPayoutAmount };
  });
  return items.sort((a, b) => a.recommendedTurnOrder - b.recommendedTurnOrder);
}

// AI작성: 원문 계좌번호 없이 서버가 모임 계좌를 결정하고 같은 요청 키로 재전송을 구분한다.
export async function requestSavingGroupDeposit(savingGroupId, { amount, requestKey }, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  if (!Number.isSafeInteger(amount) || amount < 1 || amount > 9999999999999) {
    throw new Error('금액은 1원 이상의 원 단위 정수로 입력해주세요.');
  }
  validateDepositKey(requestKey);
  const result = await savingGroupMemberResponse(
    () => client.post(`/saving-groups/${id}/account/deposits`, { amount, requestKey }, { signal }),
    '입금 결과를 확인하지 못했습니다. 같은 요청으로 다시 확인해주세요.'
  );
  return toDepositResponse(result.data);
}

// AI작성: 확인값은 메모리에서만 전달하며 재시도는 동일한 요청·확인값을 사용한다.
export async function confirmSavingGroupDeposit(savingGroupId, { requestKey, confirmationToken }, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  validateDepositKey(requestKey);
  if (typeof confirmationToken !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(confirmationToken)) {
    throw new Error('입금 확인 정보를 다시 확인해주세요.');
  }
  const result = await savingGroupMemberResponse(
    () => client.post(`/saving-groups/${id}/account/deposits/confirm`, { requestKey, confirmationToken }, { signal }),
    '입금 결과를 확인하지 못했습니다. 같은 요청으로 다시 확인해주세요.'
  );
  const data = toDepositResponse(result.data);
  if (data.status !== 'COMPLETED') throw new Error('입금 완료 결과를 확인하지 못했습니다.');
  return data;
}

function validateDepositKey(key) {
  if (typeof key !== 'string' || !/^[A-Za-z0-9_-]{16,64}$/.test(key)) throw new Error('입금 요청 정보를 확인해주세요.');
}

function toDepositResponse(data) {
  // AI작성: 허용 필드만 반환하여 계좌번호·내부 식별자 등의 부가 필드를 화면으로 전달하지 않는다.
  if (!data || !Number.isSafeInteger(data.amount) || data.amount < 1 || data.amount > 9999999999999
    || !['COMPLETED', 'PENDING_CONFIRMATION'].includes(data.status)
    || (data.status === 'COMPLETED' && (typeof data.balance !== 'number' || !Number.isFinite(data.balance)))
    || (data.status === 'PENDING_CONFIRMATION' && (typeof data.confirmationToken !== 'string'
      || !/^[A-Za-z0-9_-]{43}$/.test(data.confirmationToken)
      || typeof data.confirmationExpiresAt !== 'string'
      || !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?$/.test(data.confirmationExpiresAt)))) {
    const error = new Error('입금 응답을 확인하지 못했습니다. 같은 요청으로 다시 확인해주세요.');
    error.code = 'INVALID_RESPONSE'; throw error;
  }
  return { status: data.status, amount: data.amount,
    balance: data.status === 'COMPLETED' ? data.balance : null,
    confirmationToken: data.status === 'PENDING_CONFIRMATION' ? data.confirmationToken : null,
    confirmationExpiresAt: data.status === 'PENDING_CONFIRMATION' ? data.confirmationExpiresAt : null,
    message: typeof data.message === 'string' ? data.message : '테스트 입금 처리 결과를 확인해주세요.' };
}

// AI작성: 계원 관리 요청은 공통 JWT client를 사용하고 서버 코드·상태·취소를 보존한다.
async function savingGroupMemberResponse(send, fallbackMessage) {
  try {
    const response = await send();
    if (response.data?.success !== true) throw { response };
    return response.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    const serverError = error.response?.data?.error;
    const failure = new Error(serverError?.message || fallbackMessage);
    failure.code = serverError?.code || 'SAVING_GROUP_MEMBER_REQUEST_FAILED';
    failure.status = error.response?.status;
    throw failure;
  }
}

// AI작성: 검색어를 정리해 이름·원문 이메일 검색으로 전달하고 서버의 마스킹 응답만 표시한다.
export async function getSavingGroupMembers(savingGroupId, { keyword, page = 1, signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  if (!Number.isSafeInteger(page) || page < 1) throw new Error('페이지는 1 이상이어야 합니다.');
  if (keyword != null && typeof keyword !== 'string') throw new Error('검색어를 다시 입력해주세요.');
  const normalizedKeyword = keyword?.trim() || undefined;
  if (normalizedKeyword && normalizedKeyword.length > 20) throw new Error('검색어는 20자 이내로 입력해 주세요.');
  const result = await savingGroupMemberResponse(
    () => client.get(`/saving-groups/${id}/members`, {
      params: { ...(normalizedKeyword ? { keyword: normalizedKeyword } : {}), page }, signal,
    }),
    '계원 목록을 불러오지 못했습니다. 다시 시도해주세요.'
  );
  const meta = result.meta;
  if (!Array.isArray(result.data) || result.data.length > 10 || !meta
      || meta.page !== page || meta.size !== 10 || !Number.isInteger(meta.total) || meta.total < 0 || meta.total > 20
      || meta.totalPages !== Math.ceil(meta.total / 10)
      || result.data.some((item) => !item || !['MEMBER', 'MANAGER'].includes(item.role))) {
    const failure = new Error('계원 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.');
    failure.code = 'INVALID_RESPONSE';
    throw failure;
  }
  return {
    items: result.data.map((item) => ({
      id: savingGroupPathId(item.memberId),
      name: item.name || '이름 미등록',
      email: item.email || '이메일 미등록',
      role: item.role,
      joinedAt: formatApplicationDate(item.joinedAt),
    })),
    meta,
  };
}

// AI작성: OWNER·처리자 입력은 보내지 않으며 변경된 회원의 목표 역할만 전송한다.
export async function updateSavingGroupMemberRole(savingGroupId, memberId, role, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const targetId = savingGroupPathId(memberId);
  if (!['MEMBER', 'MANAGER'].includes(role)) throw new Error('계원 또는 회계 역할만 지정할 수 있습니다.');
  const result = await savingGroupMemberResponse(
    () => client.patch(`/saving-groups/${id}/members/${targetId}/role`, { role }, { signal }),
    '역할을 저장하지 못했습니다. 목록을 다시 확인해주세요.'
  );
  return result.data;
}

// AI작성: 자진 탈퇴에는 계모임 ID만 전달하고 기존 계원 관리 응답 처리를 재사용한다.
export async function leaveSavingGroup(savingGroupId) {
  const id = savingGroupPathId(savingGroupId);
  await savingGroupMemberResponse(
    () => client.delete(`/saving-groups/${id}/members/me`),
    '계모임에서 탈퇴하지 못했습니다. 다시 시도해 주세요.'
  );
}

// AI작성: 추방은 savingGroupId를 사용하며 처리자·사유 본문 없이 기존 JWT client로 요청한다.
export async function expelSavingGroupMember(savingGroupId, memberId, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const targetId = savingGroupPathId(memberId);
  const result = await savingGroupMemberResponse(
    () => client.patch(`/saving-groups/${id}/members/${targetId}/expel`, undefined, { signal }),
    '계원을 추방하지 못했습니다. 목록을 확인한 후 다시 시도해주세요.'
  );
  return result.data;
}

// AI작성: 기존 공통 클라이언트의 인증·오류 형식을 사용하며 계좌 정보는 전송하지 않는다.
async function payoutCall(operation) {
  try {
    const response = await operation();
    if (response.data?.success !== true) throw { response };
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    const serverError = error.response?.data?.error;
    const failure = new Error(serverError?.message || '계금 요청을 처리하지 못했습니다. 요청 내역을 확인한 후 다시 시도해주세요.');
    failure.code = serverError?.code || 'PAYOUT_REQUEST_FAILED';
    failure.status = error.response?.status;
    throw failure;
  }
}

export async function getSavingGroupPayoutOptions(savingGroupId, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  return payoutCall(() => client.get(`/saving-groups/${id}/payout-requests/options`, { signal }));
}

// AI작성: 순번계는 빈 폼을 보내고 친목계만 금액·제목·영수증을 전달한다. multipart 경계는 브라우저가 생성한다.
export async function createSavingGroupPayout(savingGroupId, { amount, title, receipt } = {}, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const body = new FormData();
  if (amount != null) body.append('amount', String(amount));
  if (title != null) body.append('title', title);
  if (receipt != null) body.append('receipt', receipt);
  return payoutCall(() => client.post(`/saving-groups/${id}/payout-requests`, body, { signal }));
}

// AI작성: 목록의 savingGroupId·요청 ID만 사용하며 승인 본문이나 처리자 정보는 보내지 않는다.
export async function approveSavingGroupPayoutRequest(savingGroupId, requestId, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const targetId = savingGroupPathId(requestId);
  return payoutCall(() => client.patch(`/saving-groups/${id}/payout-requests/${targetId}/approve`, undefined, { signal }));
}

// AI작성: 서버와 동일하게 원본 입력 길이·공백을 검증하고 사유만 전달한다. 앞뒤 공백 정리는 서버가 담당한다.
export async function rejectSavingGroupPayoutRequest(savingGroupId, requestId, rejectReason, { signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const targetId = savingGroupPathId(requestId);
  if (typeof rejectReason !== 'string' || !rejectReason.trim() || rejectReason.length > 500) {
    throw new Error('거절 사유를 공백 없이 입력하고 500자 이내로 작성해주세요.');
  }
  return payoutCall(() => client.patch(`/saving-groups/${id}/payout-requests/${targetId}/reject`, { rejectReason }, { signal }));
}

// AI작성: 서버가 저장된 모임 유형에 따라 요청을 고르므로 클라이언트는 유형·처리자 정보를 보내지 않는다.
export async function getSavingGroupPayoutRequests(savingGroupId, { status, page = 1, signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  let response;
  try {
    response = await client.get(`/saving-groups/${id}/payout-requests`, {
      params: { status: status || undefined, page }, signal,
    });
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    const failure = new Error(error.response?.data?.error?.message || '요청 목록을 불러오지 못했습니다.');
    failure.code = error.response?.data?.error?.code || 'PAYOUT_REQUEST_FAILED';
    failure.status = error.response?.status;
    throw failure;
  }
  if (response.data?.success !== true) {
    const failure = new Error(response.data?.error?.message || '요청 목록을 불러오지 못했습니다.');
    failure.code = response.data?.error?.code || 'PAYOUT_REQUEST_FAILED';
    failure.status = response.status;
    throw failure;
  }
  const { data, meta } = response.data;
  if (!Array.isArray(data) || !meta || !Number.isInteger(meta.page) || meta.page < 1
    || meta.size !== 10 || !Number.isInteger(meta.total) || meta.total < 0
    || !Number.isInteger(meta.totalPages) || meta.totalPages < 0) {
    throw new Error('요청 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.');
  }
  return {
    items: data.map((item) => ({
      ...item,
      id: savingGroupPathId(item.requestId),
      applicant: item.requesterName || '이름 미등록',
      // AI작성: 증빙 유무와 회차는 실제 응답에 있는 경우에만 화면에 표시한다.
      type: item.roundNumber != null ? '계금' : '지출',
      amount: item.amount == null || !Number.isFinite(Number(item.amount))
        ? '금액 확인 필요' : `${Number(item.amount).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}원`,
      status: { PENDING: '대기', APPROVED: '승인', REJECTED: '반려', COMPLETED: '완료' }[item.status] || '상태 확인 필요',
    })),
    meta,
  };
}

// AI작성: 관리자 계모임 심사의 API 코드와 화면 문구를 한곳에서 관리해 직접 문자열 비교를 줄인다.
export const savingGroupReviewStatus = Object.freeze({
  PENDING: 'PENDING',
  APPROVED: 'APPROVED',
  REJECTED: 'REJECTED',
});

export const savingGroupReviewType = Object.freeze({
  CREATE: 'CREATE',
  DELETE: 'DELETE',
});

export const savingGroupReviewStatusLabel = {
  [savingGroupReviewStatus.PENDING]: '대기',
  [savingGroupReviewStatus.APPROVED]: '승인',
  [savingGroupReviewStatus.REJECTED]: '반려',
};

export const savingGroupReviewTypeLabel = {
  [savingGroupReviewType.CREATE]: '개설 심사',
  [savingGroupReviewType.DELETE]: '삭제 심사',
};

class SavingGroupCreationApplicationError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SavingGroupCreationApplicationError';
    this.code = code;
  }
}

function toCreationApplicationError(error, fallbackMessage) {
  if (error instanceof SavingGroupCreationApplicationError || error.code === 'ERR_CANCELED') return error;
  const responseError = error.response?.data?.error;
  return new SavingGroupCreationApplicationError(
    responseError?.code || 'SAVING_GROUP_APPLICATION_FAILED',
    responseError?.message || fallbackMessage
  );
}

// AI작성: 화면 입력명을 서버 DTO 필드명으로 맞춘 multipart 요청을 만든다. 신청자 ID는 JWT에서 서버가 설정한다.
export async function createSavingGroupApplication(formData) {
  const requestData = new FormData();
  ['groupName', 'regionName', 'maxMembers', 'description', 'groupType', 'duesAmount', 'duesDay'].forEach((field) =>
    requestData.append(field, String(formData.get(field) || '').trim())
  );
  const image = formData.get('groupImage');
  if (image instanceof File && image.size > 0) requestData.append('groupImage', image);

  try {
    const response = await client.post('/saving-group', requestData);
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_FAILED',
        response.data?.error?.message || '계모임 개설 신청에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '계모임 개설 신청에 실패했습니다. 잠시 후 다시 시도해 주세요.');
  }
}

function formatApplicationDate(value) {
  if (!value) return '날짜 확인 필요';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '날짜 확인 필요';
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit' })
    .format(date)
    .replace(/\./g, '.')
    .replace(/\s/g, '');
}

// AI작성: 관리자 심사 화면에서 개설·삭제 신청을 같은 API로 조회하고 화면 필드로 변환한다.
export async function getSavingGroupCreationApplications({ status, type, keyword, page = 1, signal } = {}) {
  try {
    const response = await client.get('/admin/saving-group-applications', {
      params: { status: status || undefined, type: type || undefined, keyword: keyword || undefined, page },
      signal,
    });
    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '심사 목록 응답을 확인하지 못했습니다.'
      );
    }
    return {
      items: response.data.data.map((item) => ({
        id: item.approvalId,
        name: item.groupName,
        applicant: item.applicantName,
        region: item.regionName,
        appliedAt: formatApplicationDate(item.appliedAt),
        status: savingGroupReviewStatusLabel[item.status] || '상태 확인 필요',
        type: item.type,
        reviewType: savingGroupReviewTypeLabel[item.type] || '유형 확인 필요',
      })),
      meta: response.data.meta,
    };
  } catch (error) {
    throw toCreationApplicationError(error, '심사 목록을 불러오지 못했습니다.');
  }
}

export async function getSavingGroupCreationApplication(approvalId, { signal } = {}) {
  try {
    const response = await client.get(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}`, { signal });
    if (!response.data?.success || !response.data.data) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'INVALID_RESPONSE',
        response.data?.error?.message || '심사 상세 응답을 확인하지 못했습니다.'
      );
    }
    return response.data.data;
  } catch (error) {
    throw toCreationApplicationError(error, '심사 상세를 불러오지 못했습니다.');
  }
}

export async function approveSavingGroupCreationApplication(approvalId) {
  try {
    // AI작성: 외부 발급의 연결·읽기 제한 시간을 고려해 이 요청만 대기 시간을 늘리고 자동 재전송하지 않는다.
    const response = await client.patch(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}/approve`, undefined, { timeout: 30000 });
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_APPROVE_FAILED',
        response.data?.error?.message || '개설 신청 승인에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '개설 신청 승인에 실패했습니다.');
  }
}

// AI작성: 계모임 공유 일정은 기존 인증 클라이언트로 조회하고 등록한다.
export async function getSavingGroupCalendar(savingGroupId, { month, signal }) {
  const id = savingGroupPathId(savingGroupId);
  try {
    const { data: response } = await client.get(`/saving-groups/${id}/calendar`, { params: { month }, signal });
    if (response?.success !== true || !Array.isArray(response.data?.events)) {
      throw new Error('일정 조회 응답을 확인할 수 없습니다.');
    }
    return response.data;
  } catch (error) {
    throw toCreationApplicationError(error, '일정을 불러오지 못했습니다.');
  }
}

export async function createSavingGroupCalendarEvent(savingGroupId, event) {
  const id = savingGroupPathId(savingGroupId);
  try {
    const { data: response } = await client.post(`/saving-groups/${id}/calendar/events`, event);
    if (response?.success !== true || !response.data?.id) {
      throw new Error('일정 등록 응답을 확인할 수 없습니다.');
    }
    return response.data;
  } catch (error) {
    throw toCreationApplicationError(error, '일정을 저장하지 못했습니다.');
  }
}

// AI작성: 일정 ID를 문자열로 유지하고 모임 범위 삭제 API를 호출한다.
export async function deleteSavingGroupCalendarEvent(savingGroupId, scheduleId) {
  const id = savingGroupPathId(savingGroupId);
  const eventId = savingGroupPathId(scheduleId);
  try {
    const { data: response } = await client.delete(`/saving-groups/${id}/calendar/events/${eventId}`);
    if (response?.success !== true) throw new Error('일정 삭제 응답을 확인할 수 없습니다.');
  } catch (error) {
    throw toCreationApplicationError(error, '일정을 삭제하지 못했습니다.');
  }
}

// AI작성: 선택 월과 페이지를 계주 전용 납부 현황 API에 전달한다.
export async function getSavingGroupPaymentStatus(savingGroupId, { month, page = 1, signal }) {
  const id = savingGroupPathId(savingGroupId);
  const { data: response } = await client.get(`/saving-groups/${id}/dues/payment-status`, {
    params: { month, page }, signal,
  });
  if (response?.success !== true || !Array.isArray(response.data) || !response.meta) {
    throw new Error('납부 현황 응답을 확인할 수 없습니다.');
  }
  return { items: response.data, meta: response.meta };
}

/** AI작성: 공용 계좌 장부의 공개 필드와 서버 페이지 정보만 화면에 전달한다. */
export async function getSavingGroupTransactions(savingGroupId, { page = 1, signal } = {}) {
  const id = savingGroupPathId(savingGroupId);
  const { data: response } = await client.get(`/saving-groups/${id}/account/transactions`, {
    params: { page }, signal,
  });
  if (response?.success !== true || !Array.isArray(response.data) || !response.meta) {
    throw new Error('거래내역 응답을 확인할 수 없습니다.');
  }
  return {
    items: response.data.map(({ transactedAt, transactionType, amount, balanceAfter }) => ({
      transactedAt, transactionType, amount, balanceAfter,
    })),
    meta: response.meta,
  };
}

// AI작성: 공개 상세와 분리된 회원 전용 API에서 마스킹 필드만 선택해 반환한다.
export async function getSavingGroupAccount(savingGroupId, { signal } = {}) {
  const pathId = savingGroupPathId(savingGroupId);
  const failure = (detail, status) => Object.assign(new Error(detail?.message || '계좌 정보를 불러오지 못했습니다.'), {
    name: 'SavingGroupAccountError', code: detail?.code || 'SAVING_GROUP_ACCOUNT_READ_FAILED', status,
  });
  try {
    const response = await client.get(`/saving-groups/${pathId}/account`, { signal });
    if (response.data?.success !== true) throw failure(response.data?.error, response.status);
    const data = response.data.data;
    if (!data || typeof data.issued !== 'boolean' || data.testAccount !== true
      || typeof data.balance !== 'number' || !Number.isFinite(data.balance)
      || (data.issued && (typeof data.bankCode !== 'string'
        || typeof data.maskedAccountNumber !== 'string' || !/^\*{4,}\d{4}$/.test(data.maskedAccountNumber)))) {
      throw failure({ code: 'INVALID_RESPONSE', message: '계좌 정보 응답을 확인할 수 없습니다.' }, response.status);
    }
    return { issued: data.issued, bankCode: data.issued ? data.bankCode : null,
      maskedAccountNumber: data.issued ? data.maskedAccountNumber : null,
      balance: data.balance, testAccount: true };
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupAccountError') throw error;
    throw failure(error.response?.data?.error, error.response?.status);
  }
}

export async function rejectSavingGroupCreationApplication(approvalId, rejectReason) {
  try {
    const response = await client.patch(`/admin/saving-group-applications/${encodeURIComponent(approvalId)}/reject`, {
      rejectReason,
    });
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_APPLICATION_REJECT_FAILED',
        response.data?.error?.message || '개설 신청 반려에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '개설 신청 반려에 실패했습니다.');
  }
}

// AI작성: 설정 화면의 내부 savingGroupId와 계주가 입력한 사유만 삭제 심사 API에 전달한다.
export async function requestSavingGroupDeletion(savingGroupId, reason) {
  try {
    const response = await client.post(`/saving-groups/${encodeURIComponent(savingGroupId)}/deletion-applications`, {
      reason,
    });
    if (!response.data?.success) {
      throw new SavingGroupCreationApplicationError(
        response.data?.error?.code || 'SAVING_GROUP_DELETION_APPLICATION_FAILED',
        response.data?.error?.message || '계모임 삭제 신청에 실패했습니다.'
      );
    }
  } catch (error) {
    throw toCreationApplicationError(error, '계모임 삭제 신청에 실패했습니다.');
  }
}

// AI작성: DB의 양수 BIGINT 범위만 허용하고 큰 문자열 ID를 Number로 바꾸지 않는다.
function savingGroupPathId(value) {
  if (
    (typeof value === 'number' && !Number.isSafeInteger(value)) ||
    !/^[1-9]\d*$/.test(String(value)) ||
    BigInt(value) > 9223372036854775807n
  ) {
    throw new SavingGroupJoinRequestError('BAD_REQUEST', '모임 또는 신청 번호가 올바르지 않습니다.');
  }
  return String(value);
}

// AI작성: 공지 작성 실패의 서버 코드·HTTP 상태를 유지해 화면에서 권한·상태 변경을 안내한다.
function toSavingGroupNoticeError(error, status) {
  const failure = new Error(error?.message || '공지를 등록하지 못했습니다. 잠시 후 다시 시도해 주세요.');
  failure.name = 'SavingGroupNoticeError';
  failure.code = error?.code || 'SAVING_GROUP_NOTICE_WRITE_FAILED';
  failure.status = status;
  return failure;
}

// AI작성: 공통 client의 JWT 인증을 재사용하고 제목·내용만 JSON으로 전송한다. 작성자·권한은 서버에서 판단한다.
export async function createSavingGroupNotice(savingGroupId, { title, content }, { signal } = {}) {
  const groupId = savingGroupPathId(savingGroupId);
  try {
    const response = await client.post(`/saving-groups/${groupId}/notices`, { title, content }, { signal });
    if (response.data?.success !== true) {
      throw toSavingGroupNoticeError(response.data?.error, response.status);
    }
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupNoticeError') throw error;
    throw toSavingGroupNoticeError(error.response?.data?.error, error.response?.status);
  }
}

// AI작성: 고정 변경 오류의 서버 코드·문구·상태를 유지해 권한 변경과 최대 개수 초과를 안내한다.
// AI작성: 수정 오류를 작성·고정 오류와 구분하고 서버 코드·상태 및 취소 원본을 유지한다.
export async function updateSavingGroupNotice(savingGroupId, noticeId, { title, content }, { signal } = {}) {
  const savingGroupPath = savingGroupPathId(savingGroupId);
  const noticePathId = savingGroupPathId(noticeId);
  try {
    const response = await client.put(`/saving-groups/${savingGroupPath}/notices/${noticePathId}`, { title, content }, { signal });
    if (response.data?.success !== true) throw { response };
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    const serverError = error.response?.data?.error;
    const failure = new Error(serverError?.message || '공지를 수정하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    failure.code = serverError?.code || 'SAVING_GROUP_NOTICE_EDIT_FAILED';
    failure.status = error.response?.status;
    throw failure;
  }
}

// AI작성: 작성자는 요청으로 받지 않고 공통 인증 client로 삭제하며 오류·취소를 보존한다.
export async function deleteSavingGroupNotice(savingGroupId, noticeId, { signal } = {}) {
  const savingGroupPath = savingGroupPathId(savingGroupId);
  const noticePathId = savingGroupPathId(noticeId);
  try {
    const response = await client.delete(`/saving-groups/${savingGroupPath}/notices/${noticePathId}`, { signal });
    if (response.data?.success !== true) throw { response };
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    const serverError = error.response?.data?.error;
    const failure = new Error(serverError?.message || '공지를 삭제하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    failure.code = serverError?.code || 'SAVING_GROUP_NOTICE_DELETE_FAILED';
    failure.status = error.response?.status;
    throw failure;
  }
}

function toSavingGroupNoticePinError(error, status) {
  const failure = new Error(error?.message || '공지 고정을 변경하지 못했습니다. 목록을 새로 고침해 주세요.');
  failure.name = 'SavingGroupNoticePinError';
  failure.code = error?.code || 'SAVING_GROUP_NOTICE_PIN_FAILED';
  failure.status = status;
  return failure;
}

// AI작성: 토글 대신 목표 상태를 전달해 응답 유실 후 재요청에서도 상태가 뒤집히지 않게 한다.
export async function updateSavingGroupNoticePinned(savingGroupId, noticeId, pinned, { signal } = {}) {
  const savingGroupPath = savingGroupPathId(savingGroupId);
  const noticePathId = savingGroupPathId(noticeId);
  if (typeof pinned !== 'boolean') throw toSavingGroupNoticePinError({ code: 'BAD_REQUEST', message: '고정 여부를 확인해 주세요.' }, 400);
  try {
    const response = await client.patch(`/saving-groups/${savingGroupPath}/notices/${noticePathId}/pin`, { pinned }, { signal });
    if (response.data?.success !== true) throw toSavingGroupNoticePinError(response.data?.error, response.status);
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupNoticePinError') throw error;
    throw toSavingGroupNoticePinError(error.response?.data?.error, error.response?.status);
  }
}

// AI작성: 조회 오류는 작성 오류와 구분하고 서버 코드·문구·HTTP 상태를 유지한다.
function toSavingGroupNoticeDetailError(error, status) {
  const failure = new Error(error?.message || '공지를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');
  failure.name = 'SavingGroupNoticeDetailError';
  failure.code = error?.code || 'SAVING_GROUP_NOTICE_READ_FAILED';
  failure.status = status;
  return failure;
}

// AI작성: 공통 groupId와 공지 ID를 문자열로 유지하며 인증·요청 취소는 기존 client를 재사용한다.
export async function getSavingGroupNoticeDetail(savingGroupId, noticeId, { signal } = {}) {
  const savingGroupPath = savingGroupPathId(savingGroupId);
  const noticePathId = savingGroupPathId(noticeId);
  try {
    const response = await client.get(`/saving-groups/${savingGroupPath}/notices/${noticePathId}`, { signal });
    if (response.data?.success !== true) {
      throw toSavingGroupNoticeDetailError(response.data?.error, response.status);
    }
    if (!response.data.data || typeof response.data.data !== 'object' || Array.isArray(response.data.data)) {
      throw toSavingGroupNoticeDetailError(
        { code: 'INVALID_RESPONSE', message: '공지 상세 응답을 확인하지 못했습니다.' }, response.status
      );
    }
    return response.data.data;
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupNoticeDetailError') throw error;
    throw toSavingGroupNoticeDetailError(error.response?.data?.error, error.response?.status);
  }
}

// AI작성: 목록·승인·거절 모두 서버의 오류 코드를 보존하며 요청 취소는 화면 오류로 바꾸지 않는다.
async function savingGroupJoinResponse(request, fallback) {
  try {
    const response = await request();
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || fallback
      );
    }
    return response.data;
  } catch (error) {
    if (error instanceof SavingGroupJoinRequestError || error.code === 'ERR_CANCELED') throw error;
    throw new SavingGroupJoinRequestError(
      error.response?.data?.error?.code || 'JOIN_REQUEST_FAILED',
      error.response?.data?.error?.message || fallback
    );
  }
}

// AI작성: 실제 신청 목록만 조회하며 공통 client가 JWT를 자동으로 첨부한다. Mock으로 대체하지 않는다.
export async function getSavingGroupJoinRequests(savingGroupId, { status, page = 1, signal } = {}) {
  const groupId = savingGroupPathId(savingGroupId);
  const result = await savingGroupJoinResponse(
    () =>
      client.get(`/saving-groups/${groupId}/join-requests`, {
        params: { status: status || undefined, page },
        signal,
      }),
    '가입 신청 목록을 불러오지 못했습니다.'
  );
  // AI작성: 잘못된 성공 응답을 빈 목록으로 위장하거나 기술 오류 문구로 노출하지 않는다.
  if (!Array.isArray(result.data) || !result.meta || !Number.isInteger(result.meta.totalPages)) {
    throw new SavingGroupJoinRequestError(
      'INVALID_RESPONSE',
      '가입 신청 목록 응답을 확인하지 못했습니다. 다시 시도해주세요.'
    );
  }
  return {
    items: result.data.map((item) => ({
      id: savingGroupPathId(item.crewMemberId),
      applicant: item.applicant || '이름 미등록',
      verification:
        item.identityVerified && item.regionVerified
          ? '본인·지역 인증 완료'
          : item.identityVerified
            ? '본인 인증 완료'
            : item.regionVerified
              ? '지역 인증 완료'
              : '인증 미완료',
      regionInterest: item.regionName || '지역 미등록',
      message: item.applicationMessage || '신청 메시지가 없습니다.',
      status: { PENDING: '대기', APPROVED: '승인', REJECTED: '거절' }[item.membershipStatus] || '상태 확인 필요',
    })),
    meta: result.meta,
  };
}

// AI작성: 승인자·역할·본문을 보내지 않고 서버가 반환한 실제 신청 ID만 사용한다.
export async function approveSavingGroupJoin(savingGroupId, crewMemberId) {
  const groupId = savingGroupPathId(savingGroupId);
  const requestId = savingGroupPathId(crewMemberId);
  return savingGroupJoinResponse(
    () => client.patch(`/saving-groups/${groupId}/join-requests/${requestId}/approve`),
    '가입 신청 승인에 실패했습니다.'
  );
}

// AI작성: 거절 처리자·사유·상태를 보내지 않고 서버가 반환한 실제 신청 ID만 사용한다.
export async function rejectSavingGroupJoin(savingGroupId, crewMemberId) {
  const groupId = savingGroupPathId(savingGroupId);
  const requestId = savingGroupPathId(crewMemberId);
  return savingGroupJoinResponse(
    () => client.patch(`/saving-groups/${groupId}/join-requests/${requestId}/reject`),
    '가입 신청 거절에 실패했습니다.'
  );
}

class SavingGroupJoinRequestError extends Error {
  constructor(code, message) {
    super(message);
    this.name = 'SavingGroupJoinRequestError';
    this.code = code;
  }
}

// AI작성: API 지역 코드를 화면 이름과 연결하며 DB ID로 해석하지 않는다.
export const savingGroupRegionOptions = [
  { code: '01', name: '서울' },
  { code: '02', name: '경기' },
  { code: '03', name: '강원' },
  { code: '04', name: '충북' },
  { code: '05', name: '충남' },
  { code: '06', name: '전북' },
  { code: '07', name: '전남' },
  { code: '08', name: '경북' },
  { code: '09', name: '경남' },
  { code: '10', name: '제주도' },
];

// AI작성: 조회 데이터는 수정하지 않고 기존 카드의 표시용 필드로 변환한다.
export function toSavingGroupCard(item) {
  const amount = item.duesAmount == null ? null : Number(item.duesAmount);
  const dues =
    amount == null
      ? '회비 미정'
      : !Number.isSafeInteger(amount)
        ? '회비 확인 필요'
        : `월 ${amount.toLocaleString('ko-KR')}원`;
  return {
    // AI작성: 상세 주소는 계모임 고유 ID로 이동한다.
    id: item.savingGroupId,
    name: item.groupName,
    description: item.description?.trim() || '소개글이 없습니다.',
    region: item.regionName,
    district: item.regionName,
    status: { '01': '모집중', '02': '진행중' }[item.status] || '상태 확인 필요',
    members:
      item.maxMembers == null ? `${item.memberCount}명 / 정원 미정` : `${item.memberCount} / ${item.maxMembers}명`,
    monthlyDues: dues,
    nextDate: item.duesDay == null ? '미정' : `매월 ${item.duesDay}일`,
  };
}

// AI작성: 회원 역할은 서버 응답만 사용하며 승인되지 않은 사용자는 공지만 표시한다.
export function getSavingGroupMenus(role) {
  const common = ['notice', 'calendar', 'dues', 'transactionHistory', 'payoutRequest'];
  if (role === 'OWNER') return [common[0], 'payment', ...common.slice(1), 'members', 'settings', 'joinRequests'];
  return role === 'MANAGER' || role === 'MEMBER' ? common : ['notice'];
}

// AI작성: null을 0원으로 바꾸거나 소수 금액을 반올림하지 않고 표시 직전에 포맷한다.
export function formatSavingGroupAmount(value) {
  if (value == null) return '회비 미정';
  const amount = Number(value);
  if (!Number.isFinite(amount) || amount < 0) return '회비 확인 필요';
  return `${amount.toLocaleString('ko-KR', { maximumFractionDigits: 2 })}원`;
}

// AI작성: URL ID를 숫자로 변환하지 않으며 기존 API 클라이언트와 취소 신호를 재사용한다.
export async function getSavingGroupDetail(id, { signal, noticePage = 1, noticeSort = 'LATEST' } = {}) {
  // AI작성: 기존 호출은 최신순 1페이지로 유지하고 공지 조건만 기존 상세 API 쿼리로 전달한다.
  const response = await client.get(`/saving-groups/${encodeURIComponent(id)}`, {
    params: { noticePage, noticeSort }, signal,
  });
  if (!response.data.success) {
    throw new Error(response.data.error?.message || '계모임 정보를 불러오지 못했습니다.');
  }
  // AI작성: 기존 상세 필드를 유지하면서 공통 meta를 공지 전용 이름으로 화면에 전달한다.
  return { ...response.data.data, noticePageMeta: response.data.meta ?? null };
}

// AI작성: 직접 입력한 URL에서도 서버의 정수 범위와 허용 정렬만 사용하고 나머지는 기본값으로 복원한다.
export function readSavingGroupNoticeQuery(search) {
  const params = new URLSearchParams(search);
  const rawPage = params.get('noticePage') ?? '1';
  const page = /^[1-9]\d*$/.test(rawPage) && Number(rawPage) <= 2147483647 ? Number(rawPage) : 1;
  return { page, sort: params.get('noticeSort') === 'OLDEST' ? 'OLDEST' : 'LATEST' };
}

// AI작성: 공지 조회 조건만 변경해 같은 화면의 다른 쿼리는 유지한다.
export function toSavingGroupNoticeSearch(page, sort, search = '') {
  const params = new URLSearchParams(search);
  params.set('noticePage', String(page));
  params.set('noticeSort', sort);
  return params.toString();
}

// AI작성: 공지 전용 세 페이지 묶음이며 이전은 앞 묶음 끝, 다음은 뒤 묶음 시작으로 이동한다.
export function getSavingGroupNoticePagination(page, totalPages) {
  const total = Math.max(1, totalPages);
  const current = Math.min(Math.max(1, page), total);
  const start = Math.floor((current - 1) / 3) * 3 + 1;
  const end = Math.min(start + 2, total);
  return {
    pages: Array.from({ length: end - start + 1 }, (_, index) => start + index),
    previous: start > 1 ? start - 1 : null,
    next: end < total ? end + 1 : null,
  };
}

// AI작성: 계모임 전체 목록의 기존 다섯 페이지 묶음은 공지와 별개로 유지한다.
export function getSavingGroupPages(currentPage, totalPages) {
  if (totalPages < 1 || currentPage < 1 || currentPage > totalPages) return [];
  const start = Math.floor((currentPage - 1) / 5) * 5 + 1;
  return Array.from({ length: Math.min(5, totalPages - start + 1) }, (_, index) => start + index);
}

// AI작성: 공통 응답 실패를 검사하고 공개 조회에는 별도 토큰을 요구하지 않는다.
export async function getSavingGroups({ keyword, regionCode, provinceName, districtName, townName, page, signal }) {
  // AI작성: 미선택 단계는 생략하되 세종의 빈 구 이름은 보존하고 기존 호출 형태도 유지한다.
  const areaParams = {};
  if (provinceName) areaParams.provinceName = provinceName;
  if (districtName != null) areaParams.districtName = districtName;
  if (townName) areaParams.townName = townName;
  const response = await client.get('/saving-groups', {
    params: { keyword: keyword || undefined, regionCode: regionCode || undefined, ...areaParams, page },
    signal,
  });
  if (!response.data.success) {
    throw new Error(response.data.error?.message || '목록을 불러오지 못했습니다.');
  }
  // AI작성: 공통 data/meta 응답을 기존 목록 화면의 상태 필드로 변환한다.
  const { data, meta } = response.data;
  return {
    items: data.map(toSavingGroupCard),
    totalElements: meta.total,
    totalPages: meta.totalPages,
    currentPage: meta.page,
    pageSize: meta.size,
  };
}

function toNullableNumber(value) {
  if (value == null) return null;
  const number = Number(value);
  return Number.isFinite(number) ? number : null;
}

function toSavingGroupSettingsError(error, fallbackMessage, status) {
  const failure = new Error(error?.message || fallbackMessage);
  failure.name = 'SavingGroupSettingsError';
  failure.code = error?.code;
  failure.status = status;
  return failure;
}

export function toSavingGroupSettings(data) {
  const current = data.current;
  const pending = data.pending;

  return {
    id: data.savingGroupId,
    status: data.status,
    canAccessSettings: data.canAccessSettings,
    canEditSettings: data.canEditSettings,
    current:
      current == null
        ? null
        : {
            groupName: current.groupName ?? '',
            description: current.description ?? '',
            maxMembers: toNullableNumber(current.maxMembers),
            approvedMemberCount: toNullableNumber(current.approvedMemberCount),
            duesAmount: toNullableNumber(current.duesAmount),
            duesDay: toNullableNumber(current.duesDay),
          },
    pending:
      pending == null
        ? null
        : {
            duesAmount: toNullableNumber(pending.duesAmount),
            duesDay: toNullableNumber(pending.duesDay),
            effectiveAt: pending.effectiveAt,
            status: pending.status,
          },
    editableFields: Array.isArray(data.editableFields) ? data.editableFields : [],
  };
}

export async function getSavingGroupSettings(savingGroupId, { signal } = {}) {
  try {
    const response = await client.get(`/saving-groups/${savingGroupId}/settings`, { signal });
    if (!response.data.success || !response.data.data) {
      throw toSavingGroupSettingsError(response.data.error, '계모임 설정 정보를 불러오지 못했습니다.', response.status);
    }
    return toSavingGroupSettings(response.data.data);
  } catch (error) {
    if (error.code === 'ERR_CANCELED' || error.name === 'SavingGroupSettingsError') {
      throw error;
    }
    throw toSavingGroupSettingsError(
      error.response?.data?.error,
      '계모임 설정 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.',
      error.response?.status
    );
  }
}

export async function updateSavingGroupSettings(savingGroupId, changedFields) {
  try {
    const response = await client.patch(`/saving-groups/${savingGroupId}`, changedFields);
    if (!response.data.success) {
      throw toSavingGroupSettingsError(response.data.error, '계모임 설정을 저장하지 못했습니다.', response.status);
    }
    return response.data.data;
  } catch (error) {
    if (error.name === 'SavingGroupSettingsError') throw error;
    throw toSavingGroupSettingsError(
      error.response?.data?.error,
      '계모임 설정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      error.response?.status
    );
  }
}

// AI작성: 가입 신청은 별도 입력값 없이 계모임 ID만 경로에 담아 요청하고 서버 오류 코드를 화면에 전달한다.
export async function requestSavingGroupJoin(savingGroupId) {
  try {
    const response = await client.post(`/saving-groups/${savingGroupId}/join-requests`);
    if (!response.data?.success) {
      throw new SavingGroupJoinRequestError(
        response.data?.error?.code || 'JOIN_REQUEST_FAILED',
        response.data?.error?.message || '가입 신청 처리에 실패했습니다.'
      );
    }
    return response.data;
  } catch (error) {
    if (error instanceof SavingGroupJoinRequestError) {
      throw error;
    }
    const responseError = error.response?.data?.error;
    throw new SavingGroupJoinRequestError(
      responseError?.code || 'JOIN_REQUEST_FAILED',
      responseError?.message || '가입 신청 처리에 실패했습니다.'
    );
  }
}
