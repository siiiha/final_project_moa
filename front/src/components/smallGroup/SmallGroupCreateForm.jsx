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

const FullField = styled(Field)`
  margin-top: 16px;
`;

const DetailFieldGrid = styled(FieldGrid)`
  margin-top: 16px;
`;

const Control = styled.input`
  width: 100%;
  height: 42px;
  padding: 0 12px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-family: inherit;
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
  font-family: inherit;
  font-size: 13px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Introduction = styled(Textarea)`
  min-height: 98px;
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

function SmallGroupCreateForm({
  regions,
  interests,
  isRegionLoading,
  regionLoadError,
  onRegionRetry,
  onSubmit,
  onCancel,
}) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isRegionUnavailable = isRegionLoading || Boolean(regionLoadError) || regions.length === 0;
  // AI작성: 현재 상위 지역에 속한 구·동만 선택하며 동 미선택 시 구의 정규 지역명을 전달한다.
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

  // AI작성: 비동기 생성 요청이 끝날 때까지 버튼을 잠가 중복 저장과 취소 이동을 막는다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting || isRegionUnavailable || !selectedRegion?.regionName) return;

    setIsSubmitting(true);
    try {
      await onSubmit(event);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardTitle>소모임 정보 입력</CardTitle>
      <Description>이웃과 함께할 소모임의 기본 정보를 입력해주세요.</Description>
      <Form onSubmit={handleSubmit} aria-busy={isSubmitting}>
        {/* AI작성: 기존 페이지의 region → regionName 변환과 생성 요청 계약을 유지한다. */}
        <input type="hidden" name="region" value={selectedRegion?.regionName || ''} />
        <FieldGrid>
          <Field>
            소모임 이름
            <Control name="name" required placeholder="소모임 이름을 입력하세요" />
          </Field>
          <Field>
            관심사 카테고리
            <Select name="interest" required defaultValue="">
              <option value="" disabled>
                관심사를 선택하세요
              </option>
              {interests.filter((interest) => interest !== '전체').map((interest) => (
                <option key={interest} value={interest}>
                  {interest}
                </option>
              ))}
            </Select>
          </Field>
        </FieldGrid>

        {/* AI작성: 지역 세 항목은 같은 줄에 배치하고 상위 변경 시 하위 선택을 초기화한다. */}
        <DetailFieldGrid>
          <Field>
            활동 지역 시·도 (필수)
            <Select required value={province} disabled={isRegionUnavailable || isSubmitting}
              onChange={(event) => {
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
            <Select required value={districtId} disabled={!province || isRegionUnavailable || isSubmitting}
              onChange={(event) => {
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
            <Select value={townId} disabled={!district || isRegionUnavailable || isSubmitting || towns.length === 0}
              onChange={(event) => setTownId(event.target.value)}>
              <option value="">{district && towns.length === 0 ? '선택 가능한 읍·면·동이 없습니다' : '선택하지 않음 (구 단위 활동)'}</option>
              {towns.map((item) => <option key={item.regionId} value={item.regionId}>{item.townName}</option>)}
            </Select>
          </Field>
        </DetailFieldGrid>
        {/* AI작성: 조회 실패 후 다른 입력을 보존하면서 지역 목록만 다시 불러온다. */}
        {isRegionLoading && <Description role="status">활동 지역을 불러오는 중입니다.</Description>}
        {regionLoadError && <div role="alert">
          <Description>{regionLoadError}</Description>
          <ActionButton type="button" onClick={onRegionRetry}>지역 다시 불러오기</ActionButton>
        </div>}

        <FullField>
          소모임 소개글
          <Introduction name="description" required placeholder="어떤 소모임인지 소개해주세요" />
        </FullField>

        <DetailFieldGrid>
          <Field>
            정원
            <Control name="capacity" type="number" min="2" required inputMode="numeric" placeholder="예: 20" />
          </Field>
          <Field>
            만남 일정
            <Control name="meetingDate" type="date" required />
          </Field>
          <Field>
            장소
            <Control name="place" required placeholder="만날 장소를 입력하세요" />
          </Field>
        </DetailFieldGrid>

        <Actions>
          <ActionButton type="button" $variant="secondary" onClick={onCancel} disabled={isSubmitting}>
            취소
          </ActionButton>
          <ActionButton type="submit" disabled={isSubmitting || isRegionUnavailable}>
            {isSubmitting ? '생성 중...' : '소모임 만들기'}
          </ActionButton>
        </Actions>
      </Form>
    </Card>
  );
}

export default SmallGroupCreateForm;
