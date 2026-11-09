import { useState } from 'react';
import { FiCheck, FiFileText, FiUser } from 'react-icons/fi';
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

const TargetOptions = styled.fieldset`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin: 0 0 20px;
  padding: 0;
  border: 0;

  legend {
    margin-bottom: 10px;
    color: ${({ theme }) => theme.colors.text};
    font-size: 14px;
    font-weight: 700;
  }

  @media (max-width: 520px) {
    grid-template-columns: 1fr;
  }
`;

const TargetOption = styled.label`
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 84px;
  padding: 16px;
  background: ${({ $selected }) => ($selected ? 'rgba(211, 65, 57, 0.06)' : '#fff')};
  border: 2px solid ${({ $selected, theme }) => ($selected ? theme.colors.danger : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.md};
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;

  &:hover {
    border-color: ${({ theme }) => theme.colors.danger};
    transform: translateY(-1px);
  }

  &:focus-within {
    box-shadow: 0 0 0 3px rgba(211, 65, 57, 0.14);
  }

  input {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
  }
`;

const TargetIcon = styled.span`
  display: grid;
  flex: 0 0 42px;
  width: 42px;
  height: 42px;
  place-items: center;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.white : theme.colors.danger)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.danger : 'rgba(211, 65, 57, 0.1)')};
  border-radius: 50%;
  font-size: 20px;
`;

const TargetCopy = styled.span`
  display: grid;
  gap: 3px;
`;

const TargetTitle = styled.strong`
  color: ${({ theme }) => theme.colors.text};
  font-size: 14px;
`;

const TargetDescription = styled.span`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  line-height: 1.4;
`;

const SelectedMark = styled.span`
  position: absolute;
  top: 10px;
  right: 10px;
  display: grid;
  width: 20px;
  height: 20px;
  place-items: center;
  color: ${({ theme }) => theme.colors.white};
  background: ${({ theme }) => theme.colors.danger};
  border-radius: 50%;
  font-size: 13px;
`;

function ReportModal({ targets, submitting, errorMessage, onSubmit, onCancel }) {
  const [reason, setReason] = useState('');
  const [validationMessage, setValidationMessage] = useState('');
  const [selectedTargetKey, setSelectedTargetKey] = useState(
    targets?.[0] ? `${targets[0].reportType}:${targets[0].targetId}` : ''
  );

  const handleConfirm = () => {
    const trimmedReason = reason.trim();
    const selectedTarget = targets?.find(
      (target) => `${target.reportType}:${target.targetId}` === selectedTargetKey
    );
    if (!selectedTarget) {
      setValidationMessage('신고 대상을 선택해주세요.');
      return;
    }
    if (!trimmedReason) {
      setValidationMessage('신고 사유를 입력해주세요.');
      return;
    }
    setValidationMessage('');
    onSubmit(trimmedReason, selectedTarget);
  };

  return (
    <ConfirmModal
      title="신고하기"
      description="신고 내용을 확인한 후 관리자가 처리합니다. 구체적인 사유를 작성해주세요."
      confirmLabel={submitting ? '접수 중...' : '신고 접수'}
      confirmVariant="danger"
      size="wide"
      cancelFirst
      isBusy={submitting}
      onConfirm={handleConfirm}
      onCancel={onCancel}
    >
      {/* AI작성: 하나의 신고 버튼에서도 콘텐츠와 작성자를 구분해 한 대상만 접수하도록 선택 항목을 제공합니다. */}
      <TargetOptions>
        <legend>신고 대상</legend>
        {targets?.map((target) => {
          const targetKey = `${target.reportType}:${target.targetId}`;
          const isSelected = selectedTargetKey === targetKey;
          const isMemberTarget = target.reportType === 'MEMBER';
          return (
            <TargetOption key={targetKey} $selected={isSelected}>
              <input
                type="radio"
                name="reportTarget"
                value={targetKey}
                checked={isSelected}
                onChange={(event) => setSelectedTargetKey(event.target.value)}
                disabled={submitting}
              />
              <TargetIcon $selected={isSelected}>
                {isMemberTarget ? <FiUser /> : <FiFileText />}
              </TargetIcon>
              <TargetCopy>
                <TargetTitle>{target.label}</TargetTitle>
                <TargetDescription>
                  {target.description
                    || (isMemberTarget ? '작성자의 활동을 신고합니다.' : '작성된 내용을 신고합니다.')}
                </TargetDescription>
              </TargetCopy>
              {isSelected && <SelectedMark><FiCheck /></SelectedMark>}
            </TargetOption>
          );
        })}
      </TargetOptions>
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
