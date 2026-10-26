import { useState } from 'react';
import { FiImage } from 'react-icons/fi';
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

  @media (max-width: ${({ theme }) => theme.breakpoints.laptop}) {
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

const TypeField = styled.fieldset`
  min-width: 0;
  padding: 0;
  border: 0;
`;

const Legend = styled.legend`
  margin-bottom: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const TypeOptions = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
`;

const RadioInput = styled.input`
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
`;

const TypeOption = styled.label`
  min-height: 42px;
  padding: 8px 10px;
  display: grid;
  align-content: center;
  gap: 2px;
  color: ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primarySoft : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  cursor: pointer;

  strong {
    font-size: 12px;
  }

  span {
    font-size: 10px;
  }

  &:hover {
    border-color: ${({ theme }) => theme.colors.primary};
  }

  &:has(input:focus-visible) {
    outline: 3px solid rgba(23, 107, 91, 0.22);
    outline-offset: 2px;
  }
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

const UploadArea = styled.div`
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: ${({ theme }) => theme.colors.background};
  border: 1px dashed ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const FileInput = styled.input`
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
`;

const UploadButton = styled.label`
  min-height: 36px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
  }
`;

const FileName = styled.p`
  min-width: 0;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const HelpText = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
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

function SavingGroupApplicationForm({ regions, onSubmit, onCancel, isSubmitting }) {
  const [imageName, setImageName] = useState('선택된 이미지가 없습니다.');
  const [groupType, setGroupType] = useState('');

  const handleImageChange = (event) => {
    const [image] = event.target.files;
    setImageName(image ? image.name : '선택된 이미지가 없습니다.');
  };

  return (
    <Card>
      <CardTitle>계모임 개설 신청</CardTitle>
      <Description>모임 정보를 작성하면 관리자 심사 후 개설 여부가 안내됩니다.</Description>
      <Form onSubmit={onSubmit}>
        <FieldGrid>
          <Field>
            계모임 이름
            <Input name="groupName" maxLength="100" required placeholder="계모임 이름을 입력하세요" />
          </Field>
          <TypeField>
            <Legend>계모임 종류</Legend>
            <TypeOptions>
              <TypeOption $selected={groupType === '친목계'}>
                <RadioInput
                  type="radio"
                  name="groupType"
                  value="친목계"
                  required
                  checked={groupType === '친목계'}
                  onChange={(event) => setGroupType(event.target.value)}
                />
                <strong>친목계</strong>
                <span>함께 친목 활동을 하는 모임</span>
              </TypeOption>
              <TypeOption $selected={groupType === '순번계'}>
                <RadioInput
                  type="radio"
                  name="groupType"
                  value="순번계"
                  required
                  checked={groupType === '순번계'}
                  onChange={(event) => setGroupType(event.target.value)}
                />
                <strong>순번계</strong>
                <span>정해진 순서로 계금을 받는 모임</span>
              </TypeOption>
            </TypeOptions>
          </TypeField>
          <Field>
            활동 지역
            <Select name="regionName" required defaultValue="">
              <option value="" disabled>
                지역을 선택하세요
              </option>
              {regions
                .filter((region) => region !== '전체')
                .map((region) => (
                  <option key={region} value={region}>
                    {region}
                  </option>
                ))}
            </Select>
          </Field>
          <Field>
            월 회비
            <Input name="duesAmount" required inputMode="numeric" pattern="[0-9]+" placeholder="예: 30000" />
          </Field>
          <Field>
            납부일
            <Select name="duesDay" required defaultValue="">
              <option value="" disabled>
                납부일을 선택하세요
              </option>
              {Array.from({ length: 28 }, (_, index) => index + 1).map((day) => (
                <option key={day} value={day}>
                  매월 {day}일
                </option>
              ))}
            </Select>
          </Field>
          <Field>
            모집 인원
            <Input name="maxMembers" required inputMode="numeric" pattern="[0-9]+" placeholder="예: 10명" />
          </Field>
        </FieldGrid>

        <FullField>
          계모임 소개
          <Textarea name="description" maxLength="50" required placeholder="계모임의 목적과 활동 내용을 소개해주세요" />
        </FullField>

        <FullField as="div">
          <span>계모임 이미지 (선택)</span>
          <UploadArea>
            <FileInput
              id="saving-group-image"
              name="groupImage"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={handleImageChange}
            />
            <UploadButton htmlFor="saving-group-image">
              <FiImage size={15} aria-hidden="true" /> 이미지 선택
            </UploadButton>
            <FileName>{imageName}</FileName>
          </UploadArea>
          <HelpText>JPG, PNG, WEBP 형식의 이미지만 업로드할 수 있습니다.</HelpText>
        </FullField>

        <Actions>
          <ActionButton type="submit" disabled={isSubmitting}>
            {isSubmitting ? '신청 중...' : '개설 신청'}
          </ActionButton>
          <ActionButton type="button" $variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            취소
          </ActionButton>
        </Actions>
      </Form>
    </Card>
  );
}

export default SavingGroupApplicationForm;
