import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import TextInput from '../components/common/TextInput';
import {
  sendEmailVerification,
  verifyEmailCode,
  findMemberAccount,
} from '../api/member';

// AI작성: 로그인 화면과 같은 카드 형태를 사용한다.
const Main = styled.div`
  min-height: calc(100vh - 56px);
  padding: 84px 16px 48px;
  display: flex;
  justify-content: center;
  align-items: flex-start;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding-top: 42px;
  }
`;

const Card = styled.section`
  width: min(100%, 512px);
  padding: 40px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: 0 6px 18px rgba(29, 55, 48, 0.08);

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 28px;
    text-align: center;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 28px 24px;
  }
`;

const Description = styled.p`
  margin-top: 12px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 14px;
  line-height: 1.7;
  text-align: center;
`;

const Form = styled.form`
  margin-top: 28px;
  display: grid;
  gap: 16px;
`;

const Action = styled(Button)`
  width: 100%;
  min-height: 48px;

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
`;

const Message = styled.p`
  color: ${({ theme, $error }) =>
    $error ? theme.colors.danger : theme.colors.text};
  font-size: 14px;
  line-height: 1.7;
`;

const Actions = styled.div`
  margin-top: 20px;
  display: grid;
  gap: 12px;
`;

function FindAccountPage({
    purpose = 'FIND_ACCOUNT',
    onVerified,
  }) {
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [verificationRequestId, setVerificationRequestId] = useState(null);
  const [found, setFound] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [notice, setNotice] = useState('');

  const busyRef = useRef(false);
  const tokenRef = useRef(null);

  // AI작성: 계정 찾기 목적으로 인증번호를 요청한다.
  const handleSendCode = async (event) => {
    event.preventDefault();
    if (busyRef.current) return;

    busyRef.current = true;
    setIsSubmitting(true);
    setErrorMessage('');
    setNotice('');

    try {
      const requestId = await sendEmailVerification(
        email.trim(),
        purpose,
      );

      if (!Number.isSafeInteger(requestId) || requestId <= 0) {
        throw new Error('인증 요청 응답을 확인할 수 없습니다.');
      }

      setVerificationRequestId(requestId);
      setCode('');
      tokenRef.current = null;
      setNotice(
        '인증번호를 보냈습니다. 3분 안에 입력해주세요. 재발송은 1분 후 가능합니다.',
      );
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message ??
          '인증번호를 보내지 못했습니다. 잠시 후 다시 시도해주세요.',
      );
    } finally {
      busyRef.current = false;
      setIsSubmitting(false);
    }
  };

  // AI작성: 인증을 완료한 뒤에만 가입 여부를 조회한다.
  const handleVerify = async (event) => {
    event.preventDefault();
    if (busyRef.current) return;

    busyRef.current = true;
    setIsSubmitting(true);
    setErrorMessage('');

    try {
      if (!tokenRef.current) {
        const token = await verifyEmailCode(
          verificationRequestId,
          code.trim(),
        );

        if (
          typeof token !== 'string' ||
          !/^[A-Za-z0-9_-]{43}$/.test(token)
        ) {
          throw new Error('인증 응답을 확인할 수 없습니다.');
        }

        tokenRef.current = token;
      }
        
      // AI작성: 비밀번호 재설정에서는 인증 결과를 다음 입력 화면으로 전달한다.
      if (purpose === 'RESET_PASSWORD') {
        if (typeof onVerified !== 'function') {
          throw new Error('다음 인증 단계를 확인할 수 없습니다.');
        }

        onVerified({
          email: email.trim(),
          emailVerificationToken: tokenRef.current,
        });
        return;
      }

      const result = await findMemberAccount(
        email.trim(),
        tokenRef.current,
      );

      if (typeof result !== 'boolean') {
        throw new Error('가입 여부 응답을 확인할 수 없습니다.');
      }

      setFound(result);
      setCode('');
      setNotice('');
      tokenRef.current = null;
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message ??
          '가입 여부를 확인하지 못했습니다. 잠시 후 다시 시도해주세요.',
      );
    } finally {
      busyRef.current = false;
      setIsSubmitting(false);
    }
  };

  // AI작성: 다른 이메일을 확인할 때 이전 인증 정보를 초기화한다.
  const handleReset = () => {
    if (busyRef.current) return;

    setEmail('');
    setCode('');
    setVerificationRequestId(null);
    setFound(null);
    setErrorMessage('');
    setNotice('');
    tokenRef.current = null;
  };

  return (
    <Main>
      <Card aria-labelledby="find-account-title">
        <h1 id="find-account-title">
            {purpose === 'RESET_PASSWORD'
              ? '비밀번호 재설정'
              : '아이디 찾기'}
        </h1>
        <Description>
        {purpose === 'RESET_PASSWORD'
            ? '가입한 이메일을 인증한 후 새 비밀번호를 설정해주세요.'
            : '가입했을 것으로 생각되는 이메일을 인증해 주세요. 모아의 아이디는 이메일입니다.'}
        </Description>

        {found === null ? (
          <Form
            onSubmit={
              verificationRequestId === null
                ? handleSendCode
                : handleVerify
            }
          >
            <TextInput
              id="find-account-email"
              label="이메일"
              type="email"
              autoComplete="email"
              maxLength={40}
              placeholder="이메일을 입력해주세요"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={
                isSubmitting || verificationRequestId !== null
              }
            />

            {verificationRequestId !== null && (
              <TextInput
                id="find-account-code"
                label="인증번호"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                pattern="[0-9]{6}"
                maxLength={6}
                placeholder="인증번호 6자리를 입력해주세요"
                required
                value={code}
                onChange={(event) =>
                  setCode(event.target.value.replace(/\D/g, ''))
                }
                disabled={isSubmitting}
              />
            )}

            {notice && <Message role="status">{notice}</Message>}
            {errorMessage && (
              <Message $error role="alert">
                {errorMessage}
              </Message>
            )}

            <Action type="submit" disabled={isSubmitting}>
              {isSubmitting
                ? '처리 중…'
                : verificationRequestId === null
                  ? '인증번호 받기'
                    : purpose === 'RESET_PASSWORD'
                    ? '인증하고 계속'
                    : '인증하고 가입 여부 확인'}
            </Action>

            {verificationRequestId !== null && (
              <Action
                type="button"
                $variant="secondary"
                onClick={handleSendCode}
                disabled={isSubmitting}
              >
                인증번호 다시 받기
              </Action>
            )}
          </Form>
        ) : (
          <Description role="status">
            {found
              ? '입력하신 이메일로 가입된 계정이 있습니다.'
              : '입력하신 이메일로 가입된 계정이 없습니다.'}
          </Description>
        )}

        <Actions>
          {verificationRequestId !== null && (
            <Action
              type="button"
              $variant="secondary"
              onClick={handleReset}
              disabled={isSubmitting}
            >
              다른 이메일로 확인
            </Action>
          )}

          <Action
            as={Link}
            to="/member/login"
            $variant="secondary"
          >
            로그인 화면으로
          </Action>
        </Actions>
      </Card>
    </Main>
  );
}

export default FindAccountPage;

// AI작성: 계정 복구 화면에서 같은 카드와 버튼 스타일을 재사용한다.
export { Main, Card, Description, Form, Action, Message, Actions };