import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import VerificationStatusList from '../components/member/VerificationStatusList';
import { memberMenuItems, verificationItems } from '../mocks/mypage';
import RegionMap from '../components/member/RegionMap';
import SectionCard from '../components/common/SectionCard';
import Button from '../components/common/Button';

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
  const handleVerificationAction = (id) => {
    if (id === 'region') {
      setShowRegionMap(true);
    }
  };

  // AI작성: 사용자가 요청했을 때 현재 위치를 조회한다.
  const handleCurrentLocation = () => {
    if (isLocating) return;

    setLocationError('');
    setPosition(null);

    if (!navigator.geolocation) {
      setLocationError('현재 브라우저에서는 위치 확인을 지원하지 않습니다.');
      return;
    }

    setIsLocating(true);

    navigator.geolocation.getCurrentPosition(
      ({ coords }) => {
        setPosition({
          latitude: coords.latitude,
          longitude: coords.longitude,
          accuracy: coords.accuracy,
        });
        setIsLocating(false);
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
          {showRegionMap ? (
            <SectionCard title="내 활동 지역">
              <p>
                현재 위치를 확인하려면 버튼을 누르고 위치 접근을 허용해주세요.
              </p>

              <RegionMap
                latitude={position?.latitude}
                longitude={position?.longitude}
              />

              <Button
                type="button"
                onClick={handleCurrentLocation}
                disabled={isLocating}
              >
                {isLocating ? '위치 확인 중…' : '현재 위치 확인'}
              </Button>

              {locationError && <p role="alert">{locationError}</p>}
              {position && (
                <p role="status">
                  현재 위치를 지도에 표시했습니다.
                  예상 오차 반경: 약 {Math.ceil(position.accuracy).toLocaleString()}m
                </p>
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
