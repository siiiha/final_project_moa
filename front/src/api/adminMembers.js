import client from './client.js';
import { formatAdminDate } from './adminFormatters.js';

function toAdminMemberError(error, fallbackMessage) {
  const responseError = error.response?.data?.error;
  const failure = new Error(responseError?.message || fallbackMessage);
  failure.name = 'AdminMemberError';
  failure.code = responseError?.code || 'ADMIN_MEMBER_REQUEST_FAILED';
  return failure;
}

function formatStatus(status, hasActiveRestriction = false) {
  if (status === 'INACTIVE') return '탈퇴';
  if (status === 'SUSPENDED') return '이용 정지';
  if (status === 'ACTIVE' && hasActiveRestriction) return '제재';
  if (status === 'ACTIVE') return '정상';
  return '확인 필요';
}

function formatRestrictionScope(scope) {
  return {
    CONTENT: '게시글·댓글',
    CHAT: '채팅',
    REPORT: '신고',
  }[scope] || '확인 필요';
}

function maskPhone(value) {
  const digits = String(value || '').replace(/\D/g, '');
  if (digits.length < 7) return '-';
  return `${digits.slice(0, 3)}-****-${digits.slice(-4)}`;
}

function mapMember(member) {
  // AI작성: 목록 응답의 쉼표 구분 제한 영역을 기존 화면 표시 문구 배열로 변환한다.
  const restrictionScopes = String(member.activeRestrictionScopes || '')
    .split(',')
    .filter(Boolean)
    .map(formatRestrictionScope);

  return {
    id: member.memberId,
    // AI작성: 관리자 회원 식별을 위해 서버가 반환한 이름과 이메일 원문을 그대로 표시한다.
    name: member.name || '-',
    email: member.email || '-',
    region: member.regionName,
    verified: member.identityVerifiedAt && member.regionVerifiedAt ? '인증 완료' : '인증 필요',
    joinedAt: formatAdminDate(member.createdAt, { fallback: '-', compact: false }),
    // AI작성: 계정 이용 정지를 우선 표시하고 ACTIVE 회원의 기능 제한은 제재로 구분한다.
    status: formatStatus(member.accountStatus, restrictionScopes.length > 0),
    accountStatus: member.accountStatus,
    // AI작성: 관리자 계정의 이용 제한 UI를 비활성화할 수 있도록 시스템 역할을 보존한다.
    systemRole: member.systemRole,
    restrictionScopes,
  };
}

// AI작성: 관리자 회원 목록 API 응답을 기존 테이블 표시 형식으로 변환한다.
export async function getAdminMembers({
  keyword,
  provinceName,
  districtName,
  townName,
  accountStatus,
  hasActiveRestriction,
  restrictionScope,
  page = 1,
  size = 10,
  signal,
} = {}) {
  try {
    const response = await client.get('/admin/members', {
      params: {
        keyword: keyword || undefined,
        provinceName: provinceName || undefined,
        districtName: districtName || undefined,
        townName: townName || undefined,
        accountStatus: accountStatus || undefined,
        hasActiveRestriction,
        restrictionScope: restrictionScope || undefined,
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
        // AI작성: 단일 계정 정지 시각 대신 현재 적용 중인 기능별 제한 목록을 화면 형식으로 변환한다.
        restrictions: Array.isArray(data.restrictions)
          ? data.restrictions.map((restriction) => ({
              scope: restriction.restrictionScope,
              scopeLabel: formatRestrictionScope(restriction.restrictionScope),
              untilLabel: restriction.restrictedUntil
                ? formatAdminDate(restriction.restrictedUntil, { fallback: '-', compact: false })
                : '영구',
            }))
          : [],
      },
    };
  } catch (error) {
    if (error.code === 'ERR_CANCELED') throw error;
    throw toAdminMemberError(error, '회원 상세 정보를 불러오지 못했습니다.');
  }
}

export async function restrictAdminMember(memberId, restrictionScope, restrictionType, reason) {
  try {
    const response = await client.patch(`/admin/members/${encodeURIComponent(memberId)}/restriction`, {
      restrictionScope,
      restrictionType,
      reason,
    });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('회원 이용 제한 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminMemberError(error, '회원 이용 제한을 적용하지 못했습니다.');
  }
}

export async function releaseAdminMemberRestriction(memberId, restrictionScope, reason) {
  try {
    const response = await client.patch(`/admin/members/${encodeURIComponent(memberId)}/restriction/release`, {
      restrictionScope,
      reason,
    });
    // AI작성: HTTP 성공 상태만으로 처리 완료를 판단하지 않고 공통 성공 응답을 확인한다.
    if (!response.data?.success) {
      throw new Error('회원 이용 제한 해제 응답을 확인할 수 없습니다.');
    }
  } catch (error) {
    throw toAdminMemberError(error, '회원 이용 제한을 해제하지 못했습니다.');
  }
}
