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

const ErrorMessage = styled.p`
  margin-top: 14px;
  color: ${({ theme }) => theme.colors.danger};
  font-size: 12px;
  line-height: 1.5;
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

function normalizeText(value) {
  return value.trim();
}

// AI작성: 서버 설정값과 현재 입력값을 비교해 PATCH에 필요한 필드만 만들고 화면에서 확정된 범위를 먼저 검사한다.
function getSettingsChanges(settings, { name, interest, description, capacity, schedule, place }) {
  const values = {
    groupName: normalizeText(name),
    likeCategory: normalizeText(interest),
    description: normalizeText(description),
    maxMembers: Number(capacity),
    meetDate: schedule,
    location: normalizeText(place),
  };
  const initialValues = {
    groupName: normalizeText(settings.groupName),
    likeCategory: normalizeText(settings.likeCategory),
    description: normalizeText(settings.description),
    maxMembers: settings.maxMembers,
    meetDate: settings.meetDate,
    location: normalizeText(settings.location),
  };
  return Object.fromEntries(Object.entries(values)
    .filter(([field, value]) => value !== initialValues[field]));
}

function validateSettings(changes, values, approvedMemberCount) {
  const name = normalizeText(values.name);
  const interest = normalizeText(values.interest);
  const description = normalizeText(values.description);
  const location = normalizeText(values.place);
  const maxMembers = Number(values.capacity);

  if (Object.hasOwn(changes, 'groupName') && (name.length < 2 || name.length > 20)) {
    return '소모임 이름은 공백을 제외하고 2자 이상 20자 이하여야 합니다.';
  }
  if (Object.hasOwn(changes, 'likeCategory') && (!interest || interest.length > 50)) {
    return '관심사 카테고리를 입력해 주세요.';
  }
  if (Object.hasOwn(changes, 'description') && description.length > 40) {
    return '소개글은 공백을 제외하고 40자 이하여야 합니다.';
  }
  if (Object.hasOwn(changes, 'maxMembers')
    && (!Number.isInteger(maxMembers) || maxMembers < 2 || maxMembers > 20)) {
    return '정원은 2명 이상 20명 이하여야 합니다.';
  }
  if (Object.hasOwn(changes, 'maxMembers') && maxMembers < approvedMemberCount) {
    return '정원은 현재 승인 인원보다 작게 설정할 수 없습니다.';
  }
  if (Object.hasOwn(changes, 'meetDate')
    && (!values.schedule || values.schedule < getTomorrowIsoDate())) {
    return '만남일은 내일부터 선택할 수 있습니다.';
  }
  if (Object.hasOwn(changes, 'location') && (!location || location.length > 40)) {
    return '만남 장소를 입력해 주세요.';
  }
  return Object.keys(changes).length === 0 ? '변경된 설정이 없습니다.' : null;
}

function SmallGroupSettingsForm({ settings, categories, isSaving, onSubmit, onCancel, onDelete }) {
  const [name, setName] = useState(settings.groupName);
  const [interest, setInterest] = useState(settings.likeCategory);
  const [description, setDescription] = useState(settings.description);
  const [capacity, setCapacity] = useState(String(settings.maxMembers));
  const [schedule, setSchedule] = useState(settings.meetDate);
  const [place, setPlace] = useState(settings.location);
  const [formError, setFormError] = useState(null);
  const minimumCapacity = Math.max(2, settings.approvedMemberCount);
  const values = { name, interest, description, capacity, schedule, place };
  const changes = getSettingsChanges(settings, values);

  // AI작성: 탭을 닫았다가 다시 열거나 재조회하면 서버의 최신 설정값으로 폼 초깃값을 복원한다.
  useEffect(() => {
    setName(settings.groupName);
    setInterest(settings.likeCategory);
    setDescription(settings.description);
    setCapacity(String(settings.maxMembers));
    setSchedule(settings.meetDate);
    setPlace(settings.location);
    setFormError(null);
  }, [settings]);

  const handleSubmit = async (event) => {
    event.preventDefault();
    const validationError = validateSettings(changes, values, settings.approvedMemberCount);
    if (validationError) {
      setFormError(validationError);
      return;
    }

    setFormError(null);
    try {
      await onSubmit(changes);
    } catch (error) {
      setFormError(error.message || '소모임 설정을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  return (
    <>
      <Card>
        <CardTitle>소모임 설정</CardTitle>
        <Description>기본 정보와 모임 일정을 관리하세요.</Description>
        <Form noValidate onSubmit={handleSubmit}>
          <FieldGrid>
            <Field>
              소모임 이름
              <Input name="groupName" required minLength="2" maxLength="20" value={name} onChange={(event) => { setName(event.target.value); setFormError(null); }} />
            </Field>
            <Field>
              관심 카테고리
              <Select name="likeCategory" required value={interest} onChange={(event) => { setInterest(event.target.value); setFormError(null); }}>
                {categories.map((category) => (
                  <option key={category.interestId} value={category.interestName}>{category.interestName}</option>
                ))}
              </Select>
            </Field>
            <Field>
              정원
              <Input name="maxMembers" type="number" min={minimumCapacity} max="20" required value={capacity} onChange={(event) => { setCapacity(event.target.value); setFormError(null); }} />
              <Hint>현재 승인 인원은 {settings.approvedMemberCount}명입니다.</Hint>
            </Field>
            <Field>
              일정
              <Input name="meetDate" type="date" min={getTomorrowIsoDate()} required value={schedule} onChange={(event) => { setSchedule(event.target.value); setFormError(null); }} />
            </Field>
            <Field>
              장소
              <Input name="location" required maxLength="40" value={place} onChange={(event) => { setPlace(event.target.value); setFormError(null); }} />
            </Field>
          </FieldGrid>

          <FullField>
            소개글
            <Introduction name="description" maxLength="40" value={description} onChange={(event) => { setDescription(event.target.value); setFormError(null); }} />
          </FullField>

          {formError && <ErrorMessage role="alert">{formError}</ErrorMessage>}

          <Actions>
            <ActionButton type="submit" disabled={isSaving || Object.keys(changes).length === 0}>
              {isSaving ? '저장 중...' : '설정 저장'}
            </ActionButton>
            <ActionButton type="button" $variant="secondary" disabled={isSaving} onClick={onCancel}>취소</ActionButton>
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
