import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import PasswordInput from '../components/common/PasswordInput';
import { resetMemberPassword } from '../api/member';
import useAuthStore from '../store/authStore';
import FindAccountPage, {
  Main,
  Card,
  Description,
  Form,
  Action,
  Message,
  Actions,
} from './FindAccountPage';

function ResetPasswordPage() {
  const [verification, setVerification] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [passwordVisible, setPasswordVisible] = useState(false);
  const [confirmVisible, setConfirmVisible] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const busyRef = useRef(false);
  const clearToken = useAuthStore((state) => state.clearToken);

  // AI작성: 기존 이메일 인증 화면을 재사용한다.
  if (!verification && !completed) {
    return (
      <FindAccountPage
        purpose="RESET_PASSWORD"
        onVerified={setVerification}
      />
    );
  }

  // AI작성: 인증 토큰과 새 비밀번호를 서버에 전달한다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (busyRef.current) return;

    setErrorMessage('');

    if (
      newPassword.trim().length === 0 ||
      newPassword.length < 8 ||
      newPassword.length > 20
    ) {
      setErrorMessage('새 비밀번호는 8~20자로 입력해주세요.');
      return;
    }

    if (newPassword !== confirmPassword) {
      setErrorMessage('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.');
      return;
    }

    busyRef.current = true;
    setIsSubmitting(true);

    try {
      const result = await resetMemberPassword(
        verification.email,
        verification.emailVerificationToken,
        newPassword,
        confirmPassword,
      );

      if (result?.success !== true) {
        throw new Error('비밀번호 변경 결과를 확인할 수 없습니다.');
      }

      // AI작성: 현재 탭의 로그인 정보를 지우고 다시 로그인하도록 안내한다.
      clearToken();
      setNewPassword('');
      setConfirmPassword('');
      setVerification(null);
      setCompleted(true);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message ??
          '비밀번호를 변경하지 못했습니다. 다시 시도해주세요.',
      );
    } finally {
      busyRef.current = false;
      setIsSubmitting(false);
    }
  };

  // AI작성: 인증이 만료됐거나 이메일을 바꿀 때 처음부터 진행한다.
  const handleRestart = () => {
    if (busyRef.current) return;

    setVerification(null);
    setNewPassword('');
    setConfirmPassword('');
    setPasswordVisible(false);
    setConfirmVisible(false);
    setErrorMessage('');
  };

  return (
    <Main>
      <Card aria-labelledby="reset-password-title">
        <h1 id="reset-password-title">비밀번호 재설정</h1>

        {completed ? (
          <Description role="status">
            비밀번호가 변경됐습니다.
            <br />
            새 비밀번호로 로그인해주세요.
          </Description>
        ) : (
          <>
            <Description>
              이메일 인증이 완료됐습니다.
              <br />
              5분 안에 새 비밀번호를 설정해주세요.
            </Description>

            <Form onSubmit={handleSubmit}>
              <PasswordInput
                id="reset-new-password"
                label="새 비밀번호"
                placeholder="8~20자로 입력해주세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                required
                value={newPassword}
                onChange={(event) =>
                  setNewPassword(event.target.value)
                }
                isVisible={passwordVisible}
                onToggleVisibility={() =>
                  setPasswordVisible((visible) => !visible)
                }
                disabled={isSubmitting}
              />

              <PasswordInput
                id="reset-confirm-password"
                label="새 비밀번호 확인"
                placeholder="새 비밀번호를 다시 입력해주세요"
                autoComplete="new-password"
                minLength={8}
                maxLength={20}
                required
                value={confirmPassword}
                onChange={(event) =>
                  setConfirmPassword(event.target.value)
                }
                isVisible={confirmVisible}
                onToggleVisibility={() =>
                  setConfirmVisible((visible) => !visible)
                }
                disabled={isSubmitting}
              />

              {errorMessage && (
                <Message $error role="alert">
                  {errorMessage}
                </Message>
              )}

              <Action type="submit" disabled={isSubmitting}>
                {isSubmitting ? '변경 중…' : '비밀번호 변경'}
              </Action>
            </Form>
          </>
        )}

        <Actions>
          {!completed && (
            <Action
              type="button"
              $variant="secondary"
              onClick={handleRestart}
              disabled={isSubmitting}
            >
              이메일 인증 다시 하기
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

export default ResetPasswordPage;