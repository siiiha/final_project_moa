import { FiCheckCircle, FiMapPin, FiUser } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const SummaryGrid = styled.section`
  display: grid;
  grid-template-columns: 1.3fr 1fr 1fr;
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.article`
  min-height: 115px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const SummaryValue = styled.strong`
  display: block;
  margin-top: 12px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 22px;
  letter-spacing: -1px;
`;

const SummaryDescription = styled.p`
  margin-top: 2px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 10px;
`;

const DetailGrid = styled.section`
  margin-top: 14px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 0.8fr);
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Card = styled.section`
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const CardTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const InformationList = styled.dl`
  margin-top: 14px;
  display: grid;
  grid-template-columns: 86px minmax(0, 1fr);
  gap: 10px 14px;
  font-size: 12px;

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-weight: 700;
  }

  dd {
    min-width: 0;
    color: ${({ theme }) => theme.colors.secondaryText};
    overflow-wrap: anywhere;
  }
`;

const VerificationList = styled.div`
  margin-top: 14px;
  display: grid;
  gap: 9px;
`;

const VerificationItem = styled.div`
  padding: 11px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};

  p {
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    font-weight: 700;
  }

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    color: ${({ theme }) => theme.colors.primary};
    font-size: 11px;
    font-weight: 700;
  }
`;

const ManagementCard = styled(Card)`
  margin-top: 14px;
`;

const ManagementForm = styled.form`
  margin-top: 14px;
  display: grid;
  grid-template-columns: 130px 150px 145px minmax(0, 1fr) auto;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const RestrictionList = styled.div`
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`;

const RestrictionItem = styled.p`
  padding: 8px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

const ResultMessage = styled.p`
  margin-top: 10px;
  color: ${({ $error, theme }) => ($error ? theme.colors.danger : theme.colors.primary)};
  font-size: 12px;
`;

const Control = styled.select`
  height: 38px;
  padding: 0 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const ReasonInput = styled.input`
  min-width: 0;
  height: 38px;
  padding: 0 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const ListButton = styled(Button)`
  min-height: 34px;
  padding: 7px 11px;
  font-size: 11px;
`;

const BackActions = styled.div`
  margin-top: 14px;
`;

function AdminMemberDetail({
  member,
  detail,
  action,
  restrictionScope,
  reason,
  restrictionType,
  submitting,
  message,
  errorMessage,
  onActionChange,
  onRestrictionScopeChange,
  onReasonChange,
  onRestrictionTypeChange,
  onSubmit,
  onBack,
}) {
  const actionIsRestrict = action === '이용 제한';

  return (
    <>
      <SummaryGrid aria-label="회원 활동 요약">
        <SummaryCard>
          <SummaryLabel>
            <FiUser size={14} aria-hidden="true" /> 회원 상태
          </SummaryLabel>
          <SummaryValue>{member.status}</SummaryValue>
          <SummaryDescription>
            로그인 가능 여부를 나타내는 계정 상태
          </SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>참여 모임</SummaryLabel>
          <SummaryValue>{detail.joinedGatherings}개</SummaryValue>
          <SummaryDescription>현재 참여 중인 모임</SummaryDescription>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>작성 게시글</SummaryLabel>
          <SummaryValue>{detail.writtenPosts}개</SummaryValue>
          <SummaryDescription>게시판 등록 게시글</SummaryDescription>
        </SummaryCard>
      </SummaryGrid>

      <DetailGrid>
        <Card>
          <CardTitle>기본 정보</CardTitle>
          <InformationList>
            <dt>이름</dt>
            <dd>{member.name}</dd>
            <dt>이메일</dt>
            <dd>{member.email}</dd>
            <dt>휴대전화</dt>
            <dd>{detail.phone}</dd>
            <dt>활동 지역</dt>
            <dd>{detail.address}</dd>
            <dt>가입일</dt>
            <dd>{member.joinedAt}</dd>
          </InformationList>
        </Card>
        <Card>
          <CardTitle>인증 상태</CardTitle>
          <VerificationList>
            <VerificationItem>
              <p>본인 인증</p>
              <span>
                <FiCheckCircle size={13} aria-hidden="true" /> {detail.identityStatus}
              </span>
            </VerificationItem>
            <VerificationItem>
              <p>지역 인증</p>
              <span>
                <FiMapPin size={13} aria-hidden="true" /> {detail.regionStatus}
              </span>
            </VerificationItem>
          </VerificationList>
        </Card>
      </DetailGrid>

      <ManagementCard>
        <CardTitle>이용 제한 관리</CardTitle>
        {/* AI작성: 현재 적용 중인 기능별 제한을 관리자에게 범위와 종료 시각으로 구분해 표시한다. */}
        <RestrictionList>
          {detail.restrictions.length === 0
            ? <RestrictionItem>현재 적용 중인 기능 제한이 없습니다.</RestrictionItem>
            : detail.restrictions.map((restriction) => (
                <RestrictionItem key={restriction.scope}>
                  {restriction.scopeLabel} · {restriction.untilLabel}
                </RestrictionItem>
              ))}
        </RestrictionList>
        <ManagementForm onSubmit={onSubmit}>
          <Control aria-label="회원 상태 처리" value={action} onChange={(event) => onActionChange(event.target.value)}>
            <option value="이용 제한">이용 제한</option>
            <option value="제한 해제">제한 해제</option>
          </Control>
          <Control
            aria-label="이용 제한 영역"
            value={restrictionScope}
            onChange={(event) => onRestrictionScopeChange(event.target.value)}
            disabled={submitting}
          >
            <option value="CONTENT">게시글·댓글</option>
            <option value="CHAT">채팅</option>
            <option value="REPORT">신고</option>
          </Control>
          {actionIsRestrict && (
            <Control
              aria-label="이용 제한 기간"
              value={restrictionType}
              onChange={(event) => onRestrictionTypeChange(event.target.value)}
              disabled={submitting}
            >
              <option value="7_DAYS">7일</option>
              <option value="30_DAYS">30일</option>
              <option value="PERMANENT">영구</option>
            </Control>
          )}
          <ReasonInput
            value={reason}
            onChange={(event) => onReasonChange(event.target.value)}
            placeholder="처리 사유를 입력해주세요"
            maxLength={500}
            required
            disabled={submitting}
          />
          <ListButton type="submit" $variant={actionIsRestrict ? 'danger' : 'primary'} disabled={submitting}>
            {submitting ? '처리 중...' : action}
          </ListButton>
        </ManagementForm>
        {errorMessage && <ResultMessage $error role="alert">{errorMessage}</ResultMessage>}
        {message && <ResultMessage role="status">{message}</ResultMessage>}
      </ManagementCard>

      <BackActions>
        <ListButton type="button" $variant="secondary" onClick={onBack}>
          목록으로
        </ListButton>
      </BackActions>
    </>
  );
}

export default AdminMemberDetail;
