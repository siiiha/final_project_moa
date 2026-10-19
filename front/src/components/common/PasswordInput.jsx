import { FiEye, FiEyeOff } from 'react-icons/fi';
import styled from 'styled-components';

const Field = styled.div`
  display: flex;
  flex-direction: column;
  gap: 7px;

  ${({ $inline }) =>
    $inline &&
    `
      display: grid;
      grid-template-columns: 92px minmax(0, 1fr);
      align-items: center;
      gap: 8px;
    `}
`;

const Label = styled.label`
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
  letter-spacing: -0.35px;

  ${({ $inline }) =>
    $inline &&
    `
      grid-column: 1;
    `}
`;

const Control = styled.div`
  position: relative;

  ${({ $inline }) =>
    $inline &&
    `
      grid-column: 2;
    `}
`;

const Input = styled.input`
  width: 100%;
  height: ${({ $compact }) => ($compact ? '42px' : '48px')};
  padding: 0 44px 0 14px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const ToggleButton = styled.button`
  position: absolute;
  top: 50%;
  right: 10px;
  transform: translateY(-50%);
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: transparent;
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm};

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
  }
`;

function PasswordInput({ id, label, isVisible, onToggleVisibility, inline = false, compact = false, ...inputProps }) {
  return (
    <Field $inline={inline}>
      <Label htmlFor={id} $inline={inline}>
        {label}
      </Label>
      <Control $inline={inline}>
        <Input id={id} $compact={compact} type={isVisible ? 'text' : 'password'} {...inputProps} />
        <ToggleButton
          type="button"
          onClick={onToggleVisibility}
          aria-label={isVisible ? '비밀번호 숨기기' : '비밀번호 보기'}
        >
          {isVisible ? <FiEyeOff size={17} aria-hidden="true" /> : <FiEye size={17} aria-hidden="true" />}
        </ToggleButton>
      </Control>
    </Field>
  );
}

export default PasswordInput;
