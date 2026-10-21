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
const formatDuesAmount = (amount) => (amount == null ? '회비 미정' : `월 ${amount.toLocaleString('ko-KR')}원`);
const formatDuesDay = (day) => (day == null ? '납부일 미정' : `매월 ${day}일`);
const formatEffectiveAt = (effectiveAt) => {
  if (!effectiveAt) return '다음 회차부터 적용됩니다.';
  const [date, time] = effectiveAt.split('T');
  return `${date.replaceAll('-', '.')} ${time?.slice(0, 5) || ''}부터 적용됩니다.`;
};

function SavingGroupSettingsForm({ current, pending, canEditSettings, status, onSubmit, onCancel, onDeleteRequest }) {
  const [name, setName] = useState(current.groupName);
  const [capacity, setCapacity] = useState(toInputValue(current.maxMembers));
  const [description, setDescription] = useState(current.description);
  const [monthlyDues, setMonthlyDues] = useState(toInputValue(current.duesAmount));
  const [paymentDate, setPaymentDate] = useState(toInputValue(current.duesDay));

  return (
    <Card>
      <CardTitle>계모임 정보 수정</CardTitle>
      <Description>모임 정보와 회비 납부 기준을 수정할 수 있습니다.</Description>
      {!canEditSettings && (
        <ReadOnlyNotice role="status">
          {status === '03' ? '정지된 계모임은 설정을 수정할 수 없습니다.' : '현재 상태에서는 설정을 수정할 수 없습니다.'}
        </ReadOnlyNotice>
      )}
      <Form onSubmit={onSubmit}>
        <FieldGrid>
          <Field>
            계모임 이름
            <Input name="name" required disabled={!canEditSettings} value={name} onChange={(event) => setName(event.target.value)} />
          </Field>
          <Field>
            정원
            <Input
              name="capacity"
              required
              disabled={!canEditSettings}
              inputMode="numeric"
              value={capacity}
              onChange={(event) => setCapacity(event.target.value)}
            />
            <CurrentValue>현재 적용: 승인 계원 {current.approvedMemberCount ?? 0}명 / 정원 {current.maxMembers ?? '미정'}명</CurrentValue>
          </Field>
          <Field>
            월 회비금액
            <Input
              name="monthlyDues"
              required
              disabled={!canEditSettings}
              inputMode="numeric"
              value={monthlyDues}
              onChange={(event) => setMonthlyDues(event.target.value)}
            />
            <CurrentValue>현재 적용: {formatDuesAmount(current.duesAmount)}</CurrentValue>
          </Field>
          <Field>
            납부일
            <Select name="paymentDate" required disabled={!canEditSettings} value={paymentDate} onChange={(event) => setPaymentDate(event.target.value)}>
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
            required
            disabled={!canEditSettings}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="계모임의 목적과 활동 내용을 소개해주세요"
          />
        </FullField>

        <Actions>
          {canEditSettings && (
            <DeleteRequestButton type="button" $variant="danger" onClick={onDeleteRequest}>
              삭제 요청
            </DeleteRequestButton>
          )}
          <PrimaryActions>
            <ActionButton type="submit" disabled={!canEditSettings}>설정 저장</ActionButton>
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
