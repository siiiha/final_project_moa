import { FiUser } from 'react-icons/fi';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';

const Card = styled.section`
  padding: 24px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Top = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
`;

const ProfileGroup = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 14px;
`;

const ProfileImage = styled.div`
  width: 72px;
  height: 72px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 50%;
`;

const ProfileDetails = styled.div`
  min-width: 0;
  padding-top: 2px;
`;

const Name = styled.h2`
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -1px;
`;

const Email = styled.p`
  margin-top: 4px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
`;

const EditButton = styled(Button)`
  min-height: 32px;
  padding: 6px 11px;
  font-size: 11px;
`;

const BadgeList = styled.div`
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
`;

const Stats = styled.div`
  margin-top: 20px;
  padding-top: 18px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;

const Stat = styled.div`
  padding: 0 12px;
  text-align: center;
  border-right: 1px solid ${({ theme }) => theme.colors.border};

  &:first-child {
    padding-left: 0;
  }
  &:last-child {
    padding-right: 0;
    border-right: 0;
  }

  span {
    display: block;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 11px;
  }
  strong {
    display: block;
    margin-top: 4px;
    color: ${({ theme }) => theme.colors.text};
    font-size: 15px;
  }
`;

function ProfileSummary({ profile, onEdit }) {
  return (
    <Card>
      <Top>
        <ProfileGroup>
          <ProfileImage role="img" aria-label={`${profile.name} 프로필 이미지 영역`}>
            <FiUser size={31} aria-hidden="true" />
          </ProfileImage>
          <ProfileDetails>
            <Name>{profile.name}</Name>
            <Email>{profile.email}</Email>
            <BadgeList>
              {profile.badges.map((badge) => (
                <Badge key={badge}>✓ {badge}</Badge>
              ))}
            </BadgeList>
          </ProfileDetails>
        </ProfileGroup>
        <EditButton type="button" onClick={onEdit}>
          내 정보 수정
        </EditButton>
      </Top>
      <Stats>
        {profile.stats.map((stat) => (
          <Stat key={stat.label}>
            <span>{stat.label}</span>
            <strong>{stat.value}</strong>
          </Stat>
        ))}
      </Stats>
    </Card>
  );
}

export default ProfileSummary;
