import { FiSearch } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';

const Categories = styled.nav`
  display: flex;
  gap: 4px;
  border-bottom: 1px solid ${({ theme }) => theme.colors.secondaryText};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    overflow-x: auto;
  }
`;

const CategoryButton = styled.button`
  min-width: max-content;
  padding: 10px 14px;
  color: ${({ $active, theme }) => ($active ? theme.colors.primary : theme.colors.secondaryText)};
  background: ${({ $active, theme }) => ($active ? theme.colors.primarySoft : 'transparent')};
  border: 0;
  border-radius: ${({ theme }) => theme.radius.sm} ${({ theme }) => theme.radius.sm} 0 0;
  font-size: 12px;
  font-weight: ${({ $active }) => ($active ? 800 : 600)};
`;

const Form = styled.form`
  margin-top: 12px;
  display: grid;
  grid-template-columns: minmax(150px, 1fr) minmax(180px, 1.3fr) minmax(120px, 0.8fr) auto;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr 1fr;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 5px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
  font-weight: 700;
`;

const Select = styled.select`
  width: 100%;
  height: 38px;
  padding: 0 10px;
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

const SearchInput = styled.input`
  width: 100%;
  height: 38px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const FormActions = styled.div`
  align-self: end;
  display: flex;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    display: block;
  }
`;

const SearchButton = styled(Button)`
  min-height: 38px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

function PostFilterBar({
  categories,
  category,
  condition,
  keyword,
  sort,
  onCategoryChange,
  onConditionChange,
  onKeywordChange,
  onSortChange,
  onSearch,
}) {
  return (
    <>
      <Categories aria-label="게시글 카테고리">
        {categories.map((item) => (
          <CategoryButton key={item} type="button" $active={category === item} onClick={() => onCategoryChange(item)}>
            {item}
          </CategoryButton>
        ))}
      </Categories>
      <Form onSubmit={onSearch}>
        <Field>
          검색 조건
          <Select value={condition} onChange={(event) => onConditionChange(event.target.value)}>
            <option value="제목+내용">제목 + 내용</option>
            <option value="제목">제목</option>
            <option value="내용">내용</option>
          </Select>
        </Field>
        <Field>
          검색어
          <SearchInput
            value={keyword}
            onChange={(event) => onKeywordChange(event.target.value)}
            placeholder="검색어를 입력하세요"
          />
        </Field>
        <Field>
          정렬
          <Select value={sort} onChange={(event) => onSortChange(event.target.value)}>
            <option value="최신순">최신순</option>
            <option value="과거순">과거순</option>
          </Select>
        </Field>
        <FormActions>
          <SearchButton type="submit">
            <FiSearch size={14} aria-hidden="true" />
            검색
          </SearchButton>
        </FormActions>
      </Form>
    </>
  );
}

export default PostFilterBar;
