import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../common/Button';
import Checkbox from '../common/Checkbox';
import PasswordInput from '../common/PasswordInput';
import SectionCard from '../common/SectionCard';
import TextInput from '../common/TextInput';
import {
  sendEmailVerification,
  verifyEmailCode,
  checkNicknameAvailable,
  signupMember,
  memberRegionOptions,
  memberTermOptions,
  memberInterestOptions,
} from '../../api/member';

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
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [verificationRequestId, setVerificationRequestId] = useState(null);
  const [emailVerificationToken, setEmailVerificationToken] = useState('');
  const [isEmailBusy, setIsEmailBusy] = useState(false);
  const [emailMessage, setEmailMessage] = useState('');
  const [emailError, setEmailError] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);
  const [gender, setGender] = useState('');
  const [selectedInterests, setSelectedInterests] = useState([]);
  const [agreements, setAgreements] = useState({});
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [name, setName] = useState('');
  const [nickname, setNickname] = useState('');
  const [phone, setPhone] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [regionId, setRegionId] = useState('');
  const [nicknameMessage, setNicknameMessage] = useState('');
  const [isCheckingNickname, setIsCheckingNickname] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [signupError, setSignupError] = useState('');
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

    // AI작성: 인증번호를 발송하고 해당 요청 번호를 보관합니다.
  const handleVerification = async () => {
    if (isEmailBusy) return;

    const trimmedEmail = email.trim();

    if (
      trimmedEmail.length > 40 ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)
    ) {
      setEmailError('40자 이내의 올바른 이메일을 입력해주세요.');
      return;
    }

    setIsEmailBusy(true);
    setEmailError('');
    setEmailMessage('');
    setVerificationRequestId(null);
    setEmailVerificationToken('');
    setCode('');

    try {
      const requestId = await sendEmailVerification(trimmedEmail);

      if (!Number.isInteger(requestId) || requestId <= 0) {
        throw new Error('인증 요청 번호가 없습니다.');
      }

      setVerificationRequestId(requestId);
      setEmailMessage(
        '인증번호를 발송했습니다. 3분 안에 입력해주세요.',
      );
    } catch (error) {
      setEmailError(
        error.response?.data?.error?.message ??
          '인증번호 발송에 실패했습니다.',
      );
    } finally {
      setIsEmailBusy(false);
    }
  };

  // 인증번호를 확인하고 가입용 토큰을 화면 안에서만 보관
  const handleCodeVerification = async () => {
    if (isEmailBusy) return;

    if (!verificationRequestId || !/^[0-9]{6}$/.test(code.trim())) {
      setEmailError('인증번호를 요청한 뒤 숫자 6자리를 입력해주세요.');
      return;
    }

    setIsEmailBusy(true);
    setEmailError('');
    setEmailMessage('');

    try {
      const token = await verifyEmailCode(
        verificationRequestId,
        code.trim(),
      );

      if (typeof token !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(token)) {
        throw new Error('인증 결과를 확인할 수 없습니다.');
      }

      setEmailVerificationToken(token);
      setEmailMessage(
        '이메일 인증이 완료됐습니다. 5분 안에 회원가입을 완료해주세요.',
      );
    } catch (error) {
      setEmailError(
        error.response?.data?.error?.message ??
          '인증번호 확인에 실패했습니다.',
      );
    } finally {
      setIsEmailBusy(false);
    }
  };

    // 현재 입력한 닉네임의 사용 가능 여부를 확인
  const handleDuplicateCheck = async () => {
    if (isCheckingNickname || isSubmitting) return;

    const trimmedNickname = nickname.trim();

    if (trimmedNickname.length < 2 || trimmedNickname.length > 12) {
      setNicknameMessage('닉네임은 2~12자로 입력해주세요.');
      return;
    }

    setIsCheckingNickname(true);
    setNicknameMessage('');

    try {
      const available = await checkNicknameAvailable(trimmedNickname);

      if (typeof available !== 'boolean') {
        throw new Error('중복 확인 결과를 확인할 수 없습니다.');
      }

      setNicknameMessage(
        available
          ? '사용 가능한 닉네임입니다.'
          : '이미 사용 중인 닉네임입니다.',
      );
    } catch (error) {
      setNicknameMessage(
        error.response?.data?.error?.message ??
          '닉네임 중복 확인에 실패했습니다.',
      );
    } finally {
      setIsCheckingNickname(false);
    }
  };

  // AI작성: 인증 완료 토큰과 회원정보를 서버로 전송합니다.
  const handleSubmit = async (event) => {
    event.preventDefault();

    if (isSubmitting || isEmailBusy || isCheckingNickname) return;

    setSignupError('');

    if (!emailVerificationToken) {
      setSignupError('이메일 인증을 먼저 완료해주세요.');
      return;
    }

    if (
      password.length < 8 ||
      new TextEncoder().encode(password).length > 72
    ) {
      setSignupError('비밀번호는 8자 이상, UTF-8 기준 72바이트 이내로 입력해주세요.');
      return;
    }

    if (password !== confirmPassword) {
      setSignupError('비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    const trimmedName = name.trim();
    const trimmedNickname = nickname.trim();
    const trimmedPhone = phone.trim();

    if (!trimmedName || trimmedName.length > 10) {
      setSignupError('이름은 공백 없이 시작하는 1~10자로 입력해주세요.');
      return;
    }

    if (trimmedNickname.length < 2 || trimmedNickname.length > 12) {
      setSignupError('닉네임은 2~12자로 입력해주세요.');
      return;
    }

    if (!trimmedPhone || trimmedPhone.length > 20) {
      setSignupError('전화번호는 1~20자로 입력해주세요.');
      return;
    }

    const today = new Date();
    const todayText = [
      today.getFullYear(),
      String(today.getMonth() + 1).padStart(2, '0'),
      String(today.getDate()).padStart(2, '0'),
    ].join('-');

    if (!birthDate || birthDate >= todayText) {
      setSignupError('생년월일은 오늘보다 이전 날짜여야 합니다.');
      return;
    }

    if (!['MALE', 'FEMALE'].includes(gender)) {
      setSignupError('성별을 선택해주세요.');
      return;
    }

    const selectedRegionId = Number(regionId);

    if (
      !memberRegionOptions.some(
        (region) => region.regionId === selectedRegionId,
      )
    ) {
      setSignupError('활동 지역을 선택해주세요.');
      return;
    }

    const missingRequiredTerm = memberTermOptions.some(
      (term) => term.required && !agreements[term.termId],
    );

    if (missingRequiredTerm) {
      setSignupError('필수 약관에 모두 동의해주세요.');
      return;
    }

    const agreedTermIds = memberTermOptions
      .filter((term) => agreements[term.termId])
      .map((term) => term.termId);

    setIsSubmitting(true);

    try {
      const memberId = await signupMember({
        email: email.trim(),
        emailVerificationToken,
        password,
        name: trimmedName,
        nickname: trimmedNickname,
        phone: trimmedPhone,
        birthDate,
        gender,
        regionId: selectedRegionId,
        agreedTermIds,
        interestIds: selectedInterests,
      });

      if (!Number.isInteger(memberId) || memberId <= 0) {
        throw new Error('가입 결과를 확인할 수 없습니다.');
      }

      setEmailVerificationToken('');
      setPassword('');
      setConfirmPassword('');

      window.alert('회원가입이 완료됐습니다. 로그인해주세요.');
      navigate('/member/login', { replace: true });
    } catch (error) {
      const errorCode = error.response?.data?.error?.code;

      if (errorCode === 'INVALID_VERIFICATION_TOKEN') {
        setEmailVerificationToken('');
        setVerificationRequestId(null);
        setCode('');
        setEmailMessage('');
      }

      setSignupError(
        error.response?.data?.error?.message ??
          '회원가입에 실패했습니다. 서버 연결을 확인해주세요.',
      );
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      <fieldset
        disabled={isSubmitting || isCheckingNickname}
        style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
      >
        <DetailsGrid>
          <SectionCard title="계정 정보">
            <FieldList>
              <TextInput
                id="join-email"
              label="이메일"
              type="email"
              placeholder="이메일 입력"
              value={email}
              onChange={(event) => {
                setEmail(event.target.value);
                setVerificationRequestId(null);
                setEmailVerificationToken('');
                setCode('');
                setEmailMessage('');
                setEmailError('');
              }}
              maxLength={40}
              required
              disabled={isEmailBusy}
              inline
              compact
              action={
                <VerifyButton
                  type="button"
                  $variant="secondary"
                  onClick={handleVerification}
                  disabled={isEmailBusy}
                >
                  {isEmailBusy ? '처리 중…' : '인증번호 받기'}
                </VerifyButton>
              }
            />

            <TextInput
              id="verification-code"
              label="인증번호"
              inputMode="numeric"
              placeholder="6자리 입력"
              value={code}
              onChange={(event) => setCode(event.target.value)}
              maxLength={6}
              disabled={
                isEmailBusy ||
                !verificationRequestId ||
                Boolean(emailVerificationToken)
              }
              inline
              compact
              helpText="메일로 받은 인증번호를 입력해주세요."
              action={
                <VerifyButton
                  type="button"
                  $variant="secondary"
                  onClick={handleCodeVerification}
                  disabled={
                    isEmailBusy ||
                    !verificationRequestId ||
                    Boolean(emailVerificationToken)
                  }
                >
                  {emailVerificationToken ? '인증 완료' : '인증 확인'}
                </VerifyButton>
              }
            />

            {emailMessage && <p role="status">{emailMessage}</p>}
            {emailError && <p role="alert">{emailError}</p>}
                       <PasswordInput
              id="join-password"
              label="비밀번호"
              placeholder="8자 이상 입력"
              autoComplete="new-password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              minLength={8}
              required
              inline
              compact
              isVisible={isPasswordVisible}
              onToggleVisibility={() => setIsPasswordVisible((value) => !value)}
            />

            <PasswordInput
              id="join-password-confirm"
              label="비밀번호 확인"
              placeholder="비밀번호를 다시 입력"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
              required
              inline
              compact
              isVisible={isConfirmPasswordVisible}
              onToggleVisibility={() => setIsConfirmPasswordVisible((value) => !value)}
            />
          </FieldList>
        </SectionCard>

        <SectionCard title="기본 정보">
          <FieldList>
            <TextInput
              id="join-name"
              label="이름"
              placeholder="이름 입력"
              value={name}
              onChange={(event) => setName(event.target.value)}
              maxLength={10}
              required
              inline
              compact
            />

            <TextInput
              id="nickname"
              label="닉네임"
              placeholder="2~12자 입력"
              value={nickname}
              onChange={(event) => {
                setNickname(event.target.value);
                setNicknameMessage('');
              }}
              minLength={2}
              maxLength={12}
              required
              inline
              compact
              action={
                <VerifyButton
                  type="button"
                  $variant="secondary"
                  onClick={handleDuplicateCheck}
                  disabled={isCheckingNickname}
                >
                  {isCheckingNickname ? '확인 중…' : '중복 확인'}
                </VerifyButton>
              }
            />

            {nicknameMessage && <p role="status">{nicknameMessage}</p>}

            <TextInput
              id="phone"
              label="전화번호"
              type="tel"
              placeholder="전화번호 입력"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              maxLength={20}
              required
              inline
              compact
            />

            <TextInput
              id="birth-date"
              label="생년월일"
              type="date"
              value={birthDate}
              onChange={(event) => setBirthDate(event.target.value)}
              required
              inline
              compact
            />

            <GenderField>
              <GenderLabel>성별</GenderLabel>
              <GenderOptions>
                <GenderButton
                  type="button"
                  $selected={gender === 'FEMALE'}
                  onClick={() => setGender('FEMALE')}
                  aria-pressed={gender === 'FEMALE'}
                >
                  여성
                </GenderButton>
                <GenderButton
                  type="button"
                  $selected={gender === 'MALE'}
                  onClick={() => setGender('MALE')}
                  aria-pressed={gender === 'MALE'}
                >
                  남성
                </GenderButton>
              </GenderOptions>
            </GenderField>

            <label htmlFor="join-region">
              활동 지역
              <select
                id="join-region"
                value={regionId}
                onChange={(event) => setRegionId(event.target.value)}
                required
                style={{
                  display: 'block',
                  width: '100%',
                  padding: '10px',
                  marginTop: '8px',
                }}
              >
                <option value="">지역을 선택해주세요</option>
                {memberRegionOptions.map((region) => (
                  <option key={region.regionId} value={region.regionId}>
                    {region.regionName}
                  </option>
                ))}
              </select>
            </label>
          </FieldList>
        </SectionCard>
      </DetailsGrid>

      <InterestCard title="관심사">
        <InterestContent>
          <InterestDescription>
            <h3>좋아하는 활동을 선택해주세요</h3>
            <p>최대 2개 선택 · 선택사항</p>
          </InterestDescription>
          <ChipList aria-label="관심사 선택">
            {memberInterestOptions.map((interest) => (
              <Chip
                key={interest.interestId}
                type="button"
                $selected={selectedInterests.includes(interest.interestId)}
                onClick={() => handleInterestToggle(interest.interestId)}
                aria-pressed={selectedInterests.includes(interest.interestId)}
              >
                {interest.interestName}
              </Chip>
            ))}
          </ChipList>
        </InterestContent>
      </InterestCard>

      <AgreementCard title="약관 동의">
        <AgreementList>
          {memberTermOptions.map((term) => (
            <AgreementRow key={term.termId}>
              <Checkbox
                id={`agreement-${term.termId}`}
                label={`${term.required ? '[필수]' : '[선택]'} ${term.title}`}
                checked={Boolean(agreements[term.termId])}
                onChange={() => handleAgreementChange(term.termId)}
                required={term.required}
              />
              <DetailButton type="button" disabled>
                테스트 약관
              </DetailButton>
            </AgreementRow>
          ))}
        </AgreementList>
      </AgreementCard>

      {signupError && <p role="alert">{signupError}</p>}

      <SubmitButton
        type="submit"
        disabled={isSubmitting || isEmailBusy || isCheckingNickname}
      >
        {isSubmitting ? '가입 처리 중…' : '회원가입'}
        </SubmitButton>
      </fieldset>
    </Form>
  );
}

export default JoinForm;
