import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import VerificationStatusList from '../components/member/VerificationStatusList';
import { memberMenuItems } from '../mocks/mypage';
import {
  getMyProfile,
  startMyIdentityVerification,
  completeMyIdentityVerification,
} from '../api/member';
import * as PortOne from '@portone/browser-sdk/v2';
import useAuthStore from '../store/authStore';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 216px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

// AI작성: 인증 진행 결과를 구분된 안내 박스로 표시합니다.
const VerificationNotice = styled.div`
  margin-bottom: 16px;
  padding: 16px 18px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-left: 4px solid ${({ $warning, theme }) =>
    $warning ? theme.colors.warning : theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ $warning, theme }) =>
    $warning ? theme.colors.warningSoft : theme.colors.primarySoft};
  color: ${({ theme }) => theme.colors.text};
  font-size: 14px;
  line-height: 1.7;
  word-break: keep-all;

  strong {
    display: block;
    margin-bottom: 4px;
    font-weight: 700;
  }
`;

function VerificationPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('verify');
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);

  const [profile, setProfile] = useState(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationError, setVerificationError] = useState('');
  const [verificationMessage, setVerificationMessage] = useState('');

  // AI작성: 테스트용 표시 대신 로그인 회원의 실제 인증 상태를 조회합니다.
  useEffect(() => {
    let cancelled = false;

    async function loadProfile() {
      if (!token) {
        navigate('/member/login', { replace: true });
        return;
      }

      setIsProfileLoading(true);
      setProfileError('');

      try {
        const data = await getMyProfile(token);
        if (!cancelled) setProfile(data);
      } catch (error) {
        if (cancelled) return;

        if (error.response?.status === 401) {
          clearToken();
          navigate('/member/login', { replace: true });
          return;
        }

        setProfileError(
          error.response?.data?.error?.message ||
          '인증 정보를 불러오지 못했습니다. 다시 시도해주세요.',
        );
      } finally {
        if (!cancelled) setIsProfileLoading(false);
      }
    }

    loadProfile();
    return () => {
      cancelled = true;
    };
  }, [token, clearToken, navigate]);

  const verificationItems = [
    {
      id: 'identity',
      title: '본인 인증',
      status: profile?.identityVerified ? '인증 완료' : '미인증',
      description: '본인 명의의 인증 수단으로 본인 여부를 확인합니다.',
      actionLabel: isVerifying
        ? '인증 진행 중…'
        : profile?.identityVerified
          ? '인증 완료'
          : '본인인증 시작',
      disabled: isVerifying || Boolean(profile?.identityVerified),
    },
    {
      id: 'region',
      title: '지역 인증',
      status: profile?.regionVerified ? '인증 완료' : '미인증',
      description: '가입한 활동 지역과 현재 위치를 확인합니다.',
      actionLabel: '지역 인증 관리',
    },
  ];

  // AI작성: 서버 발급 번호로 인증창을 열고 서버 검증 후 화면을 갱신합니다.
  const handleVerificationAction = async (id) => {
    if (id !== 'identity' || isVerifying || !profile) return;
    if (profile.identityVerified) return;

    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    const authVersion = useAuthStore.getState().authVersion;
    const isCurrentLogin = () =>
      useAuthStore.getState().authVersion === authVersion;

    setIsVerifying(true);
    setVerificationError('');
    setVerificationMessage('');

    try {
      const start = await startMyIdentityVerification(token);
      if (!isCurrentLogin()) return;

      if (!start?.identityVerificationId || !start.storeId || !start.channelKey) {
        throw new Error('본인인증 시작 정보를 확인할 수 없습니다.');
      }

      const result = await PortOne.requestIdentityVerification({
        storeId: start.storeId,
        channelKey: start.channelKey,
        identityVerificationId: start.identityVerificationId,
        bypass: {
          inicisUnified: {
            flgFixedUser: 'N',
          },
        },
      });

      if (!isCurrentLogin()) return;

      if (!result) {
        setVerificationMessage('본인인증이 취소되었습니다.');
        return;
      }

      if (result.code !== undefined) {
        setVerificationError(
          result.message || '본인인증을 완료하지 못했습니다.',
        );
        return;
      }

      if (result.identityVerificationId !== start.identityVerificationId) {
        throw new Error('본인인증 요청 정보가 일치하지 않습니다.');
      }

      const updatedProfile = await completeMyIdentityVerification(
        useAuthStore.getState().token,
        start.identityVerificationId,
      );
      if (!isCurrentLogin()) return;

      if (!updatedProfile?.identityVerified) {
        throw new Error('본인인증 완료 상태를 확인할 수 없습니다.');
      }

      setProfile(updatedProfile);
      setVerificationMessage('본인인증이 완료되었습니다.');
    } catch (error) {
      if (!isCurrentLogin()) return;

      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setVerificationError(
        error.response?.data?.error?.message ||
        error.message ||
        '본인인증 중 문제가 발생했습니다. 다시 시도해주세요.',
      );
    } finally {
      setIsVerifying(false);
    }
  };

  return (
    <Page>
      <Heading>
        <h1>인증관리</h1>
        <p>인증 상태를 확인하고 필요한 인증을 진행하세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
                    {isVerifying && (
            <VerificationNotice role="status">
              <strong>본인인증을 진행하고 있어요</strong>
              인증창에서 인증을 완료해주세요.
            </VerificationNotice>
          )}

          {verificationError && (
            <VerificationNotice $warning role="alert">
              <strong>본인인증이 완료되지 않았어요</strong>
              {verificationError.includes('취소')
                ? '인증을 취소하셨습니다. 원하실 때 다시 시작해주세요.'
                : verificationError}
            </VerificationNotice>
          )}

          {verificationMessage && (
            <VerificationNotice role="status">
              <strong>
                {profile?.identityVerified
                  ? '본인인증이 완료됐어요'
                  : '본인인증 안내'}
              </strong>
              {verificationMessage}
            </VerificationNotice>
          )}
          {isProfileLoading ? (
            <p role="status">인증 정보를 불러오고 있습니다.</p>
          ) : profileError ? (
            <p role="alert">{profileError}</p>
          ) : profile ? (
            <VerificationStatusList
              items={verificationItems}
              onAction={handleVerificationAction}
            />
          ) : null}
        </main>
      </Grid>
    </Page>
  );
}

export default VerificationPage;
