import { useState } from 'react';
import { FiImage } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';
import Textarea from '../common/Textarea';

// AI작성: 소개글 높이를 유지하고 긴 내용은 내부 스크롤로 확인한다.
const Introduction = styled(Textarea)`
  height: 142px;
  min-height: 142px;
  resize: none;
  overflow-y: auto;
`;

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
  /* AI작성: 오류 문구 공간이 있는 필드와 같은 줄의 입력칸을 위쪽에 맞춘다. */
  align-items: start;
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.laptop}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

// AI작성: 오류가 없을 때도 같은 문구의 줄바꿈 높이를 확보한다.
const ValidationMessage = styled.small`
  visibility: ${({ $visible }) => ($visible ? 'visible' : 'hidden')};
`;

const memberRangeMessage = '모집 인원은 4~20명의 정수로 입력해 주세요.';
const duesRangeMessage = '월 회비는 10,000원~1,000,000원의 정수로 입력해 주세요.';

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

// AI작성: 원 단위를 입력칸에 고정해 숫자 편집과 분리한다.
const DuesInput = styled.span`
  position: relative;
  display: block;

  input {
    padding-right: 32px;
  }

  > span {
    position: absolute;
    top: 50%;
    right: 12px;
    transform: translateY(-50%);
    pointer-events: none;
    color: ${({ theme }) => theme.colors.muted};
  }
`;

function SavingGroupApplicationForm({ regions, regionsLoading, regionsError, onRegionRetry, onSubmit, onCancel, isSubmitting }) {
  const [duesAmount, setDuesAmount] = useState('');
  const [imageName, setImageName] = useState('선택된 이미지가 없습니다.');
  const [groupType, setGroupType] = useState('');
  // AI작성: 시·구는 필수, 동은 선택이며 현재 상위 지역에 속한 항목만 제출한다.
  const [province, setProvince] = useState('');
  const [districtId, setDistrictId] = useState('');
  const [townId, setTownId] = useState('');
  const provinces = [...new Set(regions.map((item) => item.provinceName).filter(Boolean))];
  const districts = regions.filter((item) => item.provinceName === province && !item.townName);
  const district = districts.find((item) => String(item.regionId) === districtId);
  const towns = district ? regions.filter((item) => item.townName
    && item.provinceName === district.provinceName && item.districtName === district.districtName) : [];
  const town = towns.find((item) => String(item.regionId) === townId);
  const selectedRegion = townId ? town : district;
  const regionUnavailable = regionsLoading || Boolean(regionsError);
  const [amountErrors, setAmountErrors] = useState({});
  const handleSubmit = (event) => {
    if (regionUnavailable || !selectedRegion?.regionName) {
      event.preventDefault();
      return;
    }
    // AI작성: 개설 요청에 담기는 원본 숫자값으로 범위를 검증한다.
    const fields = new FormData(event.currentTarget);
    const capacityInput = event.currentTarget.elements.namedItem('maxMembers');
    const maxMembers = capacityInput.valueAsNumber;
    const duesAmount = Number(fields.get('duesAmount'));
    const nextErrors = {};
    if (!Number.isInteger(maxMembers) || maxMembers < 4 || maxMembers > 20) {
      nextErrors.maxMembers = memberRangeMessage;
      capacityInput.focus();
    }
    if (!Number.isInteger(duesAmount) || duesAmount < 10_000 || duesAmount > 1_000_000) {
      nextErrors.duesAmount = duesRangeMessage;
    }
    setAmountErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) {
      event.preventDefault();
      return;
    }
    onSubmit(event);
  };

  const handleImageChange = (event) => {
    const [image] = event.target.files;
    setImageName(image ? image.name : '선택된 이미지가 없습니다.');
  };

  return (
    <Card>
      <CardTitle>계모임 개설 신청</CardTitle>
      <Description>모임 정보를 작성하면 관리자 심사 후 개설 여부가 안내됩니다.</Description>
      <Form onSubmit={handleSubmit}>
        {/* AI작성: 기존 생성 API의 regionName 계약에 최종 선택한 구 또는 동의 정규 이름을 전달한다. */}
        <input type="hidden" name="regionName" value={selectedRegion?.regionName || ''} />
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
          {/* AI작성: 첫 줄은 이름·종류·모집 인원, 둘째 줄은 시·구·동 순서로 배치한다. */}
          <Field>
            모집 인원
            <Input
              name="maxMembers" type="number" min="4" max="20" step="1" required inputMode="numeric" placeholder="예: 10명"
              aria-invalid={Boolean(amountErrors.maxMembers)}
              aria-describedby={amountErrors.maxMembers ? 'creation-member-range' : undefined}
              onInvalid={() => setAmountErrors((previous) => ({ ...previous, maxMembers: memberRangeMessage }))}
              onChange={(event) => {
                // AI작성: 소모임처럼 범위 밖 입력을 보정하고 재입력을 위한 빈 값은 허용한다.
                if (event.target.valueAsNumber > 20) event.target.value = '20';
                if (event.target.valueAsNumber < 4) event.target.value = '4';
                const value = event.target.value;
                const count = Number(value);
                const error = value !== '' && (!/^[0-9]+$/.test(value) || count < 4 || count > 20)
                  ? memberRangeMessage
                  : undefined;
                setAmountErrors((previous) => ({ ...previous, maxMembers: error }));
              }}
            />
            <ValidationMessage id="creation-member-range" $visible={Boolean(amountErrors.maxMembers)}
              role={amountErrors.maxMembers ? 'alert' : undefined}>
              {memberRangeMessage}
            </ValidationMessage>
          </Field>
          <Field>
            활동 지역 시·도 (필수)
            <Select required value={province} disabled={regionUnavailable || isSubmitting}
              onChange={(event) => {
                // AI작성: 시 변경 시 기존 구·동 선택을 함께 초기화한다.
                setProvince(event.target.value);
                setDistrictId('');
                setTownId('');
              }}>
              <option value="">시·도를 선택하세요</option>
              {provinces.map((item) => <option key={item} value={item}>{item}</option>)}
            </Select>
          </Field>
          <Field>
            활동 지역 시·군·구 (필수)
            <Select required value={districtId} disabled={!province || regionUnavailable || isSubmitting}
              onChange={(event) => {
                // AI작성: 구 변경 시 기존 동 선택을 초기화한다. 세종은 전체 항목으로 선택한다.
                setDistrictId(event.target.value);
                setTownId('');
              }}>
              <option value="">시·군·구를 선택하세요</option>
              {districts.map((item) => <option key={item.regionId} value={item.regionId}>
                {item.districtName || `${item.provinceName} 전체`}
              </option>)}
            </Select>
          </Field>
          <Field>
            활동 지역 읍·면·동 (선택)
            <Select value={townId} disabled={!district || regionUnavailable || isSubmitting || towns.length === 0}
              onChange={(event) => setTownId(event.target.value)}>
              <option value="">{district && towns.length === 0 ? '선택 가능한 읍·면·동이 없습니다' : '미선택 시 (구 단위 활동)'}</option>
              {towns.map((item) => <option key={item.regionId} value={item.regionId}>{item.townName}</option>)}
            </Select>
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
            월 회비
            <DuesInput>
              <Input
                required
                aria-invalid={Boolean(amountErrors.duesAmount)}
                aria-describedby={amountErrors.duesAmount ? 'creation-dues-range' : undefined}
                inputMode="numeric"
                value={duesAmount.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}
                onChange={(event) => {
                  const digits = event.target.value.replace(/,/g, '');
                  // AI작성: 표시용 쉼표를 뺀 숫자로 회비를 검증한다.
                  if (!/^\d*$/.test(digits)) return;
                  const amount = Number(digits);
                  const error = digits !== '' && (amount < 10_000 || amount > 1_000_000)
                    ? duesRangeMessage
                    : undefined;
                  setDuesAmount(digits);
                  setAmountErrors((previous) => ({ ...previous, duesAmount: error }));
                }}
                placeholder="예: 30,000"
              />
              <span aria-hidden="true">원</span>
            </DuesInput>
            {/* AI작성: 개설 요청에는 기존처럼 숫자 문자열만 담는다. */}
            <input type="hidden" name="duesAmount" value={duesAmount} />
            <ValidationMessage id="creation-dues-range" $visible={Boolean(amountErrors.duesAmount)}
              role={amountErrors.duesAmount ? 'alert' : undefined}>
              {duesRangeMessage}
            </ValidationMessage>
          </Field>
        </FieldGrid>
        {/* AI작성: 지역 조회 상태를 안내하고 입력한 다른 내용을 유지한 채 재시도한다. */}
        {regionsLoading && <HelpText role="status">지역을 불러오는 중입니다.</HelpText>}
        {regionsError && <div role="alert">
          <HelpText>{regionsError}</HelpText>
          <ActionButton type="button" onClick={onRegionRetry}>지역 다시 불러오기</ActionButton>
        </div>}

        <FullField>
          계모임 소개
          <Introduction name="description" maxLength="50" required placeholder="계모임의 목적과 활동 내용을 소개해주세요" />
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
          <ActionButton type="submit" disabled={isSubmitting || regionUnavailable}>
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
