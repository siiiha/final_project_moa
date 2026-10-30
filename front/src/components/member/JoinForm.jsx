import { useEffect, useState } from 'react';
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
  signupSocialMember,
  getMemberRegions,
  getMemberTerms,
  memberInterestOptions,
} from '../../api/member';
import useAuthStore from '../../store/authStore';
import ConfirmModal from '../common/ConfirmModal';
import { interestIcons } from './interestsIcons';

const Form = styled.form`
  width: 100%;
`;

const DetailsGrid = styled.div`
  display: grid;
  grid-template-columns: ${({ $isSocialSignup }) =>
    $isSocialSignup ? '1fr' : 'repeat(2, minmax(0, 1fr))'};
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const FieldList = styled.div`
  display: grid;
  gap: 12px;
`;

// AI작성: 활동 지역 선택창을 기존 회원가입 입력창의 디자인과 통일한다.
const RegionField = styled.div`
  > p {
    margin-bottom: 12px;
    color: ${({ theme }) => theme.colors.text};
    font-size: 12px;
    font-weight: 700;
    letter-spacing: -0.35px;
  }

  label {
    min-width: 0;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    font-weight: 700;
    line-height: 1.5;
  }

  select {
    box-sizing: border-box;
    min-width: 0;
    height: 48px;
    color: ${({ theme }) => theme.colors.text};
    background-color: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
    font-family: inherit;
    font-size: 13px;
    font-weight: 400;
    cursor: pointer;
    transition: border-color 0.15s, box-shadow 0.15s;
  }

  select:hover:not(:disabled) {
    border-color: ${({ theme }) => theme.colors.primary};
  }

  select:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  select:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background-color: #eef2f1;
    cursor: not-allowed;
  }

  label[for='join-town'] {
    grid-column: 1 / -1;
  }
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

// AI작성: 안내 문구 아래에 관심사 버튼을 격자로 배치한다.
const InterestContent = styled.div`
  display: grid;
  gap: 16px;
`;

const InterestDescription = styled.div`
  min-width: 0;

  h3 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 16px;
    letter-spacing: -0.5px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 13px;
    line-height: 1.5;
  }
`;

// AI작성: PC는 4열, 모바일과 소셜 인증 창은 2열로 표시한다.
const ChipList = styled.div`
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
  }

  &:disabled {
    opacity: 0.45;
    cursor: not-allowed;
  }
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

function JoinForm({ isSocialSignup = false, signupToken = '' }) {
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);
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
  // AI작성: 서버의 지역 목록과 시·도 선택 상태를 관리한다.
  const [memberRegionOptions, setMemberRegionOptions] = useState([]);
  const [provinceName, setProvinceName] = useState('');
  const [districtRegionId, setDistrictRegionId] = useState('');
  const [isRegionsLoading, setIsRegionsLoading] = useState(true);
  const [regionsError, setRegionsError] = useState('');
  const [nicknameMessage, setNicknameMessage] = useState('');
  const [isCheckingNickname, setIsCheckingNickname] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [signupError, setSignupError] = useState('');
  // AI작성: 서버에서 조회한 약관과 조회 상태를 관리한다.
  const [memberTermOptions, setMemberTermOptions] = useState([]);
  const [isTermsLoading, setIsTermsLoading] = useState(true);
  const [termsError, setTermsError] = useState('');
  // AI작성: 사용자가 선택한 약관을 내용 보기 모달에 표시한다.
  const [selectedTerm, setSelectedTerm] = useState(null);

  useEffect(() => {
    let active = true;

    async function loadTerms() {
      try {
        const terms = await getMemberTerms();

        if (!Array.isArray(terms) || terms.length === 0) {
          throw new Error('약관 목록이 없습니다.');
        }

        if (active) {
          setMemberTermOptions(terms);
        }
      } catch {
        if (active) {
          setTermsError('약관을 불러오지 못했습니다. 새로고침해주세요.');
        }
      } finally {
        if (active) {
          setIsTermsLoading(false);
        }
      }
    }

    loadTerms();

    return () => {
      active = false;
    };
  }, []);

  // AI작성: 일반·소셜 가입에서 사용할 지역 목록을 서버에서 조회한다.
  useEffect(() => {
    let active = true;

    async function loadRegions() {
      try {
        const regions = await getMemberRegions();

        if (!Array.isArray(regions) || regions.length === 0) {
          throw new Error('지역 목록이 없습니다.');
        }

        if (active) {
          setMemberRegionOptions(regions);
        }
      } catch {
        if (active) {
          setRegionsError('지역을 불러오지 못했습니다. 새로고침해주세요.');
        }
      } finally {
        if (active) {
          setIsRegionsLoading(false);
        }
      }
    }

    loadRegions();

    return () => {
      active = false;
    };
  }, []);

  const handleInterestToggle = (interest) => {
    setSelectedInterests((current) => {
      if (current.includes(interest)) return current.filter((item) => item !== interest);
      if (current.length >= 3) return current;
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

    // AI작성: 약관을 정상적으로 조회한 경우에만 가입을 진행한다.
    if (isTermsLoading || termsError || memberTermOptions.length === 0) {
      setSignupError(
        termsError || '약관을 불러오는 중입니다. 잠시 후 다시 시도해주세요.',
      );
      return;
    }

    if (!isSocialSignup && !emailVerificationToken) {
      setSignupError('이메일 인증을 먼저 완료해주세요.');
      return;
    }

    // AI작성: 일반 가입에서만 비밀번호 길이를 검사한다.
    if (
      !isSocialSignup &&
      (password.length < 8 ||
        new TextEncoder().encode(password).length > 72)
    ) {
      setSignupError('비밀번호는 8자 이상, UTF-8 기준 72바이트 이내로 입력해주세요.');
      return;
    }

    // AI작성: 일반 가입에서만 비밀번호 확인을 검사한다.
    if (!isSocialSignup && password !== confirmPassword) {
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
    const selectedDistrict = memberRegionOptions.find(
      (region) => region.regionId === Number(districtRegionId),
    );

    const selectedTown = memberRegionOptions.find(
      (region) => region.regionId === selectedRegionId,
    );

    if (
      !selectedDistrict ||
      !selectedTown?.townName ||
      selectedDistrict.provinceName !== provinceName ||
      selectedTown.provinceName !== provinceName ||
      selectedTown.districtName !== selectedDistrict.districtName
    ) {
      setSignupError('활동 지역의 시·도, 시·군·구, 읍·면·동을 모두 선택해주세요.');
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
      // AI작성: 일반 가입과 소셜 가입에 공통으로 사용하는 정보다.
      const request = {
        name: trimmedName,
        nickname: trimmedNickname,
        phone: trimmedPhone,
        birthDate,
        gender,
        regionId: selectedRegionId,
        agreedTermIds,
        interestIds: selectedInterests,
      };

      if (isSocialSignup) {
        const result = await signupSocialMember({
          ...request,
          signupToken,
        });

        if (
          result?.signupRequired !== false ||
          typeof result.token !== 'string' ||
          !result.token.trim()
        ) {
          throw new Error('소셜 가입 결과를 확인할 수 없습니다.');
        }

        // AI작성: 가입 완료 후 임시 정보를 제거한다.
        sessionStorage.removeItem('moa-social-signup');
        setEmailVerificationToken('');

        // AI작성: 기존 인증 창에서 가입했다면 부모 화면에 로그인 결과를 전달한다.
        if (window.opener && !window.opener.closed) {
          window.opener.postMessage(
            {
              type: 'MOA_SOCIAL_RESULT',
              result,
            },
            window.location.origin,
          );
          return;
        }

        // AI작성: 일반 창에서 가입했다면 현재 화면에 로그인 상태를 반영한다.
        setToken(result.token);
        navigate('/', { replace: true });
        return;
      }

      const memberId = await signupMember({
        ...request,
        email: email.trim(),
        emailVerificationToken,
        password,
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
        <DetailsGrid $isSocialSignup={isSocialSignup}>
          {!isSocialSignup && (
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
                {/* AI작성: 일반 가입에서만 비밀번호 입력란을 표시한다. */}
                {!isSocialSignup && (
                  <>
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
                      onToggleVisibility={() =>
                        setIsConfirmPasswordVisible((value) => !value)
                      }
                    />
                  </>
                )}

              </FieldList>
            </SectionCard>
          )}
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

              <RegionField>
                <p>활동 지역</p>

                <div
                  style={{
                    display: 'grid',
                    gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                    gap: '10px',
                    marginTop: '8px',
                  }}
                >
                  <label htmlFor="join-province">
                    시·도
                    <select
                      id="join-province"
                      value={provinceName}
                      onChange={(event) => {
                        setProvinceName(event.target.value);
                        setDistrictRegionId('');
                        setRegionId('');
                      }}
                      disabled={isRegionsLoading || Boolean(regionsError) || isSubmitting}
                      required
                      style={{
                        display: 'block',
                        width: '100%',
                        minHeight: '44px',
                        marginTop: '6px',
                        padding: '8px',
                      }}
                    >
                      <option value="">시·도 선택</option>
                      {[...new Set(memberRegionOptions.map((region) => region.provinceName))]
                        .map((province) => (
                          <option key={province} value={province}>
                            {province}
                          </option>
                        ))}
                    </select>
                  </label>

                  <label htmlFor="join-region">
                    시·군·구
                    <select
                      id="join-region"
                      value={districtRegionId}
                      onChange={(event) => {
                        setDistrictRegionId(event.target.value);
                        setRegionId('');
                      }}
                      disabled={
                        !provinceName ||
                        isRegionsLoading ||
                        Boolean(regionsError) ||
                        isSubmitting
                      }
                      required
                      style={{
                        display: 'block',
                        width: '100%',
                        minHeight: '44px',
                        marginTop: '6px',
                        padding: '8px',
                      }}
                    >
                      <option value="">시·군·구 선택</option>
                      {memberRegionOptions
                        .filter(
                          (region) =>
                            region.provinceName === provinceName &&
                            !region.townName,
                        )
                        .map((region) => (
                          <option key={region.regionId} value={region.regionId}>
                            {region.districtName || '세종특별자치시 전체'}
                          </option>
                        ))}
                    </select>
                  </label>

                  <label htmlFor="join-town">
                    읍·면·동
                    <select
                      id="join-town"
                      value={regionId}
                      onChange={(event) => setRegionId(event.target.value)}
                      disabled={
                        !districtRegionId ||
                        isRegionsLoading ||
                        Boolean(regionsError) ||
                        isSubmitting
                      }
                      required
                      style={{
                        display: 'block',
                        width: '100%',
                        minHeight: '44px',
                        marginTop: '6px',
                        padding: '8px',
                      }}
                    >
                      <option value="">읍·면·동 선택</option>
                      {memberRegionOptions
                        .filter((region) => {
                          const district = memberRegionOptions.find(
                            (item) =>
                              item.regionId === Number(districtRegionId),
                          );

                          return (
                            district &&
                            region.townName &&
                            region.provinceName === district.provinceName &&
                            region.districtName === district.districtName
                          );
                        })
                        .map((region) => (
                          <option key={region.regionId} value={region.regionId}>
                            {region.townName}
                          </option>
                        ))}
                    </select>
                  </label>
                </div>

                {isRegionsLoading && (
                  <p role="status">지역 목록을 불러오고 있습니다.</p>
                )}
                {regionsError && <p role="alert">{regionsError}</p>}
              </RegionField>
            </FieldList>
          </SectionCard>
        </DetailsGrid>

        <InterestCard title="관심사">
          <InterestContent>
            <InterestDescription>
              <h3>어떤 활동에 관심이 있으신가요?</h3>
              <p aria-live="polite">
                최대 3개 선택 · 선택사항 · 선택 {selectedInterests.length}/3
              </p>
            </InterestDescription>

            <ChipList aria-label="관심사 선택">
              {memberInterestOptions.map((interest) => {
                const selected = selectedInterests.includes(
                  interest.interestId,
                );
                const InterestIcon = interestIcons[interest.interestId];
                return (
                  <Chip
                    key={interest.interestId}
                    type="button"
                    $selected={selected}
                    onClick={() => handleInterestToggle(interest.interestId)}
                    aria-pressed={selected}
                    disabled={!selected && selectedInterests.length >= 3}
                  >
                    {InterestIcon && (
                      <InterestIcon size={18} aria-hidden="true" />
                    )}
                    <span>{interest.interestName}</span>
                  </Chip>
                );
              })}
            </ChipList>
          </InterestContent>
        </InterestCard>

        <AgreementCard title="약관 동의">
          {isTermsLoading && (
            <p role="status">약관을 불러오고 있습니다.</p>
          )}
          {termsError && <p role="alert">{termsError}</p>}

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
                <DetailButton
                  type="button"
                  onClick={() => setSelectedTerm(term)}
                >
                  내용 보기
                </DetailButton>
              </AgreementRow>
            ))}
          </AgreementList>
        </AgreementCard>

        {signupError && <p role="alert">{signupError}</p>}

        <SubmitButton
          type="submit"
          disabled={
            isSubmitting ||
            isEmailBusy ||
            isCheckingNickname ||
            isTermsLoading ||
            Boolean(termsError) ||
            memberTermOptions.length === 0
          }
        >
          {isSubmitting ? '가입 처리 중…' : '회원가입'}
        </SubmitButton>
      </fieldset>
      {/* AI작성: 약관 본문을 확인하고 닫아도 동의 여부는 직접 선택한다. */}
      {selectedTerm && (
        <ConfirmModal
          title={selectedTerm.title}
          description={`버전: ${selectedTerm.version}`}
          size="wide"
          confirmLabel="닫기"
          showCancel={false}
          onConfirm={() => setSelectedTerm(null)}
          onCancel={() => setSelectedTerm(null)}
        >
          <div
            style={{
              maxHeight: '50vh',
              overflowY: 'auto',
              whiteSpace: 'pre-wrap',
              overflowWrap: 'anywhere',
              lineHeight: 1.7,
            }}
          >
            {selectedTerm.content}
          </div>
        </ConfirmModal>
      )}
    </Form>
  );
}

export default JoinForm;
