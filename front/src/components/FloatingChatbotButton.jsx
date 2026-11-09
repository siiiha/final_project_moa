import styled from 'styled-components';
import { LuX } from 'react-icons/lu';

const Launcher = styled.button`
  position: fixed;
  right: 16px;
  bottom: 16px;
  width: 70px;
  height: 70px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;
  background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.primary};
  box-shadow: 0 6px 20px rgb(0 0 0 / 15%);
  cursor: pointer;
  z-index: 31;

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
  }

    /* AI작성: 닫기 아이콘을 민트색 원으로 감싸 부드럽게 표시합니다. */
  span {
    display: grid;
    place-items: center;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: ${({ theme }) => theme.colors.primarySoft};
    color: ${({ theme }) => theme.colors.primary};
    font-size: 32px;
    pointer-events: none;
    transition: background 160ms ease;
  }

  &:hover {
    box-shadow: 0 8px 24px rgb(0 0 0 / 18%);
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
`;

export default function FloatingChatbotButton({ isOpen, onToggle, imageSrc }) {
  return (
    <Launcher
      type="button"
      aria-label={isOpen ? '모아 챗봇 닫기' : '모아 챗봇 열기'}
      aria-expanded={isOpen}
      aria-controls={isOpen ? 'moa-chatbot-panel' : undefined}
      onClick={onToggle}
    >
      {/* AI작성: 대화창이 열리면 닫기 아이콘을 표시합니다. */}
      <span aria-hidden="true">
        {isOpen ? (
          <LuX size={26} strokeWidth={1.8} />
        ) : imageSrc ? (
          <img src={imageSrc} alt="" draggable={false} />
        ) : (
          '🌱'
        )}
      </span>
    </Launcher>
  );
}