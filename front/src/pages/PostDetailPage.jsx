import { useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import PostDetail from '../components/post/PostDetail';
import Button from '../components/common/Button';
import ConfirmModal from '../components/common/ConfirmModal';
import { postComments, postItems } from '../mocks/posts';

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
  const { state } = useLocation();
  const { id } = useParams();
  const [commentText, setCommentText] = useState('');
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const post = state?.post || postItems.find((item) => item.id === Number(id));
  const comments = state?.comments || postComments;

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

  if (!post) {
    return (
      <Page>
        <EmptyCard>
          <p>존재하지 않거나 삭제된 게시글입니다.</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/post/list')}>
            목록으로
          </Button>
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
