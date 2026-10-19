import styled from 'styled-components';

const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  min-height: 25px;
  padding: 4px 9px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: 11px;
  font-weight: 700;
  letter-spacing: -0.25px;
`;

export default Badge;
