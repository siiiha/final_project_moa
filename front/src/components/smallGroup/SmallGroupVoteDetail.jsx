import {
  FiArrowLeft, FiCalendar, FiCheckCircle, FiClock, FiEyeOff, FiList, FiLock, FiUsers,
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

const VoteLayout = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.85fr);
  gap: 20px;

  @media (max-width: 760px) {
    grid-template-columns: 1fr;
  }
`;

const VotePanel = styled.div`
  min-width: 0;
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

const SectionDescription = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const ChoiceList = styled.div`
  margin-top: 12px;
  display: grid;
  gap: 10px;
`;

const Choice = styled.label`
  min-height: 62px;
  padding: 12px 14px;
  display: flex;
  align-items: center;
  gap: 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $selected, theme }) => ($selected ? theme.colors.primarySoft : theme.colors.white)};
  border: 1px solid ${({ $selected, theme }) => ($selected ? theme.colors.primary : theme.colors.border)};
  border-radius: ${({ theme }) => theme.radius.sm};
  cursor: ${({ $disabled }) => ($disabled ? 'default' : 'pointer')};

  &:focus-within {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }
`;

const ChoiceInput = styled.input`
  width: 18px;
  height: 18px;
  margin: 0;
  accent-color: ${({ theme }) => theme.colors.primary};
  flex: 0 0 auto;
`;

const ChoiceContent = styled.span`
  font-size: 14px;
  font-weight: 700;
`;

const ActionRow = styled.div`
  margin-top: 18px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
`;

const DisabledButton = styled(Button)`
  opacity: 0.52;
  cursor: not-allowed;
`;

const ResultPanel = styled.aside`
  min-height: 100%;
  padding: 18px;
  background: ${({ theme }) => theme.colors.background};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const ResultTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
`;

const ParticipationSummary = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const ResultList = styled.ol`
  margin-top: 14px;
  display: grid;
  gap: 9px;
  list-style: none;
  counter-reset: result-option;
`;

const ResultOption = styled.li`
  padding: 12px;
  display: grid;
  gap: 8px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  counter-increment: result-option;

  &::before {
    content: counter(result-option);
    width: 22px;
    height: 22px;
    display: grid;
    place-items: center;
    color: ${({ theme }) => theme.colors.secondaryText};
    background: ${({ theme }) => theme.colors.background};
    border-radius: 50%;
    font-size: 11px;
    font-weight: 800;
  }
`;

const ResultOptionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
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

const LockedResult = styled.div`
  min-height: 205px;
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  text-align: center;

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 14px;
  }

  p {
    max-width: 210px;
    font-size: 12px;
    line-height: 1.55;
  }
`;

function formatDateTime(value) {
  if (!value) return '마감일 미정';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
  }).format(date);
}

// AI작성: 참여 API가 연결되기 전에는 선택 상태와 결과 패널 표시만 제어하며 서버 데이터를 변경하지 않는다.
function SmallGroupVoteDetail({
  vote, onBack, selectedOptionIds, onSelectOption, isResultOpen, onShowResults,
}) {
  const isClosed = vote.status === 'CLOSED';
  const selectedOptionIdSet = new Set(selectedOptionIds);
  const canShowVoterNicknames = !vote.anonymous && vote.myParticipation.participated;
  const choiceDisabled = isClosed || vote.myParticipation.participated;

  return (
    <Card>
      <BackButton type="button" $variant="secondary" $size="small" onClick={onBack}>
        <FiArrowLeft size={14} aria-hidden="true" /> 투표 목록으로
      </BackButton>
      <VoteLayout>
        <VotePanel>
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

          <SectionTitle>투표할 항목을 선택해주세요.</SectionTitle>
          <SectionDescription>{vote.multipleChoice ? '복수 선택할 수 있습니다.' : '한 개의 항목만 선택할 수 있습니다.'}</SectionDescription>
          <ChoiceList aria-label="투표 선택 항목">
            {vote.options.map((option) => {
              const selected = selectedOptionIdSet.has(option.smallGroupVoteOptionId);
              return (
                <Choice key={option.smallGroupVoteOptionId} $selected={selected} $disabled={choiceDisabled}>
                  <ChoiceInput
                    type={vote.multipleChoice ? 'checkbox' : 'radio'}
                    name={`small-group-vote-${vote.smallGroupVoteId}`}
                    checked={selected}
                    disabled={choiceDisabled}
                    onChange={() => onSelectOption(option.smallGroupVoteOptionId)}
                  />
                  <ChoiceContent>{option.content}</ChoiceContent>
                </Choice>
              );
            })}
          </ChoiceList>
          <ActionRow>
            <DisabledButton type="button" disabled title="투표 참여 기능은 준비 중입니다.">
              투표하기
            </DisabledButton>
            <Button
              type="button"
              $variant="secondary"
              disabled={!vote.myParticipation.participated}
              onClick={onShowResults}
            >
              결과 보기
            </Button>
          </ActionRow>
          {!isClosed && <Notice>투표 참여 기능은 준비 중입니다.</Notice>}
        </VotePanel>

        <ResultPanel aria-live="polite">
          {isResultOpen ? (
            <>
              <ResultTitle>투표 결과</ResultTitle>
              <ParticipationSummary>총 {vote.participantCount}명이 참여했습니다.</ParticipationSummary>
              <ResultList aria-label="투표 항목별 결과">
                {vote.options.map((option) => {
                  const selected = selectedOptionIdSet.has(option.smallGroupVoteOptionId);
                  return (
                    <ResultOption key={option.smallGroupVoteOptionId}>
                      <ResultOptionHeader>
                        <ChoiceContent>{option.content}</ChoiceContent>
                        <VoteCount>{option.voteCount}표</VoteCount>
                      </ResultOptionHeader>
                      {selected && <Selected><FiCheckCircle size={13} aria-hidden="true" /> 내가 선택함</Selected>}
                      {canShowVoterNicknames && option.voterNicknames.length > 0 && (
                        <VoterNames>참여자: {option.voterNicknames.join(', ')}</VoterNames>
                      )}
                    </ResultOption>
                  );
                })}
              </ResultList>
              {isClosed && <Notice>마감된 투표 결과는 마감 후 24시간 동안만 확인할 수 있습니다.</Notice>}
            </>
          ) : (
            <LockedResult>
              <FiLock size={24} aria-hidden="true" />
              <strong>투표 결과</strong>
              <p>투표에 참여한 뒤 결과 보기 버튼을 눌러 확인할 수 있습니다.</p>
            </LockedResult>
          )}
        </ResultPanel>
      </VoteLayout>
    </Card>
  );
}

export default SmallGroupVoteDetail;
