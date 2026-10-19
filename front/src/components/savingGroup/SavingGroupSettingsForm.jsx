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
`;

const DeleteRequestButton = styled(ActionButton)`
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const getCapacity = (members) => members.split('/')[1]?.trim().replace('명', '') || '';
const getMonthlyDues = (monthlyDues) => monthlyDues.replace('월 ', '').replace('원', '');
const getPaymentDate = (nextDate) => nextDate.replace('매월 ', '').replace('일', '');

function SavingGroupSettingsForm({ group, onSubmit, onCancel, onDeleteRequest }) {
  const [name, setName] = useState(group.name);
  const [capacity, setCapacity] = useState(getCapacity(group.members));
  const [description, setDescription] = useState(group.description);
  const [monthlyDues, setMonthlyDues] = useState(getMonthlyDues(group.monthlyDues));
  const [paymentDate, setPaymentDate] = useState(getPaymentDate(group.nextDate));

  return (
    <Card>
      <CardTitle>계모임 정보 수정</CardTitle>
      <Description>모임 정보와 회비 납부 기준을 수정할 수 있습니다.</Description>
      <Form onSubmit={onSubmit}>
        <FieldGrid>
          <Field>
            계모임 이름
            <Input name="name" required value={name} onChange={(event) => setName(event.target.value)} />
          </Field>
          <Field>
            정원
            <Input
              name="capacity"
              required
              inputMode="numeric"
              value={capacity}
              onChange={(event) => setCapacity(event.target.value)}
            />
          </Field>
          <Field>
            월 회비금액
            <Input
              name="monthlyDues"
              required
              inputMode="numeric"
              value={monthlyDues}
              onChange={(event) => setMonthlyDues(event.target.value)}
            />
          </Field>
          <Field>
            납부일
            <Select name="paymentDate" required value={paymentDate} onChange={(event) => setPaymentDate(event.target.value)}>
              {Array.from({ length: 28 }, (_, index) => index + 1).map((day) => (
                <option key={day} value={day}>
                  매월 {day}일
                </option>
              ))}
            </Select>
          </Field>
        </FieldGrid>

        <FullField>
          소개글
          <Textarea
            name="description"
            required
            value={description}
            onChange={(event) => setDescription(event.target.value)}
            placeholder="계모임의 목적과 활동 내용을 소개해주세요"
          />
        </FullField>

        <Actions>
          <DeleteRequestButton type="button" $variant="danger" onClick={onDeleteRequest}>
            삭제 요청
          </DeleteRequestButton>
          <PrimaryActions>
            <ActionButton type="submit">설정 저장</ActionButton>
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
