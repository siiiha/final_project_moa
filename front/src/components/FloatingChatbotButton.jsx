import styled from 'styled-components';

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
  box-shadow: 0 6px 20px rgb(0 0 0 / 15%);
  cursor: pointer;
  z-index: 31;

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
      <span aria-hidden="true">
        {imageSrc ? <img src={imageSrc} alt="" draggable={false} /> : '🌱'}
      </span>
    </Launcher>
  );
}