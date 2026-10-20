import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import { getPostDetail } from '../api/postApi';
import PostDetail from '../components/post/PostDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import { postComments } from '../mocks/posts';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 20px 0 48px;
`;

const Breadcrumb = styled.p`
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const Title = styled.h1`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -1.2px;
  line-height: 1.35;
`;

const Content = styled.main`
  margin-top: 14px;
`;

const EmptyCard = styled.section`
  padding: 42px 20px;
  text-align: center;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};

  p {
    margin-bottom: 16px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 13px;
  }
`;

function PostDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [commentText, setCommentText] = useState('');
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [post, setPost] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const comments = postComments;

  useEffect(() => {
    let isActive = true;

    async function loadPostDetail() {
      setIsLoading(true);
      setErrorMessage('');

      try {
        // AI작성: 상세 API 응답을 기존 PostDetail 컴포넌트의 표시 형식으로 변환합니다.
        const response = await getPostDetail(id);
        if (!response.success) {
          throw new Error(response.error?.message || '게시글을 불러오지 못했습니다.');
        }

        if (!isActive) return;

        const detail = response.data;
        setPost({
          id: detail.postId,
          category: detail.categoryName || '기타',
          title: detail.title,
          content: detail.content,
          author: detail.writerNickname || '알 수 없음',
          date: detail.createdAtStr || '',
          views: detail.viewCount,
        });
      } catch (error) {
        if (!isActive) return;
        setPost(null);
        setErrorMessage(
          error.response?.data?.error?.message || error.message || '게시글을 불러오지 못했습니다.'
        );
      } finally {
        if (isActive) setIsLoading(false);
      }
    }

    loadPostDetail();

    return () => {
      isActive = false;
    };
  }, [id]);

  const handleEdit = () => {
    navigate(`/post/${id}/edit`);
  };

  const handleDelete = () => {
    setIsDeleteModalOpen(true);
  };

  const handleDeleteConfirm = () => {
    // TODO: Spring Boot 게시글 삭제 API 요청이 성공하면 게시글 목록 화면으로 이동합니다.
    navigate('/post/list');
  };

  const handleCommentSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 댓글 등록 API 요청 후 댓글 목록을 갱신합니다.
  };

  if (isLoading || !post) {
    return (
      <Page>
        <EmptyCard>
          <p>{isLoading ? '게시글을 불러오는 중입니다.' : errorMessage || '존재하지 않거나 삭제된 게시글입니다.'}</p>
          {!isLoading && (
            <Button type="button" $variant="secondary" onClick={() => navigate('/post/list')}>
              목록으로
            </Button>
          )}
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Breadcrumb>게시판 &gt; 게시글 상세</Breadcrumb>
      <Title>{post.title}</Title>
      <Content>
        <PostDetail
          post={post}
          comments={comments}
          commentText={commentText}
          onCommentChange={setCommentText}
          onCommentSubmit={handleCommentSubmit}
          onEdit={handleEdit}
          onDelete={handleDelete}
          onBack={() => navigate('/post/list')}
        />
      </Content>
      {isDeleteModalOpen && (
        <ConfirmModal
          title="게시글 삭제"
          description="게시글을 삭제하시겠습니까? 삭제 후에는 목록에서 확인할 수 없습니다."
          confirmLabel="게시글 삭제"
          confirmVariant="danger"
          size="wide"
          cancelFirst
          onConfirm={handleDeleteConfirm}
          onCancel={() => setIsDeleteModalOpen(false)}
        />
      )}
    </Page>
  );
}

export default PostDetailPage;
