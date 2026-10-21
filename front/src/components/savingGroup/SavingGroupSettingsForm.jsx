import { useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import Textarea from '../common/Textarea';

const Card = styled.section`
  padding: 22px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 18px;
  }
`;

const CardTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.8px;
`;

const Description = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
`;

const Form = styled.form`
  margin-top: 22px;
`;

const FieldGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const FullField = styled(Field)`
  margin-top: 16px;
`;

const CurrentValue = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  font-weight: 500;
`;

const FieldError = styled.p`
  color: ${({ theme }) => theme.colors.danger};
  font-size: 11px;
  font-weight: 600;
`;

const FormError = styled.p`
  margin-top: 16px;
  padding: 12px 14px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.danger};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
`;

const PendingNotice = styled.section`
  margin-top: 16px;
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const PendingTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 13px;
  font-weight: 800;
`;

const PendingText = styled.p`
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const ReadOnlyNotice = styled.p`
  margin-top: 16px;
  padding: 12px 14px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.6;
`;

const Input = styled.input`
  width: 100%;
  height: 42px;
  padding: 0 12px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: ${({ theme }) => theme.colors.background};
    cursor: not-allowed;
  }
`;

const Select = styled.select`
  width: 100%;
  height: 42px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }

  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: ${({ theme }) => theme.colors.background};
    cursor: not-allowed;
  }
`;

const Actions = styled.div`
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const PrimaryActions = styled.div`
  display: flex;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
`;

const ActionButton = styled(Button)`
  min-height: 40px;
  padding: 9px 16px;
  font-size: 13px;

  &:disabled {
    cursor: not-allowed;
    opacity: 0.55;
  }
`;

const DeleteRequestButton = styled(ActionButton)`
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const toInputValue = (value) => (value == null ? '' : String(value));
const toNumber = (value) => Number(value);
const formatDuesAmount = (amount) => (amount == null ? '회비 미정' : `월 ${amount.toLocaleString('ko-KR')}원`);
const formatDuesDay = (day) => (day == null ? '납부일 미정' : `매월 ${day}일`);
const formatEffectiveAt = (effectiveAt) => {
  if (!effectiveAt) return '다음 회차부터 적용됩니다.';
  const [date, time] = effectiveAt.split('T');
  return `${date.replaceAll('-', '.')} ${time?.slice(0, 5) || ''}부터 적용됩니다.`;
};

function SavingGroupSettingsForm({
  current, pending, canEditSettings, status, isSaving, submitErrorMessage, onSubmit, onCancel, onDeleteRequest,
}) {
  const isReadOnly = status === '03' || !canEditSettings;
  const initialValues = {
    groupName: toInputValue(current.groupName),
    description: toInputValue(current.description),
    maxMembers: toInputValue(current.maxMembers),
    duesAmount: toInputValue(current.duesAmount),
    duesDay: toInputValue(current.duesDay),
  };
  const [name, setName] = useState(initialValues.groupName);
  const [capacity, setCapacity] = useState(initialValues.maxMembers);
  const [description, setDescription] = useState(initialValues.description);
  const [monthlyDues, setMonthlyDues] = useState(initialValues.duesAmount);
  const [paymentDate, setPaymentDate] = useState(initialValues.duesDay);
  const [errors, setErrors] = useState({});

  const clearError = (fieldName) => {
    setErrors((previous) => ({ ...previous, form: '', [fieldName]: '' }));
  };

  const clearFormError = () => {
    setErrors((previous) => ({ ...previous, form: '' }));
  };

  const getChangedFields = () => {
    const changedFields = {};
    if (name !== initialValues.groupName) changedFields.groupName = name;
    if (description !== initialValues.description) changedFields.description = description;
    if (capacity !== initialValues.maxMembers) changedFields.maxMembers = toNumber(capacity);
    if (monthlyDues !== initialValues.duesAmount) changedFields.duesAmount = toNumber(monthlyDues);
    if (paymentDate !== initialValues.duesDay) changedFields.duesDay = toNumber(paymentDate);
    return changedFields;
  };

  const validateChangedFields = (changedFields) => {
    const nextErrors = {};

    if (Object.keys(changedFields).length === 0) {
      nextErrors.form = '변경한 항목이 없습니다.';
    }

    if (changedFields.groupName !== undefined) {
      const normalizedName = changedFields.groupName.trim();
      if (normalizedName.length < 2 || normalizedName.length > 20) {
        nextErrors.groupName = '계모임 이름은 공백을 제외하고 2~20자로 입력해주세요.';
      }
    }

    if (changedFields.description !== undefined && changedFields.description.trim().length > 40) {
      nextErrors.description = '소개글은 공백을 제외하고 40자 이하로 입력해주세요.';
    }

    if (changedFields.maxMembers !== undefined
      && (!Number.isInteger(changedFields.maxMembers)
        || changedFields.maxMembers < 4 || changedFields.maxMembers > 20)) {
      nextErrors.maxMembers = '정원은 4~20명의 정수로 입력해주세요.';
    }

    return nextErrors;
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    if (isReadOnly || isSaving) return;

    const changedFields = getChangedFields();
    const nextErrors = validateChangedFields(changedFields);
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;

    onSubmit(changedFields);
  };

  return (
    <Card>
      <CardTitle>계모임 정보 수정</CardTitle>
      <Description>모임 정보와 회비 납부 기준을 수정할 수 있습니다.</Description>
      {isReadOnly && (
        <ReadOnlyNotice role="status">
          {status === '03' ? '정지된 계모임은 설정을 수정할 수 없습니다.' : '현재 상태에서는 설정을 수정할 수 없습니다.'}
        </ReadOnlyNotice>
      )}
      <Form onSubmit={handleSubmit}>
        <FieldGrid>
          <Field>
            계모임 이름
            <Input
              name="name"
              disabled={isReadOnly}
              value={name}
              aria-invalid={Boolean(errors.groupName)}
              aria-describedby={errors.groupName ? 'group-name-error' : undefined}
              onChange={(event) => {
                setName(event.target.value);
                clearError('groupName');
              }}
            />
            {errors.groupName && <FieldError id="group-name-error">{errors.groupName}</FieldError>}
          </Field>
          <Field>
            정원
            <Input
              name="capacity"
              disabled={isReadOnly}
              inputMode="numeric"
              value={capacity}
              aria-invalid={Boolean(errors.maxMembers)}
              aria-describedby={errors.maxMembers ? 'max-members-error' : undefined}
              onChange={(event) => {
                setCapacity(event.target.value);
                clearError('maxMembers');
              }}
            />
            <CurrentValue>현재 적용: 승인 계원 {current.approvedMemberCount ?? 0}명 / 정원 {current.maxMembers ?? '미정'}명</CurrentValue>
            {errors.maxMembers && <FieldError id="max-members-error">{errors.maxMembers}</FieldError>}
          </Field>
          <Field>
            월 회비금액
            <Input
              name="monthlyDues"
              disabled={isReadOnly}
              inputMode="numeric"
              value={monthlyDues}
              onChange={(event) => {
                setMonthlyDues(event.target.value);
                clearFormError();
              }}
            />
            <CurrentValue>현재 적용: {formatDuesAmount(current.duesAmount)}</CurrentValue>
          </Field>
          <Field>
            납부일
            <Select
              name="paymentDate"
              disabled={isReadOnly}
              value={paymentDate}
              onChange={(event) => {
                setPaymentDate(event.target.value);
                clearFormError();
              }}
            >
              {Array.from({ length: 28 }, (_, index) => index + 1).map((day) => (
                <option key={day} value={day}>
                  매월 {day}일
                </option>
              ))}
            </Select>
            <CurrentValue>현재 적용: {formatDuesDay(current.duesDay)}</CurrentValue>
          </Field>
        </FieldGrid>

        {pending && (
          <PendingNotice>
            <PendingTitle>다음 회차 적용 예정</PendingTitle>
            <PendingText>
              회비: {pending.duesAmount == null ? '현재 회비 유지' : formatDuesAmount(pending.duesAmount)} · 납부일:{' '}
              {pending.duesDay == null ? '현재 납부일 유지' : formatDuesDay(pending.duesDay)}
            </PendingText>
            <PendingText>{formatEffectiveAt(pending.effectiveAt)}</PendingText>
          </PendingNotice>
        )}

        <FullField>
          소개글
          <Textarea
            name="description"
            disabled={isReadOnly}
            value={description}
            aria-invalid={Boolean(errors.description)}
            aria-describedby={errors.description ? 'description-error' : undefined}
            onChange={(event) => {
              setDescription(event.target.value);
              clearError('description');
            }}
            placeholder="계모임의 목적과 활동 내용을 소개해주세요"
          />
          <CurrentValue>비워두면 소개글이 삭제됩니다.</CurrentValue>
          {errors.description && <FieldError id="description-error">{errors.description}</FieldError>}
        </FullField>

        {errors.form && <FormError role="alert">{errors.form}</FormError>}
        {submitErrorMessage && <FormError role="alert">{submitErrorMessage}</FormError>}

        <Actions>
          {!isReadOnly && (
            <DeleteRequestButton type="button" $variant="danger" onClick={onDeleteRequest}>
              삭제 요청
            </DeleteRequestButton>
          )}
          <PrimaryActions>
            <ActionButton type="submit" disabled={isReadOnly || isSaving}>
              {isSaving ? '저장 중...' : '설정 저장'}
            </ActionButton>
            <ActionButton type="button" $variant="secondary" onClick={onCancel}>
              취소
            </ActionButton>
          </PrimaryActions>
        </Actions>
      </Form>
    </Card>
  );
}

export default SavingGroupSettingsForm;
