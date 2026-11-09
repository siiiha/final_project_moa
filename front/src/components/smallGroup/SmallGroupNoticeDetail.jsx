import styled from 'styled-components';

// AI작성: 기존 소모임 테마를 재사용하고 긴 제목·본문과 모바일 화면에서도 읽기 쉬운 여백을 둔다.
const Article = styled.article`
  padding: 24px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
  overflow-wrap: anywhere;
  h1 { font-size: 24px; line-height: 1.5; color: ${({ theme }) => theme.colors.text}; }
`;
const Meta = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 8px 24px;
  margin-top: 16px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 16px;
`;
const Body = styled.div`
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid ${({ theme }) => theme.colors.border};
  white-space: pre-wrap;
  font-size: 18px;
  line-height: 1.8;
  color: ${({ theme }) => theme.colors.text};
`;

// AI작성: 수정·삭제 버튼을 본문 아래 카드 내부 우측에 배치하고 좁은 화면에서는 줄바꿈을 허용한다.
const Actions = styled.div`
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
`;

// AI작성: 작성 폼에서 저장한 일반 텍스트를 HTML로 해석하지 않고 줄바꿈을 유지한다. 내부 ID는 표시하지 않는다.
export default function SmallGroupNoticeDetail({ notice, actions }) {
  const date = notice.createdAt ? new Date(notice.createdAt) : null;
  const validDate = date && !Number.isNaN(date.getTime());
  return (
    <Article aria-labelledby="small-group-notice-title">
      <h1 id="small-group-notice-title">{notice.title}</h1>
      <Meta>
        <p>작성자: {notice.writerNickname || '작성자 정보 없음'}</p>
        <p>작성일: {validDate ? (
          <time dateTime={notice.createdAt}>
            {new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' }).format(date)}
          </time>
        ) : '날짜 확인 필요'}</p>
      </Meta>
      <Body>{notice.content}</Body>
      {actions && <Actions>{actions}</Actions>}
    </Article>
  );
}
