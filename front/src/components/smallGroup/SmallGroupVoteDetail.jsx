import {
  FiArrowLeft, FiCalendar, FiCheckCircle, FiClock, FiEyeOff, FiList, FiUsers,
} from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  padding: 20px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const BackButton = styled(Button)`
  margin-bottom: 18px;
`;

const Header = styled.header`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
`;

const Title = styled.h1`
  color: ${({ theme }) => theme.colors.text};
  font-size: 22px;
  font-weight: 800;
  letter-spacing: -1px;
`;

const Status = styled.span`
  min-height: 25px;
  padding: 4px 9px;
  display: inline-flex;
  align-items: center;
  color: ${({ $closed, theme }) => ($closed ? theme.colors.secondaryText : theme.colors.primary)};
  background: ${({ $closed, theme }) => ($closed ? theme.colors.background : theme.colors.primarySoft)};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
`;

const Meta = styled.div`
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 7px 13px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;

  span {
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
`;

const Notice = styled.p`
  margin-top: 18px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  line-height: 1.55;
`;

const SectionTitle = styled.h2`
  margin-top: 24px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const OptionList = styled.ol`
  margin-top: 12px;
  display: grid;
  gap: 10px;
  list-style: none;
  counter-reset: vote-option;
`;

const Option = styled.li`
  padding: 13px;
  position: relative;
  display: grid;
  gap: 9px;
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primarySoft : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  counter-increment: vote-option;

  &::before {
    content: counter(vote-option);
    width: 22px;
    height: 22px;
    display: grid;
    place-items: center;
    color: ${({ $selected, theme }) => ($selected ? theme.colors.white : theme.colors.secondaryText)};
    background: ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.background)};
    border-radius: 50%;
    font-size: 11px;
    font-weight: 800;
  }
`;

const OptionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const OptionContent = styled.strong`
  flex: 1;
  color: ${({ theme }) => theme.colors.text};
  font-size: 14px;
`;

const VoteCount = styled.span`
  color: ${({ theme }) => theme.colors.primary};
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
`;

const Selected = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 11px;
  font-weight: 800;
`;

const VoterNames = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.55;
`;

const ParticipationSummary = styled.p`
  margin-top: 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

function formatDateTime(value) {
  if (!value) return '마감일 미정';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date);
}

// AI작성: 상세 조회 응답만 표시하며 실제 참여·수정 요청은 별도 기능이 구현될 때 연결한다.
function SmallGroupVoteDetail({ vote, onBack }) {
  const isClosed = vote.status === 'CLOSED';
  const selectedOptionIds = new Set(vote.myParticipation.selectedOptionIds);
  const canShowVoterNicknames = !vote.anonymous && vote.myParticipation.participated;

  return (
    <Card>
      <BackButton type="button" $variant="secondary" $size="small" onClick={onBack}>
        <FiArrowLeft size={14} aria-hidden="true" /> 투표 목록으로
      </BackButton>
      <Header>
        <Title>{vote.title}</Title>
        <Status $closed={isClosed}>{isClosed ? '마감' : '진행 중'}</Status>
      </Header>
      <Meta>
        <span><FiUsers size={13} aria-hidden="true" /> 작성자 {vote.creatorNickname}</span>
        <span><FiCalendar size={13} aria-hidden="true" /> 생성 {formatDateTime(vote.createdAt)}</span>
        <span><FiClock size={13} aria-hidden="true" /> 마감 {formatDateTime(vote.endsAt)}</span>
        <span><FiList size={13} aria-hidden="true" /> {vote.multipleChoice ? '복수 선택' : '단일 선택'}</span>
        {vote.anonymous && <span><FiEyeOff size={13} aria-hidden="true" /> 익명 투표</span>}
      </Meta>

      {vote.anonymous ? (
        <Notice>익명 투표는 참여자와 선택 항목을 공개하지 않으며, 항목별 득표 수만 표시합니다.</Notice>
      ) : !vote.myParticipation.participated ? (
        <Notice>공개 투표의 참여자 닉네임은 투표 참여를 완료한 뒤에 확인할 수 있습니다.</Notice>
      ) : null}

      <SectionTitle>투표 결과</SectionTitle>
      <ParticipationSummary>총 {vote.participantCount}명이 참여했습니다.</ParticipationSummary>
      <OptionList aria-label="투표 보기와 결과">
        {vote.options.map((option) => {
          const selected = selectedOptionIds.has(option.smallGroupVoteOptionId);
          return (
            <Option key={option.smallGroupVoteOptionId} $selected={selected}>
              <OptionHeader>
                <OptionContent>{option.content}</OptionContent>
                <VoteCount>{option.voteCount}표</VoteCount>
              </OptionHeader>
              {selected && <Selected><FiCheckCircle size={13} aria-hidden="true" /> 내가 선택함</Selected>}
              {canShowVoterNicknames && option.voterNicknames.length > 0 && (
                <VoterNames>참여자: {option.voterNicknames.join(', ')}</VoterNames>
              )}
            </Option>
          );
        })}
      </OptionList>

      <Notice>
        {isClosed
          ? '마감된 투표 결과는 마감 후 24시간 동안만 확인할 수 있습니다.'
          : '투표 참여 기능은 준비 중입니다.'}
      </Notice>
    </Card>
  );
}

export default SmallGroupVoteDetail;
