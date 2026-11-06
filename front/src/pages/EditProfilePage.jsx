import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { getMyProfile, verifyMypagePassword } from '../api/member';
import ConfirmModal from '../components/common/ConfirmModal';
import PasswordInput from '../components/common/PasswordInput';
import MemberSidebar from '../components/layout/MemberSidebar';
import EditProfileForm from '../components/member/EditProfileForm';
import { MEMBER_MENU_ITEMS } from '../constants/memberMenu';
import useAuthStore from '../store/authStore';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;
const Heading = styled.div`
  margin-left: 216px;
  h1 {
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
function EditProfilePage() {
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const [activeMenu, setActiveMenu] = useState('edit');
  const [access, setAccess] = useState('loading');
  const [currentPassword, setCurrentPassword] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [accountProfile, setAccountProfile] = useState(null);
  
  useEffect(() => {
    if (!token) {
      navigate('/member/login', { replace: true });
      return;
    }

    let cancelled = false;
    setAccess('loading');

    async function checkAccount() {
      try {
        const profile = await getMyProfile(token);
        if (cancelled) return;

        if (typeof profile?.hasPassword !== 'boolean') {
          throw new Error('계정의 비밀번호 설정 여부를 확인할 수 없습니다.');
        }

        // AI작성: 조회한 회원정보를 보관해 수정 폼에서 재사용합니다.
        setAccountProfile(profile);
        setAccess(profile.hasPassword ? 'locked' : 'allowed');
      } catch (error) {
        if (cancelled) return;
        setErrorMessage(error.response?.data?.error?.message
          ?? '계정 정보를 확인하지 못했습니다.');
        setAccess('error');
      }
    }

    checkAccount();
    return () => {
      cancelled = true;
    };
  }, [token, navigate]);

  async function handleVerify() {
    if (isVerifying || !currentPassword.trim()) return;

    setIsVerifying(true);
    setErrorMessage('');

    try {
      await verifyMypagePassword(currentPassword);
      setAccess('allowed');
    } catch (error) {
      setErrorMessage(error.response?.data?.error?.message
        ?? '비밀번호를 확인하지 못했습니다.');
    } finally {
      setCurrentPassword('');
      setIsVerifying(false);
    }
  }

  if (!token) return null;

  return (
    <Page>
      <Heading>
        <h1>내 정보 수정</h1>
        <p>변경할 정보를 입력하고 저장해주세요.</p>
      </Heading>
      <Grid>
        <MemberSidebar
          items={MEMBER_MENU_ITEMS}
          activeItem={activeMenu}
          onSelect={setActiveMenu}
          onWithdraw={() => navigate('/member/mypage/delete')}
        />
        <main>
          {access === 'loading' && <p role="status">계정 정보를 확인하고 있습니다.</p>}
          {access === 'error' && <p role="alert">{errorMessage}</p>}
          {access === 'allowed' && accountProfile && (
            <EditProfileForm
              key={token}
              initialProfile={accountProfile}
            />
          )}
        </main>
      </Grid>

      {access === 'locked' && (
        <ConfirmModal
          title="비밀번호 확인"
          description="내 정보를 수정하려면 현재 비밀번호를 확인해 주세요."
          confirmLabel={isVerifying ? '확인 중…' : '확인'}
          cancelFirst
          onConfirm={handleVerify}
          onCancel={() => navigate('/member/mypage', { replace: true })}
          isBusy={isVerifying}
        >
          <PasswordInput
            id="edit-profile-password"
            label="현재 비밀번호"
            placeholder="현재 비밀번호를 입력해 주세요"
            autoComplete="current-password"
            value={currentPassword}
            onChange={(event) => {
              setCurrentPassword(event.target.value);
              setErrorMessage('');
            }}
            isVisible={isPasswordVisible}
            onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') {
                event.preventDefault();
                handleVerify();
              }
            }}
            disabled={isVerifying}
            autoFocus
          />
          {errorMessage && <p role="alert">{errorMessage}</p>}
        </ConfirmModal>
      )}
    </Page>
  );
}
export default EditProfilePage;
