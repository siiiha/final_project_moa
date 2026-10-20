import { useState } from 'react';
import { FiCamera, FiUser } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import PasswordInput from '../common/PasswordInput';
import TextInput from '../common/TextInput';
import { interestOptions } from '../../mocks/join';

const Card = styled.section`
  padding: 22px;
  display: grid;
  grid-template-columns: 190px minmax(0, 1fr);
  gap: 24px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const AvatarPanel = styled.section`
  padding: 20px 14px 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
`;

const Avatar = styled.div`
  width: 92px;
  height: 92px;
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

function EditProfileForm() {
  const navigate = useNavigate();
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);
  const [selectedInterests, setSelectedInterests] = useState(['산책', '독서']);

  const toggleInterest = (interest) => {
    setSelectedInterests((current) => {
      if (current.includes(interest)) return current.filter((item) => item !== interest);
      if (current.length === 2) return current;
      return [...current, interest];
    });
  };

  const handlePendingAction = () => {
    // TODO: Spring Boot 프로필 이미지·닉네임 중복 확인 API를 연결합니다.
  };

  const handleUpdateRequest = () => {
    // TODO: 수정 버튼 클릭 시 form submit이 아닌 Spring Boot 회원 정보 수정 API 요청을 보냅니다.
  };

  const handleCancel = () => {
    navigate('/member/mypage');
  };

  return (
    <Card>
      <AvatarPanel>
        <Avatar role="img" aria-label="회원 프로필 이미지 영역">
          <FiUser size={38} aria-hidden="true" />
        </Avatar>
        <AvatarLabel>프로필 사진</AvatarLabel>
        <AvatarActions>
          <AvatarButton type="button" onClick={handlePendingAction}>
            <FiCamera size={12} aria-hidden="true" /> 사진 변경
          </AvatarButton>
          <AvatarButton type="button" $danger onClick={handlePendingAction}>
            삭제
          </AvatarButton>
        </AvatarActions>
      </AvatarPanel>
      <FormArea>
        <TextInput id="edit-name" label="이름" defaultValue="김모아" inline compact />
        <TextInput
          id="edit-nickname"
          label="닉네임"
          defaultValue="모아이웃"
          inline
          compact
          action={
            <VerifyButton type="button" $variant="secondary" onClick={handlePendingAction}>
              중복 확인
            </VerifyButton>
          }
        />
        <TextInput id="edit-phone" label="전화번호" defaultValue="010-1234-5678" type="tel" inline compact />
        <TextInput id="edit-email" label="이메일" defaultValue="moa@example.com" type="email" disabled inline compact />
        <Divider />
        <PasswordInput
          id="edit-password"
          label="비밀번호 변경"
          placeholder="변경할 경우 8자 이상 입력"
          autoComplete="new-password"
          inline
          compact
          isVisible={isPasswordVisible}
          onToggleVisibility={() => setIsPasswordVisible((value) => !value)}
        />
        <PasswordInput
          id="edit-password-confirm"
          label="비밀번호 변경 확인"
          placeholder="새 비밀번호를 다시 입력"
          autoComplete="new-password"
          inline
          compact
          isVisible={isConfirmPasswordVisible}
          onToggleVisibility={() => setIsConfirmPasswordVisible((value) => !value)}
        />
        <InterestField>
          <Label>관심사</Label>
          <ChipList aria-label="관심사 선택">
            {interestOptions.map((interest) => (
              <Chip
                key={interest}
                type="button"
                $selected={selectedInterests.includes(interest)}
                onClick={() => toggleInterest(interest)}
                aria-pressed={selectedInterests.includes(interest)}
              >
                {selectedInterests.includes(interest) ? '✓ ' : ''}
                {interest}
              </Chip>
            ))}
          </ChipList>
          <Hint>관심사는 최대 2개까지 선택할 수 있습니다.</Hint>
        </InterestField>
        <FormActions>
          <ActionButton type="button" onClick={handleUpdateRequest}>
            수정
          </ActionButton>
          <ActionButton type="button" $variant="secondary" onClick={handleCancel}>
            취소
          </ActionButton>
        </FormActions>
      </FormArea>
    </Card>
  );
}

export default EditProfileForm;
