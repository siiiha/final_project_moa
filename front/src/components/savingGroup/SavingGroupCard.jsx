import { FiCalendar, FiMapPin, FiUsers } from 'react-icons/fi';
import { Link } from 'react-router-dom';
import styled from 'styled-components';
import Badge from '../common/Badge';

const Card = styled(Link)`
  overflow: hidden;
  display: block;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
  transition:
    border-color 160ms ease,
    box-shadow 160ms ease,
    transform 160ms ease;

  &:hover {
    border-color: ${({ theme }) => theme.colors.primary};
    box-shadow: 0 6px 18px rgba(29, 55, 48, 0.1);
    transform: translateY(-2px);
  }
`;

const ImagePlaceholder = styled.div`
  width: 100%;
  height: 142px;
  background: ${({ theme }) => theme.colors.primarySoft};
  border-bottom: 1px solid ${({ theme }) => theme.colors.border};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    height: 132px;
  }
`;

const Body = styled.div`
  padding: 16px;
`;

const TopLine = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
`;

const Region = styled.span`
  color: ${({ theme }) => theme.colors.primary};
  font-size: 11px;
  font-weight: 800;
`;

const StatusBadge = styled(Badge)`
  color: ${({ $ongoing, theme }) => ($ongoing ? theme.colors.secondaryText : theme.colors.primary)};
  background: ${({ $ongoing, theme }) => ($ongoing ? theme.colors.background : theme.colors.primarySoft)};
`;

const Title = styled.h2`
  margin-top: 9px;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.7px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Description = styled.p`
  min-height: 38px;
  margin-top: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.6;
`;

const MetaList = styled.div`
  margin-top: 13px;
  padding-top: 12px;
  display: grid;
  gap: 7px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const Meta = styled.p`
  display: flex;
  align-items: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

function SavingGroupCard({ group }) {
  const isOngoing = group.status === '진행중';

  return (
    <Card to={`/saving-groups/${group.id}`} aria-label={`${group.name} 상세 보기`}>
      <ImagePlaceholder role="img" aria-label={`${group.name} 대표 이미지 영역`} />
      <Body>
        <TopLine>
          <Region>{group.region}</Region>
          <StatusBadge $ongoing={isOngoing}>{group.status}</StatusBadge>
        </TopLine>
        <Title>{group.name}</Title>
        <Description>{group.description}</Description>
        <MetaList>
          <Meta>
            <FiMapPin size={13} aria-hidden="true" /> {group.district}
          </Meta>
          <Meta>
            <FiUsers size={13} aria-hidden="true" /> {group.members} · {group.monthlyDues}
          </Meta>
          <Meta>
            <FiCalendar size={13} aria-hidden="true" /> 납부일 {group.nextDate}
          </Meta>
        </MetaList>
      </Body>
    </Card>
  );
}

export default SavingGroupCard;
