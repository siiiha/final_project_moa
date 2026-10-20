import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import PostCreateForm from '../components/post/PostCreateForm';
import { postCategories, postItems } from '../mocks/posts';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 20px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Heading = styled.div`
  margin-top: 8px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 24px;
    font-weight: 800;
    letter-spacing: -1.2px;
  }

  p {
    margin-top: 6px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }
`;

const Content = styled.main`
  margin-top: 14px;
`;

function PostCreatePage() {
  const navigate = useNavigate();
  const { id: postId } = useParams();
  const isEditMode = Boolean(postId);
  const editingPost = postItems.find((item) => item.id === Number(postId));
  const [category, setCategory] = useState(editingPost?.category || '행사·축제');
  const [title, setTitle] = useState(editingPost?.title || '');
  const [content, setContent] = useState(editingPost?.content || '');
  const [imageName, setImageName] = useState(editingPost?.imageName || '');

  const handleImageChange = (event) => {
    // TODO: Spring Boot 이미지 업로드 API와 연결해 업로드한 이미지 URL을 게시글 등록 요청에 포함합니다.
    setImageName(event.target.files?.[0]?.name || '');
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 게시글 등록·수정 API 요청 후 게시글 상세 페이지로 이동합니다.
    const today = new Date();
    const createdDate = `${today.getFullYear()}.${String(today.getMonth() + 1).padStart(2, '0')}.${String(today.getDate()).padStart(2, '0')}`;
    const targetPostId = isEditMode ? postId : 'new';

    navigate(`/post/${targetPostId}`, {
      state: {
        post: {
          id: targetPostId,
          category,
          title,
          content,
          author: editingPost?.author || '모아이웃',
          date: editingPost?.date || createdDate,
          views: editingPost?.views || 0,
          imageName,
        },
        comments: [],
      },
    });
  };

  const handleCancel = () => {
    navigate(isEditMode ? `/post/${postId}` : '/post/list');
  };

  return (
    <Page>
      <Breadcrumb>게시판 &gt; {isEditMode ? '게시글 수정' : '게시글 작성'}</Breadcrumb>
      <Heading>
        <h1>{isEditMode ? '게시글 수정' : '게시글 작성'}</h1>
        <p>
          {isEditMode
            ? '수정할 내용을 입력한 뒤 변경사항을 저장해주세요.'
            : '동네 이웃들과 나누고 싶은 이야기를 작성해주세요.'}
        </p>
      </Heading>
      <Content>
        <PostCreateForm
          categories={postCategories.filter((item) => item !== '전체')}
          category={category}
          title={title}
          content={content}
          imageName={imageName}
          onCategoryChange={setCategory}
          onTitleChange={setTitle}
          onContentChange={setContent}
          onImageChange={handleImageChange}
          onSubmit={handleSubmit}
          onCancel={handleCancel}
          submitLabel={isEditMode ? '수정' : '게시글 등록'}
        />
      </Content>
    </Page>
  );
}

export default PostCreatePage;
