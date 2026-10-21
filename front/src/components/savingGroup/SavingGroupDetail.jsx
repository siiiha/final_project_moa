import { FiBell, FiCalendar, FiMapPin, FiTrendingUp, FiUser, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import SavingGroupMenuTabs from './SavingGroupMenuTabs';
import SavingGroupJoinRequestsPanel from './SavingGroupJoinRequestsPanel';
import SavingGroupMembersPanel from './SavingGroupMembersPanel';
import SavingGroupPayoutRequestPanel from './SavingGroupPayoutRequestPanel';
import SavingGroupPaymentStatusPanel from './SavingGroupPaymentStatusPanel';
import SavingGroupTransactionHistoryPanel from './SavingGroupTransactionHistoryPanel';
import SavingGroupDashboardPanel from './SavingGroupDashboardPanel';

const HeaderCard = styled.section`
  padding: 22px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const HeaderTop = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const TitleLine = styled.div`
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 9px;
`;

const Title = styled.h1`
  color: ${({ theme }) => theme.colors.text};
  font-size: 25px;
  font-weight: 800;
  letter-spacing: -1.25px;
`;

const StatusBadge = styled(Badge)`
  color: ${({ $ongoing, theme }) => ($ongoing ? theme.colors.secondaryText : theme.colors.primary)};
  background: ${({ $ongoing, theme }) => ($ongoing ? theme.colors.background : theme.colors.primarySoft)};
`;

const JoinButton = styled(Button)`
  min-height: 38px;
  padding: 8px 14px;
  flex: 0 0 auto;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const Description = styled.p`
  margin-top: 9px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 13px;
  line-height: 1.65;
`;

const MetaList = styled.div`
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 9px 18px;
`;

const Meta = styled.p`
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Layout = styled.div`
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(260px, 0.8fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const JoinRequestsContent = styled.div`
  margin-top: 16px;
`;

const MembersContent = styled.div`
  margin-top: 22px;
`;

const MainColumn = styled.div`
  display: grid;
  gap: 16px;
`;

const SideColumn = styled.aside`
  display: grid;
  align-content: start;
  gap: 16px;
`;

const Card = styled.section`
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const CardTitle = styled.h2`
  display: flex;
  align-items: center;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const ImagePlaceholder = styled.div`
  height: 168px;
  margin-top: 14px;
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    height: 132px;
  }
`;

const Content = styled.p`
  margin-top: 14px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.75;
`;

const Notice = styled.div`
  margin-top: 14px;
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};

  strong {
    color: ${({ theme }) => theme.colors.text};
    font-size: 13px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    line-height: 1.65;
  }
`;

const Schedule = styled.div`
  margin-top: 14px;
  padding: 14px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};

  strong {
    display: block;
    color: ${({ theme }) => theme.colors.text};
    font-size: 13px;
  }

  span {
    margin-top: 4px;
    display: block;
    font-size: 12px;
    line-height: 1.6;
  }
`;

const Dues = styled.strong`
  margin-top: 14px;
  display: block;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 22px;
  letter-spacing: -0.9px;
`;

const DetailList = styled.dl`
  margin-top: 16px;
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr);
  gap: 10px 12px;
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

const ActionButton = styled(Button)`
  width: 100%;
  margin-top: 16px;
  min-height: 38px;
  font-size: 12px;
`;

function SavingGroupDetail({
  group,
  detail,
  activeMenu,
  onMenuChange,
  onJoinRequest,
  onBack,
  canAccessSettings = false,
}) {
  const isOngoing = group.status === '진행중';
  const showSettingsMenu = canAccessSettings || group.canAccessSettings === true;

  return (
    <>
      <HeaderCard>
        <HeaderTop>
          <TitleLine>
            <Title>{group.name}</Title>
            <StatusBadge $ongoing={isOngoing}>{group.status}</StatusBadge>
          </TitleLine>
          <JoinButton type="button" onClick={onJoinRequest}>
            가입 신청하기
          </JoinButton>
        </HeaderTop>
        <Description>{group.description}</Description>
        <MetaList>
          <Meta>
            <FiMapPin size={13} aria-hidden="true" /> {group.district}
          </Meta>
          <Meta>
            <FiUsers size={13} aria-hidden="true" /> {group.members}
          </Meta>
          <Meta>
            <FiUser size={13} aria-hidden="true" /> 모임장 {detail.leader}
          </Meta>
        </MetaList>
        <SavingGroupMenuTabs
          activeMenu={activeMenu}
          onMenuChange={onMenuChange}
          canAccessSettings={showSettingsMenu}
          settingsPath={`/saving-groups/${group.id}/settings`}
        />
      </HeaderCard>

      {activeMenu === 'members' ? (
        <MembersContent>
          <SavingGroupMembersPanel />
        </MembersContent>
      ) : activeMenu === 'payment' ? (
        <SavingGroupPaymentStatusPanel />
      ) : activeMenu === 'dues' ? (
        <SavingGroupDashboardPanel onViewTransactions={() => onMenuChange('transactionHistory')} />
      ) : activeMenu === 'transactionHistory' ? (
        <SavingGroupTransactionHistoryPanel />
      ) : activeMenu === 'payoutRequest' ? (
        <SavingGroupPayoutRequestPanel groupType={group.groupType} />
      ) : activeMenu === 'joinRequests' ? (
        <JoinRequestsContent>
          <SavingGroupJoinRequestsPanel />
        </JoinRequestsContent>
      ) : (
        <Layout>
          <MainColumn>
            <Card>
              <CardTitle>모임 소개</CardTitle>
              <ImagePlaceholder role="img" aria-label={`${group.name} 모임 사진 영역`} />
              <Content>{detail.goal}</Content>
            </Card>
            <Card>
              <CardTitle>
                <FiBell size={16} aria-hidden="true" /> 최근 공지
              </CardTitle>
              <Notice>
                <strong>{detail.noticeTitle}</strong>
                <p>{detail.noticeContent}</p>
              </Notice>
            </Card>
          </MainColumn>

          <SideColumn>
            <Card>
              <CardTitle>
                <FiTrendingUp size={16} aria-hidden="true" /> 회비 현황
              </CardTitle>
              <Dues>{group.monthlyDues}</Dues>
              <Content>다음 납부일 {group.nextDate}</Content>
            </Card>
            <Card>
              <CardTitle>
                <FiCalendar size={16} aria-hidden="true" /> 다음 모임
              </CardTitle>
              <Schedule>
                <strong>{detail.nextMeeting}</strong>
                <span>{detail.meetingPlace}</span>
              </Schedule>
            </Card>
            <Card>
              <CardTitle>모임 정보</CardTitle>
              <DetailList>
                <dt>개설일</dt>
                <dd>{detail.createdAt}</dd>
                <dt>모임장</dt>
                <dd>{detail.leader}</dd>
                <dt>최근 활동</dt>
                <dd>{detail.recentActivity}</dd>
              </DetailList>
              <ActionButton type="button" $variant="secondary" onClick={onBack}>
                계모임 목록으로
              </ActionButton>
            </Card>
          </SideColumn>
        </Layout>
      )}
    </>
  );
}

export default SavingGroupDetail;
