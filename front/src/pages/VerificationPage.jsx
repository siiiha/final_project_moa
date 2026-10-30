import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import MemberSidebar from '../components/layout/MemberSidebar';
import VerificationStatusList from '../components/member/VerificationStatusList';
import { memberMenuItems } from '../mocks/mypage';
import RegionMap from '../components/member/RegionMap';
import SectionCard from '../components/common/SectionCard';
import Button from '../components/common/Button';
import {
  getMyProfile,
  verifyMyRegion,
  startMyIdentityVerification,
  completeMyIdentityVerification,
  getMemberRegions,
  updateMyRegion,
} from '../api/member';
import * as PortOne from '@portone/browser-sdk/v2';
import useAuthStore from '../store/authStore';
const MAX_LOCATION_ACCURACY_M = 500;
// AI작성: UTC 인증 시각을 사용자 기기의 시간대와 관계없이 한국 시간으로 표시한다.
const koreanDateTimeFormatter = new Intl.DateTimeFormat('ko-KR', {
  timeZone: 'Asia/Seoul',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hourCycle: 'h23',
});

function formatKoreanDateTime(value) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return '시각 확인 필요';
  }

  return koreanDateTimeFormatter.format(date);
}
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

// AI작성: 지역 정보, 지도, 안내와 버튼을 일정한 간격으로 배치한다.
const RegionContent = styled.div`
  display: grid;
  gap: 16px;
`;

const RegionSummary = styled.dl`
  margin: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  dt {
    margin-bottom: 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 14px;
    font-weight: 700;
  }

  dd {
    margin: 0;
    padding: 14px;
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.background};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
    font-size: 16px;
    font-weight: 700;
  }

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
`;

// AI작성: 안내·주의·오류 상태를 문구와 색상으로 함께 구분한다.
const RegionNotice = styled.p`
  margin: 0;
  padding: 14px 16px;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 14px;
  line-height: 1.7;
  word-break: keep-all;
  color: ${({ $tone, theme }) =>
    $tone === 'error'
      ? theme.colors.danger
      : $tone === 'warning'
        ? theme.colors.warning
        : theme.colors.primary};
  background: ${({ $tone, theme }) =>
    $tone === 'error'
      ? theme.colors.dangerSoft
      : $tone === 'warning'
        ? theme.colors.warningSoft
        : theme.colors.primarySoft};
`;

const RegionActions = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 10px;

  > button {
    min-height: 46px;
  }

  > button:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }

  @media (max-width: 520px) {
    > button {
      width: 100%;
    }
  }
`;

// AI작성: 기존 인증관리 화면과 동일한 색상으로 지역 변경 폼을 표시한다.
const RegionEditor = styled.form`
  display: grid;
  gap: 14px;
  padding: 20px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.background};

  > strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 17px;
  }

  > p {
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 14px;
    line-height: 1.6;
  }

  > p[role='alert'] {
    color: ${({ theme }) => theme.colors.danger};
  }

  fieldset {
    min-width: 0;
    margin: 0;
    padding: 0;
    border: 0;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
  }

  label {
    min-width: 0;
    color: ${({ theme }) => theme.colors.text};
    font-size: 14px;
    font-weight: 700;
  }

  select {
    display: block;
    box-sizing: border-box;
    width: 100%;
    min-width: 0;
    height: 48px;
    margin-top: 7px;
    padding: 0 12px;
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
    font: inherit;
  }

  select:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }

  select:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: ${({ theme }) => theme.colors.background};
    cursor: not-allowed;
  }

  @media (max-width: 640px) {
    fieldset {
      grid-template-columns: 1fr;
    }
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
  const [isIdentityVerifying, setIsIdentityVerifying] = useState(false);
  const [identityError, setIdentityError] = useState('');
  const [identityMessage, setIdentityMessage] = useState('');
  // AI작성: 활동 지역 변경 화면의 선택값과 처리 상태를 관리한다.
  const [isRegionEditing, setIsRegionEditing] = useState(false);
  const [regionOptions, setRegionOptions] = useState([]);
  const [selectedProvince, setSelectedProvince] = useState('');
  const [selectedDistrictId, setSelectedDistrictId] = useState('');
  const [selectedTownId, setSelectedTownId] = useState('');
  const [isRegionsLoading, setIsRegionsLoading] = useState(false);
  const [isRegionSaving, setIsRegionSaving] = useState(false);
  const [regionEditError, setRegionEditError] = useState('');

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
        description: '본인 명의의 인증 수단으로 본인 여부를 확인합니다.',
        actionLabel: isIdentityVerifying ? '인증 진행 중…' : profile.identityVerified ? '인증 완료' : '본인인증 시작',
        disabled: isIdentityVerifying || isVerifying || Boolean(profile.identityVerified),
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
    if (isIdentityVerifying || isVerifying) return;
    if (id === 'region') {
      setShowRegionMap(true);
    } else if (id === 'identity') {
      handleIdentityVerification(id);
    }
  };
  // AI작성: 서버 발급 번호로 인증창을 열고 서버 검증 후 화면을 갱신합니다.
  const handleIdentityVerification = async (id) => {
    if (id !== 'identity' || isIdentityVerifying || !profile) return;
    if (profile.identityVerified) return;

    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    const authVersion = useAuthStore.getState().authVersion;
    const isCurrentLogin = () =>
      useAuthStore.getState().authVersion === authVersion;

    setIsIdentityVerifying(true);
    setIdentityError('');
    setIdentityMessage('');

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
        setIdentityMessage('본인인증이 취소되었습니다.');
        return;
      }

      if (result.code !== undefined) {
        setIdentityError(
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
      setIdentityMessage('본인인증이 완료되었습니다.');
    } catch (error) {
      if (!isCurrentLogin()) return;

      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setIdentityError(
        error.response?.data?.error?.message ||
        error.message ||
        '본인인증 중 문제가 발생했습니다. 다시 시도해주세요.',
      );
    } finally {
      setIsIdentityVerifying(false);
    }
  };

  // AI작성: 서버가 인증 성공을 반환한 경우에만 실제 인증 상태를 갱신한다.
  const handleRegionVerification = async () => {
    if (isVerifying || isLocating || isRegionEditing || isRegionSaving) return;

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
    if (isLocating || isVerifying || isRegionEditing || isRegionSaving) return;

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

  // AI작성: 기존 지역 목록 API를 재사용하고 현재 가입 지역을 미리 선택한다.
  const handleOpenRegionEditor = async () => {
    if (
      !profile ||
      isRegionsLoading ||
      isRegionSaving ||
      isVerifying ||
      isLocating ||
      isIdentityVerifying
    ) return;

    const authVersion = useAuthStore.getState().authVersion;

    setIsRegionEditing(true);
    setIsRegionsLoading(true);
    setRegionEditError('');
    setSelectedProvince('');
    setSelectedDistrictId('');
    setSelectedTownId('');

    try {
      const regions = await getMemberRegions();

      if (useAuthStore.getState().authVersion !== authVersion) return;

      if (!Array.isArray(regions) || regions.length === 0) {
        throw new Error('활동 지역 목록을 불러오지 못했습니다.');
      }

      setRegionOptions(regions);
      setSelectedProvince(profile.provinceName || '');

      const district = regions.find(
        (region) =>
          !region.townName &&
          region.provinceName === profile.provinceName &&
          region.districtName === profile.districtName,
      );

      setSelectedDistrictId(district ? String(district.regionId) : '');
      setSelectedTownId(profile.townName ? String(profile.regionId) : '');
    } catch (error) {
      if (useAuthStore.getState().authVersion !== authVersion) return;

      setRegionEditError(
        error.response?.data?.error?.message ||
        error.message ||
        '지역 목록을 불러오지 못했습니다. 다시 시도해주세요.',
      );
    } finally {
      setIsRegionsLoading(false);
    }
  };
  // AI작성: 선택한 읍·면·동을 저장하고 서버가 반환한 인증 상태로 갱신한다.
  const handleSaveRegion = async (event) => {
    event.preventDefault();

    if (
      !profile ||
      isRegionSaving ||
      isRegionsLoading ||
      isVerifying ||
      isLocating ||
      isIdentityVerifying
    ) return;

    const currentToken = useAuthStore.getState().token;

    if (!currentToken) {
      navigate('/member/login', { replace: true });
      return;
    }

    const district = regionOptions.find(
      (region) => region.regionId === Number(selectedDistrictId),
    );
    const town = regionOptions.find(
      (region) => region.regionId === Number(selectedTownId),
    );

    if (
      !district ||
      !town?.townName ||
      district.provinceName !== selectedProvince ||
      town.provinceName !== selectedProvince ||
      town.districtName !== district.districtName
    ) {
      setRegionEditError('시·도, 시·군·구, 읍·면·동을 모두 선택해주세요.');
      return;
    }

    const authVersion = useAuthStore.getState().authVersion;
    const isCurrentLogin = () =>
      useAuthStore.getState().authVersion === authVersion;
    const regionChanged = profile.regionId !== town.regionId;

    setIsRegionSaving(true);
    setRegionEditError('');
    setVerificationError('');
    setVerificationMessage('');

    try {
      const updatedProfile = await updateMyRegion(currentToken, town.regionId);

      if (!isCurrentLogin()) return;

      if (!updatedProfile || updatedProfile.regionId !== town.regionId) {
        throw new Error('저장된 활동 지역을 확인하지 못했습니다.');
      }

      setProfile(updatedProfile);
      setIsRegionEditing(false);

      if (regionChanged) {
        setPosition(null);
        setLocationError('');
      }

      setVerificationMessage(
        regionChanged
          ? '활동 지역을 변경했습니다. 선택한 지역에서 위치 인증을 진행해주세요.'
          : '기존 활동 지역을 유지했습니다.',
      );
    } catch (error) {
      if (!isCurrentLogin()) return;

      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setRegionEditError(
        error.response?.data?.error?.message ||
        error.message ||
        '활동 지역을 저장하지 못했습니다. 다시 시도해주세요.',
      );
    } finally {
      setIsRegionSaving(false);
    }
  };
  // AI작성: 선택한 시·도와 시·군·구에 해당하는 하위 지역만 표시한다.
  const provinceOptions = [
    ...new Set(regionOptions.map((region) => region.provinceName)),
  ];

  const districtOptions = regionOptions.filter(
    (region) =>
      region.provinceName === selectedProvince &&
      !region.townName,
  );

  const selectedDistrict = districtOptions.find(
    (region) => region.regionId === Number(selectedDistrictId),
  );

  const townOptions = selectedDistrict
    ? regionOptions.filter(
      (region) =>
        region.townName &&
        region.provinceName === selectedDistrict.provinceName &&
        region.districtName === selectedDistrict.districtName,
    )
    : [];

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
          {isIdentityVerifying && (
            <VerificationNotice role="status">
              <strong>본인인증을 진행하고 있어요</strong>
              인증창에서 인증을 완료해주세요.
            </VerificationNotice>
          )}

          {identityError && (
            <VerificationNotice $warning role="alert">
              <strong>본인인증이 완료되지 않았어요</strong>
              {identityError.includes('취소')
                ? '인증을 취소하셨습니다. 원하실 때 다시 시작해주세요.'
                : identityError}
            </VerificationNotice>
          )}

          {identityMessage && (
            <VerificationNotice role="status">
              <strong>
                {profile?.identityVerified
                  ? '본인인증이 완료됐어요'
                  : '본인인증 안내'}
              </strong>
              {identityMessage}
            </VerificationNotice>
          )}
          {isProfileLoading ? (
            <p role="status">인증 정보를 불러오고 있습니다.</p>
          ) : profileError ? (
            <p role="alert">{profileError}</p>
          ) : !profile ? null : showRegionMap ? (
            <SectionCard title="지역 인증">
              <RegionContent>
                <RegionSummary>
                  <div>
                    <dt>가입한 시·도</dt>
                    <dd>{profile.provinceName || profile.regionName || '미등록'}</dd>
                  </div>
                  <div>
                    <dt>시·군·구</dt>
                    <dd>
                      {profile.districtName ||
                        (profile.provinceName === '세종특별자치시'
                          ? '세종특별자치시 전체'
                          : '세부 지역 확인 필요')}
                    </dd>
                  </div>
                  <div>
                    <dt>읍·면·동</dt>
                    <dd>{profile.townName || '읍·면·동 설정 필요'}</dd>
                  </div>
                </RegionSummary>
                {isRegionEditing ? (
                  <RegionEditor onSubmit={handleSaveRegion}>
                    <strong>활동 지역 변경</strong>
                    <p>다른 지역으로 변경하면 기존 지역 인증이 초기화됩니다.</p>

                    {isRegionsLoading && (
                      <p role="status">지역 목록을 불러오고 있습니다.</p>
                    )}

                    <fieldset disabled={isRegionsLoading || isRegionSaving}>
                      <label htmlFor="verify-province">
                        시·도
                        <select
                          id="verify-province"
                          value={selectedProvince}
                          onChange={(event) => {
                            setSelectedProvince(event.target.value);
                            setSelectedDistrictId('');
                            setSelectedTownId('');
                          }}
                          required
                        >
                          <option value="">시·도 선택</option>
                          {provinceOptions.map((province) => (
                            <option key={province} value={province}>
                              {province}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label htmlFor="verify-district">
                        시·군·구
                        <select
                          id="verify-district"
                          value={selectedDistrictId}
                          onChange={(event) => {
                            setSelectedDistrictId(event.target.value);
                            setSelectedTownId('');
                          }}
                          disabled={!selectedProvince}
                          required
                        >
                          <option value="">시·군·구 선택</option>
                          {districtOptions.map((region) => (
                            <option key={region.regionId} value={region.regionId}>
                              {region.districtName || '세종특별자치시 전체'}
                            </option>
                          ))}
                        </select>
                      </label>

                      <label htmlFor="verify-town">
                        읍·면·동
                        <select
                          id="verify-town"
                          value={selectedTownId}
                          onChange={(event) =>
                            setSelectedTownId(event.target.value)
                          }
                          disabled={!selectedDistrictId}
                          required
                        >
                          <option value="">읍·면·동 선택</option>
                          {townOptions.map((region) => (
                            <option key={region.regionId} value={region.regionId}>
                              {region.townName}
                            </option>
                          ))}
                        </select>
                      </label>
                    </fieldset>

                    {regionEditError && (
                      <p role="alert">{regionEditError}</p>
                    )}

                    <RegionActions>
                      <Button
                        type="submit"
                        disabled={
                          isRegionsLoading ||
                          isRegionSaving ||
                          !selectedTownId
                        }
                      >
                        {isRegionSaving ? '저장 중…' : '활동 지역 저장'}
                      </Button>
                      <Button
                        type="button"
                        $variant="secondary"
                        disabled={isRegionsLoading || isRegionSaving}
                        onClick={() => setIsRegionEditing(false)}
                      >
                        취소
                      </Button>
                    </RegionActions>
                  </RegionEditor>
                ) : (
                  <RegionActions>
                    <Button
                      type="button"
                      $variant="secondary"
                      onClick={handleOpenRegionEditor}
                      disabled={isVerifying || isLocating || isIdentityVerifying}
                    >
                      활동 지역 변경
                    </Button>
                  </RegionActions>
                )}

                <RegionNotice>
                  {profile.regionVerified ? (
                    <>
                      가입한 활동 지역의 인증이 완료되어 있습니다.
                      {profile.regionVerifiedAt && (
                        <>
                          <br />
                          인증 완료 시각:{' '}
                          <time dateTime={profile.regionVerifiedAt}>
                            {formatKoreanDateTime(profile.regionVerifiedAt)}
                          </time>
                          {' '}(한국 시간)
                        </>
                      )}
                    </>
                  ) : (
                    '가입한 활동 지역에서 현재 위치를 확인해주세요. 예상 오차 반경이 500m 이하일 때 인증할 수 있습니다.'
                  )}
                </RegionNotice>

                <RegionMap
                  latitude={position?.latitude}
                  longitude={position?.longitude}
                />

                {locationError ? (
                  <RegionNotice $tone="warning" role="alert">
                    {locationError}
                    {position && (
                      <>
                        <br />
                        예상 오차 반경: 약{' '}
                        {Math.ceil(position.accuracy).toLocaleString()}m
                      </>
                    )}
                  </RegionNotice>
                ) : position ? (
                  <RegionNotice role="status">
                    위치를 확인했습니다.
                    <br />
                    예상 오차 반경: 약{' '}
                    {Math.ceil(position.accuracy).toLocaleString()}m
                    {!profile.regionVerified && (
                      <>
                        <br />
                        ‘이 위치로 인증하기’를 누르면 활동 지역과 일치하는지 확인합니다.
                      </>
                    )}
                  </RegionNotice>
                ) : (
                  <RegionNotice>
                    {isLocating
                      ? '현재 위치를 확인하고 있습니다. 잠시만 기다려주세요.'
                      : '아래 ‘현재 위치 확인’ 버튼을 눌러 시작해주세요.'}
                  </RegionNotice>
                )}

                {verificationError && (
                  <RegionNotice $tone="error" role="alert">
                    {verificationError}
                  </RegionNotice>
                )}
                {verificationMessage && (
                  <RegionNotice role="status">
                    {verificationMessage}
                  </RegionNotice>
                )}

                <RegionActions>
                  <Button
                    type="button"
                    $variant="secondary"
                    onClick={handleCurrentLocation}
                    disabled={isLocating || isVerifying || isRegionEditing || isRegionSaving}
                  >
                    {isLocating
                      ? '위치 확인 중…'
                      : position
                        ? '현재 위치 다시 확인'
                        : '현재 위치 확인'}
                  </Button>

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
                      isRegionEditing ||
                      isRegionSaving ||
                      profile.regionVerified
                    }
                  >
                    {isVerifying
                      ? '지역 확인 중…'
                      : profile.regionVerified
                        ? '지역 인증 완료'
                        : '이 위치로 인증하기'}
                  </Button>

                  <Button
                    type="button"
                    $variant="secondary"
                    disabled={isLocating || isVerifying || isRegionEditing || isRegionSaving}
                    onClick={() => setShowRegionMap(false)}
                  >
                    인증관리로 돌아가기
                  </Button>
                </RegionActions>
              </RegionContent>
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
