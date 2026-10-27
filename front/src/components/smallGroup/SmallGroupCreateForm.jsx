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
  onSubmit,
  onCancel,
}) {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const isRegionUnavailable = isRegionLoading || Boolean(regionLoadError) || regions.length === 0;

  // AI작성: 비동기 생성 요청이 끝날 때까지 버튼을 잠가 중복 저장과 취소 이동을 막는다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;

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
        <FieldGrid>
          <Field>
            소모임 이름
            <Control name="name" required placeholder="소모임 이름을 입력하세요" />
          </Field>
          <Field>
            활동 지역
            <Select name="region" required defaultValue="" disabled={isRegionUnavailable}>
              <option value="" disabled>
                {isRegionLoading
                  ? '활동 지역을 불러오는 중입니다.'
                  : regionLoadError
                    ? '활동 지역을 불러오지 못했습니다.'
                    : regions.length === 0
                      ? '등록된 활동 지역이 없습니다.'
                      : '활동 지역을 선택하세요'}
              </option>
              {regions.map((region) => (
                <option key={region.regionId} value={region.regionName}>
                  {region.regionName}
                </option>
              ))}
            </Select>
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
