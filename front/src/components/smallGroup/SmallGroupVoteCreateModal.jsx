import { useEffect, useLayoutEffect, useRef, useState } from 'react';
import { FiMove, FiPlus, FiTrash2, FiX } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const VIEWPORT_MARGIN = 16;

const Overlay = styled.div`
  position: fixed;
  z-index: 30;
  inset: 0;
  background: rgba(26, 41, 36, 0.38);
`;

const Dialog = styled.section`
  width: min(560px, calc(100vw - 32px));
  max-height: calc(100vh - 32px);
  position: fixed;
  overflow-y: auto;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.lg};
  box-shadow: 0 18px 42px rgba(20, 37, 32, 0.22);
`;

const DragHandle = styled.div`
  min-height: 48px;
  padding: 0 15px 0 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: grab;
  touch-action: none;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  user-select: none;

  &:active {
    cursor: grabbing;
  }
`;

const DialogTitle = styled.h2`
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const CloseButton = styled.button`
  width: 30px;
  height: 30px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};

  &:hover:not(:disabled) {
    background: ${({ theme }) => theme.colors.background};
  }
`;

const Form = styled.form`
  padding: 18px;
`;

const Field = styled.label`
  display: block;
  font-size: 13px;
  font-weight: 700;

  & + & {
    margin-top: 18px;
  }
`;

const Required = styled.span`
  margin-left: 3px;
  color: ${({ theme }) => theme.colors.danger};
`;

const Input = styled.input`
  width: 100%;
  min-height: 42px;
  margin-top: 8px;
  padding: 9px 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 14px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 3px solid rgba(23, 107, 91, 0.16);
  }
`;

const Hint = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  font-weight: 400;
  line-height: 1.5;
`;

const OptionList = styled.div`
  margin-top: 8px;
  display: grid;
  gap: 8px;
`;

const OptionRow = styled.div`
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) 32px;
  align-items: center;
  gap: 7px;
`;

const OptionNumber = styled.span`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  font-weight: 700;
  text-align: center;
`;

const RemoveOptionButton = styled.button`
  width: 32px;
  height: 32px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};

  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
  }
`;

const AddOptionButton = styled.button`
  min-height: 34px;
  margin-top: 9px;
  padding: 6px 10px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

const ChoiceGroup = styled.div`
  margin-top: 8px;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
`;

const Choice = styled.label`
  min-height: 42px;
  padding: 9px 10px;
  display: flex;
  align-items: center;
  gap: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primarySoft : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: ${({ $selected }) => ($selected ? 800 : 600)};
`;

const ErrorMessage = styled.p`
  margin-top: 16px;
  padding: 10px 12px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.5;
`;

const Actions = styled.div`
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
`;

const ActionButton = styled(Button)`
  min-height: 42px;
  font-size: 13px;
`;

function clampPosition(position, dialog) {
  if (!dialog) return position;
  const maxX = Math.max(VIEWPORT_MARGIN, window.innerWidth - dialog.offsetWidth - VIEWPORT_MARGIN);
  const maxY = Math.max(VIEWPORT_MARGIN, window.innerHeight - dialog.offsetHeight - VIEWPORT_MARGIN);
  return {
    x: Math.min(Math.max(VIEWPORT_MARGIN, position.x), maxX),
    y: Math.min(Math.max(VIEWPORT_MARGIN, position.y), maxY),
  };
}

function SmallGroupVoteCreateModal({ isSubmitting, onClose, onSubmit }) {
  const dialogRef = useRef(null);
  const dragRef = useRef(null);
  const [position, setPosition] = useState({ x: VIEWPORT_MARGIN, y: VIEWPORT_MARGIN });
  const [title, setTitle] = useState('');
  const [options, setOptions] = useState(['', '']);
  const [multipleChoice, setMultipleChoice] = useState('');
  const [anonymous, setAnonymous] = useState('');
  const [endsAt, setEndsAt] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  // AI작성: 처음 열 때 모달을 화면 중앙에 배치하고, 이후 이동은 제목 바 드래그에서만 처리한다.
  useLayoutEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    setPosition(clampPosition({
      x: (window.innerWidth - dialog.offsetWidth) / 2,
      y: Math.max(VIEWPORT_MARGIN, (window.innerHeight - dialog.offsetHeight) / 5),
    }, dialog));
  }, []);

  // AI작성: 오류 메시지·보기 행 추가로 높이가 바뀌어도 모달 전체가 화면 밖으로 밀려나지 않게 한다.
  useLayoutEffect(() => {
    setPosition((current) => clampPosition(current, dialogRef.current));
  }, [errorMessage, options.length]);

  useEffect(() => {
    const handleKeyDown = (event) => {
      if (event.key === 'Escape' && !isSubmitting) onClose();
    };
    const handleResize = () => setPosition((current) => clampPosition(current, dialogRef.current));
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('resize', handleResize);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('resize', handleResize);
    };
  }, [isSubmitting, onClose]);

  const handlePointerDown = (event) => {
    if (event.button !== 0 || isSubmitting) return;
    const dialog = dialogRef.current;
    if (!dialog) return;
    const bounds = dialog.getBoundingClientRect();
    dragRef.current = {
      pointerId: event.pointerId,
      offsetX: event.clientX - bounds.left,
      offsetY: event.clientY - bounds.top,
    };
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event) => {
    const drag = dragRef.current;
    if (!drag || drag.pointerId !== event.pointerId) return;
    setPosition(clampPosition({
      x: event.clientX - drag.offsetX,
      y: event.clientY - drag.offsetY,
    }, dialogRef.current));
  };

  const finishDragging = (event) => {
    if (dragRef.current?.pointerId === event.pointerId) dragRef.current = null;
  };

  const updateOption = (index, value) => {
    setOptions((current) => current.map((option, optionIndex) => (optionIndex === index ? value : option)));
  };

  const removeOption = (index) => {
    setOptions((current) => current.length <= 2 ? current : current.filter((_, optionIndex) => optionIndex !== index));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;
    if (title.trim().length < 2 || title.trim().length > 12) {
      setErrorMessage('투표 제목은 공백을 제외하고 2자 이상 12자 이하여야 합니다.');
      return;
    }
    if (multipleChoice === '' || anonymous === '') {
      setErrorMessage('복수 선택 여부와 익명 여부를 모두 선택해 주세요.');
      return;
    }

    setErrorMessage('');
    try {
      await onSubmit({
        title,
        options,
        multipleChoice,
        anonymous,
        endsAt: endsAt || null,
      });
    } catch (error) {
      setErrorMessage(error.message || '투표를 생성하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  return (
    <Overlay
      role="presentation"
      onPointerDown={(event) => {
        if (event.target === event.currentTarget && !isSubmitting) onClose();
      }}
    >
      <Dialog
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="small-group-vote-create-title"
        style={{ left: `${position.x}px`, top: `${position.y}px` }}
      >
        <DragHandle
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={finishDragging}
          onPointerCancel={finishDragging}
        >
          <DialogTitle id="small-group-vote-create-title">
            <FiMove size={18} aria-hidden="true" /> 투표 생성
          </DialogTitle>
          <CloseButton
            type="button"
            onPointerDown={(event) => event.stopPropagation()}
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="투표 생성 창 닫기"
          >
            <FiX size={18} aria-hidden="true" />
          </CloseButton>
        </DragHandle>

        <Form onSubmit={handleSubmit}>
          <Field>
            투표 제목<Required aria-hidden="true">*</Required>
            <Input value={title} onChange={(event) => setTitle(event.target.value)} maxLength={12} disabled={isSubmitting} />
            <Hint>공백을 제외하고 2자 이상 12자 이하로 입력해 주세요.</Hint>
          </Field>

          <Field>
            투표 보기<Required aria-hidden="true">*</Required>
            <OptionList>
              {options.map((option, index) => (
                <OptionRow key={`vote-option-${index}`}>
                  <OptionNumber>{index + 1}</OptionNumber>
                  <Input
                    value={option}
                    onChange={(event) => updateOption(index, event.target.value)}
                    maxLength={12}
                    disabled={isSubmitting}
                    aria-label={`투표 보기 ${index + 1}`}
                  />
                  <RemoveOptionButton
                    type="button"
                    onClick={() => removeOption(index)}
                    disabled={isSubmitting || options.length <= 2}
                    aria-label={`투표 보기 ${index + 1} 삭제`}
                  >
                    <FiTrash2 size={15} aria-hidden="true" />
                  </RemoveOptionButton>
                </OptionRow>
              ))}
            </OptionList>
            {options.length < 8 && (
              <AddOptionButton type="button" onClick={() => setOptions((current) => [...current, ''])} disabled={isSubmitting}>
                <FiPlus size={15} aria-hidden="true" /> 보기 추가
              </AddOptionButton>
            )}
            <Hint>보기는 최대 8개입니다. 빈 보기와 같은 보기는 저장 시 자동으로 정리됩니다.</Hint>
          </Field>

          <Field>
            복수 선택<Required aria-hidden="true">*</Required>
            <ChoiceGroup>
              <Choice $selected={multipleChoice === false}>
                <input type="radio" name="multiple-choice" checked={multipleChoice === false} onChange={() => setMultipleChoice(false)} disabled={isSubmitting} />
                하나만 선택
              </Choice>
              <Choice $selected={multipleChoice === true}>
                <input type="radio" name="multiple-choice" checked={multipleChoice === true} onChange={() => setMultipleChoice(true)} disabled={isSubmitting} />
                여러 개 선택
              </Choice>
            </ChoiceGroup>
          </Field>

          <Field>
            익명 여부<Required aria-hidden="true">*</Required>
            <ChoiceGroup>
              <Choice $selected={anonymous === true}>
                <input type="radio" name="anonymous" checked={anonymous === true} onChange={() => setAnonymous(true)} disabled={isSubmitting} />
                익명 투표
              </Choice>
              <Choice $selected={anonymous === false}>
                <input type="radio" name="anonymous" checked={anonymous === false} onChange={() => setAnonymous(false)} disabled={isSubmitting} />
                공개 투표
              </Choice>
            </ChoiceGroup>
            <Hint>공개 투표는 실제 투표를 완료한 회원만 회원별 선택 내용을 확인할 수 있습니다.</Hint>
          </Field>

          <Field>
            마감 일시
            <Input type="datetime-local" value={endsAt} onChange={(event) => setEndsAt(event.target.value)} disabled={isSubmitting} />
            <Hint>선택 사항이며, 설정하면 현재 시각 기준 10분 이후여야 합니다.</Hint>
          </Field>

          {errorMessage && <ErrorMessage role="alert">{errorMessage}</ErrorMessage>}
          <Actions>
            <ActionButton type="button" $variant="secondary" onClick={onClose} disabled={isSubmitting}>취소</ActionButton>
            <ActionButton type="submit" disabled={isSubmitting}>{isSubmitting ? '생성 중...' : '투표 생성'}</ActionButton>
          </Actions>
        </Form>
      </Dialog>
    </Overlay>
  );
}

export default SmallGroupVoteCreateModal;
