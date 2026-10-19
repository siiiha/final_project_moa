import { FiImage } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const Card = styled.section`
  margin-top: 16px;
  padding: 20px 24px 4px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;
const Title = styled.h2`
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;
const List = styled.div`
  margin-top: 12px;
`;
const Row = styled.article`
  padding: 15px 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
`;
const MeetingContent = styled.div`
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 13px;
`;
const MeetingImage = styled.div`
  width: 76px;
  height: 56px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  color: ${({ theme }) => theme.colors.muted};
  background: ${({ theme }) => theme.colors.primarySoft};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
`;
const MeetingTitle = styled.h3`
  font-size: 14px;
  font-weight: 800;
  letter-spacing: -0.55px;
`;
const Description = styled.p`
  margin-top: 3px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;
const Meta = styled.p`
  margin-top: 4px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;
const ViewButton = styled(Button)`
  min-height: 31px;
  padding: 6px 10px;
  font-size: 11px;
`;
function MeetingList({ meetings, onView }) {
  return (
    <Card>
      <Title>내 참여 모임</Title>
      <List>
        {meetings.map((meeting) => (
          <Row key={meeting.id}>
            <MeetingContent>
              <MeetingImage role="img" aria-label={`${meeting.title} 모임 이미지 영역`}>
                <FiImage size={19} aria-hidden="true" />
              </MeetingImage>
              <div>
                <MeetingTitle>{meeting.title}</MeetingTitle>
                <Description>{meeting.description}</Description>
                <Meta>{meeting.meta}</Meta>
              </div>
            </MeetingContent>
            <ViewButton type="button" $variant="secondary" onClick={() => onView(meeting.id)}>
              모임 보기
            </ViewButton>
          </Row>
        ))}
      </List>
    </Card>
  );
}
export default MeetingList;
