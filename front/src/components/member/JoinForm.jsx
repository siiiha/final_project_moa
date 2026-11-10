import { useEffect, useMemo, useRef, useState } from 'react';
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
  max-width: ${({ $isDetailsStep }) => ($isDetailsStep ? '560px' : 'none')};
  margin-inline: auto;
`;

const FieldList = styled.div`
  display: grid;
  gap: 12px;
`;
// AI작성: 이메일 오류 안내를 회원가입 화면에서 명확하게 표시합니다.
const EmailFeedback = styled.p`
  && {
    margin: 4px 0 0;
    color: ${({ theme }) => theme.colors.danger};
    font-size: 13px;
    line-height: 1.5;
  }
`;

// AI작성: 오류 안내 공간을 확보해 입력 중 화면이 움직이지 않도록 합니다.
const PasswordFeedback = styled.p`
  && {
    min-height: 20px;
    margin: 8px 0 0;
    color: ${({ theme }) => theme.colors.danger};
    font-size: 13px;
    line-height: 1.5;
  }
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

const GenderPicker = styled.button`
  min-height: 42px;
  margin-bottom: 28px;
  padding: 8px 0;
  border: 0;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  background: transparent;
  color: inherit;
  font: inherit;
  text-align: left;
  cursor: pointer;
`;

const GenderDialog = styled.dialog`
  box-sizing: border-box;
  inset: 0;
  margin: auto;
  width: min(400px, calc(100% - 32px));
  padding: 28px 24px 20px;
  border: 0;
  border-radius: 16px;
  background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.text};

  &::backdrop {
    background: rgb(0 0 0 / 40%);
  }

  h3 {
    margin: 0 30px 16px 0;
    font-size: 18px;
  }

  button {
    border: 0;
    background: transparent;
    color: inherit;
    font: inherit;
    cursor: pointer;
  }

  .close {
    position: absolute;
    top: 12px;
    right: 14px;
    font-size: 24px;
  }

  .option {
    display: block;
    width: 100%;
    min-height: 48px;
    padding: 0 12px;
    border-radius: 8px;
    text-align: left;
  }

  .option:hover,
  .option[aria-pressed='true'] {
    background: ${({ theme }) => theme.colors.primarySoft};
  }
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

const TermsStep = styled.div`
  width: 100%;
  max-width: 520px;
  margin: 0 auto;

  > button:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
`;

const AllAgreement = styled.div`
  padding: 4px 12px;
  border-radius: ${({ theme }) => theme.radius.sm};
  background: ${({ theme }) => theme.colors.primarySoft};

    > label {
    min-height: 48px;
    gap: 10px;
    font-size: 18px;
    font-weight: 800;

    > span {
      width: 22px;
      height: 22px;
      border-radius: 50%;
    }
  }
`;

const TermsHint = styled.p`
  margin: 12px 0 0;
  color: #9a3412;
  font-size: 14px;
  line-height: 1.5;

  &:empty {
    margin: 0;
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

// AI작성: 약관 목록 아래에서 개인정보 이용 목적을 펼쳐 볼 수 있다.
const PrivacyNotice = styled.details`
  margin-top: 20px;
  padding: 14px 16px;
  border-radius: ${({ theme }) => theme.radius.sm};
  background: ${({ theme }) => theme.colors.primarySoft};
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 14px;
  line-height: 1.6;
  overflow-wrap: anywhere;

  summary {
    cursor: pointer;
    color: ${({ theme }) => theme.colors.text};
    font-weight: 700;
  }

  p {
    margin: 8px 0 0;
  }
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

const SignupFieldGroup = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 6px;
  margin-bottom: 20px;

  > label {
    font-size: 13px;
    line-height: 20px;
  }

  > input,
  > ${GenderPicker} {
    box-sizing: border-box;
    width: 100%;
    min-height: 44px;
    margin: 0;
    padding: 10px 0;
    line-height: 22px;
  }

  > p {
    margin: 0;
    line-height: 20px;
  }
`;

// AI작성: 마지막 단계에서는 이전 입력 정보를 접어 관심사 선택에 집중하도록 합니다.
const PreviousInformation = styled.details`
  margin-top: 4px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 12px;

  > summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    min-height: 72px;
    padding: 16px;
    box-sizing: border-box;
    list-style: none;
    cursor: pointer;
    color: ${({ theme }) => theme.colors.text};

    &::-webkit-details-marker { display: none; }
    &:focus-visible {
      outline: 2px solid ${({ theme }) => theme.colors.primary};
      outline-offset: 3px;
      border-radius: 12px;
    }
    strong { display: block; font-size: 15px; }
    small {
      display: block;
      margin-top: 5px;
      font-size: 12px;
      line-height: 1.5;
      color: ${({ theme }) => theme.colors.secondaryText};
    }
  }

  .review-action {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
    font-size: 13px;
    color: ${({ theme }) => theme.colors.primary};
  }
  .review-action::after {
    content: '';
    width: 7px;
    height: 7px;
    border-right: 1.5px solid currentColor;
    border-bottom: 1.5px solid currentColor;
    transform: rotate(45deg);
  }
  .review-close { display: none; }
  &[open] .review-open { display: none; }
  &[open] .review-close { display: inline; }
  &[open] .review-action::after { transform: rotate(225deg); }
  > div {
    padding: 20px 16px 0;
    border-top: 1px solid ${({ theme }) => theme.colors.border};
  }
`;

const SignupInputCard = styled.section`
  box-sizing: border-box;
  width: 100%;
  max-width: 520px;
  min-height: 460px;
  margin-inline: auto;
  padding: clamp(24px, 6vw, 50px);
  display: flex;
  flex-direction: column;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 20px;

  h2 {
    margin: 0 0 40px;
    font-size: 22px;
    font-weight: 700;
  }

  label,
  p {
    color: ${({ theme }) => theme.colors.secondaryText};
  }

  input {
    width: 100%;
    box-sizing: border-box;
    padding: 8px 0;
    border: 0;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: 0;
    background: transparent;
    font: inherit;
  }

  input:-webkit-autofill,
  input:-webkit-autofill:hover,
  input:-webkit-autofill:focus {
    box-shadow: inset 0 0 0 1000px ${({ theme }) => theme.colors.white};
    -webkit-text-fill-color: ${({ theme }) => theme.colors.text};
    caret-color: ${({ theme }) => theme.colors.text};
  }

  input:focus {
    outline: none;
    border-bottom-color: ${({ theme }) => theme.colors.primary};
  }

  p {
    margin: 10px 0 24px;
    font-size: 14px;
  }

  ${SubmitButton} {
    min-height: 48px;
    margin-top: auto;
    font-size: 15px;
  }

  ${SubmitButton}:disabled {
    background: #f3f3f3;
    border-color: transparent;
    color: #666;
  }
`;


function JoinForm({ isSocialSignup = false, signupToken = '' }) {
  const navigate = useNavigate();
  const setToken = useAuthStore((state) => state.setToken);
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [verificationRequestId, setVerificationRequestId] = useState(null);
  const [emailVerificationToken, setEmailVerificationToken] = useState('');
  const [verificationDeadline, setVerificationDeadline] = useState(0);
  const [remainingSeconds, setRemainingSeconds] = useState(0);

  useEffect(() => {
    if (!verificationRequestId || !verificationDeadline || emailVerificationToken) {
      return;
    }

    const updateRemaining = () => {
      const seconds = Math.max(0, Math.ceil((verificationDeadline - Date.now()) / 1000));
      setRemainingSeconds(seconds);
      return seconds;
    };

    if (updateRemaining() === 0) return;

    const timerId = window.setInterval(() => {
      if (updateRemaining() === 0) window.clearInterval(timerId);
    }, 250);

    return () => window.clearInterval(timerId);
  }, [verificationRequestId, verificationDeadline, emailVerificationToken]);
  const [isEmailBusy, setIsEmailBusy] = useState(false);
  const [emailMessage, setEmailMessage] = useState('');
  const [emailError, setEmailError] = useState('');
  const [isPasswordVisible, setIsPasswordVisible] = useState(false);
  const [isConfirmPasswordVisible, setIsConfirmPasswordVisible] = useState(false);
  const [gender, setGender] = useState('');
  const genderDialogRef = useRef(null);
  const signupCardRef = useRef(null);
  // AI작성: 검증 오류가 있으면 접힌 입력 정보를 펼칩니다.
  const previousInformationRef = useRef(null);
  const [selectedInterests, setSelectedInterests] = useState([]);
  const [agreements, setAgreements] = useState({});
  const [signupStep, setSignupStep] = useState('terms');
  // AI작성: 다음 단계가 표시되면 새 입력 카드의 시작 위치로 이동합니다.
  useEffect(() => {
    if (signupStep === 'terms') return;

    const frameId = window.requestAnimationFrame(() => {
      signupCardRef.current?.scrollIntoView({
        behavior: 'auto',
        block: 'start',
      });
    });

    return () => window.cancelAnimationFrame(frameId);
  }, [signupStep]);
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  // AI작성: 비밀번호 입력을 마친 뒤 오류를 표시하고, 수정하면 즉시 갱신합니다.
  const [isPasswordTouched, setIsPasswordTouched] = useState(false);
  const [hasPasswordSubmitAttempt, setHasPasswordSubmitAttempt] = useState(false);
  const passwordError =
    password.length < 8 || password.length > 20
      ? '비밀번호는 8~20자로 입력해 주세요.'
      : !/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!-/:-@\[-`{-~])[!-~]{8,20}$/.test(password)
        ? '영문, 숫자, 특수문자를 각각 하나 이상 포함해 주세요.'
        : '';
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
  const missingRequiredTerms = memberTermOptions.filter(
    (term) => term.required && !agreements[term.termId],
  );

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

      setVerificationDeadline(Date.now() + 180_000);
      setRemainingSeconds(180);
      setVerificationRequestId(requestId);
      setEmailMessage(
        '인증번호를 발송했습니다. 3분 안에 입력해주세요.',
      );
    } catch (error) {
      // AI작성: 이미 가입된 이메일에는 로그인 방법을 안내합니다.
      const errorCode = error.response?.data?.error?.code;

      setEmailError(
        errorCode === 'EMAIL_ALREADY_EXISTS'
          ? '이미 가입된 이메일이에요. 로그인하거나 다른 이메일을 입력해 주세요.'
          : error.response?.data?.error?.message ??
          '인증번호를 보내지 못했어요. 잠시 후 다시 시도해 주세요.',
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
      setEmailMessage('이메일 인증이 완료됐어요.');
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
    if (signupStep !== 'details') return;

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

    if (
      !isSocialSignup &&
      !/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!-/:-@\[-`{-~])[!-~]{8,20}$/.test(password)
    ) {
      setSignupError(
        '영문, 숫자, 특수문자를 각각 포함해 8~20자로 설정해 주세요.'
      );
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
  // AI작성: 지역 데이터나 선택값이 바뀔 때만 목록을 다시 계산합니다.
  const provinceOptions = useMemo(
    () => [...new Set(memberRegionOptions.map((region) => region.provinceName))],
    [memberRegionOptions],
  );

  const districtOptions = useMemo(
    () => memberRegionOptions.filter(
      (region) => region.provinceName === provinceName && !region.townName,
    ),
    [memberRegionOptions, provinceName],
  );

  const townOptions = useMemo(() => {
    // AI작성: 선택한 시·군·구를 한 번만 찾아 반복 검색을 줄입니다.
    const district = memberRegionOptions.find(
      (region) => region.regionId === Number(districtRegionId),
    );

    if (!district) return [];

    return memberRegionOptions.filter(
      (region) =>
        region.townName &&
        region.provinceName === district.provinceName &&
        region.districtName === district.districtName,
    );
  }, [memberRegionOptions, districtRegionId]);

  const steps = [
    'name', 'birthDate', 'gender', 'phone',
    ...(!isSocialSignup ? ['email', 'password'] : []),
    'nickname', 'region', 'details',
  ];
  const stepIndex = steps.indexOf(signupStep);
  const titles = {
    name: '이름을 입력해 주세요',
    birthDate: '생년월일을 입력해 주세요',
    gender: '성별을 선택해 주세요',
    phone: '전화번호를 입력해 주세요',
    email: '이메일을 인증해 주세요',
    password: '비밀번호를 설정해 주세요',
    nickname: '닉네임을 입력해 주세요',
    region: '활동 지역을 선택해 주세요',
    details: '관심사를 선택해 주세요',
  };

  const getStepError = () => {
    const includes = (step) => steps.indexOf(step) <= stepIndex;
    let message = '';

    if (!/^[\p{L}\p{M}]+$/u.test(name) || name.length > 10) {
      message = '이름은 공백이나 특수기호 없이 1~10자로 입력해 주세요.';
    } else if (includes('birthDate')) {
      const date = new Date(`${birthDate}T00:00:00Z`);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (Number.isNaN(date.getTime()) || date.toISOString().slice(0, 10) !== birthDate ||
        new Date(`${birthDate}T00:00:00`) >= today) {
        message = '오늘보다 이전의 올바른 생년월일을 입력해 주세요.';
      }
    }

    if (!message && includes('gender') && !gender) {
      message = '성별을 선택해 주세요.';
    }
    if (!message && includes('phone') && (!phone.trim() || phone.trim().length > 20)) {
      message = '전화번호를 입력해 주세요.';
    }
    if (!message && !isSocialSignup && includes('email') && !emailVerificationToken) {
      message = '이메일 인증을 완료해 주세요.';
    }
    if (!message && !isSocialSignup && includes('password')) {
      // AI작성: 다음 단계로 이동하기 전에 비밀번호 조합과 길이를 검사합니다.
      if (
        !/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!-/:-@\[-`{-~])[!-~]{8,20}$/.test(password)
      ) {
        message = '영문, 숫자, 특수문자를 각각 포함해 8~20자로 설정해 주세요.';
      } else if (password !== confirmPassword) {
        message = '비밀번호와 비밀번호 확인이 일치하지 않습니다.';
      }
    }
    if (!message && includes('nickname') && (nickname.trim().length < 2 || nickname.trim().length > 12)) {
      message = '닉네임을 2~12자로 입력해 주세요.';
    }
    if (!message && includes('region') &&
      !memberRegionOptions.some((region) => region.regionId === Number(regionId) && region.townName)) {
      message = '활동 지역의 읍·면·동까지 선택해 주세요.';
    }

    return message;
  };

  // AI작성: 비밀번호 오류는 해당 입력칸 아래에 표시하고 중복 안내를 방지합니다.
  const validatePasswordFields = () => {
    const passwordStepIndex = steps.indexOf('password');
    if (isSocialSignup || stepIndex < passwordStepIndex) return true;

    setIsPasswordTouched(true);
    setHasPasswordSubmitAttempt(true);

    if (passwordError || password !== confirmPassword) {
      setSignupError('');
      return false;
    }

    return true;
  };

  const advanceSignup = () => {
    if (!validatePasswordFields()) return;

    const message = getStepError();
    setSignupError(message);
    if (!message) setSignupStep(steps[stepIndex + 1]);
  };

  // AI작성: 숨겨진 입력칸의 오류를 사용자가 확인하고 수정할 수 있도록 합니다.
  const revealPreviousInformation = () => {
    const review = previousInformationRef.current;
    if (!review) return;
    review.open = true;
    window.requestAnimationFrame(() => {
      const invalidInput = review.querySelector(':invalid, [aria-invalid="true"]');
      if (invalidInput) {
        invalidInput.focus();
        invalidInput.scrollIntoView({ block: 'center' });
      } else {
        review.scrollIntoView({ block: 'start' });
      }
    });
  };

  const submitSignup = (event) => {
    event.preventDefault();
    if (signupStep !== 'details') return;
    if (!validatePasswordFields()) {
      revealPreviousInformation();
      return;
    }

    const message = getStepError();
    if (message) {
      setSignupError(message);
      revealPreviousInformation();
      return;
    }

    return handleSubmit(event);
  };

  const fields = {
    name: (
      <>
        <label htmlFor="join-name">이름</label>
        <input
          id="join-name"
          autoComplete="name"
          aria-describedby={signupStep === 'name' ? 'signup-name-hint' : undefined}
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={10}
          required
        />
        {signupStep === 'name' && (<p id="signup-name-hint">
          실제 이름을 입력해 주세요. (특수기호, 공백 사용 불가)
        </p>)}
      </>
    ),
    birthDate: (
      <>
        <label htmlFor="birth-date">생년월일</label>
        <input
          id="birth-date"
          aria-label="생년월일"
          aria-describedby={signupStep === 'birthDate' ? 'signup-birth-hint' : undefined}
          type="text"
          inputMode="numeric"
          placeholder="0000.00.00"
          value={birthDate.replaceAll('-', '.')}
          onChange={(event) => {
            const digits = event.target.value.replace(/\D/g, '').slice(0, 8);
            const parts = [digits.slice(0, 4), digits.slice(4, 6), digits.slice(6)];

            setBirthDate(parts.filter(Boolean).join('-'));
            setSignupError('');
          }}
        />
        {signupStep === 'birthDate' && (<p id="signup-birth-hint">생년월일은 8자리 숫자로 입력해 주세요.</p>)}
      </>
    ),
    gender: (
      <>
        <label htmlFor="signup-gender-picker">성별</label>
        <GenderPicker
          id="signup-gender-picker"
          type="button"
          aria-haspopup="dialog"
          onClick={() => genderDialogRef.current.showModal()}
        >
          {gender === 'MALE' ? '남성' : gender === 'FEMALE' ? '여성' : '성별'}
        </GenderPicker>

        <GenderDialog ref={genderDialogRef} aria-labelledby="gender-picker-title">
          <button
            type="button"
            className="close"
            aria-label="성별 선택 닫기"
            onClick={() => genderDialogRef.current.close()}
          >
            ×
          </button>
          <h3 id="gender-picker-title">성별을 선택해 주세요</h3>

          {[
            ['MALE', '남성'],
            ['FEMALE', '여성'],
          ].map(([value, label]) => (
            <button
              key={value}
              type="button"
              className="option"
              aria-pressed={gender === value}
              onClick={() => {
                setGender(value);
                genderDialogRef.current.close();
              }}
            >
              {label}
            </button>
          ))}
        </GenderDialog>
      </>
    ),
    phone: (
      <>
        <label htmlFor="signup-phone">전화번호</label>
        <input
          id="signup-phone"
          type="tel"
          autoComplete="tel"
          placeholder="010-1234-5678"
          aria-describedby={signupStep === 'phone' ? 'signup-phone-hint' : undefined}
          value={phone}
          onChange={(event) => {
            const digits = event.target.value.replace(/\D/g, '').slice(0, 11);
            const parts = [digits.slice(0, 3), digits.slice(3, 7), digits.slice(7)];

            setPhone(parts.filter(Boolean).join('-'));
          }}
          required
        />
        {signupStep === 'phone' && (<p id="signup-phone-hint">사용 중인 전화번호를 입력해 주세요.</p>)}
      </>
    ),
    email: (
      <>
        {signupStep === 'email' ? (
          <>
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
                      remainingSeconds === 0 ||
                      Boolean(emailVerificationToken)
                    }
                  >
                    {emailVerificationToken ? '인증 완료' : '인증 확인'}
                  </VerifyButton>
                }
              />

              {emailMessage && <p role="status">{emailMessage}</p>}
              {verificationRequestId && !emailVerificationToken && (
                remainingSeconds > 0 ? (
                  <p
                    style={{
                      color: remainingSeconds <= 30 ? '#B42318' : '#166457',
                      fontWeight: 700,
                      fontVariantNumeric: 'tabular-nums',
                    }}
                  >
                    남은 시간 {Math.floor(remainingSeconds / 60)}:
                    {String(remainingSeconds % 60).padStart(2, '0')}
                  </p>
                ) : (
                  <p role="status">인증번호가 만료됐습니다. 인증번호를 다시 받아 주세요.</p>
                )
              )}
              {/* AI작성: 이메일 중복 및 인증 오류를 빨간색으로 안내합니다. */}
              {emailError && <EmailFeedback role="alert">{emailError}</EmailFeedback>}
            </FieldList>
          </>
        ) : (
          <>
            <label htmlFor="verified-signup-email">인증한 이메일</label>
            <input id="verified-signup-email" value={email.trim()} readOnly />
          </>
        )}
      </>
    ),
    password: (
      <>
        <FieldList>
          <PasswordInput
            id="join-password"
            label="비밀번호"
            placeholder="8~20자, 영문·숫자·특수문자 포함"
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            onBlur={() => setIsPasswordTouched(true)}
            aria-invalid={isPasswordTouched && Boolean(passwordError)}
            aria-describedby="join-password-policy"
            minLength={8}
            required
            inline
            compact
            isVisible={isPasswordVisible}
            onToggleVisibility={() => setIsPasswordVisible((value) => !value)}
          />
          {/* AI작성: 입력칸을 벗어난 뒤 조건에 맞지 않으면 안내합니다. */}
          <PasswordFeedback id="join-password-policy" role="status">
            {isPasswordTouched ? passwordError : ''}
          </PasswordFeedback>

          <PasswordInput
            id="join-password-confirm"
            label="비밀번호 확인"
            placeholder="비밀번호를 다시 입력"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => setConfirmPassword(event.target.value)}
            aria-invalid={(confirmPassword !== '' || hasPasswordSubmitAttempt) && password !== confirmPassword}
            aria-describedby="join-password-feedback"
            required
            inline
            compact
            isVisible={isConfirmPasswordVisible}
            onToggleVisibility={() =>
              setIsConfirmPasswordVisible((value) => !value)
            }
          />
        </FieldList>

        {/* AI작성: 이전 단계에서도 비밀번호 확인값의 불일치를 즉시 안내합니다. */}
        <PasswordFeedback id="join-password-feedback" role="status">
          {(confirmPassword !== '' || hasPasswordSubmitAttempt) && password !== confirmPassword
            ? '비밀번호가 일치하지 않아요. 다시 확인해 주세요.'
            : ''}
        </PasswordFeedback>

      </>
    ),
    nickname: (
      <>
        <FieldList>
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
        </FieldList>
        {signupStep === 'nickname' && (<p>모아에서 사용할 닉네임을 2~12자로 입력해 주세요.</p>)}
      </>
    ),
    region: (
      <>
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
                {provinceOptions.map((province) => (
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
                {districtOptions.map((region) => (
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
                {townOptions.map((region) => (
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
      </>
    ),
    details: (
      <>
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
      </>
    ),
  };

  return (
    <Form onSubmit={submitSignup} $isDetailsStep={signupStep !== 'terms'}>
      <fieldset
        disabled={isSubmitting || isCheckingNickname}
        style={{ border: 0, padding: 0, margin: 0, minWidth: 0 }}
      >
        {signupStep === 'terms' && (
          <TermsStep>
            <AgreementCard title="회원가입을 위한 약관 동의가 필요해요">
              {isTermsLoading && (
                <p role="status">약관을 불러오고 있습니다.</p>
              )}
              {termsError && <p role="alert">{termsError}</p>}

              {memberTermOptions.length > 0 && (
                <>
                  <AgreementList>
                    <AllAgreement>
                      <Checkbox
                        id="agreement-all"
                        label="전체 동의하기"
                        checked={memberTermOptions.every(
                          (term) => Boolean(agreements[term.termId]),
                        )}
                        onChange={() => {
                          setAgreements((current) => {
                            const allChecked = memberTermOptions.every(
                              (term) => Boolean(current[term.termId]),
                            );
                            return Object.fromEntries(
                              memberTermOptions.map((term) => [term.termId, !allChecked]),
                            );
                          });
                        }}
                      />
                    </AllAgreement>

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
                          aria-label={`${term.title} 전문 보기`}
                          onClick={() => setSelectedTerm(term)}
                        >
                          보기
                        </DetailButton>
                      </AgreementRow>
                    ))}
                  </AgreementList>

                  <PrivacyNotice>
                    <summary>개인정보 수집·이용 안내</summary>
                    <p>
                      가입 시 입력한 정보는 회원 식별, 계정 관리, 활동 지역에
                      맞는 서비스 제공에 사용합니다. 수집 항목과 보유 기간은
                      위의 개인정보 수집·이용 동의 전문에서 확인할 수 있습니다.
                    </p>
                  </PrivacyNotice>
                </>
              )}
            </AgreementCard>

            <TermsHint aria-live="polite">
              {Object.values(agreements).some(Boolean) &&
                missingRequiredTerms.length > 0 &&
                (missingRequiredTerms.length === 1
                  ? `‘${missingRequiredTerms[0].title}’ 항목에 동의해 주세요.`
                  : '남은 필수 약관에 모두 동의해 주세요.')}
            </TermsHint>

            <SubmitButton
              type="button"
              disabled={
                isTermsLoading ||
                Boolean(termsError) ||
                memberTermOptions.length === 0 ||
                memberTermOptions.some(
                  (term) => term.required && !agreements[term.termId],
                )
              }
              onClick={() => {
                setSignupError('');
                setSignupStep('name');
              }}
            >
              다음
            </SubmitButton>
          </TermsStep>
        )}

        {stepIndex >= 0 && (
          <SignupInputCard
            ref={signupCardRef}
            aria-labelledby="signup-current-title"
          >
            <h2 id="signup-current-title">{titles[signupStep]}</h2>

            {/* AI작성: 마지막 단계에서만 이전 입력 정보를 접고, 기존 입력칸을 그대로 재사용합니다. */}
            {signupStep === 'details' ? (
              <>
                <SignupFieldGroup key="details">{fields.details}</SignupFieldGroup>
                <PreviousInformation
                  ref={previousInformationRef}
                  onInvalidCapture={revealPreviousInformation}
                >
                  <summary>
                    <span>
                      <strong>입력한 정보 확인</strong>
                      <small>이름·연락처·활동 지역</small>
                    </span>
                    <span className="review-action">
                      <span className="review-open">펼치기</span>
                      <span className="review-close">접기</span>
                    </span>
                  </summary>
                  <div>
                    {steps.slice(0, stepIndex).reverse().map((step) => (
                      <SignupFieldGroup key={step}>
                        {fields[step]}
                      </SignupFieldGroup>
                    ))}
                  </div>
                </PreviousInformation>
              </>
            ) : (
              steps.slice(0, stepIndex + 1).reverse().map((step) => (
                <SignupFieldGroup key={step}>
                  {fields[step]}
                </SignupFieldGroup>
              ))
            )}

            {signupError && <p role="alert">{signupError}</p>}

            {/* AI작성: 관심사를 선택하고 입력 정보를 확인한 뒤 가입하도록 버튼을 아래에 배치합니다. */}
            {signupStep === 'details' ? (
              <SubmitButton
                key="signup-submit"
                type="submit"
                style={{ marginTop: 24 }}
                disabled={
                  isSubmitting ||
                  isEmailBusy ||
                  isCheckingNickname ||
                  isTermsLoading ||
                  Boolean(termsError) ||
                  memberTermOptions.length === 0
                }
              >
                {isSubmitting ? '가입 처리 중…' : '가입 완료'}
              </SubmitButton>
            ) : (
              <SubmitButton
                key="signup-next"
                type="button"
                disabled={
                  isEmailBusy ||
                  (signupStep === 'email' && !emailVerificationToken)
                }
                onClick={advanceSignup}
              >
                다음
              </SubmitButton>
            )}
          </SignupInputCard>
        )}
      </fieldset>
      {/* AI작성: 약관 본문을 확인하고 닫아도 동의 여부는 직접 선택한다. */}
      {
        selectedTerm && (
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
        )
      }
    </Form >
  );
}

export default JoinForm;