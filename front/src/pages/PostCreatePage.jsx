import { useEffect, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { createPost, getPostCategories, getPostDetail, updatePost } from '../api/postApi';
import PostCreateForm from '../components/post/PostCreateForm';
import useAuthStore from '../store/authStore';


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

const StatusMessage = styled.p`
  padding: 32px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
`;

function PostCreatePage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { id: postId } = useParams();
  const isEditMode = Boolean(postId);
  const token = useAuthStore((state) => state.token);
  const [category, setCategory] = useState('');
  const [categories, setCategories] = useState([]);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [imageName, setImageName] = useState('');
  const [isLoadingPost, setIsLoadingPost] = useState(isEditMode);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');

  useEffect(() => {
    let isActive = true;
    getPostCategories()
      .then((response) => {
        if (!response.success) throw new Error(response.error?.message || '카테고리를 불러오지 못했습니다.');
        if (!isActive) return;
        const loaded = response.data || [];
        setCategories(loaded);
        setCategory((current) => current || loaded[0]?.categoryName || '');
      })
      .catch((error) => isActive && setErrorMessage(error.response?.data?.error?.message || error.message))
      .finally(() => isActive && setIsLoadingCategories(false));
    return () => { isActive = false; };
  }, []);

  useEffect(() => {
    if (!isEditMode) return undefined;

    let isActive = true;

    async function loadPost() {
      setIsLoadingPost(true);
      setErrorMessage('');

      try {
        // AI작성: 수정 화면에 기존 게시글의 카테고리·제목·내용을 표시합니다.
        const response = await getPostDetail(postId);
        if (!response.success || !response.data) {
          throw new Error(response.error?.message || '게시글을 불러오지 못했습니다.');
        }

        if (!isActive) return;

        setCategory(response.data.categoryName || '');
        setTitle(response.data.title || '');
        setContent(response.data.content || '');
      } catch (error) {
        if (!isActive) return;
        setErrorMessage(
          error.response?.data?.error?.message || error.message || '게시글을 불러오지 못했습니다.'
        );
      } finally {
        if (isActive) setIsLoadingPost(false);
      }
    }

    loadPost();

    return () => {
      isActive = false;
    };
  }, [isEditMode, postId]);

  const handleImageChange = (event) => {
    // TODO: Spring Boot 이미지 업로드 API와 연결해 업로드한 이미지 URL을 게시글 등록 요청에 포함합니다.
    setImageName(event.target.files?.[0]?.name || '');
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (isSubmitting || isLoadingPost) return;
    if (!token) {
      navigate(`/member/login?redirect=${encodeURIComponent(location.pathname)}`);
      return;
    }

    const categoryId = categories.find((item) => item.categoryName === category)?.categoryId;
    if (!categoryId) {
      setErrorMessage('올바른 카테고리를 선택해주세요.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage('');

    try {
      let response;

      if (isEditMode) {
        response = await updatePost(postId, {
          categoryId,
          title: title.trim(),
          content: content.trim(),
        });
      } else {
        response = await createPost({
          categoryId,
          title: title.trim(),
          content: content.trim(),
        });
      }

      if (!response.success || !response.data) {
        throw new Error(response.error?.message || `게시글 ${isEditMode ? '수정' : '등록'}에 실패했습니다.`);
      }

      navigate(`/post/${isEditMode ? postId : response.data}`);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.error?.message ||
          error.message ||
          `게시글 ${isEditMode ? '수정' : '등록'}에 실패했습니다.`
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
        {isLoadingPost || isLoadingCategories ? (
          <StatusMessage>게시글을 불러오는 중입니다.</StatusMessage>
        ) : categories.length === 0 ? (
          <StatusMessage role="alert">게시글 카테고리가 없습니다. 관리자에게 문의해주세요.</StatusMessage>
        ) : (
          <PostCreateForm
            categories={categories.map((item) => item.categoryName)}
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
            submitLabel={isSubmitting ? (isEditMode ? '수정 중...' : '등록 중...') : isEditMode ? '수정' : '게시글 등록'}
          />
        )}
      </Content>
    </Page>
  );
}

export default PostCreatePage;
