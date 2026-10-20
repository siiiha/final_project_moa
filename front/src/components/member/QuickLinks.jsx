import { FiBell, FiMapPin, FiShield } from 'react-icons/fi';
import styled from 'styled-components';

const Area = styled.aside`
  display: grid;
  align-content: start;
  gap: 12px;
`;
const Heading = styled.h2`
  padding: 0 2px;
  font-size: 16px;
  font-weight: 800;
  letter-spacing: -0.75px;
`;
const Card = styled.button`
  padding: 15px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
  text-align: left;
  &:hover {
    border-color: ${({ theme }) => theme.colors.primary};
  }
`;
const IconWrap = styled.span`
  color: ${({ theme }) => theme.colors.primary};
`;
const LinkTitle = styled.strong`
  display: block;
  font-size: 13px;
  letter-spacing: -0.35px;
`;
const Description = styled.span`
  display: block;
  margin-top: 5px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  line-height: 1.5;
`;
const iconMap = { verify: FiShield, location: FiMapPin, alerts: FiBell };
function QuickLinks({ links, onSelect }) {
  return (
    <Area>
      <Heading>바로가기</Heading>
      {links.map((link) => {
        const Icon = iconMap[link.icon];
        return (
          <Card key={link.id} type="button" onClick={() => onSelect(link.id)}>
            <IconWrap>
              <Icon size={17} aria-hidden="true" />
            </IconWrap>
            <span>
              <LinkTitle>{link.title}</LinkTitle>
              <Description>{link.description}</Description>
            </span>
          </Card>
        );
      })}
    </Area>
  );
}
export default QuickLinks;
