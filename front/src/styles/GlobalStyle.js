import { createGlobalStyle } from 'styled-components';

// 웹사이트 전체에 공통으로 적용될 스타일(CSS Reset 및 기본 설정)을 정의합니다.
const GlobalStyle = createGlobalStyle`
  /* 1. 모든 요소 초기화 */
  *, *::before, *::after {
    margin: 0;         /* 브라우저 기본 외곽 여백 제거 */
    padding: 0;        /* 브라우저 기본 내부 여백 제거 */
    box-sizing: border-box; /* 패딩과 테두리를 포함하여 요소의 크기를 계산 (레이아웃 뒤틀림 방지) */
  }

  /* 2. 본문(body) 기본 스타일 설정 */
  body {
    /* 웹사이트 기본 글꼴 지정 (Pretendard를 최우선으로 적용하고 없으면 순서대로 대체) */
    font-family: 'Pretendard', 'Apple SD Gothic Neo', 'Malgun Gothic', sans-serif;
    
    /* ThemeProvider에서 설정한 다크모드/라이트모드 테마의 글자 색상을 적용 */
    color: ${({ theme }) => theme.colors.text};
    
    /* ThemeProvider에서 설정한 다크모드/라이트모드 테마의 배경 색상을 적용 */
    background: ${({ theme }) => theme.colors.background};
    
    /* 글자 행간(줄간격)을 1.5배로 설정하여 가독성 향상 */
    line-height: 1.5;
  }

  /* 3. 링크(a) 태그 스타일 초기화 */
  a {
    color: inherit;         /* 부모 요소의 글자 색상을 그대로 상속받음 (기본 파란색 제거) */
    text-decoration: none;  /* 링크 밑줄 제거 */
  }

  /* 4. 버튼(button) 태그 스타일 초기화 */
  button {
    font: inherit;      /* 부모 요소의 글꼴 스타일(크기, 두께 등)을 그대로 상속받음 */
    cursor: pointer;    /* 버튼에 마우스를 올렸을 때 클릭 가능한 손가락 모양 커서로 변경 */
  }

  /* 5. 목록(ul, ol) 태그 스타일 초기화 */
  ul, ol {
    list-style: none;   /* 리스트 왼쪽에 자동으로 붙는 점(•)이나 숫자 기호 제거 */
  }
`;

export default GlobalStyle;
