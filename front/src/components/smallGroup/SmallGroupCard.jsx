import { FiChevronRight, FiMapPin, FiUsers } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';

const Card = styled.button`
  min-width: 0;
  padding: 17px;
  display: flex;
  flex-direction: column;
  color: inherit;
  text-align: left;
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

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 15px;
  }
`;

const TopLine = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
`;

const Title = styled.h2`
  overflow: hidden;
  color: ${({ theme }) => theme.colors.text};
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.7px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Capacity = styled.span`
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const MetaList = styled.div`
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
`;

const MetaBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  font-size: 10px;
`;

const Description = styled.p`
  min-height: 39px;
  margin-top: 11px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  line-height: 1.65;
`;

const Details = styled.div`
  margin-top: 12px;
  padding-top: 11px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const Detail = styled.span`
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const ViewLabel = styled.span`
  width: 100%;
  min-height: 31px;
  margin-top: 14px;
  padding: 6px 10px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-color: transparent;
  font-size: 11px;

  &:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
    border-color: ${({ theme }) => theme.colors.primary};
  }
`;

function SmallGroupCard({ group, onView }) {
  return (
    <Card type="button" onClick={() => onView(group.id)} aria-label={`${group.name} 상세 보기`}>
      <TopLine>
        <Title>{group.name}</Title>
        <Capacity>{group.participants}</Capacity>
      </TopLine>
      <MetaList>
        <MetaBadge>{group.region}</MetaBadge>
        <MetaBadge>{group.interest}</MetaBadge>
      </MetaList>
      <Description>{group.description}</Description>
      <Details>
        <Detail><FiMapPin size={12} aria-hidden="true" /> {group.region}</Detail>
        <Detail><FiUsers size={12} aria-hidden="true" /> {group.participants}</Detail>
      </Details>
      <ViewLabel>
        모임 보기 <FiChevronRight size={13} aria-hidden="true" />
      </ViewLabel>
    </Card>
  );
}

export default SmallGroupCard;
