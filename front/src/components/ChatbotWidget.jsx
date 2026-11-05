import { useEffect, useRef, useState } from 'react';
import styled from 'styled-components';
import { LuArrowUp, LuSprout } from 'react-icons/lu';
import { sendChatbotMessage } from '../api/chatbot';
import FloatingChatbotButton from './FloatingChatbotButton';

const Widget = styled.aside`
  position: fixed;
  right: 16px;
  bottom: 96px;
  z-index: 30;
  display: grid;
  justify-items: end;
  gap: 10px;
`;

const Panel = styled.section`
  box-sizing: border-box;
  width: min(420px, calc(100vw - 32px));
  height: min(600px, calc(100dvh - 112px));
  display: flex;
  flex-direction: column;
  padding: 16px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 24px;
  background: ${({ theme }) => theme.colors.white};
  box-shadow: 0 8px 28px rgb(0 0 0 / 12%);

  h2 {
display: flex;
align-items: center;
gap: 8px;

svg {
  color: ${({ theme }) => theme.colors.primary};
}
    margin: 0;
    font-size: 17px;
  }

  form {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  padding: 8px;
  border-radius: 18px;
  background: ${({ theme }) => theme.colors.primarySoft};
}

input {
  flex: 1;
  min-width: 0;
  padding: 10px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: inherit;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: -2px;
  }
}
`;

const Messages = styled.div`
    display: flex;
    flex-direction: column;
    gap: 14px;
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    margin: 12px 0;

    .message-row {
        display: flex;
        align-items: flex-start;
        align-self: flex-start;
        gap: 8px;
        max-width: 90%;
    }

    .message-avatar {
        display: grid;
        place-items: center;
        flex: 0 0 32px;
        height: 32px;
        border-radius: 10px;
        background: ${({ theme }) => theme.colors.primarySoft};
        color: ${({ theme }) => theme.colors.primary};
    }

    .message-body {
        min-width: 0;
    }

    .message-name {
        display: block;
        margin-bottom: 4px;
        color: ${({ theme }) => theme.colors.secondaryText};
        font-size: 12px;
        font-weight: 600;
    }

    p {
        margin: 0;
        padding: 9px 12px;
        border-radius: 4px 12px 12px 12px;
        background: ${({ theme }) => theme.colors.primarySoft};
        line-height: 1.5;
        white-space: pre-wrap;
        overflow-wrap: anywhere;
    }

    .message-row[data-speaker='나'] {
        align-self: flex-end;
        max-width: 85%;
    }

    .message-row[data-speaker='나'] p {
        border-radius: 12px 4px 12px 12px;
        background: ${({ theme }) => theme.colors.primary};
        color: ${({ theme }) => theme.colors.white};
    }
`;

const Action = styled.button`
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  padding: 0;
  border: 0;
  border-radius: 8px;
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.white};
  cursor: pointer;

  &:disabled {
    opacity: 0.5;
    cursor: default;
  }
`;

const SUGGESTED_QUESTIONS = [
    '모아는 어떤 웹인가요?',
    '계모임은 어떻게 참여하나요?',
    '활동 지역은 어떻게 인증하나요?',
];

const SuggestedQuestions = styled.div`
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    flex-shrink: 0;
    gap: 8px;
    margin-bottom: 12px;

    button {
        width: min(100%, 228px);
        box-sizing: border-box;
        min-height: 36px;
        padding: 7px 12px;
        text-align: center;
        border: 1px solid ${({ theme }) => theme.colors.primary};
        border-radius: 999px;
        background: ${({ theme }) => theme.colors.white};
        color: ${({ theme }) => theme.colors.primary};
        font: inherit;
        font-size: 13px;
        cursor: pointer;
    }

    button:disabled {
        opacity: 0.5;
        cursor: default;
    }
`;
const PrivacyHint = styled.p`
    flex-shrink: 0;
    margin: 8px 0 0;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    line-height: 1.5;
    text-align: center;
`;

export default function ChatbotWidget() {
    const [isOpen, setIsOpen] = useState(false);
    const [question, setQuestion] = useState('');
    const [messages, setMessages] = useState([
        { speaker: '모아', text: '안녕하세요! 모아입니다. 무엇을 도와드릴까요?' },
    ]);
    const [isSending, setIsSending] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const sendingRef = useRef(false);
    const messagesRef = useRef(null);

    // 질문, 대기 말풍선, 답변이 표시되면 대화창의 맨 아래를 보여줍니다.
    useEffect(() => {
        const messageList = messagesRef.current;
        if (!messageList) return;

        messageList.scrollTop = messageList.scrollHeight;
    }, [isOpen, messages, isSending]);

    function handleSubmit(event) {
        event.preventDefault();
        void sendQuestion(question);
    }

    async function sendQuestion(text) {
        const message = text.trim();
        // 답변을 기다리는 동안 클릭이나 Enter로 같은 요청을 다시 보내지 않습니다.
        if (!message || sendingRef.current) return;

        sendingRef.current = true;
        setIsSending(true);
        setErrorMessage('');

        // 전송한 질문을 먼저 표시하고 입력창을 비웁니다.
        setMessages((previous) => [
            ...previous,
            { speaker: '나', text: message },
        ]);
        setQuestion('');

        try {
            const reply = await sendChatbotMessage(message);

            // 응답이 도착하면 모아의 답변을 추가합니다.
            setMessages((previous) => [
                ...previous,
                { speaker: '모아', text: reply },
            ]);
            
        } catch (error) {
            if (error.response?.status === 429) {
                setErrorMessage('질문을 여러 번 보내셨어요. 잠시 후 다시 시도해 주세요.');
            } else {
                setErrorMessage('답변을 받지 못했어요. 잠시 후 다시 보내 주세요.');
            }
        } finally {
            sendingRef.current = false;
            setIsSending(false);
        }
    }

    return (
        <Widget aria-label="모아 챗봇">
            {isOpen && (
                <Panel id="moa-chatbot-panel" aria-labelledby="moa-chatbot-title">
                    <h2 id="moa-chatbot-title">
                        <LuSprout size={20} aria-hidden="true" />
                        모아 챗봇
                    </h2>
                    <Messages ref={messagesRef} role="log" aria-label="대화 내용">
                        {messages.map(({ speaker, text }, index) => (
                            <div className="message-row" data-speaker={speaker} key={index}>
                                {speaker === '모아' && (
                                    <span className="message-avatar" aria-hidden="true">
                                        <LuSprout size={17} />
                                    </span>
                                )}
                                <div className="message-body">
                                    {speaker === '모아' && (
                                        <span className="message-name">모아</span>
                                    )}
                                    <p>{text}</p>
                                </div>
                            </div>
                        ))}
                        {/* 응답을 기다리는 동안 모아의 말풍선으로 안내합니다. */}
                        {isSending && (
                            <div className="message-row" data-speaker="모아">
                                <span className="message-avatar" aria-hidden="true">
                                    <LuSprout size={17} />
                                </span>
                                <div className="message-body">
                                    <span className="message-name">모아</span>
                                    <p>잠시만요, 모아가 답변 중이에요…</p>
                                </div>
                            </div>
                        )}
                    </Messages>

                    {messages.length === 1 && (
                        <SuggestedQuestions role="group" aria-label="추천 질문">
                            {SUGGESTED_QUESTIONS.map((suggestion) => (
                                <button
                                    key={suggestion}
                                    type="button"
                                    disabled={isSending}
                                    onClick={() => sendQuestion(suggestion)}
                                >
                                    {suggestion}
                                </button>
                            ))}
                        </SuggestedQuestions>
                    )}
                    {errorMessage && <p role="alert">{errorMessage}</p>}
                    <form onSubmit={handleSubmit}>
                        <input
                            aria-label="챗봇에게 물어볼 내용"
                            placeholder="궁금한 내용을 입력해 주세요"
                            value={question}
                            onChange={(event) => setQuestion(event.target.value)}
                            maxLength={500}
                            readOnly={isSending}
                        />
                        <Action
                            type="submit"
                            aria-label="메시지 보내기"
                            disabled={isSending || !question.trim()}
                        >
                            <LuArrowUp size={18} aria-hidden="true" />
                        </Action>
                    </form>
                    <PrivacyHint>
                        전화번호나 계좌번호와 같은 민감한 개인정보는 빼고 질문해 주세요.
                    </PrivacyHint>
                </Panel>
            )}
            <FloatingChatbotButton
                isOpen={isOpen}
                onToggle={() => setIsOpen((open) => !open)}
            />
        </Widget>
    );
}