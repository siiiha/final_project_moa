import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import * as PortOne from '@portone/browser-sdk/v2';
import { startAccountRecovery, completeAccountRecovery } from '../api/member';
import { Main, Card, Description, Action, Message, Actions } from './FindAccountPage';

const providerLabels = { KAKAO: '카카오', NAVER: '네이버' };

export default function PortOneFindAccountPage() {
    const busyRef = useRef(false);
    const [isBusy, setIsBusy] = useState(false);
    const [accounts, setAccounts] = useState(null);
    const [message, setMessage] = useState('');

    const handleVerify = async () => {
        if (busyRef.current) return;

        busyRef.current = true;
        setIsBusy(true);
        setAccounts(null);
        setMessage('');

        try {
            // 서버에서 이번 본인인증에 사용할 일회용 요청을 발급받습니다.
            const start = await startAccountRecovery();

            const result = await PortOne.requestIdentityVerification({
                storeId: start.storeId,
                channelKey: start.channelKey,
                identityVerificationId: start.identityVerificationId,
                bypass: { inicisUnified: { flgFixedUser: 'N' } },
            });

            if (!result || result.code !== undefined) {
                setMessage('본인인증을 완료하지 못했어요. 다시 시도해 주세요.');
                return;
            }

            // 서버가 본인인증 결과와 계정 연결을 확인한 뒤 로그인 정보를 반환합니다.
            const foundAccounts = await completeAccountRecovery(
                start.identityVerificationId,
                start.requestToken,
            );
            setAccounts(foundAccounts);
        } catch (error) {
            setMessage(
                error.response?.data?.error?.message ??
                '아이디를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.',
            );
        } finally {
            busyRef.current = false;
            setIsBusy(false);
        }
    };

    return (
        <Main>
            <Card>
                <h1>아이디 찾기</h1>
                                <Description>모아에서 본인인증을 완료한 계정의 아이디만 찾을 수 있어요.</Description>

                <Actions>
                    {message && <Message $error role="alert">{message}</Message>}

                    {accounts !== null && (
                        <div role="status">
                            <Message>
                                {accounts.length > 0
                                    ? '본인인증 정보와 연결된 계정이에요.'
                                    : '본인인증 정보와 연결된 계정을 찾지 못했어요.'}
                            </Message>
                            {accounts.length > 0 && (
                                <ul>
                                    {accounts.map(({ email, provider }) => (
                                        <li key={`${email ?? ''}-${provider ?? ''}`}>
                                            {email}
                                            {email && provider && ' · '}
                                            {provider && `${providerLabels[provider] ?? provider} 로그인`}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    )}

                    <Action type="button" onClick={handleVerify} disabled={isBusy}>
                        {isBusy ? '본인인증 확인 중…' : '본인인증 시작'}
                    </Action>
                    <Action as={Link} to="/member/login" $variant="secondary">
                        로그인 화면으로
                    </Action>
                </Actions>
            </Card>
        </Main>
    );
}