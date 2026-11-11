import { useEffect, useRef, useState } from 'react';
import { FiCamera } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import PasswordInput from '../common/PasswordInput';
import TextInput from '../common/TextInput';
import {
  updateMyProfile,
  memberInterestOptions,
  uploadMyProfileImage,
  deleteMyProfileImage,
  checkNicknameAvailable,
} from '../../api/member';
import useAuthStore from '../../store/authStore';
import ProfileImage from './ProfileImage';
import { interestIcons } from './interestsIcons';

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

// AI작성: 사진 카드와 사진 오류를 같은 열에 배치합니다.
const PhotoColumn = styled.div`
  align-self: start;
  display: grid;
  gap: 12px;

  > p {
    margin: 0;
    word-break: keep-all;
    overflow-wrap: anywhere;
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

// AI작성: 사진을 고정된 원형 영역 안에 표시합니다.
const Avatar = styled.div`
  width: 158px;
  height: 158px;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;

  > img {
    position: absolute;
    inset: 0;
    width: 100%;
    height: 100%;
    object-fit: cover;
    object-position: center;
  }
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

// AI작성: 사진 저장 안내와 파일 제한을 간결하게 표시합니다.
const PhotoHint = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.7;
  text-align: center;
  word-break: keep-all;

  span {
    display: block;
    margin-top: 3px;
    font-size: 11px;
  }
`;

const FormArea = styled.div`
  display: grid;
  align-content: start;
  gap: 12px;

  label {
    word-break: keep-all;
  }

  /* AI작성: 수정 불가 항목을 회색으로 구분하고 내용 복사는 허용합니다. */
  input[readonly] {
    background: #eef2f1;
    color: ${({ theme }) => theme.colors.secondaryText};
    cursor: default;
  }

  input[readonly]:focus {
    border-color: ${({ theme }) => theme.colors.border};
    box-shadow: none;
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

// AI작성: 관심사를 동일한 크기의 버튼으로 표시한다.
const ChipList = styled.div`
  min-width: 0;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
`;

const Chip = styled.button`
  min-width: 0;
  min-height: 48px;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: ${({ $selected, theme }) =>
    $selected ? theme.colors.white : theme.colors.secondaryText};
  background: ${({ $selected, theme }) =>
    $selected ? theme.colors.primary : theme.colors.white};
  border: 1px solid ${({ $selected, theme }) =>
    $selected ? theme.colors.primary : theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 14px;
  font-weight: 700;
  line-height: 1.5;
  cursor: pointer;

  > svg {
    flex-shrink: 0;
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
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

// AI작성: 비밀번호 오류를 기존 서비스의 빨간색으로 안내합니다.
const PasswordFeedback = styled.p`
  margin: 0;
  color: ${({ theme }) => theme.colors.danger};
  font-size: 13px;
  line-height: 1.5;
`;

function EditProfileForm({ initialProfile }) {
  const navigate = useNavigate();
  const token = useAuthStore((state) => state.token);
  const clearToken = useAuthStore((state) => state.clearToken);
  const photoInputRef = useRef(null);
  // AI작성: 상위 화면에서 조회한 정보를 사용해 추가 조회 없이 입력칸을 채웁니다.
  const [profile, setProfile] = useState(initialProfile);
  const [nickname, setNickname] = useState(initialProfile?.nickname ?? '');
  const [selectedInterests, setSelectedInterests] = useState(
    () => (initialProfile?.interests ?? []).map((interest) => interest.interestId),
  );
  const [isSaving, setIsSaving] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmNewPassword, setConfirmNewPassword] = useState('');
  // AI작성: 비밀번호를 변경할 때만 입력 조건과 확인값을 검사합니다.
  const [isPasswordTouched, setIsPasswordTouched] = useState(false);
  const [isPasswordConfirmTouched, setIsPasswordConfirmTouched] = useState(false);

  const passwordChangeRequested =
    newPassword !== '' || confirmNewPassword !== '';

  const passwordError = !passwordChangeRequested
    ? ''
    : newPassword.length < 8 || newPassword.length > 20
      ? '비밀번호는 8~20자로 입력해 주세요.'
      : !/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!-/:-@\[-`{-~])[!-~]{8,20}$/.test(newPassword)
        ? '영문, 숫자, 특수문자를 각각 하나 이상 포함해 주세요.'
        : '';
  const [isNewPasswordVisible, setIsNewPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] =
    useState(false);

  // AI작성: 사진 등록 진행 상태와 실패 상태를 관리합니다.
  const [photoStatus, setPhotoStatus] = useState('');
  // AI작성: 사진 오류를 회원정보 수정 오류와 분리합니다.
  const [photoError, setPhotoError] = useState('');
  
  // AI작성: 입력을 멈춘 뒤 닉네임을 확인하고, 이전 요청의 늦은 응답은 무시합니다.
  const [nicknameCheck, setNicknameCheck] = useState({
    value: '',
    status: 'idle',
  });

  useEffect(() => {
    const value = nickname.trim();
    const originalNickname = (profile?.nickname ?? '').trim();

    if (
      value === originalNickname ||
      value.length < 2 ||
      value.length > 12
    ) {
      return;
    }

    let cancelled = false;

    const timer = window.setTimeout(async () => {
      try {
        const available = await checkNicknameAvailable(value);

        if (typeof available !== 'boolean') {
          throw new Error('닉네임 확인 결과가 올바르지 않습니다.');
        }

        if (!cancelled) {
          setNicknameCheck({
            value,
            status: available ? 'available' : 'unavailable',
          });
        }
      } catch {
        if (!cancelled) {
          setNicknameCheck({ value, status: 'error' });
        }
      }
    }, 500);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [nickname, profile?.nickname]);
  // AI작성: 현재 입력값에 해당하는 확인 결과만 안내합니다.
  const nicknameValue = nickname.trim();

  const nicknameStatus =
    nicknameValue === (profile?.nickname ?? '').trim()
      ? 'idle'
      : nicknameValue.length < 2 || nicknameValue.length > 12
        ? 'invalid'
        : nicknameCheck.value === nicknameValue
          ? nicknameCheck.status
          : 'checking';

  const nicknameMessages = {
    idle: '닉네임은 2~12자로 입력해 주세요.',
    invalid: '닉네임은 2~12자로 입력해 주세요.',
    checking: '닉네임을 확인하고 있어요.',
    available: '사용 가능한 닉네임이에요.',
    unavailable: '이미 사용 중인 닉네임이에요.',
    error: '닉네임을 확인하지 못했어요. 잠시 후 다시 입력해 주세요.',
  };

  // AI작성: 기존 선택은 해제할 수 있고, 새 선택은 최대 3개로 제한한다.
  const toggleInterest = (interestId) => {
    setSelectedInterests((current) => {
      if (current.includes(interestId)) {
        return current.filter((id) => id !== interestId);
      }

      if (current.length >= 3) return current;

      return [...current, interestId];
    });
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

    // AI작성: 수정 요청 시 입력칸 아래에 오류를 표시합니다.
    setIsPasswordTouched(true);
    setIsPasswordConfirmTouched(true);

    if (
      passwordChangeRequested &&
      (passwordError || newPassword !== confirmNewPassword)
    ) {
      setErrorMessage('');
      return;
    }

    setIsSaving(true);
    setErrorMessage('');

    try {
      const updatedProfile = await updateMyProfile(token, {
        nickname: trimmedNickname,
        interestIds: selectedInterests,
        newPassword,
        confirmNewPassword,
      });

      if (!updatedProfile) {
        throw new Error('수정 결과를 확인할 수 없습니다.');
      }

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

  // AI작성: 선택한 사진을 저장하고 현재 화면에 반영합니다.
  const handlePhotoChange = async (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';

    if (!file || isSaving) return;

    setPhotoError('');
    setPhotoStatus('');

    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setPhotoError('JPG 또는 PNG 사진을 선택해 주세요.');
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      setPhotoError('사진은 5MB 이하로 선택해 주세요.');
      return;
    }

    setIsSaving(true);
    setPhotoStatus('uploading');

    try {
      const result = await uploadMyProfileImage(token, file);
      setPhotoStatus('');

      setProfile((current) =>
        current
          ? { ...current, profileImageUrl: result.profileImageUrl }
          : current,
      );
    } catch (error) {
      // AI작성: 실패해도 기존 사진은 유지하고 카드 아래에 이유를 안내합니다.
      setPhotoStatus('');

      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setPhotoError(
        error.response?.data?.error?.message ??
        '사진을 저장하지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
    } finally {
      setIsSaving(false);
    }
  };

  // AI작성: 사진 삭제 성공 후 기본 프로필로 표시합니다.
  const handlePhotoDelete = async () => {
    if (isSaving || !profile?.profileImageUrl) return;

    setPhotoError('');
    setPhotoStatus('');
    setIsSaving(true);

    try {
      await deleteMyProfileImage(token);
      setPhotoStatus('');

      setProfile((current) =>
        current
          ? { ...current, profileImageUrl: null }
          : current,
      );
    } catch (error) {
      if (error.response?.status === 401) {
        clearToken();
        navigate('/member/login', { replace: true });
        return;
      }

      setPhotoError(
        error.response?.data?.error?.message ??
        '사진을 삭제하지 못했어요. 잠시 후 다시 시도해 주세요.',
      );
    } finally {
      setIsSaving(false);
    }
  };

  if (!profile) {
    return <p role="alert">{errorMessage || '회원정보가 없습니다.'}</p>;
  }

  return (
    <Card>
      <PhotoColumn>
        <AvatarPanel>
          <Avatar aria-busy={photoStatus === 'uploading'}>
            <ProfileImage
              imageUrl={profile.profileImageUrl}
              size={72}
              status={photoStatus}
            />
          </Avatar>
          <AvatarLabel>프로필 사진</AvatarLabel>
          <input
            ref={photoInputRef}
            type="file"
            accept="image/jpeg,image/png"
            hidden
            onChange={handlePhotoChange}
          />
          <AvatarActions>
            <AvatarButton
              type="button"
              disabled={isSaving}
              onClick={() => photoInputRef.current?.click()}
            >
              <FiCamera size={12} aria-hidden="true" /> 사진 변경
            </AvatarButton>
            <AvatarButton
              type="button"
              $danger
              disabled={isSaving || !profile.profileImageUrl}
              onClick={handlePhotoDelete}
            >
              삭제
            </AvatarButton>
          </AvatarActions>
          <PhotoHint>
            <span>JPG·PNG · 최대 5MB</span>
          </PhotoHint>
        </AvatarPanel>
        {/* AI작성: 용량·형식·저장·삭제 오류를 사진 카드 바로 아래에 표시합니다. */}
        {photoError && (
          <ErrorNotice role="alert">{photoError}</ErrorNotice>
        )}
      </PhotoColumn>

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
          onChange={(event) => {
            setNickname(event.target.value);
            setNicknameCheck({ value: '', status: 'idle' });
          }}
          minLength={2}
          maxLength={12}
          required
          disabled={isSaving}
          helpText={
            <span
              role="status"
              style={{
                color:
                  nicknameStatus === 'available'
                    ? '#2E7D32'
                    : ['invalid', 'unavailable', 'error'].includes(nicknameStatus)
                      ? '#c9463f'
                      : '#51625e',
              }}
            >
              {nicknameMessages[nicknameStatus]}
            </span>
          }
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

        {/* AI작성: 새 비밀번호를 입력한 경우에만 변경합니다. */}
        <PasswordInput
          id="edit-new-password"
          label="새 비밀번호"
          placeholder="8~20자, 영문·숫자·특수문자 포함"
          autoComplete="new-password"
          value={newPassword}
          onChange={(event) => setNewPassword(event.target.value)}
          onBlur={() => setIsPasswordTouched(true)}
          aria-invalid={isPasswordTouched && Boolean(passwordError)}
          aria-describedby="edit-password-feedback"
          disabled={isSaving}
          inline
          compact
          isVisible={isNewPasswordVisible}
          onToggleVisibility={() =>
            setIsNewPasswordVisible((value) => !value)
          }
        />
        <PasswordFeedback id="edit-password-feedback" role="status">
          {isPasswordTouched ? passwordError : ''}
        </PasswordFeedback>

        <PasswordInput
          id="edit-new-password-confirm"
          label="새 비밀번호 확인"
          placeholder="새 비밀번호를 다시 입력"
          autoComplete="new-password"
          value={confirmNewPassword}
          onChange={(event) =>
            setConfirmNewPassword(event.target.value)
          }
          onBlur={() => setIsPasswordConfirmTouched(true)}
          aria-describedby="edit-password-confirm-feedback"
          disabled={isSaving}
          inline
          compact
          isVisible={isConfirmPasswordVisible}
          onToggleVisibility={() =>
            setIsConfirmPasswordVisible((value) => !value)
          }
        />
        <PasswordFeedback id="edit-password-confirm-feedback" role="status">
          {(confirmNewPassword !== '' || isPasswordConfirmTouched) &&
            passwordChangeRequested &&
            newPassword !== confirmNewPassword
            ? '새 비밀번호가 일치하지 않아요. 다시 확인해 주세요.'
            : ''}
        </PasswordFeedback>

        <Divider />

        <InterestField>
          <Label>관심사</Label>
          <ChipList aria-label="관심사 선택">
            {memberInterestOptions.map((interest) => {
              const selected = selectedInterests.includes(interest.interestId);
              const InterestIcon = interestIcons[interest.interestId];

              return (
                <Chip
                  key={interest.interestId}
                  type="button"
                  disabled={
                    isSaving || (!selected && selectedInterests.length >= 3)
                  }
                  $selected={selected}
                  aria-pressed={selected}
                  onClick={() => toggleInterest(interest.interestId)}
                >
                  {InterestIcon && (
                    <InterestIcon size={18} aria-hidden="true" />
                  )}
                  <span>{interest.interestName}</span>
                </Chip>
              );
            })}
          </ChipList>
          <Hint aria-live="polite">
            최대 3개 선택 · 선택 {selectedInterests.length}/3 · 모두 해제하면 관심사 없이 저장됩니다.
          </Hint>
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