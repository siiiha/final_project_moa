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
  // AI작성: 주요 기능과 같은 계열이지만 강조도를 낮춘 보조 동작에 사용합니다.
  primarySoft: css`
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.primarySoft};
    border-color: ${({ theme }) => theme.colors.primary};
    &:hover {
      color: ${({ theme }) => theme.colors.primary};
      background: ${({ theme }) => theme.colors.primarySoftHover};
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

  &:focus-visible {
    outline: 2px solid ${({ theme }) => theme.colors.primary};
    outline-offset: 2px;
  }

  ${({ $variant = 'primary' }) => variants[$variant]}

  /* AI작성: 공통 버튼의 비활성 상태가 유형별 hover 스타일보다 우선하도록 통일합니다. */
  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: ${({ theme }) => theme.colors.background};
    border-color: ${({ theme }) => theme.colors.border};
    cursor: not-allowed;
    transform: none;
  }
`;

export default Button;
