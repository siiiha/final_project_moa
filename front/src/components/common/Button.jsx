import styled, { css } from 'styled-components';

const variants = {
  primary: css`
    color: ${({ theme }) => theme.colors.white};
    background: ${({ theme }) => theme.colors.primary};
    border-color: ${({ theme }) => theme.colors.primary};
    &:hover {
      background: ${({ theme }) => theme.colors.primaryDark};
      border-color: ${({ theme }) => theme.colors.primaryDark};
    }
  `,
  secondary: css`
    color: ${({ theme }) => theme.colors.text};
    background: ${({ theme }) => theme.colors.white};
    border-color: ${({ theme }) => theme.colors.border};
    &:hover {
      background: ${({ theme }) => theme.colors.background};
    }
  `,
  danger: css`
    color: ${({ theme }) => theme.colors.white};
    background: ${({ theme }) => theme.colors.danger};
    border-color: ${({ theme }) => theme.colors.danger};
    &:hover {
      background: ${({ theme }) => theme.colors.dangerDark};
      border-color: ${({ theme }) => theme.colors.dangerDark};
    }
  `,
};

const Button = styled.button`
  min-height: ${({ $size }) => ($size === 'small' ? '30px' : '42px')};
  padding: ${({ $size }) => ($size === 'small' ? '5px 10px' : '10px 16px')};
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid transparent;
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: ${({ $size }) => ($size === 'small' ? '11px' : '14px')};
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
  transition:
    background-color 160ms ease,
    border-color 160ms ease,
    transform 160ms ease;
  &:active {
    transform: translateY(1px);
  }
  ${({ $variant = 'primary' }) => variants[$variant]}
`;

export default Button;
