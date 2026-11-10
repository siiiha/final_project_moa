import { Fragment, useEffect, useRef, useState } from 'react';
import { FiSend } from 'react-icons/fi';
import styled from 'styled-components';
import { connectSavingGroupChat, fetchChatHistory, findChatRoom } from '../../api/savingGroupChat.js';
import useAuthStore from '../../store/authStore.js';

const Panel = styled.section`
  margin-top: 20px;
  background: white;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 14px;
  overflow: hidden;
`;

const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 18px 24px;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  h2 { font-size: 24px; }
  p { color: ${({ theme }) => theme.colors.secondaryText}; }
  strong { color: ${({ theme }) => theme.colors.primary}; }
`;

const History = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 280px;
  max-height: 460px;
  overflow-y: auto;
  padding: 20px;
`;

const Day = styled.div`
  align-self: center;
  padding: 4px 14px;
  border-radius: 20px;
  background: #eef1f0;
`;

const Message = styled.div`
  align-self: ${({ $mine }) => ($mine ? 'flex-end' : 'flex-start')};
  max-width: 86%;
  text-align: ${({ $mine }) => ($mine ? 'right' : 'left')};
  strong, time { display: block; }
  time { color: ${({ theme }) => theme.colors.secondaryText}; font-size: 13px; }
  p {
    text-align: left;
    padding: 10px 14px;
    border-radius: 13px;
    background: ${({ $mine, theme }) => ($mine ? theme.colors.primarySoft : '#eef1f0')};
    font-size: 17px;
    white-space: pre-wrap;
    overflow-wrap: anywhere;
  }
`;

const Composer = styled.form`
  display: flex;
  gap: 10px;
  padding: 16px;
  textarea {
    flex: 1;
    min-width: 0;
    min-height: 50px;
    resize: vertical;
    padding: 12px;
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: 8px;
    font: inherit;
  }
  textarea:disabled {
    background: #eef1f0;
    color: ${({ theme }) => theme.colors.secondaryText};
    cursor: not-allowed;
  }
  button {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 0 16px;
    background: ${({ theme }) => theme.colors.primary};
    color: white;
    border: 0;
    border-radius: 8px;
    font: inherit;
    font-weight: 700;
  }
  button:disabled {
    opacity: 0.55;
    cursor: not-allowed;
  }
`;

const dayFormat = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' });
const timeFormat = new Intl.DateTimeFormat('ko-KR', { hour: 'numeric', minute: '2-digit' });

// AI작성: 큰 입력창과 좌우 말풍선으로 계모임 채팅 시안을 구현한다.
export default function SavingGroupChatPanel({ group }) {
  const [draft, setDraft] = useState('');
  const [messages, setMessages] = useState([]);
  const [connected, setConnected] = useState(false);
  const [status, setStatus] = useState('채팅방에 연결하고 있습니다.');
  const [error, setError] = useState('');
  const [memberId, setMemberId] = useState(null);
  const [chatRestricted, setChatRestricted] = useState(false);
  const authVersion = useAuthStore((state) => state.authVersion);
  const connection = useRef(null);
  const history = useRef(null);
  const { savingGroupId } = group;

  useEffect(() => {
    const abort = new AbortController();
    const { signal } = abort;
    let chat;
    setConnected(false);
    setMessages([]);
    setError('');
    setChatRestricted(false);
    setStatus('채팅방에 연결하고 있습니다.');

    // AI작성: REST와 STOMP 응답 순서에 관계없이 메시지 번호로 합치고 중복을 제거한다.
    function merge(incoming) {
      if (signal.aborted) return;
      setMessages((current) => {
        const byId = new Map([...current, ...incoming].map((message) => [message.messageId, message]));
        return [...byId.values()].sort((left, right) => left.messageId - right.messageId);
      });
    }

    function showError(message) {
      if (!signal.aborted) setError(message);
    }

    async function start() {
      try {
        const room = await findChatRoom(savingGroupId, signal);
        const { roomId, chatRestricted: restricted } = room;
        if (signal.aborted) return;
        setChatRestricted(restricted);
        const payload = useAuthStore.getState().token?.split('.')[1];
        const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
        setMemberId(JSON.parse(window.atob(normalized)).sub);
        chat = connectSavingGroupChat(roomId, authVersion, {
          onConnect() {
            if (signal.aborted) return;
            setConnected(true);
            setStatus('');
            setError('');
            fetchChatHistory(roomId, signal).then(merge).catch(() =>
              showError('이전 대화를 불러오지 못했습니다.'));
          },
          onMessage: (message) => merge([message]),
          onError: showError,
          onDisconnect(retrying) {
            if (signal.aborted) return;
            setConnected(false);
            setStatus(retrying ? '채팅방에 다시 연결하고 있습니다.' : '');
          },
        });
        connection.current = chat;
      } catch {
        if (signal.aborted) return;
        setStatus('');
        showError('채팅방을 열지 못했습니다. 가입 상태를 확인해 주세요.');
      }
    }

    void start();
    return () => {
      abort.abort();
      connection.current = null;
      void chat?.close();
    };
  }, [savingGroupId, authVersion]);

  useEffect(() => {
    if (history.current) history.current.scrollTop = history.current.scrollHeight;
  }, [messages]);

  function submit(event) {
    event.preventDefault();
    if (chatRestricted || !draft.trim()) return;
    try {
      if (connection.current?.send(draft)) {
        setDraft('');
        setError('');
      }
    } catch {
      setError('메시지를 보내지 못했습니다.');
    }
  }

  // AI작성: 한글 조합과 Shift+Enter를 유지하고 Enter는 보내기 버튼과 같은 경로로 처리한다.
  function handleMessageKeyDown(event) {
    if (event.key !== 'Enter' || event.shiftKey
        || event.nativeEvent.isComposing || event.keyCode === 229) return;

    event.preventDefault();
    if (connected && !chatRestricted && draft.trim()) {
      event.currentTarget.form?.requestSubmit();
    }
  }

  return (
    <Panel aria-label="계모임 채팅방">
      <Header>
        <div><h2>채팅방</h2><p>계모임 회원 전용</p></div>
        <strong>참여 회원 {group.memberCount}명</strong>
      </Header>
      <History ref={history} role="log" aria-label="채팅 메시지">
        {status && <p role="status">{status}</p>}
        {error && <p role="alert">{error}</p>}
        {!messages.length && !status && !error && <p>첫 메시지를 보내 대화를 시작해 보세요.</p>}
        {messages.map((message, index) => {
          const mine = message.senderId != null && String(message.senderId) === String(memberId);
          const date = new Date(message.createdAt);
          const previousDay = messages[index - 1]?.createdAt.slice(0, 10);
          return (
            <Fragment key={message.messageId}>
              {message.createdAt.slice(0, 10) !== previousDay && <Day>{dayFormat.format(date)}</Day>}
              <Message $mine={mine}>
                {!mine && <strong>{message.senderName || '알 수 없는 회원'}</strong>}
                <p>{message.content}</p>
                <time dateTime={message.createdAt}>{timeFormat.format(date)}</time>
              </Message>
            </Fragment>
          );
        })}
      </History>
      <Composer onSubmit={submit}>
        <textarea aria-label="메시지"
          placeholder={chatRestricted ? '채팅 이용이 제한되었습니다.' : '메시지를 입력하세요'}
          maxLength={1000} disabled={chatRestricted}
          rows={1} value={draft} onChange={(event) => setDraft(event.target.value)}
          onKeyDown={handleMessageKeyDown} />
        <button type="submit" disabled={chatRestricted || !connected || !draft.trim()}>
          <FiSend aria-hidden="true" /> 보내기
        </button>
      </Composer>
    </Panel>
  );
}
