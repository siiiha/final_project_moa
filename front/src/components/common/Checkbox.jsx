import { FiCheck } from 'react-icons/fi';
import styled from 'styled-components';

const Label = styled.label`
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
  cursor: pointer;
`;

const NativeCheckbox = styled.input`
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;

  &:focus-visible + span {
    outline: 3px solid rgba(23, 107, 91, 0.38);
    outline-offset: 2px;
  }
`;

const Box = styled.span`
  width: 17px;
  height: 17px;
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  color: ${({ theme }) => theme.colors.white};
  background: ${({ $checked, theme }) => ($checked ? theme.colors.primary : theme.colors.white)};
  border: 1px solid ${({ $checked, theme }) => ($checked ? theme.colors.primary : theme.colors.border)};
  border-radius: 4px;
`;

function Checkbox({ id, label, checked, onChange, ...inputProps }) {
  return (
    <Label htmlFor={id}>
      <NativeCheckbox id={id} type="checkbox" checked={checked} onChange={onChange} {...inputProps} />
      <Box $checked={checked}>{checked && <FiCheck size={13} strokeWidth={3} aria-hidden="true" />}</Box>
      {label}
    </Label>
  );
}

export default Checkbox;
