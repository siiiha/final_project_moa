import { FiCalendar, FiChevronRight, FiMapPin, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';
import SmallGroupMenuTabs, { getSmallGroupMenuItems } from './SmallGroupMenuTabs';
import SmallGroupSettingsForm from './SmallGroupSettingsForm';
import SmallGroupJoinRequestsPanel from './SmallGroupJoinRequestsPanel';

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

const NoticeButton = styled(Button)`
  min-height: 28px;
  padding: 5px 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 10px;
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

const JoinRequestsContent = styled.div`
  margin-top: 16px;
`;

function formatDate(value) {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric',
  }).format(date);
}

function SmallGroupDetail({
  group,
  myRole = null,
  activeMenu,
  settings,
  settingsCategories,
  isSettingsLoading,
  isSettingsSaving,
  settingsErrorMessage,
  onMenuChange,
  onSettingsSubmit,
  onSettingsCancel,
  onRetrySettings,
  onDelete,
  onJoinRequest,
  isJoinSubmitting,
}) {
  const allowedMenuItems = getSmallGroupMenuItems(myRole);
  // AI작성: 이미 승인된 OWNER·MEMBER는 재신청할 수 없으므로 상세 화면에서 버튼을 노출하지 않는다.
  const canRequestJoin = !['OWNER', 'MEMBER'].includes(myRole);
  // AI작성: 역할 변경 또는 URL 전환 후 권한 밖 탭이 남아도 공개 기본 탭으로 되돌린다.
  const currentMenu = allowedMenuItems.some((item) => item.id === activeMenu) ? activeMenu : 'notice';
  const activeLabel = {
    directions: '길찾기',
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
          {canRequestJoin && (
            <JoinButton
              type="button"
              onClick={onJoinRequest}
              disabled={isJoinSubmitting}
              aria-busy={isJoinSubmitting}
            >
              {isJoinSubmitting ? '가입 신청 중...' : '가입 신청하기'}
            </JoinButton>
          )}
        </ProfileTop>
        <SmallGroupMenuTabs myRole={myRole} activeMenu={currentMenu} onMenuChange={onMenuChange} />
      </Profile>

      {currentMenu === 'settings' ? (
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
          <SmallGroupJoinRequestsPanel groupName={group.name} />
        </JoinRequestsContent>
      ) : currentMenu !== 'notice' ? (
        <Placeholder>{activeLabel} 기능은 준비 중입니다.</Placeholder>
      ) : (
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
              <CardHeading>
                <CardTitle>최근 공지</CardTitle>
                {/* AI작성: 공지 목록·상세 API 구현 전에는 이동을 연결하지 않는다. */}
                <NoticeButton type="button" $variant="secondary" disabled title="공지 목록 기능은 준비 중입니다.">
                  공지 전체 보기
                </NoticeButton>
              </CardHeading>
              <NoticeTable aria-label="최근 공지 목록">
                <NoticeRow $header>
                  <span>제목</span><span>작성자</span><span>작성일</span>
                </NoticeRow>
                {group.notices.length === 0 ? (
                  <NoticeEmpty>등록된 공지가 없습니다.</NoticeEmpty>
                ) : group.notices.map((notice, index) => (
                  <NoticeRow key={notice.noticeId} $last={index === group.notices.length - 1}>
                    <span>{notice.title}</span>
                    <span>{notice.writerNickname}</span>
                    <span>{formatDate(notice.createdAt)}</span>
                  </NoticeRow>
                ))}
              </NoticeTable>
            </Card>
          </Column>
        </ContentGrid>
      )}
    </>
  );
}

export default SmallGroupDetail;
