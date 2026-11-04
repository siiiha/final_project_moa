import { getSmallGroupNoticePagination } from '../../api/smallGroups';
import SmallGroupNoticeCreateForm from './SmallGroupNoticeCreateForm';
import { useCallback, useEffect, useId, useState } from 'react';
import { FiCalendar, FiChevronRight, FiMapPin, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';
import GroupReportButton from '../common/GroupReportButton';
import GroupJoinAction, { GroupJoinReason } from '../common/GroupJoinAction';
import SmallGroupMenuTabs, { getSmallGroupMenuItems } from './SmallGroupMenuTabs';
import SmallGroupSettingsForm from './SmallGroupSettingsForm';
import SmallGroupJoinRequestsPanel from './SmallGroupJoinRequestsPanel';
import SmallGroupVoteCreateModal from './SmallGroupVoteCreateModal';
import SmallGroupReviewCreateModal from './SmallGroupReviewCreateModal';
import SmallGroupReviewsPanel from './SmallGroupReviewsPanel';
import SmallGroupReviewWriteAction from './SmallGroupReviewWriteAction';
import SmallGroupPlacesPanel from './SmallGroupPlacesPanel';
import useAuthStore from '../../store/authStore';

const Profile = styled.section`
  padding: 6px 0 0;
`;

const ProfileTop = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const ProfileImage = styled.div`
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 35% 32%, #f6dfbf 0 14%, transparent 14.5%),
    radial-gradient(circle at 65% 70%, #8ea985 0 24%, transparent 24.5%),
    linear-gradient(135deg, #789779 0%, #d8e4d2 54%, #577962 100%);
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const TitleBlock = styled.div`
  flex: 1 1 auto;
  min-width: 0;
`;

const JoinButton = styled(Button)`
  min-height: 38px;
  padding: 8px 14px;
  flex: 0 0 auto;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    min-height: 34px;
    padding: 7px 10px;
    font-size: 12px;
  }
`;

const ProfileActions = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const Title = styled.h1`
  overflow: hidden;
  color: ${({ theme }) => theme.colors.text};
  font-size: 23px;
  font-weight: 800;
  letter-spacing: -1.2px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Meta = styled.p`
  margin-top: 5px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 5px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const ContentGrid = styled.div`
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(290px, 0.85fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Column = styled.div`
  display: grid;
  align-content: start;
  gap: 12px;
`;

const Card = styled.section`
  padding: 16px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const CardHeading = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const CardTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const IntroBody = styled.div`
  margin-top: 13px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 136px;
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const IntroText = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.7;
`;

const ActivityImage = styled.div`
  min-height: 116px;
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(ellipse at 70% 32%, rgba(255, 255, 255, 0.72) 0 17%, transparent 17.5%),
    linear-gradient(155deg, #96b8c4 0 42%, #dbe7d1 42% 58%, #668e5c 58% 100%);
  border-radius: ${({ theme }) => theme.radius.sm};

  &::after {
    width: 70%;
    height: 34%;
    position: absolute;
    right: 0;
    bottom: -8%;
    left: 15%;
    content: '';
    background: rgba(54, 86, 57, 0.52);
    border-radius: 50% 50% 0 0;
  }
`;

const PlaceContent = styled.div`
  margin-top: 13px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
`;

const Place = styled.p`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const MapButton = styled(Button)`
  min-height: 31px;
  padding: 6px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const ScheduleRow = styled.div`
  margin-top: 13px;
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) auto;
  align-items: center;
  gap: 11px;
`;

const CalendarIcon = styled.div`
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const ScheduleText = styled.div`
  min-width: 0;

  strong,
  span {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 12px;
  }

  span {
    margin-top: 4px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
  }
`;

const TextButton = styled.button`
  padding: 0;
  color: ${({ theme }) => theme.colors.primary};
  background: transparent;
  border: 0;
  font-size: 11px;
  font-weight: 700;
`;

const NoticeTable = styled.div`
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const NoticeRow = styled.div`
  padding: 9px 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 66px 72px;
  gap: 8px;
  color: ${({ $header, theme }) => ($header ? theme.colors.secondaryText : theme.colors.text)};
  background: ${({ $header, theme }) => ($header ? theme.colors.background : theme.colors.white)};
  border-bottom: ${({ $last, theme }) => ($last ? 0 : `1px solid ${theme.colors.border}`)};
  font-size: 10px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
`;

const NoticeEmpty = styled.p`
  padding: 16px 10px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  text-align: center;
`;

const Placeholder = styled.section`
  margin-top: 16px;
  padding: 46px 20px;
  color: ${({ theme }) => theme.colors.muted};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  font-size: 13px;
  text-align: center;
`;

const SettingsContent = styled.div`
  margin-top: 16px;
`;

// AI작성: 안내와 후기 목록을 같은 카드에 배치하고 본문은 왼쪽 정렬로 읽기 쉽게 표시한다.
const ReviewCard = styled(Placeholder)`
  padding: 24px 20px;
  color: ${({ theme }) => theme.colors.text};
  text-align: left;
`;

const ReviewNotice = styled.p`
  margin: 0 0 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 16px;
  line-height: 1.6;
  text-align: center;
`;

const JoinRequestsContent = styled.div`
  margin-top: 16px;
`;

const PollContent = styled.section`
  margin-top: 16px;
  position: relative;
`;

const PollHeading = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
`;

const PollTitle = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const VoteCreateButton = styled(Button)`
  min-height: 38px;
  padding: 8px 12px;
  font-size: 12px;
`;

const VoteTable = styled.div`
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const VoteRow = styled.div`
  padding: 11px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 74px 160px;
  gap: 8px;
  color: ${({ $header, theme }) => ($header ? theme.colors.secondaryText : theme.colors.text)};
  background: ${({ $header, theme }) => ($header ? theme.colors.background : theme.colors.white)};
  border-bottom: ${({ $last, theme }) => ($last ? 0 : `1px solid ${theme.colors.border}`)};
  font-size: 11px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: minmax(0, 1fr) 66px 132px;
    padding: 10px 8px;
    gap: 4px;
    font-size: 10px;
  }
`;

const VoteItem = styled.button`
  width: 100%;
  padding: 11px 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 74px 160px;
  gap: 8px;
  color: ${({ theme }) => theme.colors.text};
  text-align: left;
  background: ${({ theme }) => theme.colors.white};
  border: 0;
  border-bottom: ${({ $last, theme }) => ($last ? 0 : `1px solid ${theme.colors.border}`)};
  cursor: pointer;
  font-size: 11px;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
  }

  &:focus-visible {
    outline: 3px solid rgba(23, 107, 91, 0.22);
    outline-offset: -3px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: minmax(0, 1fr) 66px 132px;
    padding: 10px 8px;
    gap: 4px;
    font-size: 10px;
  }
`;

const VoteStatusCell = styled.div`
  min-width: 0;
`;

const VoteStatusBadge = styled.span`
  padding: 4px 7px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: ${({ $closed, theme }) => ($closed ? theme.colors.muted : theme.colors.primary)};
  background: ${({ $closed, theme }) => ($closed ? theme.colors.background : theme.colors.primarySoft)};
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  white-space: nowrap;
`;

function formatDate(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric',
  }).format(date);
}

// AI작성: 투표 목록의 마감 시각은 날짜뿐 아니라 시·분까지 표시해 마감 여부를 명확히 한다.
function formatDateTime(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric', hour: 'numeric', minute: '2-digit',
  }).format(date);
}

function SmallGroupDetail({
  group,
  groupId,
  smallGroupId,
  myRole = null,
  activeMenu,
  settings,
  settingsCategories,
  isSettingsLoading,
  isSettingsSaving,
  settingsErrorMessage,
  votes = [],
  isVoteLoading,
  voteErrorMessage,
  onMenuChange,
  onSettingsSubmit,
  onSettingsCancel,
  onRetrySettings,
  onDelete,
  onJoinRequest,
  isJoinSubmitting,
  onCreateVote,
  isVoteCreating,
  onRetryVotes,
  onVoteDetail,
  onNoticeRefresh,
  onNoticeDetail,
  noticePageMeta,
  noticeSort = 'LATEST',
  onNoticeQueryChange,
}) {
  // AI작성: 기존 버튼과 별도 안내 영역을 접근성 ID로 연결한다.
  const joinReasonId = useId();
  // AI작성: 목록은 계모임과 같은 세 페이지 번호 묶음을 사용한다.
  const noticePagination = getSmallGroupNoticePagination(noticePageMeta?.page || 1, noticePageMeta?.totalPages || 0);
  const allowedMenuItems = getSmallGroupMenuItems(myRole);
  const [isVoteCreateModalOpen, setIsVoteCreateModalOpen] = useState(false);
  // AI작성: 완료 안내는 소모임별로 구분하고 최종 등록 가능 여부는 서버에서 검증한다.
  const [reviewModalGroupId, setReviewModalGroupId] = useState(null);
  const [reviewResult, setReviewResult] = useState(null);
  // AI작성: 로그인 계정이 바뀌면 이전 계정의 완료 상태와 작성 여부를 재사용하지 않는다.
  const authVersion = useAuthStore((state) => state.authVersion);
  // AI작성: 로그아웃 시 이전 역할 정보가 남아도 장소 검색 화면과 위치 상태를 즉시 폐기한다.
  const hasLogin = useAuthStore((state) => Boolean(state.token));
  const reviewCompleted = reviewResult?.smallGroupId === smallGroupId && reviewResult?.authVersion === authVersion;
  // AI작성: 실제 등록 성공 때만 목록을 다시 마운트해 이전 요청을 취소하고 첫 페이지부터 재조회한다.
  const [reviewListVersion, setReviewListVersion] = useState(0);
  const closeReviewModal = useCallback(() => setReviewModalGroupId(null), [setReviewModalGroupId]);
  const [reviewNow, setReviewNow] = useState(() => new Date());
  // AI작성: 페이지를 오래 열어 두어도 모임 다음 날에는 작성 버튼이 활성화되도록 한국 날짜를 갱신한다.
  useEffect(() => {
    const timer = window.setInterval(() => setReviewNow(new Date()), 30_000);
    return () => window.clearInterval(timer);
  }, []);
  const koreaDateParts = new Intl.DateTimeFormat('en-US', {
    timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(reviewNow);
  const datePart = (type) => koreaDateParts.find((part) => part.type === type)?.value;
  const todayInKorea = `${datePart('year')}-${datePart('month')}-${datePart('day')}`;
  const reviewRestriction = myRole !== 'MEMBER'
    ? (myRole === 'OWNER' ? '소모임 개설자는 후기를 작성할 수 없습니다.' : '승인된 일반 모임원만 후기를 작성할 수 있습니다.')
    : !group.meetDate ? '모임 날짜가 설정되지 않아 후기를 작성할 수 없습니다.'
      : group.meetDate >= todayInKorea ? '후기는 모임 다음 날부터 작성할 수 있습니다.' : '';
  // AI작성: 역할 변경 또는 URL 전환 후 권한 밖 탭이 남아도 공개 기본 탭으로 되돌린다.
  const currentMenu = allowedMenuItems.some((item) => item.id === activeMenu) ? activeMenu : 'notice';
  const activeLabel = {
    directions: '장소 찾기',
    poll: '투표',
    review: '후기',
    settings: '설정',
    joinRequests: '신청 목록',
  }[currentMenu];
  const meetDate = formatDate(group.meetDate) || '일정 조율 중';
  const location = group.location || '모임에서 추후 안내';
  const createdAt = formatDate(group.createdAt);

  return (
    <>
      <Profile>
        <ProfileTop>
          <ProfileImage role="img" aria-label={`${group.name} 대표 이미지 영역`} />
          <TitleBlock>
            <Title>{group.name}</Title>
            <Meta>
              <span>{group.region}</span>
              <span>·</span>
              <span>{group.interest}</span>
              <span>·</span>
              <FiUsers size={12} aria-hidden="true" />
              <span>{group.participants}</span>
              {/* AI작성: 상세 카드와 중복되는 개설자·생성일은 모임 기본 정보 옆에서만 표시한다. */}
              {group.creatorNickname && <><span>·</span><span>개설자 {group.creatorNickname}</span></>}
              {createdAt && <><span>·</span><span>생성일 {createdAt}</span></>}
            </Meta>
          </TitleBlock>
          <ProfileActions>
            {/* AI작성: 가입 완료 상태도 표시하며 기존 JoinButton의 크기는 유지한다. */}
            <GroupJoinAction group={group} onJoinRequest={onJoinRequest}
              isSubmitting={isJoinSubmitting} ButtonComponent={JoinButton} reasonId={joinReasonId} />
            {/* AI작성: OWNER의 본인 신고 진입을 숨기고 그 외 사용자는 공통 신고 흐름을 재사용한다. */}
            {myRole !== 'OWNER' && (
              <GroupReportButton groupId={groupId} groupLabel="소모임" />
            )}
          </ProfileActions>
        </ProfileTop>
        <GroupJoinReason group={group} isSubmitting={isJoinSubmitting} id={joinReasonId} />
        <SmallGroupMenuTabs myRole={myRole} activeMenu={currentMenu} onMenuChange={onMenuChange} />
      </Profile>

      {currentMenu === 'poll' ? (
        <PollContent>
          <PollHeading>
            <PollTitle>투표 목록</PollTitle>
            {myRole === 'OWNER' && (
              <VoteCreateButton type="button" onClick={() => setIsVoteCreateModalOpen(true)}>
                투표 생성
              </VoteCreateButton>
            )}
          </PollHeading>
          {isVoteLoading ? (
            <Placeholder role="status">투표 목록을 불러오는 중입니다.</Placeholder>
          ) : voteErrorMessage ? (
            <Placeholder role="alert">
              <p>{voteErrorMessage}</p>
              <Button type="button" onClick={onRetryVotes}>다시 시도</Button>
            </Placeholder>
          ) : votes.length === 0 ? (
            <Placeholder>등록된 투표가 없습니다.</Placeholder>
          ) : (
            <VoteTable aria-label="소모임 투표 목록">
              <VoteRow $header>
                <span>투표명</span><span>상태</span><span>마감시각</span>
              </VoteRow>
              {votes.map((vote, index) => (
                <VoteItem
                  key={vote.smallGroupVoteId}
                  type="button"
                  $last={index === votes.length - 1}
                  onClick={() => onVoteDetail(vote.smallGroupVoteId)}
                  aria-label={`${vote.title} 투표 상세 보기`}
                >
                  <span>{vote.title}</span>
                  <VoteStatusCell>
                    <VoteStatusBadge $closed={vote.status === 'CLOSED'}>
                      {vote.status === 'CLOSED' ? '종료' : '진행 중'}
                    </VoteStatusBadge>
                  </VoteStatusCell>
                  <span>{formatDateTime(vote.endsAt) || '마감시각 미설정'}</span>
                </VoteItem>
              ))}
            </VoteTable>
          )}
          {isVoteCreateModalOpen && (
            <SmallGroupVoteCreateModal
              isSubmitting={isVoteCreating}
              onClose={() => setIsVoteCreateModalOpen(false)}
              onSubmit={async (request) => {
                await onCreateVote(request);
                setIsVoteCreateModalOpen(false);
              }}
            />
          )}
        </PollContent>
      ) : currentMenu === 'review' ? (
        <PollContent>
          {/* AI작성: 공개 목록은 작성 권한과 분리하고 기존 작성 버튼·모달은 승인된 일반 모임원에게만 제공한다. */}
          <PollHeading>
            <PollTitle>소모임 후기</PollTitle>
            {!reviewRestriction && !reviewCompleted && (
              <SmallGroupReviewWriteAction
                key={`${smallGroupId}:${authVersion}`}
                smallGroupId={smallGroupId}
                onWrite={() => setReviewModalGroupId(smallGroupId)}
              />
            )}
          </PollHeading>
          <ReviewCard aria-label="소모임 후기">
            <ReviewNotice role="status">
              {reviewRestriction || (reviewCompleted
                ? reviewResult.message : '별점과 짧은 코멘트로 모임 후기를 남겨 주세요. 후기는 한 번만 작성할 수 있습니다.')}
            </ReviewNotice>
            <SmallGroupReviewsPanel key={`${smallGroupId}:${myRole || 'PUBLIC'}:${reviewListVersion}`} smallGroupId={smallGroupId} />
          </ReviewCard>
          {!reviewRestriction && reviewModalGroupId === smallGroupId && (
            <SmallGroupReviewCreateModal
              key={smallGroupId}
              smallGroupId={smallGroupId}
              onClose={closeReviewModal}
              onRegistered={(message, { created }) => {
                setReviewResult({ smallGroupId, authVersion, message });
                if (created) setReviewListVersion((previous) => previous + 1);
                closeReviewModal();
              }}
            />
          )}
        </PollContent>
      ) : currentMenu === 'directions' ? (
        /* AI작성: 역할·계정·소모임 전환 때 이전 위치·결과·요청을 재사용하지 않는다. */
        hasLogin ? <SmallGroupPlacesPanel key={`${smallGroupId}:${myRole}:${authVersion}`} smallGroupId={smallGroupId} />
          : <Placeholder role="alert">로그인 후 이용해 주세요.</Placeholder>
      ) : currentMenu === 'settings' ? (
        <SettingsContent>
          {isSettingsLoading ? (
            <Placeholder role="status">소모임 설정 정보를 불러오는 중입니다.</Placeholder>
          ) : settingsErrorMessage ? (
            <Placeholder role="alert">
              <p>{settingsErrorMessage}</p>
              <Button type="button" onClick={onRetrySettings}>다시 시도</Button>
            </Placeholder>
          ) : settings ? (
            <SmallGroupSettingsForm
              settings={settings}
              categories={settingsCategories}
              isSaving={isSettingsSaving}
              onSubmit={onSettingsSubmit}
              onCancel={onSettingsCancel}
              onDelete={onDelete}
            />
          ) : null}
        </SettingsContent>
      ) : currentMenu === 'joinRequests' ? (
        <JoinRequestsContent>
          <SmallGroupJoinRequestsPanel smallGroupId={smallGroupId} groupName={group.name} />
        </JoinRequestsContent>
      ) : currentMenu !== 'notice' ? (
        <Placeholder>{activeLabel} 기능은 준비 중입니다.</Placeholder>
      ) : (
        /* AI작성: PC 메인은 왼쪽 소개·공지, 오른쪽 일정·장소 순서로 배치한다. */
        <ContentGrid>
          <Column>
            <Card>
              <CardTitle>모임 소개</CardTitle>
              <IntroBody>
                <IntroText>{group.description}</IntroText>
                <ActivityImage role="img" aria-label={`${group.name} 활동 이미지 영역`} />
              </IntroBody>
            </Card>
            <Card>
              <SmallGroupNoticeCreateForm group={{ ...group, smallGroupId, myRole }} onRefresh={onNoticeRefresh}
                heading={<CardTitle>공지</CardTitle>}>
              {/* AI작성: 전체 보기 화면 없이 메인에서 정렬·페이징한다. */}
              <div role="group" aria-label="공지 정렬" style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                {['LATEST', 'OLDEST'].map((sort) => <Button key={sort} type="button" $variant="secondary"
                  aria-pressed={noticeSort === sort} onClick={() => onNoticeQueryChange(1, sort)}>
                  {sort === 'LATEST' ? '최신순' : '오래된순'}</Button>)}
              </div>
              <NoticeTable aria-label="공지 목록">
                <NoticeRow $header>
                  <span>제목</span><span>작성자</span><span>작성일</span>
                </NoticeRow>
                {group.notices.length === 0 ? (
                  <NoticeEmpty>등록된 공지가 없습니다.</NoticeEmpty>
                ) : group.notices.map((notice, index) => (
                  <NoticeRow key={notice.noticeId} $last={index === group.notices.length - 1}>
                    <button type="button" onClick={() => onNoticeDetail(notice.noticeId)} style={{ background: 'none', border: 0, textAlign: 'left', font: 'inherit', color: 'inherit', cursor: 'pointer', overflowWrap: 'anywhere' }}>{notice.title}</button>
                    <span>{notice.writerNickname}</span>
                    <span>{formatDate(notice.createdAt)}</span>
                  </NoticeRow>
                ))}
              </NoticeTable>
              {noticePageMeta?.totalPages > 0 && <nav aria-label="공지 페이지" style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                {noticePagination.previous && <Button type="button" $variant="secondary" onClick={() => onNoticeQueryChange(noticePagination.previous, noticeSort)}>이전</Button>}
                {noticePagination.pages.map((page) => <Button key={page} type="button" $variant="secondary"
                  aria-current={noticePageMeta.page === page ? 'page' : undefined}
                  onClick={() => onNoticeQueryChange(page, noticeSort)}>{page}</Button>)}
                {noticePagination.next && <Button type="button" $variant="secondary" onClick={() => onNoticeQueryChange(noticePagination.next, noticeSort)}>다음</Button>}
              </nav>}
              </SmallGroupNoticeCreateForm>
            </Card>
          </Column>

          <Column>
            <Card>
              <CardHeading>
                <CardTitle>다음 모임 일정</CardTitle>
                {/* AI작성: 소모임 일정 목록 API가 준비되기 전에는 화면 이동을 연결하지 않는다. */}
                <TextButton type="button" disabled title="일정 목록 기능은 준비 중입니다.">일정 확인</TextButton>
              </CardHeading>
              <ScheduleRow>
                <CalendarIcon><FiCalendar size={17} aria-hidden="true" /></CalendarIcon>
                <ScheduleText>
                  <strong>{meetDate}</strong>
                  <span>{location}</span>
                </ScheduleText>
              </ScheduleRow>
            </Card>
            <Card>
              <CardTitle>장소</CardTitle>
              <PlaceContent>
                <Place><FiMapPin size={14} aria-hidden="true" /> {location}</Place>
                {/* AI작성: 길찾기 API가 확정되기 전에는 이동을 발생시키지 않는다. */}
                <MapButton type="button" $variant="secondary" disabled title="길찾기 기능은 준비 중입니다.">
                  선택 장소 길찾기 <FiChevronRight size={12} aria-hidden="true" />
                </MapButton>
              </PlaceContent>
            </Card>
          </Column>
        </ContentGrid>
      )}
    </>
  );
}

export default SmallGroupDetail;
