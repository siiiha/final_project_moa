import { useRef, useState } from 'react';
import styled from 'styled-components';
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
  width: min(360px, calc(100vw - 32px));
  padding: 16px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 16px;
  background: ${({ theme }) => theme.colors.white};
  box-shadow: 0 8px 28px rgb(0 0 0 / 12%);

  h2 {
    margin: 0;
    font-size: 17px;
  }

  form {
    display: flex;
    gap: 8px;
  }

  input {
    flex: 1;
    min-width: 0;
    padding: 10px;
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: 8px;
  }
`;

const Messages = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: min(300px, 40vh);
  overflow-y: auto;
  margin: 12px 0;

  p {
    margin: 0;
    padding: 10px 12px;
    border-radius: 10px;
    background: ${({ theme }) => theme.colors.primarySoft};
    white-space: pre-wrap;
    overflow-wrap: anywhere;
    align-self: flex-start;
    max-width: 85%;
  }
  p[data-speaker='나'] {
  align-self: flex-end;
  background: ${({ theme }) => theme.colors.primary};
  color: ${({ theme }) => theme.colors.white};
}
`;

const Action = styled.button`
  padding: 10px 14px;
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

export default function ChatbotWidget() {
    const [isOpen, setIsOpen] = useState(false);
    const [question, setQuestion] = useState('');
    const [messages, setMessages] = useState([
        { speaker: '모아', text: '안녕하세요! 모아입니다. 무엇을 도와드릴까요?' },
    ]);
    const [isSending, setIsSending] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const sendingRef = useRef(false);

    async function handleSubmit(event) {
        event.preventDefault();
        const message = question.trim();

        // 답변을 기다리는 동안 클릭이나 Enter로 같은 요청을 다시 보내지 않습니다.
        if (!message || sendingRef.current) return;

        sendingRef.current = true;
        setIsSending(true);
        setErrorMessage('');

        try {
            const reply = await sendChatbotMessage(message);
            setMessages((previous) => [
                ...previous,
                { speaker: '나', text: message },
                { speaker: '모아', text: reply },
            ]);
            setQuestion('');
        } catch {
            // 전송에 실패하면 입력을 남겨 두어 다시 보낼 수 있게 합니다.
            setErrorMessage('답변을 받지 못했어요. 잠시 후 다시 보내 주세요.');
        } finally {
            sendingRef.current = false;
            setIsSending(false);
        }
    }

    return (
        <Widget aria-label="모아 챗봇">
            {isOpen && (
                <Panel id="moa-chatbot-panel" aria-labelledby="moa-chatbot-title">
                    <h2 id="moa-chatbot-title">모아 챗봇</h2>
                    <Messages role="log" aria-label="대화 내용">
                        {messages.map(({ speaker, text }, index) => (
                            <p key={index} data-speaker={speaker}>
                                <strong>{speaker}</strong> {text}
                            </p>
                        ))}
                    </Messages>
                    <p>전화번호나 계좌번호 등 개인정보는 입력하지 마세요.</p>
                    {isSending && <p role="status">답변을 준비하고 있어요.</p>}
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
                        <Action type="submit" disabled={isSending || !question.trim()}>
                            보내기
                        </Action>
                    </form>
                </Panel>
            )}
            <FloatingChatbotButton
                isOpen={isOpen}
                onToggle={() => setIsOpen((open) => !open)}
            />
        </Widget>
    );
}