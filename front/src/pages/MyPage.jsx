import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import MeetingList from '../components/member/MeetingList';
import ProfileSummary from '../components/member/ProfileSummary';
import QuickLinks from '../components/member/QuickLinks';
import { memberMenuItems, participatingMeetings, quickLinks } from '../mocks/mypage';
import { getMyProfile } from '../api/member';
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
  grid-template-columns: 200px minmax(0, 1fr) 210px;
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

function MyPage() {
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);

  const [activeMenu, setActiveMenu] = useState('overview');
  const [profile, setProfile] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');

  // 로그인한 회원정보를 조회하고 화면을 떠나면 결과 반영을 중단
  useEffect(() => {
    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    let cancelled = false;

    async function loadProfile() {
      setIsLoading(true);
      setErrorMessage('');
      setProfile(null);

      try {
        const data = await getMyProfile(token);

        if (!data) {
          throw new Error('회원정보가 없습니다.');
        }

        if (!cancelled) {
          setProfile(data);
        }
      } catch (error) {
        if (cancelled) return;

        if (error.response?.status === 401) {
          clearToken();
          navigate('/member/login', { replace: true });
          return;
        }

        setErrorMessage(
          error.response?.data?.error?.message ??
            '회원정보를 불러오지 못했습니다.',
        );
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      cancelled = true;
    };
  }, [token, clearToken, navigate]);

  // 서버 응답을 기존 프로필 카드의 표시 형식으로 변환
  const summary = profile
    ? {
        name: profile.nickname,
        email: profile.email,
        badges: [
          ...(profile.identityVerified ? ['본인 인증 완료'] : []),
          ...(profile.regionVerified ? ['지역 인증 완료'] : []),
        ],
        stats: [
          {
            label: '나이',
            value: profile.age == null ? '-' : `${profile.age}세`,
          },
          {
            label: '선택한 관심사',
            value: `${profile.interests?.length ?? 0}개`,
          },
        ],
      }
    : null;

  const handlePendingAction = () => {
    // 참여 모임과 바로가기 기능은 별도로 연결
  };

  return (
    <Page>
      <Heading>
        <h1>마이페이지</h1>
        <p>내 정보와 참여 중인 모임을 한눈에 확인하세요.</p>
      </Heading>

      <Grid>
        <MemberSidebar
          items={memberMenuItems}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />

        <main>
          {isLoading && <p role="status">회원정보를 불러오는 중입니다.</p>}
          {errorMessage && <p role="alert">{errorMessage}</p>}

          {!isLoading && !errorMessage && summary && (
            <ProfileSummary
              profile={summary}
              onEdit={() => navigate('/member/mypage/edit')}
            />
          )}

          <p>참여 모임 예시 데이터</p>
          <MeetingList
            meetings={participatingMeetings}
            onView={handlePendingAction}
          />
        </main>

        <QuickLinks links={quickLinks} onSelect={handlePendingAction} />
      </Grid>
    </Page>
  );
}

export default MyPage;