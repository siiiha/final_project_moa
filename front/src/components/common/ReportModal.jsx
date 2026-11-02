import { useState } from 'react';
import styled from 'styled-components';
import ConfirmModal from './ConfirmModal';

const ReasonInput = styled.textarea`
  width: 100%;
  min-height: 130px;
  padding: 12px;
  resize: vertical;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font: inherit;
  line-height: 1.6;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Count = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  text-align: right;
`;

const ErrorMessage = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.danger};
  font-size: 12px;
`;

function ReportModal({ targetLabel, submitting, errorMessage, onSubmit, onCancel }) {
  const [reason, setReason] = useState('');
  const [validationMessage, setValidationMessage] = useState('');

  const handleConfirm = () => {
    const trimmedReason = reason.trim();
    if (!trimmedReason) {
      setValidationMessage('신고 사유를 입력해주세요.');
      return;
    }
    setValidationMessage('');
    onSubmit(trimmedReason);
  };

  return (
    <ConfirmModal
      title={`${targetLabel} 신고`}
      description="신고 내용을 확인한 후 관리자가 처리합니다. 구체적인 사유를 작성해주세요."
      confirmLabel={submitting ? '접수 중...' : '신고 접수'}
      confirmVariant="danger"
      size="wide"
      cancelFirst
      isBusy={submitting}
      onConfirm={handleConfirm}
      onCancel={onCancel}
    >
      {/* AI작성: 신고 사유는 백엔드 검증 기준과 동일하게 최대 500자로 제한한다. */}
      <ReasonInput
        value={reason}
        onChange={(event) => {
          setReason(event.target.value);
          if (validationMessage) setValidationMessage('');
        }}
        maxLength={500}
        disabled={submitting}
        placeholder="신고 사유를 입력해주세요."
        aria-label="신고 사유"
        autoFocus
      />
      <Count>{reason.length}/500자</Count>
      {(validationMessage || errorMessage) && (
        <ErrorMessage role="alert">{validationMessage || errorMessage}</ErrorMessage>
      )}
    </ConfirmModal>
  );
}

export default ReportModal;
