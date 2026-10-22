import { useEffect, useState } from 'react';
import { FiCamera, FiUser } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import PasswordInput from '../common/PasswordInput';
import TextInput from '../common/TextInput';
import {
  getMyProfile,
  updateMyProfile,
  memberInterestOptions,
} from '../../api/member';
import useAuthStore from '../../store/authStore';

const Card = styled.section`
  padding: 28px 34px 16px;
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 42px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const AvatarPanel = styled.section`
  padding: 26px 18px;
  align-self: start; 
  display: flex;
  flex-direction: column;
  align-items: center;
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
`;

const Avatar = styled.div`
  width: 158px;
  height: 158px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;
`;

const AvatarLabel = styled.strong`
  margin-top: 10px;
  font-size: 13px;
`;

const AvatarActions = styled.div`
  width: 100%;
  margin-top: 10px;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px;
`;

const AvatarButton = styled.button`
  min-height: 30px;
  padding: 5px;
  color: ${({ $danger, theme }) => ($danger ? '#c9463f' : theme.colors.secondaryText)};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 10px;
  font-weight: 700;
`;

const FormArea = styled.div`
  display: grid;
  align-content: start;
  gap: 12px;
  label {
    word-break: keep-all;
  }
`;

const VerifyButton = styled(Button)`
  min-width: 82px;
  min-height: 42px;
  padding: 7px 10px;
  font-size: 11px;
`;

const Divider = styled.div`
  height: 1px;
  margin: 7px 0 2px;
  background: ${({ theme }) => theme.colors.border};
`;

const InterestField = styled.section`
  margin-top: 5px;
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: start;
  gap: 8px;
`;

const Label = styled.h2`
  padding-top: 7px;
  font-size: 12px;
  font-weight: 700;
`;

const ChipList = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
`;

const Chip = styled.button`
  min-height: 31px;
  padding: 5px 12px;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.white : theme.colors.secondaryText)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 11px;
  font-weight: 700;
`;

const Hint = styled.p`
  grid-column: 2;
  margin-top: -3px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

const FormActions = styled.div`
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
`;

const ActionButton = styled(Button)`
  min-height: 40px;
  padding: 8px 18px;
  font-size: 12px;
`;

// AI작성: Figma의 비밀번호 변경 안내 스타일을 적용합니다.
const PasswordHint = styled.p`
  margin-left: 100px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.6;
`;
// AI작성: 저장 오류를 눈에 띄는 안내 상자로 보여줍니다.

const ErrorNotice = styled.p`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border: 1px solid ${({ theme }) => theme.colors.danger};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
  line-height: 1.5;
`;

function EditProfileForm() {
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);

  const [profile, setProfile] = useState(null);
  const [nickname, setNickname] = useState('');
  const [currentPassword, setCurrentPassword] = useState('');
  const [selectedInterests, setSelectedInterests] = useState([]);
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  const [isNewPasswordVisible, setIsNewPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] =
    useState(false);

  // 수정 화면에 현재 회원정보를 채움.
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
          setNickname(data.nickname ?? '');
          setSelectedInterests(
            (data.interests ?? []).map((interest) => interest.interestId),
          );
          setCurrentPassword('');
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
        if (!cancelled) setIsLoading(false);
      }
    }

    loadProfile();

    return () => {
      cancelled = true;
    };
  }, [token, clearToken, navigate]);

  const toggleInterest = (interestId) => {
    setSelectedInterests((current) =>
      current.includes(interestId)
        ? current.filter((id) => id !== interestId)
        : [...current, interestId],
    );
  };

  // 비밀번호는 저장소에 보관하지 않고 수정 요청에만 사용
  const handleUpdateRequest = async (event) => {
    event.preventDefault();

    if (isSaving || !profile) return;

    const trimmedNickname = nickname.trim();

    if (trimmedNickname.length < 2 || trimmedNickname.length > 12) {
      setErrorMessage('닉네임은 2~12자로 입력해주세요.');
      return;
    }

    if (!currentPassword.trim()) {
      setErrorMessage('현재 비밀번호를 입력해주세요.');
      return;
    }

        // AI작성: 비밀번호 변경을 입력한 경우 길이와 확인값을 검사합니다.
    const passwordChangeRequested =
      newPassword !== '' || confirmNewPassword !== '';

    if (passwordChangeRequested) {
      if (!newPassword.trim() || newPassword.length < 8) {
        setErrorMessage('새 비밀번호는 8자 이상 입력해주세요.');
        return;
      }

      if (new TextEncoder().encode(newPassword).length > 72) {
        setErrorMessage('비밀번호가 너무 깁니다. 더 짧게 입력해주세요.');
        return;
      }

      if (newPassword !== confirmNewPassword) {
        setErrorMessage('새 비밀번호와 확인 입력값이 일치하지 않습니다.');
        return;
      }
    }

    setIsSaving(true);
    setErrorMessage('');

    try {
      const updatedProfile = await updateMyProfile(token, {
        currentPassword,
        nickname: trimmedNickname,
        interestIds: selectedInterests,
        newPassword,
        confirmNewPassword,
      });

      if (!updatedProfile) {
        throw new Error('수정 결과를 확인할 수 없습니다.');
      }

      setCurrentPassword('');
      setNewPassword('');
      setConfirmNewPassword('');
      navigate('/member/mypage');
    } catch (error) {
      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

            setErrorMessage(
        error.response?.data?.error?.code ===
          'NEW_PASSWORD_SAME_AS_CURRENT'
          ? '지금 사용 중인 비밀번호예요. 다른 새 비밀번호를 입력해주세요.'
          : error.response?.data?.error?.message ??
              '회원정보를 수정하지 못했습니다.',
      );

    } finally {
      setIsSaving(false);
    }
  };

  if (isLoading) {
    return <p role="status">회원정보를 불러오는 중입니다.</p>;
  }

  if (!profile) {
    return <p role="alert">{errorMessage || '회원정보가 없습니다.'}</p>;
  }

  return (
    <Card>
      <AvatarPanel>
        <Avatar role="img" aria-label="회원 프로필 이미지 영역">
          <FiUser size={38} aria-hidden="true" />
        </Avatar>
        <AvatarLabel>프로필 사진</AvatarLabel>
        <AvatarActions>
          <AvatarButton type="button" disabled>
            <FiCamera size={12} aria-hidden="true" /> 사진 변경
          </AvatarButton>
          <AvatarButton type="button" $danger disabled>
            삭제
          </AvatarButton>
        </AvatarActions>
        <p>사진 기능은 준비 중입니다.</p>
      </AvatarPanel>

      <FormArea as="form" onSubmit={handleUpdateRequest}>
        <TextInput
          id="edit-name"
          label="이름"
          value={profile.name ?? ''}
          readOnly
          inline
          compact
        />

        <TextInput
          id="edit-nickname"
          label="닉네임"
          value={nickname}
          onChange={(event) => setNickname(event.target.value)}
          minLength={2}
          maxLength={12}
          required
          disabled={isSaving}
          helpText="2~12자 · 중복 여부는 저장 시 확인합니다."
          inline
          compact
        />

        <TextInput
          id="edit-phone"
          label="전화번호"
          type="tel"
          value={profile.phone ?? ''}
          readOnly
          inline
          compact
        />

        <TextInput
          id="edit-email"
          label="이메일"
          type="email"
          value={profile.email ?? ''}
          readOnly
          inline
          compact
        />

        <Divider />

        <PasswordInput
          id="edit-current-password"
          label="현재 비밀번호"
          placeholder="본인 확인을 위해 입력해주세요"
          autoComplete="current-password"
          value={currentPassword}
          onChange={(event) => setCurrentPassword(event.target.value)}
          required
          disabled={isSaving}
          inline
          compact
          isVisible={isPasswordVisible}
          onToggleVisibility={() => setIsPasswordVisible((value) => !value)}
        />

                {/* AI작성: 새 비밀번호를 입력한 경우에만 변경합니다. */}
        <PasswordInput
          id="edit-new-password"
          label="비밀번호 변경"
          placeholder="변경할 경우 8자 이상 입력"
          autoComplete="new-password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          minLength={8}
          disabled={isSaving}
          inline
          compact
          isVisible={isNewPasswordVisible}
          onToggleVisibility={() =>
            setIsNewPasswordVisible((value) => !value)
          }
        />

        <PasswordInput
          id="edit-new-password-confirm"
          label="비밀번호 변경 확인"
          placeholder="새 비밀번호를 다시 입력"
          autoComplete="new-password"
          value={confirmNewPassword}
          onChange={(event) =>
            setConfirmNewPassword(event.target.value)
          }
          disabled={isSaving}
          inline
          compact
          isVisible={isConfirmPasswordVisible}
          onToggleVisibility={() =>
            setIsConfirmPasswordVisible((value) => !value)
          }
        />

        <PasswordHint>
          비밀번호를 변경하지 않으려면 두 칸 모두 비워두세요.
        </PasswordHint>

        <Divider />

        <InterestField>
          <Label>관심사</Label>
          <ChipList aria-label="관심사 선택">
            {memberInterestOptions.map((interest) => (
              <Chip
                key={interest.interestId}
                type="button"
                disabled={isSaving}
                $selected={selectedInterests.includes(interest.interestId)}
                aria-pressed={selectedInterests.includes(interest.interestId)}
                onClick={() => toggleInterest(interest.interestId)}
              >
                {selectedInterests.includes(interest.interestId) ? '✓ ' : ''}
                {interest.interestName}
              </Chip>
            ))}
          </ChipList>
          <Hint>모두 해제하면 관심사를 선택하지 않은 상태로 저장됩니다.</Hint>
        </InterestField>

        {errorMessage && (
          <ErrorNotice role="alert">{errorMessage}</ErrorNotice>
        )}

        <FormActions>
          <ActionButton type="submit" disabled={isSaving}>
            {isSaving ? '저장 중…' : '수정'}
          </ActionButton>
          <ActionButton
            type="button"
            $variant="secondary"
            disabled={isSaving}
            onClick={() => navigate('/member/mypage')}
          >
            취소
          </ActionButton>
        </FormActions>
      </FormArea>
    </Card>
  );
}

export default EditProfileForm;