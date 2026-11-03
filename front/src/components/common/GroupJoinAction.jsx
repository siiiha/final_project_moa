import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from './Button';

// AI작성: 상태 문구와 제한 판단만 공유하며 각 화면의 기존 버튼 스타일은 변경하지 않는다.
const statusLabels = {
  APPROVED: '가입 완료',
  PENDING: '심사 중',
  REJECTED: '재신청 불가',
  EXPELLED: '재가입 불가',
};
const statusMessages = {
  APPROVED: '이미 가입한 모임입니다.',
  PENDING: '가입 심사가 진행 중입니다.',
  REJECTED: '가입 신청이 거절되어 이 모임에 다시 신청할 수 없습니다.',
  EXPELLED: '추방된 모임에는 다시 가입 신청할 수 없습니다.',
};
const restrictionLabels = {
  UNAUTHORIZED: '로그인 후 신청',
  LOGIN_REQUIRED: '로그인 후 신청',
  IDENTITY_VERIFICATION_REQUIRED: '본인 인증하기',
  ALREADY_JOINED_SAVING_GROUP: '가입 완료',
  ALREADY_JOINED_SMALL_GROUP: '가입 완료',
  JOIN_REQUEST_ALREADY_PENDING: '심사 중',
  JOIN_REQUEST_DUPLICATED: '심사 중',
  JOIN_REAPPLICATION_FORBIDDEN: '재신청 불가',
  SAVING_GROUP_JOIN_UNAVAILABLE: '모집 마감',
  SAVING_GROUP_CAPACITY_FULL: '정원 마감',
  SMALL_GROUP_CAPACITY_FULL: '정원 마감',
  SAVING_GROUP_CAPACITY_NOT_CONFIGURED: '정원 확인 필요',
  SMALL_GROUP_CAPACITY_NOT_CONFIGURED: '정원 확인 필요',
  SMALL_GROUP_MEETING_ENDED: '일정 종료',
  SMALL_GROUP_DATE_NOT_CONFIGURED: '일정 미설정',
};

// AI작성: 제한 안내는 버튼 바깥에 표시해 문구 길이가 버튼 영역을 넓히지 않게 한다.
const Reason = styled.p`
  margin: 8px 0 0;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.6;
  overflow-wrap: anywhere;
`;

// AI작성: 버튼과 별도 안내에서 같은 상태 판단을 재사용한다.
function getGroupJoinState(group, isSubmitting = false) {
  const statusLabel = statusLabels[group.membershipStatus];
  const canApply = !statusLabel && group.canApply === true;
  const code = group.joinRestrictionCode;
  const requiresLogin = !statusLabel && !canApply && ['UNAUTHORIZED', 'LOGIN_REQUIRED'].includes(code);
  const requiresVerification = !statusLabel && !canApply && code === 'IDENTITY_VERIFICATION_REQUIRED';
  const hasDecision = typeof group.canApply === 'boolean';

  return {
    canApply,
    requiresLogin,
    requiresVerification,
    disabled: isSubmitting || (!canApply && !requiresLogin && !requiresVerification),
    label: isSubmitting ? '가입 신청 중...'
      : statusLabel || (canApply ? '가입 신청하기'
        : restrictionLabels[code] || (hasDecision ? '신청 불가' : '상태 확인 필요')),
    message: isSubmitting ? '가입 신청을 처리하고 있습니다.'
      : statusMessages[group.membershipStatus] || (canApply
        ? (group.membershipStatus === 'LEFT' ? '다시 신청하면 가입 심사가 진행됩니다.' : '')
        : group.joinRestrictionMessage || (hasDecision
          ? '현재 가입 신청할 수 없습니다.' : '가입 신청 상태를 확인하지 못했습니다. 화면을 새로고침해 주세요.')),
  };
}

// AI작성: 두 화면이 각자 보유한 JoinButton을 받아 치수·여백·모바일 스타일을 그대로 사용한다.
function GroupJoinAction({ group, onJoinRequest, isSubmitting = false, ButtonComponent = Button, reasonId }) {
  const navigate = useNavigate();
  const location = useLocation();
  const state = getGroupJoinState(group, isSubmitting);

  const handleClick = () => {
    if (state.disabled) return;
    if (state.requiresLogin) {
      const redirect = `${location.pathname}${location.search}`;
      navigate(`/member/login?redirect=${encodeURIComponent(redirect)}`);
    } else if (state.requiresVerification) {
      navigate('/member/verify');
    } else if (state.canApply) {
      onJoinRequest();
    }
  };

  return (
    <ButtonComponent
      type="button"
      onClick={handleClick}
      disabled={state.disabled}
      aria-busy={isSubmitting}
      aria-describedby={state.message ? reasonId : undefined}
    >
      {state.label}
    </ButtonComponent>
  );
}

export function GroupJoinReason({ group, isSubmitting = false, id }) {
  const { message } = getGroupJoinState(group, isSubmitting);
  return message ? <Reason id={id} role="status" aria-live="polite">{message}</Reason> : null;
}

export default GroupJoinAction;
