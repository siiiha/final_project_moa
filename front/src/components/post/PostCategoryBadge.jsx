import styled from 'styled-components';

// AI작성: 커뮤니티와 관리자 화면에서 같은 카테고리 색상 규칙을 재사용한다.
function getCategoryTone(category, theme) {
  if (category === '자유게시판') return { color: theme.colors.primary, background: theme.colors.primarySoft };
  if (category?.includes('질문')) return { color: '#b4234d', background: '#fff0f5' };
  if (category?.includes('동네')) return { color: '#9a5b08', background: '#fff4dd' };
  if (category?.includes('거래') || category?.includes('나눔')) {
    return { color: '#2457a6', background: '#edf4ff' };
  }
  return { color: theme.colors.secondaryText, background: theme.colors.background };
}

const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  min-height: ${({ $size }) => ($size === 'detail' ? '25px' : 'auto')};
  padding: ${({ $size }) => ($size === 'detail' ? '4px 9px' : $size === 'list' ? '4px 7px' : '4px 8px')};
  color: ${({ $category, theme }) => getCategoryTone($category, theme).color};
  background: ${({ $category, theme }) => getCategoryTone($category, theme).background};
  border-radius: ${({ theme }) => theme.radius.pill};
  font-size: ${({ $size }) => ($size === 'detail' ? '11px' : '10px')};
  font-weight: 700;
  letter-spacing: ${({ $size }) => ($size === 'detail' ? '-0.25px' : 'normal')};
  white-space: nowrap;
`;

function PostCategoryBadge({ category, size }) {
  return <Badge $category={category} $size={size}>{category}</Badge>;
}

export default PostCategoryBadge;
