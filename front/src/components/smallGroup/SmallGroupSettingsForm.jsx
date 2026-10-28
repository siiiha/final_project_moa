import { useEffect, useState } from 'react';
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
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

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
  font-family: inherit;
  font-size: 13px;

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
  font-family: inherit;
  font-size: 13px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Introduction = styled(Textarea)`
  min-height: 96px;
`;

const Actions = styled.div`
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
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

const Hint = styled.span`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  font-weight: 400;
`;

const DeleteCard = styled.section`
  margin-top: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: ${({ theme }) => theme.colors.dangerSoft};
  border: 1px solid #f5d6d3;
  border-radius: ${({ theme }) => theme.radius.md};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const DeleteTitle = styled.h2`
  color: ${({ theme }) => theme.colors.danger};
  font-size: 14px;
  font-weight: 800;
`;

const DeleteDescription = styled.p`
  margin-top: 5px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const DeleteButton = styled(Button)`
  min-height: 34px;
  padding: 7px 11px;
  font-size: 11px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

function getTomorrowIsoDate() {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const year = tomorrow.getFullYear();
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const day = String(tomorrow.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function SmallGroupSettingsForm({ settings, categories, onSubmit, onCancel, onDelete }) {
  const [name, setName] = useState(settings.groupName);
  const [interest, setInterest] = useState(settings.likeCategory);
  const [description, setDescription] = useState(settings.description);
  const [capacity, setCapacity] = useState(String(settings.maxMembers));
  const [schedule, setSchedule] = useState(settings.meetDate);
  const [place, setPlace] = useState(settings.location);
  const minimumCapacity = Math.max(2, settings.approvedMemberCount);

  // AI작성: 탭을 닫았다가 다시 열거나 재조회하면 서버의 최신 설정값으로 폼 초깃값을 복원한다.
  useEffect(() => {
    setName(settings.groupName);
    setInterest(settings.likeCategory);
    setDescription(settings.description);
    setCapacity(String(settings.maxMembers));
    setSchedule(settings.meetDate);
    setPlace(settings.location);
  }, [settings]);

  return (
    <>
      <Card>
        <CardTitle>소모임 설정</CardTitle>
        <Description>기본 정보와 모임 일정을 관리하세요.</Description>
        <Form onSubmit={onSubmit}>
          <FieldGrid>
            <Field>
              소모임 이름
              <Input name="groupName" required minLength="2" maxLength="20" value={name} onChange={(event) => setName(event.target.value)} />
            </Field>
            <Field>
              관심 카테고리
              <Select name="likeCategory" required value={interest} onChange={(event) => setInterest(event.target.value)}>
                {categories.map((category) => (
                  <option key={category.interestId} value={category.interestName}>{category.interestName}</option>
                ))}
              </Select>
            </Field>
            <Field>
              정원
              <Input name="maxMembers" type="number" min={minimumCapacity} max="20" required value={capacity} onChange={(event) => setCapacity(event.target.value)} />
              <Hint>현재 승인 인원은 {settings.approvedMemberCount}명입니다.</Hint>
            </Field>
            <Field>
              일정
              <Input name="meetDate" type="date" min={getTomorrowIsoDate()} required value={schedule} onChange={(event) => setSchedule(event.target.value)} />
            </Field>
            <Field>
              장소
              <Input name="location" required maxLength="40" value={place} onChange={(event) => setPlace(event.target.value)} />
            </Field>
          </FieldGrid>

          <FullField>
            소개글
            <Introduction name="description" required maxLength="40" value={description} onChange={(event) => setDescription(event.target.value)} />
          </FullField>

          <Actions>
            <ActionButton type="submit" disabled title="소모임 수정 API 구현 후 저장할 수 있습니다.">설정 저장</ActionButton>
            <ActionButton type="button" $variant="secondary" onClick={onCancel}>취소</ActionButton>
          </Actions>
        </Form>
      </Card>

      <DeleteCard>
        <div>
          <DeleteTitle>소모임 삭제</DeleteTitle>
          <DeleteDescription>삭제 후 공지와 투표 등 모임 정보를 더 이상 이용할 수 없습니다.</DeleteDescription>
        </div>
        <DeleteButton type="button" $variant="danger" onClick={onDelete}>소모임 삭제</DeleteButton>
      </DeleteCard>
    </>
  );
}

export default SmallGroupSettingsForm;
