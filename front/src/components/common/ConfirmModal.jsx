import styled from 'styled-components';
import { FiX } from 'react-icons/fi';
import Button from './Button';

const Overlay = styled.div`
  position: fixed;
  z-index: 20;
  inset: 0;
  padding: 24px 16px;
  display: grid;
  place-items: center;
  background: rgba(26, 41, 36, 0.38);
`;

const Dialog = styled.section`
  position: relative;
  width: min(100%, ${({ $size }) => ($size === 'wide' ? '600px' : '390px')});
  padding: 28px;
  background: ${({ theme }) => theme.colors.white};
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: 0 18px 42px rgba(20, 37, 32, 0.2);
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 21px;
  font-weight: 800;
  letter-spacing: -1px;
`;

const CloseButton = styled.button`
  position: absolute;
  top: 16px;
  right: 16px;
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};

  &:hover {
    background: ${({ theme }) => theme.colors.background};
  }
`;

const Description = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.6;
`;

const Content = styled.div`
  margin-top: 22px;
`;

const Actions = styled.div`
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
`;

const ActionButton = styled(Button)`
  min-height: 40px;
  padding: 8px 17px;
  font-size: 12px;
`;

function ConfirmModal({
  title,
  description,
  children,
  onConfirm,
  onCancel,
  confirmLabel = '확인',
  cancelLabel = '취소',
  confirmVariant = 'primary',
  size = 'default',
  cancelFirst = false,
  showCancel = true,
  isBusy = false,
}) {
  const cancelButton = (
    <ActionButton type="button" $variant="secondary" onClick={onCancel} disabled={isBusy}>
      {cancelLabel}
    </ActionButton>
  );

  const confirmButton = (
    <ActionButton type="button" $variant={confirmVariant} onClick={onConfirm} disabled={isBusy}>
      {confirmLabel}
    </ActionButton>
  );

  return (
    <Overlay>
      <Dialog $size={size} role="dialog" aria-modal="true" aria-labelledby="confirm-modal-title">
        <Title id="confirm-modal-title">{title}</Title>
        <CloseButton type="button" onClick={onCancel} aria-label="모달 닫기" disabled={isBusy}>
          <FiX size={17} aria-hidden="true" />
        </CloseButton>
        {description && <Description>{description}</Description>}
        {children && <Content>{children}</Content>}
        <Actions>
          {showCancel ? (cancelFirst ? cancelButton : confirmButton) : confirmButton}
          {showCancel && (cancelFirst ? confirmButton : cancelButton)}
        </Actions>
      </Dialog>
    </Overlay>
  );
}

export default ConfirmModal;
