import { formatSavingGroupAmount, getSavingGroupMenus } from '../../api/savingGroups.js';
import { FiBell, FiMapPin, FiTrendingUp, FiUsers } from 'react-icons/fi';
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

// AI작성: 공개 상세는 API 응답만 표시하고 범위 밖인 업무 탭에는 Mock 데이터를 섞지 않는다.
function SavingGroupDetail({ group, activeMenu, onMenuChange, onJoinRequest, isJoinSubmitting, onBack }) {
  const allowedMenus = getSavingGroupMenus(group.myRole);
  const currentMenu = allowedMenus.includes(activeMenu) ? activeMenu : 'notice';
  const paymentDay = group.duesDay == null ? '납부일 미정'
    : group.duesDay < 1 || group.duesDay > 28 ? '납부일 확인 필요' : '매월 ' + group.duesDay + '일';
  const statusLabel = { '01': '개설', '02': '진행중' }[group.status] || '상태 확인 필요';

  return (
    <>
      <HeaderCard>
        <HeaderTop>
          <TitleLine>
            <Title>{group.groupName}</Title>
            <StatusBadge $ongoing={group.status === '02'}>{statusLabel}</StatusBadge>
          </TitleLine>
          <JoinButton type="button" onClick={onJoinRequest} disabled={isJoinSubmitting} aria-busy={isJoinSubmitting}>
            {isJoinSubmitting ? '가입 신청 중...' : '가입 신청하기'}
          </JoinButton>
        </HeaderTop>
        <Description>{group.description?.trim() || '소개글이 없습니다.'}</Description>
        <MetaList>
          <Meta><FiMapPin size={16} aria-hidden="true" /> {group.regionName}</Meta>
          <Meta><FiUsers size={16} aria-hidden="true" /> {group.memberCount}명 / {group.maxMembers == null ? '정원 미정' : '정원 ' + group.maxMembers + '명'}</Meta>
        </MetaList>
        <SavingGroupMenuTabs
          activeMenu={activeMenu}
          onMenuChange={onMenuChange}
          canAccessSettings={allowedMenus.includes('settings')}
          settingsPath={`/saving-groups/${group.groupId}/settings`}
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
        // AI작성: 모임 전환 시 이전 조회·필터·상세 상태를 초기화한다. 실제 인가는 서버 연결 시 적용한다.
        <SavingGroupTransactionHistoryPanel key={group.id} groupId={group.id} />
      ) : activeMenu === 'payoutRequest' ? (
        <SavingGroupPayoutRequestPanel groupType={group.groupType} />
      ) : activeMenu === 'joinRequests' ? (
        <JoinRequestsContent>
          {/* AI작성: 화면 모임이 바뀌면 요청·승인 상태를 초기화하고 해당 모임의 실제 신청만 조회한다. */}
          <SavingGroupJoinRequestsPanel key={group.id} savingGroupId={String(group.id)} />
        </JoinRequestsContent>
      ) : (
        <Layout>
          <MainColumn>
            <Card>
              <CardTitle>모임 소개</CardTitle>
              <Content>{group.description?.trim() || '소개글이 없습니다.'}</Content>
            </Card>
            <Card>
              <CardTitle><FiBell size={18} aria-hidden="true" /> 최근 공지</CardTitle>
              {group.notices.length === 0 ? <Content>등록된 공지가 없습니다.</Content>
                : group.notices.map((notice) => (
                  <Notice key={notice.noticeId}><strong>{notice.title}</strong></Notice>
                ))}
            </Card>
          </MainColumn>

          <SideColumn>
            <Card>
              <CardTitle><FiTrendingUp size={18} aria-hidden="true" /> 월 회비</CardTitle>
              <Dues>{formatSavingGroupAmount(group.duesAmount)}</Dues>
              <Content>납부일: {paymentDay}</Content>
            </Card>
            <Card>
              <CardTitle>모임 정보</CardTitle>
              <DetailList>
                <dt>지역</dt><dd>{group.regionName}</dd>
                <dt>계원 수</dt><dd>{group.memberCount}명</dd>
                <dt>정원</dt><dd>{group.maxMembers == null ? '미정' : group.maxMembers + '명'}</dd>
                <dt>상태</dt><dd>{statusLabel}</dd>
              </DetailList>
              <ActionButton type="button" $variant="secondary" onClick={onBack}>계모임 목록으로</ActionButton>
            </Card>
          </SideColumn>
        </Layout>
      )}
    </>
  );
}

export default SavingGroupDetail;
