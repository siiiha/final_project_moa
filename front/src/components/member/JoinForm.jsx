import { useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import PasswordInput from '../common/PasswordInput';
import SectionCard from '../common/SectionCard';
import TextInput from '../common/TextInput';
import { agreementOptions, interestOptions } from '../../mocks/join';

const Form = styled.form`
  width: 100%;
`;

const DetailsGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const FieldList = styled.div`
  display: grid;
  gap: 12px;
`;

const VerifyButton = styled(Button)`
  min-width: 88px;
  min-height: 42px;
  padding: 8px 11px;
  font-size: 11px;
`;

const GenderField = styled.div`
  min-width: 0;
  display: grid;
  grid-template-columns: 92px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
`;

const GenderLabel = styled.p`
  grid-column: 1;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
  letter-spacing: -0.35px;
`;

const GenderOptions = styled.div`
  grid-column: 2;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
`;

const GenderButton = styled.button`
  height: 42px;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primarySoft : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

const InterestCard = styled(SectionCard)`
  margin-top: 16px;

  > div {
    padding: 15px 18px;
  }
`;

const InterestContent = styled.div`
  display: flex;
  align-items: center;
  gap: 24px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
`;

const InterestDescription = styled.div`
  min-width: 230px;

  h3 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 16px;
    letter-spacing: -0.75px;
  }

  p {
    margin-top: 4px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
  }
`;

const ChipList = styled.div`
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    justify-content: flex-start;
  }
`;

const Chip = styled.button`
  min-height: 32px;
  padding: 6px 14px;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.white : theme.colors.secondaryText)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 11px;
  font-weight: 700;
`;

const AgreementCard = styled(SectionCard)`
  margin-top: 16px;

  > div {
    padding: 17px 18px;
  }
`;

const AgreementList = styled.div`
  display: grid;
  gap: 13px;
`;

const AgreementRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
`;

const DetailButton = styled.button`
  min-height: 28px;
  padding: 5px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 10px;
  font-weight: 700;
`;

const SubmitButton = styled(Button)`
  width: 100%;
  min-height: 44px;
  margin-top: 16px;
  font-size: 13px;
`;

function JoinForm() {
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);
  const [gender, setGender] = useState('female');
  const [selectedInterests, setSelectedInterests] = useState([]);
  const [agreements, setAgreements] = useState({ service: false, privacy: false });

  const handleInterestToggle = (interest) => {
    setSelectedInterests((current) => {
      if (current.includes(interest)) return current.filter((item) => item !== interest);
      if (current.length === 2) return current;
      return [...current, interest];
    });
  };

  const handleAgreementChange = (id) => {
    setAgreements((current) => ({ ...current, [id]: !current[id] }));
  };

  const handleVerification = () => {
    // TODO: Spring Boot 이메일 인증 API 연동 후 인증번호 발송 및 확인 상태를 반영합니다.
  };

  const handleDuplicateCheck = () => {
    // TODO: Spring Boot 중복 확인 API 연동 후 이메일·닉네임 사용 가능 여부를 표시합니다.
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 회원가입 API 연동 후 가입 완료 화면 또는 로그인 화면으로 이동합니다.
  };

  return (
    <Form onSubmit={handleSubmit}>
      <DetailsGrid>
        <SectionCard title="계정 정보">
          <FieldList>
            <TextInput
              id="join-email"
              label="이메일"
              type="email"
              placeholder="이메일 입력"
              inline
              compact
              action={
                <VerifyButton type="button" $variant="secondary" onClick={handleDuplicateCheck}>
                  중복 확인
                </VerifyButton>
              }
            />
            <TextInput
              id="verification-code"
              label="인증번호"
              inputMode="numeric"
              placeholder="6자리 입력"
              inline
              compact
              helpText="이메일 인증은 실제 서비스 연결 후 제공됩니다."
              action={
                <VerifyButton type="button" $variant="secondary" onClick={handleVerification}>
                  인증번호 받기
                </VerifyButton>
              }
            />
            <PasswordInput
              id="join-password"
              label="비밀번호"
              placeholder="8자 이상 입력"
              autoComplete="new-password"
              inline
              compact
              isVisible={isPasswordVisible}
              onToggleVisibility={() => setIsPasswordVisible((visible) => !visible)}
            />
            <PasswordInput
              id="join-password-confirm"
              label="비밀번호 확인"
              placeholder="비밀번호를 다시 입력"
              autoComplete="new-password"
              inline
              compact
              isVisible={isConfirmPasswordVisible}
              onToggleVisibility={() => setIsConfirmPasswordVisible((visible) => !visible)}
            />
          </FieldList>
        </SectionCard>
        <SectionCard title="기본 정보">
          <FieldList>
            <TextInput id="join-name" label="이름" placeholder="이름 입력" inline compact />
            <TextInput
              id="nickname"
              label="닉네임"
              placeholder="이웃에게 보이는 이름"
              inline
              compact
              action={
                <VerifyButton type="button" $variant="secondary" onClick={handleDuplicateCheck}>
                  중복 확인
                </VerifyButton>
              }
            />
            <TextInput id="phone" label="전화번호" type="tel" placeholder="010-1234-5678" inline compact />
            <TextInput id="birth-year" label="생년월일 (선택)" inputMode="numeric" placeholder="1990" inline compact />
            <GenderField>
              <GenderLabel>성별 (선택)</GenderLabel>
              <GenderOptions>
                <GenderButton
                  type="button"
                  $selected={gender === 'female'}
                  onClick={() => setGender('female')}
                  aria-pressed={gender === 'female'}
                >
                  여성
                </GenderButton>
                <GenderButton
                  type="button"
                  $selected={gender === 'male'}
                  onClick={() => setGender('male')}
                  aria-pressed={gender === 'male'}
                >
                  남성
                </GenderButton>
              </GenderOptions>
            </GenderField>
          </FieldList>
        </SectionCard>
      </DetailsGrid>

      <InterestCard title="관심사">
        <InterestContent>
          <InterestDescription>
            <h3>좋아하는 활동을 선택해주세요</h3>
            <p>최대 2개 선택</p>
          </InterestDescription>
          <ChipList aria-label="관심사 선택">
            {interestOptions.map((interest) => (
              <Chip
                key={interest}
                type="button"
                $selected={selectedInterests.includes(interest)}
                onClick={() => handleInterestToggle(interest)}
                aria-pressed={selectedInterests.includes(interest)}
              >
                {interest}
              </Chip>
            ))}
          </ChipList>
        </InterestContent>
      </InterestCard>

      <AgreementCard title="약관 동의">
        <AgreementList>
          {agreementOptions.map((agreement) => (
            <AgreementRow key={agreement.id}>
              <Checkbox
                id={`agreement-${agreement.id}`}
                label={agreement.label}
                checked={agreements[agreement.id]}
                onChange={() => handleAgreementChange(agreement.id)}
              />
              <DetailButton type="button">내용 보기 ›</DetailButton>
            </AgreementRow>
          ))}
        </AgreementList>
      </AgreementCard>
      <SubmitButton type="submit">회원가입</SubmitButton>
    </Form>
  );
}

export default JoinForm;
