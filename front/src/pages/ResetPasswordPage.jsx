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
  // AI작성: 입력을 마친 뒤 오류를 표시하고 수정하면 즉시 갱신합니다.
  const [isPasswordTouched, setIsPasswordTouched] = useState(false);
  const [isConfirmTouched, setIsConfirmTouched] = useState(false);

  const passwordError =
    newPassword.length < 8 || newPassword.length > 20
      ? '비밀번호는 8~20자로 입력해 주세요.'
      : !/^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[!-/:-@\[-`{-~])[!-~]{8,20}$/.test(newPassword)
        ? '영문, 숫자, 특수문자를 각각 하나 이상 포함해 주세요.'
        : '';

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

    // AI작성: 변경 요청 시 입력칸 아래의 오류 안내를 활성화합니다.
    setIsPasswordTouched(true);
    setIsConfirmTouched(true);

    if (passwordError || newPassword !== confirmPassword) {
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
    setIsPasswordTouched(false);
    setIsConfirmTouched(false);
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

            <Form onSubmit={handleSubmit} noValidate>
              <PasswordInput
                id="reset-new-password"
                label="새 비밀번호"
                placeholder="8~20자, 영문·숫자·특수문자 포함"
                autoComplete="new-password"
                required
                onBlur={() => setIsPasswordTouched(true)}
                aria-invalid={isPasswordTouched && Boolean(passwordError)}
                aria-describedby="reset-password-feedback"
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

              {isPasswordTouched && passwordError && (
                <Message $error id="reset-password-feedback" role="status">
                  {passwordError}
                </Message>
              )}

              <PasswordInput
                id="reset-confirm-password"
                label="새 비밀번호 확인"
                placeholder="새 비밀번호를 다시 입력해주세요"
                autoComplete="new-password"
                required
                onBlur={() => setIsConfirmTouched(true)}
                aria-invalid={(confirmPassword !== '' || isConfirmTouched) && newPassword !== confirmPassword}
                aria-describedby="reset-confirm-feedback"
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

              {(confirmPassword !== '' || isConfirmTouched) &&
                newPassword !== confirmPassword && (
                  <Message $error id="reset-confirm-feedback" role="status">
                    새 비밀번호가 일치하지 않아요. 다시 확인해 주세요.
                  </Message>
                )}

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