import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { createPost } from '../api/postApi';
import PostCreateForm from '../components/post/PostCreateForm';
import { postCategories, postItems } from '../mocks/posts';

const CATEGORY_ID_BY_NAME = {
  '행사·축제': 1,
  '일자리': 2,
  '거래·나눔': 3,
};

// AI작성: 인증 연동 전에만 사용하는 임시 회원 ID이며, 추후 로그인 회원 ID로 교체해야 합니다.
const TEMP_WRITER_ID = 44;

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

const ErrorMessage = styled.p`
  margin-bottom: 12px;
  color: ${({ theme }) => theme.colors.danger || '#c62828'};
  font-size: 13px;
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
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  const handleImageChange = (event) => {
    // TODO: Spring Boot 이미지 업로드 API와 연결해 업로드한 이미지 URL을 게시글 등록 요청에 포함합니다.
    setImageName(event.target.files?.[0]?.name || '');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting) return;

    if (isEditMode) {
      setErrorMessage('게시글 수정 API는 다음 단계에서 연결합니다.');
      return;
    }

    const categoryId = CATEGORY_ID_BY_NAME[category];
    if (!categoryId) {
      setErrorMessage('올바른 카테고리를 선택해주세요.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      // AI작성: 인증 연동 후 TEMP_WRITER_ID를 로그인 회원 ID로 교체해야 합니다.
      const response = await createPost({
        writerId: TEMP_WRITER_ID,
        categoryId,
        title: title.trim(),
        content: content.trim(),
      });

      if (!response.success || !response.data) {
        throw new Error(response.error?.message || '게시글 등록에 실패했습니다.');
      }

      navigate(`/post/${response.data}`);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message || error.message || '게시글 등록에 실패했습니다.'
      );
    } finally {
      setIsSubmitting(false);
    }
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
        {errorMessage && <ErrorMessage role="alert">{errorMessage}</ErrorMessage>}
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
          submitLabel={isSubmitting ? '등록 중...' : isEditMode ? '수정' : '게시글 등록'}
        />
      </Content>
    </Page>
  );
}

export default PostCreatePage;
