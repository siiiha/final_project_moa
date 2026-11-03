import { useEffect, useRef, useState } from 'react';
import { FaStar, FaRegStar, FaUndo } from 'react-icons/fa';
import styled from 'styled-components';
import ConfirmModal from '../common/ConfirmModal';
import { createSmallGroupReview } from '../../api/smallGroups';

// AI작성: 기존 공통 모달을 재사용하고 입력 영역은 큰 글씨와 충분한 간격으로 구성한다.
const Form = styled.form`
  display: grid;
  gap: 20px;
  font-size: 16px;
  line-height: 1.6;

  label { display: grid; gap: 8px; font-weight: 700; }
  textarea {
    width: 100%;
    min-height: 48px;
    padding: 12px;
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border};
    border-radius: ${({ theme }) => theme.radius.sm};
    font: inherit;
  }
  textarea { min-height: 110px; resize: vertical; }
  textarea:focus-visible {
    outline: 3px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

// AI작성: 각 별의 좌우를 별도 버튼으로 나눠 마우스·터치·키보드로 0.5점 단위를 선택한다.
const RatingField = styled.fieldset`
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
  legend { margin-bottom: 8px; font-weight: 700; }
`;

const Stars = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
`;

const Star = styled.span`
  display: inline-block;
  position: relative;
  width: 48px;
  height: 48px;
  flex: 0 0 48px;
  color: ${({ theme }) => theme.colors.secondaryText};
  svg { position: absolute; top: 3px; left: 3px; width: 42px; height: 42px; }
`;

const StarFill = styled.span`
  position: absolute;
  inset: 0 auto 0 0;
  width: ${({ $fill }) => `${$fill}%`};
  overflow: hidden;
  color: #c88400;
  pointer-events: none;
`;

const HalfStarButton = styled.button`
  position: absolute;
  top: 0;
  bottom: 0;
  left: ${({ $right }) => ($right ? '50%' : '0')};
  width: 50%;
  padding: 0;
  background: transparent;
  border: 0;
  border-radius: 4px;
  cursor: pointer;
  &:focus-visible { outline: 3px solid ${({ theme }) => theme.colors.primary}; outline-offset: -2px; }
  &:disabled { cursor: wait; }
`;

// AI작성: 작은 초기화 아이콘에 테마 초록색을 적용하고 누르기 쉬운 44px 영역은 유지한다.
const ResetRatingButton = styled.button`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  min-height: 44px;
  margin-left: 4px;
  padding: 8px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.primary};
  border-radius: ${({ theme }) => theme.radius.sm};
  font: inherit;
  font-size: 16px;
  cursor: pointer;
  &:focus-visible { outline: 3px solid ${({ theme }) => theme.colors.primary}; outline-offset: 2px; }
  &:disabled { cursor: wait; }
`;

const Hint = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 15px;
`;

const ErrorMessage = styled.p`
  padding: 12px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const ModalContainer = styled.div`
  [role='dialog'] { max-height: calc(100dvh - 48px); overflow-y: auto; }
  [role='dialog'] > div:last-child > button { min-height: 48px; font-size: 16px; }
`;

// AI작성: 등록 중 연속 요청·닫기를 막고, 실패 시 입력을 유지하며 서버 중복 응답은 완료 안내로 전환한다.
function SmallGroupReviewCreateModal({ smallGroupId, onClose, onRegistered }) {
  const containerRef = useRef(null);
  const submitLockRef = useRef(false);
  const [rating, setRating] = useState('');
  const [content, setContent] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  // AI작성: 별점 오류를 분리해 별점을 선택할 때 다른 입력·서버 오류는 유지한다.
  const [ratingError, setRatingError] = useState('');

  const handleRatingChange = (value) => {
    setRating(value);
    setRatingError('');
  };

  // AI작성: 모달 안으로 초점을 이동·제한하고 닫힌 뒤 작성 버튼으로 돌려준다.
  useEffect(() => {
    const previousFocus = document.activeElement;
    const container = containerRef.current;
    container.querySelector('[data-rating-choice]')?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    const handleKeyDown = (event) => {
      if (event.key === 'Escape') {
        event.preventDefault();
        if (!submitLockRef.current) onClose();
      }
      if (event.key !== 'Tab') return;
      const elements = [...container.querySelectorAll('button:not(:disabled), textarea:not(:disabled)')];
      if (!elements.length) {
        event.preventDefault();
        return;
      }
      const first = elements[0];
      const last = elements[elements.length - 1];
      if (event.shiftKey && (document.activeElement === first || !elements.includes(document.activeElement))) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && (document.activeElement === last || !elements.includes(document.activeElement))) {
        event.preventDefault();
        first.focus();
      }
    };
    container.addEventListener('keydown', handleKeyDown);
    return () => {
      container.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = previousOverflow;
      if (previousFocus?.isConnected) previousFocus.focus();
    };
  }, [onClose]);

  const handleSubmit = async (event) => {
    event?.preventDefault();
    if (submitLockRef.current) return;
    // AI작성: 별점을 선택하지 않으면 0점으로 전송하고 선택한 점수는 기존 범위로 검증한다.
    const score = rating === '' ? 0 : Number(rating);
    if (!Number.isFinite(score) || score < 0 || score > 5 || !Number.isInteger(score * 2)) {
      setErrorMessage('');
      setRatingError('별점을 0~5점, 0.5점 단위로 선택해 주세요.');
      containerRef.current?.querySelector('[data-rating-choice]')?.focus();
      return;
    }
    setRatingError('');
    if (!content.trim() || content.length > 30) {
      setErrorMessage('코멘트를 1자 이상 30자 이하로 입력해 주세요.');
      return;
    }
    submitLockRef.current = true;
    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await createSmallGroupReview(smallGroupId, { rating: score, content });
      // AI작성: 실제 저장 성공 여부를 전달해 중복 등록 안내와 목록 갱신을 구분한다.
      onRegistered('후기가 등록되었습니다. 소모임 후기는 한 번만 작성할 수 있습니다.', { created: true });
    } catch (error) {
      if (error.code === 'SMALL_GROUP_REVIEW_ALREADY_EXISTS') {
        onRegistered('이미 후기를 등록한 소모임입니다. 후기는 한 번만 작성할 수 있습니다.', { created: false });
      } else {
        setErrorMessage(error.message || '후기를 등록하지 못했습니다. 다시 시도해 주세요.');
      }
    } finally {
      submitLockRef.current = false;
      setIsSubmitting(false);
    }
  };

  return (
    <ModalContainer ref={containerRef}>
      <ConfirmModal
        title="후기 작성"
        description="모임은 어떠셨나요? 후기는 한 번만 작성할 수 있습니다."
        onCancel={onClose}
        onConfirm={handleSubmit}
        confirmLabel={isSubmitting ? '등록 중...' : '후기 등록'}
        cancelLabel="취소"
        cancelFirst
        isBusy={isSubmitting}
      >
        <Form onSubmit={handleSubmit} aria-busy={isSubmitting}>
          {/* AI작성: 미선택 시 0점 처리 안내와 점수 오류를 별점 영역에 연결한다. */}
          <RatingField
            disabled={isSubmitting}
            aria-invalid={Boolean(ratingError)}
            aria-describedby={`small-group-review-rating-help${ratingError ? ' small-group-review-rating-error' : ''}`}
          >
            <legend>별점</legend>
            <Stars>
              {Array.from({ length: 5 }, (_, index) => (
                <Star key={index}>
                  <FaRegStar aria-hidden="true" />
                  <StarFill $fill={Math.min(1, Math.max(0, Number(rating) - index)) * 100}>
                    <FaStar aria-hidden="true" />
                  </StarFill>
                  {[0.5, 1].map((step) => {
                    const score = index + step;
                    return (
                      <HalfStarButton
                        key={step}
                        type="button"
                        data-rating-choice
                        $right={step === 1}
                        aria-label={`${score}점 선택`}
                        aria-pressed={rating !== '' && Number(rating) === score}
                        disabled={isSubmitting}
                        title={`${score}점`}
                        onClick={() => handleRatingChange(String(score))}
                      />
                    );
                  })}
                </Star>
              ))}
              {/* AI작성: 초기화하면 미선택 상태로 돌아가며 등록 시 기존 정책에 따라 0점으로 전송한다. */}
              <ResetRatingButton
                type="button"
                aria-label="별점 초기화"
                title="별점 초기화"
                onClick={() => handleRatingChange('')}
                disabled={isSubmitting}
              >
                <FaUndo aria-hidden="true" />
              </ResetRatingButton>
            </Stars>
            <Hint id="small-group-review-rating-help" role="status">{rating === '' ? '별점을 선택하지 않으면 0점으로 등록됩니다.' : `${rating}점 / 5점`}</Hint>
            {ratingError && <ErrorMessage id="small-group-review-rating-error" role="alert">{ratingError}</ErrorMessage>}
          </RatingField>
          <label>
            코멘트 (필수)
            <textarea
              value={content}
              onChange={(event) => setContent(event.target.value)}
              minLength={1}
              maxLength={30}
              required
              disabled={isSubmitting}
              aria-describedby="small-group-review-length"
              placeholder="모임에 대한 소감을 남겨 주세요"
            />
          </label>
          <Hint id="small-group-review-length">{content.length} / 30자 · 공백만 입력할 수 없습니다.</Hint>
          {errorMessage && <ErrorMessage role="alert">{errorMessage}</ErrorMessage>}
        </Form>
      </ConfirmModal>
    </ModalContainer>
  );
}

export default SmallGroupReviewCreateModal;
