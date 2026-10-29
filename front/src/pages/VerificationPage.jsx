import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import VerificationStatusList from '../components/member/VerificationStatusList';
import { memberMenuItems } from '../mocks/mypage';
import RegionMap from '../components/member/RegionMap';
import SectionCard from '../components/common/SectionCard';
import Button from '../components/common/Button';
import { getMyProfile, verifyMyRegion } from '../api/member';
import useAuthStore from '../store/authStore';
const MAX_LOCATION_ACCURACY_M = 500;
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

function VerificationPage() {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('verify');
  const [showRegionMap, setShowRegionMap] = useState(false);
  const [position, setPosition] = useState(null);
  const [isLocating, setIsLocating] = useState(false);
  const [locationError, setLocationError] = useState('');
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);
  const [profile, setProfile] = useState(null);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [verificationMessage, setVerificationMessage] = useState('');
  const [verificationError, setVerificationError] = useState('');

  // AI작성: 기존 회원 조회 API로 실제 인증 상태와 활동 지역을 가져온다.
  useEffect(() => {
    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    let active = true;

    async function loadProfile() {
      setIsProfileLoading(true);
      setProfileError('');
      setProfile(null);

      try {
        const data = await getMyProfile(token);

        if (!data) {
          throw new Error('회원정보가 없습니다.');
        }

        if (active) {
          setProfile(data);
        }
      } catch (error) {
        if (!active) return;

        if (error.response?.status === 401) {
          clearToken();
          navigate('/member/login', { replace: true });
          return;
        }

        setProfileError('인증 정보를 불러오지 못했습니다. 새로고침해주세요.');
      } finally {
        if (active) {
          setIsProfileLoading(false);
        }
      }
    }

    loadProfile();

    return () => {
      active = false;
    };
  }, [token, clearToken, navigate]);

  // AI작성: 기존 인증 목록 컴포넌트에 실제 회원의 인증 상태를 전달한다.
  const verificationItems = profile
    ? [
      {
        id: 'identity',
        title: '본인 인증',
        status: profile.identityVerified ? '인증 완료' : '미인증',
        description: '본인 명의의 휴대전화로 인증합니다.',
        actionLabel: '본인 인증 안내',
      },
      {
        id: 'region',
        title: '지역 인증',
        status: profile.regionVerified ? '인증 완료' : '미인증',
        description: `활동 지역 · ${profile.regionName || '미등록'}`,
        actionLabel: profile.regionVerified
          ? '인증 지역 확인'
          : '지역 인증하기',
      },
    ]
    : [];

  const handleVerificationAction = (id) => {
    if (id === 'region') {
      setShowRegionMap(true);
    }
  };
  // AI작성: 서버가 인증 성공을 반환한 경우에만 실제 인증 상태를 갱신한다.
  const handleRegionVerification = async () => {
    if (isVerifying || isLocating) return;

    setVerificationError('');
    setVerificationMessage('');

    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    if (!position
      || !Number.isFinite(position.accuracy)
      || position.accuracy <= 0
      || position.accuracy > MAX_LOCATION_ACCURACY_M) {
      setVerificationError('먼저 정확한 현재 위치를 확인해주세요.');
      return;
    }

    setIsVerifying(true);

    try {
      const data = await verifyMyRegion(token, position);

      if (!data?.regionVerified) {
        throw new Error('인증 결과를 확인하지 못했습니다.');
      }

      setProfile(data);
      setVerificationMessage('지역 인증이 완료됐습니다.');
    } catch (error) {
      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setVerificationError(
        error.response?.data?.error?.message ||
        '지역 인증을 완료하지 못했습니다. 다시 시도해주세요.',
      );
    } finally {
      setIsVerifying(false);
    }
  };

  // AI작성: 사용자가 요청했을 때 현재 위치를 조회한다.
  const handleCurrentLocation = () => {
    if (isLocating || isVerifying) return;

    setLocationError('');
    setPosition(null);
    setVerificationError('');
    setVerificationMessage('');

    if (!navigator.geolocation) {
      setLocationError('현재 브라우저에서는 위치 확인을 지원하지 않습니다.');
      return;
    }

    setIsLocating(true);

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setIsLocating(false);

        if (!Number.isFinite(coords.accuracy) || coords.accuracy <= 0) {
          setLocationError(
            '위치 정확도를 확인할 수 없습니다. 다시 시도해주세요.',
          );
          return;
        }

        setPosition({
          latitude: coords.latitude,
          longitude: coords.longitude,
          accuracy: coords.accuracy,
        });

        // AI작성: 기준보다 부정확한 위치는 인증에 사용하지 않는다.
        if (coords.accuracy > MAX_LOCATION_ACCURACY_M) {
          setLocationError(
            '위치가 정확하지 않아 인증을 진행할 수 없습니다. ' +
            '예상 오차 반경이 500m 이하일 때 다시 시도해주세요.',
          );
        }
      },

      (error) => {
        const messages = {
          1: '위치 접근이 차단됐습니다. 브라우저 설정에서 위치 권한을 허용해주세요.',
          2: '현재 위치를 확인할 수 없습니다. 잠시 후 다시 시도해주세요.',
          3: '위치 확인 시간이 초과됐습니다. 다시 시도해주세요.',
        };

        setLocationError(
          messages[error.code] || '위치 확인에 실패했습니다.',
        );
        setIsLocating(false);
      },
      {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0,
      },
    );
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
          {isProfileLoading ? (
            <p role="status">인증 정보를 불러오고 있습니다.</p>
          ) : profileError ? (
            <p role="alert">{profileError}</p>
          ) : !profile ? null : showRegionMap ? (
            <SectionCard title="내 활동 지역">
              <p>
                가입한 활동 지역: <strong>{profile?.regionName || '미등록'}</strong>
              </p>
              <p>
                현재 위치가 가입한 활동 지역과 일치하면 지역 인증을 완료할 수 있습니다.
              </p>

              <RegionMap
                latitude={position?.latitude}
                longitude={position?.longitude}
              />

              <Button
                type="button"
                onClick={handleCurrentLocation}
                disabled={isLocating || isVerifying}
              >
                {isLocating ? '위치 확인 중…' : '현재 위치 확인'}
              </Button>

              {locationError && <p role="alert">{locationError}</p>}
              {position && (
                <p role="status">
                  {position.accuracy <= MAX_LOCATION_ACCURACY_M
                    ? '위치를 확인했습니다. 활동 지역 일치 여부를 확인해주세요.'
                    : '대략적인 위치를 표시하고 있습니다.'}
                  {' '}예상 오차 반경: 약{' '}
                  {Math.ceil(position.accuracy).toLocaleString()}m
                </p>
              )}
              <Button
                type="button"
                onClick={handleRegionVerification}
                disabled={
                  !position ||
                  !Number.isFinite(position.accuracy) ||
                  position.accuracy <= 0 ||
                  position.accuracy > MAX_LOCATION_ACCURACY_M ||
                  isLocating ||
                  isVerifying ||
                  profile?.regionVerified
                }
              >
                {isVerifying
                  ? '지역 확인 중…'
                  : profile?.regionVerified
                    ? '지역 인증 완료'
                    : '이 위치로 인증하기'}
              </Button>

              {verificationError && (
                <p role="alert">{verificationError}</p>
              )}
              {verificationMessage && (
                <p role="status">{verificationMessage}</p>
              )}

              <Button
                type="button"
                $variant="secondary"
                onClick={() => setShowRegionMap(false)}
              >
                인증관리로 돌아가기
              </Button>
            </SectionCard>
          ) : (
            <VerificationStatusList
              items={verificationItems}
              onAction={handleVerificationAction}
            />
          )}
        </main>
      </Grid>
    </Page>
  );
}

export default VerificationPage;
