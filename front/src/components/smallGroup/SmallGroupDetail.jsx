import { FiCalendar, FiChevronRight, FiMapPin, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import SmallGroupMenuTabs from './SmallGroupMenuTabs';
import SmallGroupSettingsForm from './SmallGroupSettingsForm';
import SmallGroupJoinRequestsPanel from './SmallGroupJoinRequestsPanel';

const Profile = styled.section`
  padding: 6px 0 0;
`;

const ProfileTop = styled.div`
  display: flex;
  align-items: center;
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
  min-width: 0;
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

const StatusBadge = styled(Badge)`
  min-height: 20px;
  padding: 2px 7px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 10px;
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

  strong {
    display: block;
    overflow: hidden;
    color: ${({ theme }) => theme.colors.text};
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span {
    margin-top: 4px;
    display: block;
    overflow: hidden;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
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
  grid-template-columns: minmax(0, 1fr) 66px 42px;
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

function SmallGroupDetail({
  group,
  detail,
  activeMenu,
  interests,
  onMenuChange,
  onSettingsSubmit,
  onSettingsCancel,
  onDelete,
}) {
  const activeLabel = {
    directions: '길찾기',
    poll: '투표',
    review: '후기',
    settings: '설정',
    joinRequests: '신청 목록',
  }[activeMenu];

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
              <StatusBadge>{detail.status}</StatusBadge>
            </Meta>
          </TitleBlock>
        </ProfileTop>
        <SmallGroupMenuTabs activeMenu={activeMenu} onMenuChange={onMenuChange} />
      </Profile>

      {activeMenu === 'settings' ? (
        <SettingsContent>
          <SmallGroupSettingsForm
            group={group}
            detail={detail}
            interests={interests}
            onSubmit={onSettingsSubmit}
            onCancel={onSettingsCancel}
            onDelete={onDelete}
          />
        </SettingsContent>
      ) : activeMenu === 'joinRequests' ? (
        <JoinRequestsContent>
          <SmallGroupJoinRequestsPanel groupName={group.name} />
        </JoinRequestsContent>
      ) : activeMenu !== 'notice' ? (
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
                <Place><FiMapPin size={14} aria-hidden="true" /> {detail.meetingPlace}</Place>
                <MapButton type="button" $variant="secondary" onClick={() => {}}>
                  선택 장소 길찾기 <FiChevronRight size={12} aria-hidden="true" />
                </MapButton>
              </PlaceContent>
            </Card>
          </Column>

          <Column>
            <Card>
              <CardHeading>
                <CardTitle>다음 모임 일정</CardTitle>
                <TextButton type="button" onClick={() => {}}>일정 확인</TextButton>
              </CardHeading>
              <ScheduleRow>
                <CalendarIcon><FiCalendar size={17} aria-hidden="true" /></CalendarIcon>
                <ScheduleText>
                  <strong>{detail.nextMeeting}</strong>
                  <span>{detail.meetingPlace}</span>
                </ScheduleText>
              </ScheduleRow>
            </Card>
            <Card>
              <CardHeading>
                <CardTitle>최근 공지</CardTitle>
                <NoticeButton type="button" $variant="secondary">공지 전체 보기</NoticeButton>
              </CardHeading>
              <NoticeTable aria-label="최근 공지 목록">
                <NoticeRow $header>
                  <span>제목</span><span>작성자</span><span>작성일</span>
                </NoticeRow>
                {detail.notices.map((notice, index) => (
                  <NoticeRow key={notice.id} $last={index === detail.notices.length - 1}>
                    <span>{notice.title}</span><span>{notice.author}</span><span>{notice.date}</span>
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
