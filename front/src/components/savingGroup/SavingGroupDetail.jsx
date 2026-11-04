import { useId } from 'react';
import { Link } from 'react-router-dom';
import { formatSavingGroupAmount, getSavingGroupMenus } from '../../api/savingGroups.js';
import { FiBell, FiMapPin, FiTrendingUp, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import GroupReportButton from '../common/GroupReportButton';
import GroupJoinAction, { GroupJoinReason } from '../common/GroupJoinAction';
import SavingGroupMenuTabs from './SavingGroupMenuTabs';
import SavingGroupJoinRequestsPanel from './SavingGroupJoinRequestsPanel';
import SavingGroupMembersPanel from './SavingGroupMembersPanel';
import SavingGroupPayoutRequestPanel from './SavingGroupPayoutRequestPanel';
import SavingGroupPaymentStatusPanel from './SavingGroupPaymentStatusPanel';
import SavingGroupTransactionHistoryPanel from './SavingGroupTransactionHistoryPanel';
import SavingGroupDashboardPanel from './SavingGroupDashboardPanel';
import SavingGroupNoticeCreateForm from './SavingGroupNoticeCreateForm';

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

const HeaderActions = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
    align-items: stretch;
    flex-direction: column;
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

// AI작성: 소모임과 동일한 제목·작성자·작성일 열을 표시하되 긴 내용과 좁은 화면에서도 읽을 수 있게 한다.
const NoticeTable = styled.div`
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;
// AI작성: 승인 회원에게만 제공하는 공지 상세 링크는 키보드 초점과 긴 제목 줄바꿈을 지원한다.
const NoticeLink = styled(Link)`
  color: ${({ theme }) => theme.colors.primary};
  text-decoration: underline;
  overflow-wrap: anywhere;
  &:focus-visible { outline: 2px solid currentColor; outline-offset: 3px; }
`;
const NoticeRow = styled.div`
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(64px, 0.4fr) minmax(110px, 0.6fr);
  gap: 12px;
  padding: 12px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 14px;
  line-height: 1.6;
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};
  &:last-child { border-bottom: 0; }
  &[role='row']:first-child {
    background: ${({ theme }) => theme.colors.background};
    font-weight: 700;
  }
  span { min-width: 0; overflow-wrap: anywhere; }
  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: minmax(0, 1fr) 58px 90px;
    gap: 6px;
    padding: 10px 8px;
  }
`;

// AI작성: 소모임과 같은 한국어 연월일 형식으로 표시하며 누락·잘못된 날짜는 안내 문구로 대체한다.
function formatNoticeDate(value) {
  if (!value) return '날짜 확인 필요';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '날짜 확인 필요';
  return new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }).format(date);
}

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
function SavingGroupDetail({ group, activeMenu, onMenuChange, onJoinRequest, isJoinSubmitting, onBack, onNoticeRefresh }) {
  // AI작성: 기존 버튼과 별도 안내 영역을 접근성 ID로 연결한다.
  const joinReasonId = useId();
  // AI작성: 작성 권한과 달리 승인된 일반 계원도 상세 조회 링크를 사용할 수 있다.
  const canReadNotice = group.membershipStatus === 'APPROVED'
    && ['OWNER', 'MANAGER', 'MEMBER'].includes(group.myRole) && ['01', '02'].includes(group.status);
  const allowedMenus = getSavingGroupMenus(group.myRole);
  // AI작성: 허용되지 않은 탭 상태가 남아 있어도 공지로 되돌려 계주 전용 패널을 렌더링하지 않는다.
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
          <HeaderActions>
            {/* AI작성: 기존 JoinButton의 스타일을 그대로 사용하고 상태·동작만 연결한다. */}
            <GroupJoinAction group={group} onJoinRequest={onJoinRequest}
              isSubmitting={isJoinSubmitting} ButtonComponent={JoinButton} reasonId={joinReasonId} />
            {/* AI작성: 모임장은 본인 모임을 신고할 수 없으므로 일반 사용자에게만 공통 신고 버튼을 제공한다. */}
            {group.myRole !== 'OWNER' && (
              <GroupReportButton groupId={group.groupId} groupLabel="계모임" />
            )}
          </HeaderActions>
        </HeaderTop>
        <GroupJoinReason group={group} isSubmitting={isJoinSubmitting} id={joinReasonId} />
        <Description>{group.description?.trim() || '소개글이 없습니다.'}</Description>
        <MetaList>
          <Meta><FiMapPin size={16} aria-hidden="true" /> {group.regionName}</Meta>
          <Meta><FiUsers size={16} aria-hidden="true" /> {group.memberCount}명 / {group.maxMembers == null ? '정원 미정' : '정원 ' + group.maxMembers + '명'}</Meta>
        </MetaList>
        <SavingGroupMenuTabs
          activeMenu={currentMenu}
          onMenuChange={onMenuChange}
          allowedMenus={allowedMenus}
          settingsPath={`/saving-groups/${group.groupId}/settings`}
        />
      </HeaderCard>

      {currentMenu === 'members' ? (
        <MembersContent>
          <SavingGroupMembersPanel />
        </MembersContent>
      ) : currentMenu === 'payment' ? (
        <SavingGroupPaymentStatusPanel />
      ) : currentMenu === 'dues' ? (
        <SavingGroupDashboardPanel onViewTransactions={() => onMenuChange('transactionHistory')} />
      ) : currentMenu === 'transactionHistory' ? (
        <SavingGroupTransactionHistoryPanel />
      ) : currentMenu === 'payoutRequest' ? (
        <SavingGroupPayoutRequestPanel groupType={group.groupType} />
      ) : currentMenu === 'joinRequests' ? (
        <JoinRequestsContent>
          {/* AI작성: 화면 모임이 바뀌면 요청·승인 상태를 초기화하고 API가 요구하는 savingGroupId로 실제 신청만 조회한다. */}
          <SavingGroupJoinRequestsPanel key={group.savingGroupId} savingGroupId={String(group.savingGroupId)} />
        </JoinRequestsContent>
      ) : (
        <Layout>
          <MainColumn>
            <Card>
              <CardTitle>모임 소개</CardTitle>
              <Content>{group.description?.trim() || '소개글이 없습니다.'}</Content>
            </Card>
            <Card>
              {/* AI작성: 공지 작성은 실제 계모임 API만 사용하며 역할·회원 상태·모임 상태에 따라 표시한다. */}
              <SavingGroupNoticeCreateForm group={group} onRefresh={onNoticeRefresh}
                heading={<CardTitle><FiBell size={18} aria-hidden="true" /> 최근 공지</CardTitle>}>
                {group.notices.length === 0 ? <Content>등록된 공지가 없습니다.</Content>
                  : (
                    <NoticeTable role="table" aria-label="최근 공지 목록">
                      <NoticeRow role="row">
                        <span role="columnheader">제목</span>
                        <span role="columnheader">작성자</span>
                        <span role="columnheader">작성일</span>
                      </NoticeRow>
                      {group.notices.map((notice) => (
                        <NoticeRow role="row" key={notice.noticeId}>
                          <span role="cell">
                            {canReadNotice ? (
                              <NoticeLink to={`/saving-groups/${group.groupId}/notices/${notice.noticeId}`}>
                                {notice.title}
                              </NoticeLink>
                            ) : notice.title}
                          </span>
                          <span role="cell">{notice.writerNickname || '작성자 정보 없음'}</span>
                          <span role="cell">{formatNoticeDate(notice.createdAt)}</span>
                        </NoticeRow>
                      ))}
                    </NoticeTable>
                  )}
              </SavingGroupNoticeCreateForm>
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
