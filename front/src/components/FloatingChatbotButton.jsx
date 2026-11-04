import { useRef, useState } from 'react';
import styled, { keyframes } from 'styled-components';

const BUTTON_SIZE = 70;

const float = keyframes`
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
`;

const Launcher = styled.button`
  position: fixed;
  right: 16px;
  bottom: 16px;
  width: ${BUTTON_SIZE}px;
  height: ${BUTTON_SIZE}px;
  display: grid;
  place-items: center;
  padding: 0;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;
  background: ${({ theme }) => theme.colors.white};
  box-shadow: 0 6px 20px rgb(0 0 0 / 15%);
  cursor: grab;
  touch-action: none;
  user-select: none;
  z-index: 31;

  &:active {
    cursor: grabbing;
  }

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 3px;
  }

  span {
    display: grid;
    place-items: center;
    width: 52px;
    height: 52px;
    font-size: 36px;
    pointer-events: none;
    animation: ${float} 3s ease-in-out infinite;
  }

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }

  &:active span,
  &[aria-expanded='true'] span {
    animation: none;
  }

  @media (prefers-reduced-motion: reduce) {
    span {
      animation: none;
    }
  }
`;

export default function FloatingChatbotButton({
  isOpen,
  onToggle,
  imageSrc,
}) {
  const [position, setPosition] = useState(null);
  const gesture = useRef(null);
  const didDrag = useRef(false);

  function handlePointerDown(event) {
    if (!event.isPrimary || event.button !== 0) return;

    const bounds = event.currentTarget.getBoundingClientRect();
    didDrag.current = false;
    gesture.current = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      left: bounds.left,
      top: bounds.top,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  }

  function handlePointerMove(event) {
    const start = gesture.current;
    if (!start || start.pointerId !== event.pointerId) return;

    const dx = event.clientX - start.startX;
    const dy = event.clientY - start.startY;

    // 손가락이나 마우스가 조금 흔들려도 일반 클릭으로 처리합니다.
    if (!didDrag.current && Math.hypot(dx, dy) < 6) return;

    didDrag.current = true;
    setPosition({ left: start.left + dx, top: start.top + dy });
  }

  function handlePointerEnd(event) {
    if (gesture.current?.pointerId === event.pointerId) {
      gesture.current = null;
    }
  }

  return (
    <Launcher
      type="button"
      aria-label={isOpen ? '모아 챗봇 닫기' : '모아 챗봇 열기'}
      aria-expanded={isOpen}
      aria-controls={isOpen ? 'moa-chatbot-panel' : undefined}
      style={position ? {
        left: `clamp(0px, ${position.left}px, calc(100vw - ${BUTTON_SIZE}px))`,
        top: `clamp(0px, ${position.top}px, calc(100dvh - ${BUTTON_SIZE}px))`,
        right: 'auto',
        bottom: 'auto',
      } : undefined}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerEnd}
      onPointerCancel={handlePointerEnd}
      onLostPointerCapture={handlePointerEnd}
      onClick={(event) => {
        // 드래그를 끝낼 때 발생하는 클릭은 무시하고, 키보드 클릭은 허용합니다.
        if (event.detail === 0 || !didDrag.current) onToggle();
      }}
    >
      <span aria-hidden="true">
        {imageSrc ? <img src={imageSrc} alt="" draggable={false} /> : '🌱'}
      </span>
    </Launcher>
  );
}