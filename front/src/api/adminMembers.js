import client from './client.js';

function toAdminMemberError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminMemberError';
  failure.code = responseError?.code || 'ADMIN_MEMBER_REQUEST_FAILED';
  return failure;
}

function formatDate(value) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  return new Intl.DateTimeFormat('ko-KR').format(date);
}

function formatStatus(status) {
  if (status === 'ACTIVE') return '정상';
  if (status === 'SUSPENDED') return '제재';
  if (status === 'INACTIVE') return '탈퇴';
  return '확인 필요';
}

// AI작성: 관리자 화면에서도 개인정보 노출을 줄이기 위해 표시 직전에 마스킹한다.
function maskName(value) {
  const name = String(value || '');
  if (!name) return '-';
  if (name.length === 1) return '*';
  if (name.length === 2) return `${name[0]}*`;
  return `${name[0]}${'*'.repeat(name.length - 2)}${name.at(-1)}`;
}

function maskEmail(value) {
  const [localPart, domain] = String(value || '').split('@');
  if (!localPart || !domain) return '-';
  return `${localPart[0]}***@${domain}`;
}

function maskPhone(value) {
  const digits = String(value || '').replace(/\D/g, '');
  if (digits.length < 7) return '-';
  return `${digits.slice(0, 3)}-****-${digits.slice(-4)}`;
}

function mapMember(member) {
  return {
    id: member.memberId,
    name: maskName(member.name),
    email: maskEmail(member.email),
    region: member.regionName,
    verified: member.identityVerifiedAt && member.regionVerifiedAt ? '인증 완료' : '인증 필요',
    joinedAt: formatDate(member.createdAt),
    status: formatStatus(member.accountStatus),
    accountStatus: member.accountStatus,
  };
}

// AI작성: 관리자 회원 목록 API 응답을 기존 테이블 표시 형식으로 변환한다.
export async function getAdminMembers({ keyword, regionName, accountStatus, page = 1, size = 10, signal } = {}) {
  try {
    const response = await client.get('/admin/members', {
      params: {
        keyword: keyword || undefined,
        regionName: regionName || undefined,
        accountStatus: accountStatus || undefined,
        page,
        size,
      },
      signal,
    });
    if (!response.data?.success || !Array.isArray(response.data.data) || !response.data.meta) {
      throw new Error('회원 목록 응답을 확인할 수 없습니다.');
    }
    return { items: response.data.data.map(mapMember), meta: response.data.meta };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminMemberError(error, '회원 목록을 불러오지 못했습니다.');
  }
}

// AI작성: 상세 화면에서 사용하는 이름과 활동 집계 형태로 서버 응답을 정리한다.
export async function getAdminMember(memberId, signal) {
  try {
    const response = await client.get(`/admin/members/${encodeURIComponent(memberId)}`, { signal });
    if (!response.data?.success || !response.data.data) {
      throw new Error('회원 상세 응답을 확인할 수 없습니다.');
    }
    const data = response.data.data;
    return {
      member: mapMember(data),
      detail: {
        phone: maskPhone(data.phone),
        address: data.regionName,
        identityStatus: data.identityVerifiedAt ? '인증 완료' : '미인증',
        regionStatus: data.regionVerifiedAt ? '인증 완료' : '미인증',
        joinedGatherings: data.joinedGroupCount ?? 0,
        writtenPosts: data.writtenPostCount ?? 0,
        suspendedUntil: data.suspendedUntil ? formatDate(data.suspendedUntil) : null,
      },
    };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminMemberError(error, '회원 상세 정보를 불러오지 못했습니다.');
  }
}

export async function restrictAdminMember(memberId, restrictionType, reason) {
  try {
    const response = await client.patch(`/admin/members/${encodeURIComponent(memberId)}/restriction`, { restrictionType, reason });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('회원 이용 제한 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminMemberError(error, '회원 이용 제한을 적용하지 못했습니다.');
  }
}

export async function releaseAdminMemberRestriction(memberId, reason) {
  try {
    const response = await client.patch(`/admin/members/${encodeURIComponent(memberId)}/restriction/release`, { reason });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('회원 이용 제한 해제 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminMemberError(error, '회원 이용 제한을 해제하지 못했습니다.');
  }
}
