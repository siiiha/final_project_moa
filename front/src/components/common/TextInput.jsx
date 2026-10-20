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
  display: flex;
  gap: 8px;

  ${({ $inline }) =>
    $inline &&
    `
      grid-column: 2;
    `}
`;

const Input = styled.input`
  width: 100%;
  min-width: 0;
  flex: 1;
  height: ${({ $compact }) => ($compact ? '42px' : '48px')};
  padding: 0 14px;
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

  &:disabled {
    color: ${({ theme }) => theme.colors.muted};
    background: #eef2f1;
    cursor: not-allowed;
  }
`;

const HelpText = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
  letter-spacing: -0.35px;

  ${({ $inline }) =>
    $inline &&
    `
      grid-column: 2;
      margin-top: -3px;
    `}
`;

function TextInput({ id, label, action, helpText, inline = false, compact = false, ...inputProps }) {
  return (
    <Field $inline={inline}>
      <Label htmlFor={id} $inline={inline}>
        {label}
      </Label>
      <Control $inline={inline}>
        <Input id={id} $compact={compact} {...inputProps} />
        {action}
      </Control>
      {helpText && <HelpText $inline={inline}>{helpText}</HelpText>}
    </Field>
  );
}

export default TextInput;
