import { FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import SmallGroupMenuTabs, { getSmallGroupMenuItems } from './SmallGroupMenuTabs';
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

const DetailList = styled.dl`
  margin-top: 12px;
  display: grid;
  gap: 9px;
`;

const DetailRow = styled.div`
  padding-bottom: 9px;
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 8px;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};

  &:last-child {
    padding-bottom: 0;
    border-bottom: 0;
  }

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
  }

  dd {
    margin: 0;
    overflow: hidden;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
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
  detail,
  myRole = null,
  activeMenu,
  interests,
  onMenuChange,
  onSettingsSubmit,
  onSettingsCancel,
  onDelete,
}) {
  const allowedMenuItems = getSmallGroupMenuItems(myRole);
  // AI작성: 역할 변경 또는 URL 전환 후 권한 밖 탭이 남아도 공개 기본 탭으로 되돌린다.
  const currentMenu = allowedMenuItems.some((item) => item.id === activeMenu) ? activeMenu : 'notice';
  const activeLabel = {
    directions: '길찾기',
    poll: '투표',
    review: '후기',
    settings: '설정',
    joinRequests: '신청 목록',
  }[currentMenu];
  const detailItems = [
    ['개설자', group.creatorNickname],
    ['생성일', formatDate(group.createdAt)],
    ['모임 날짜', formatDate(group.meetDate)],
    ['장소', group.location],
  ].filter(([, value]) => value);

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
            </Meta>
          </TitleBlock>
        </ProfileTop>
        <SmallGroupMenuTabs myRole={myRole} activeMenu={currentMenu} onMenuChange={onMenuChange} />
      </Profile>

      {currentMenu === 'settings' ? (
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
          </Column>

          <Column>
            {detailItems.length > 0 && (
              <Card>
                <CardTitle>모임 정보</CardTitle>
                <DetailList>
                  {detailItems.map(([label, value]) => (
                    <DetailRow key={label}>
                      <dt>{label}</dt><dd>{value}</dd>
                    </DetailRow>
                  ))}
                </DetailList>
              </Card>
            )}
          </Column>
        </ContentGrid>
      )}
    </>
  );
}

export default SmallGroupDetail;
