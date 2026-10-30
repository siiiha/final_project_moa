import { FiImage } from 'react-icons/fi';
import styled, { css } from 'styled-components';
import Button from '../common/Button';

const Form = styled.form`
  padding: 22px 14px 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    padding: 18px 12px 12px;
  }
`;

const TopFields = styled.div`
  display: grid;
  grid-template-columns: 175px minmax(0, 1fr);
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 6px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const ControlStyle = css`
  width: 100%;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Select = styled.select`
  ${ControlStyle}
  height: 38px;
  padding: 0 10px;
`;

const Input = styled.input`
  ${ControlStyle}
  height: 38px;
  padding: 0 11px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }
`;

const ContentField = styled(Field)`
  margin-top: 14px;
`;

// AI작성: 게시글 본문 입력창의 높이를 고정하고 긴 글은 내부 스크롤로 확인하게 한다.
const Textarea = styled.textarea`
  ${ControlStyle}
  height: 320px;
  padding: 12px;
  line-height: 1.6;
  resize: none;
  overflow-y: auto;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    height: 200px;
  }
`;

const Attachment = styled.div`
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const UploadLabel = styled.label`
  min-height: 36px;
  padding: 8px 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  align-self: flex-start;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    color: ${({ theme }) => theme.colors.primary};
    background: ${({ theme }) => theme.colors.background};
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-self: stretch;
  }
`;

const FileInput = styled.input`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
`;

const FileName = styled.p`
  overflow: hidden;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Actions = styled.div`
  margin-top: 14px;
  padding-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: grid;
    grid-template-columns: 1fr 1fr;
  }
`;

function PostCreateForm({
  categories,
  category,
  title,
  content,
  imageName,
  onCategoryChange,
  onTitleChange,
  onContentChange,
  onImageChange,
  onSubmit,
  onCancel,
  submitLabel,
}) {
  return (
    <Form onSubmit={onSubmit}>
      <TopFields>
        <Field>
          카테고리
          <Select value={category} onChange={(event) => onCategoryChange(event.target.value)}>
            {categories.map((item) => (
              <option key={item} value={item}>
                {item}
              </option>
            ))}
          </Select>
        </Field>
        <Field>
          제목
          {/* AI작성: 백엔드 제목 Validation과 동일하게 입력 단계에서 100자로 제한합니다. */}
          <Input
            value={title}
            onChange={(event) => onTitleChange(event.target.value)}
            placeholder="제목을 입력하세요"
            maxLength={100}
            required
          />
        </Field>
      </TopFields>
      <ContentField>
        내용
        {/* AI작성: 백엔드 본문 Validation과 동일하게 입력 단계에서 5,000자로 제한합니다. */}
        <Textarea
          value={content}
          onChange={(event) => onContentChange(event.target.value)}
          placeholder="내용을 입력하세요"
          maxLength={5000}
          required
        />
      </ContentField>
      <Attachment>
        <UploadLabel htmlFor="post-image">
          <FiImage size={15} aria-hidden="true" />
          이미지 첨부
        </UploadLabel>
        <FileInput id="post-image" type="file" accept="image/*" onChange={onImageChange} />
        <FileName>{imageName || '첨부할 이미지 파일을 선택하세요. (선택)'}</FileName>
      </Attachment>
      <Actions>
        <Button type="submit">{submitLabel}</Button>
        <Button type="button" $variant="secondary" onClick={onCancel}>
          취소
        </Button>
      </Actions>
    </Form>
  );
}

export default PostCreateForm;
