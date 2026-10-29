import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import AdminCommentFilterBar from '../components/admin/AdminCommentFilterBar';
import AdminCommentTable from '../components/admin/AdminCommentTable';
import Badge from '../components/common/Badge';
import ConfirmModal from '../components/common/ConfirmModal';
import AdminSidebar from '../components/layout/AdminSidebar';
import { deleteAdminComment, getAdminComments } from '../api/adminComments';
import { adminMenuItems } from '../mocks/admin';

const PAGE_SIZE = 10;

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;

  h1 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 26px;
    font-weight: 800;
    letter-spacing: -1.35px;
  }

  p {
    margin-top: 5px;
    color: ${({ theme }) => theme.colors.muted};
    font-size: 12px;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    margin-left: 0;
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: flex-start;
    flex-direction: column;
  }
`;

const AdminBadge = styled(Badge)`
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.border};
`;

const Grid = styled.div`
  margin-top: 22px;
  display: grid;
  grid-template-columns: 202px minmax(0, 1fr);
  gap: 16px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: 1fr;
  }
`;

const Content = styled.main`
  padding-top: 4px;
`;

const ErrorMessage = styled.p`
  margin-top: 16px;
  padding: 14px 16px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;
`;

function AdminCommentsPage() {
  const navigate = useNavigate();
  const [comments, setComments] = useState([]);
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [region, setRegion] = useState('');
  const [status, setStatus] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadComments() {
      setLoading(true);
      setErrorMessage('');
      try {
        const result = await getAdminComments({
          keyword: submittedQuery,
          regionName: region,
          status,
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setComments(result.items);
        setPageMeta(result.meta);
        // AI작성: 마지막 페이지의 마지막 댓글이 삭제된 경우 유효한 이전 페이지를 다시 조회한다.
        if (result.meta.totalPages > 0 && currentPage > result.meta.totalPages) {
          setCurrentPage(result.meta.totalPages);
        }
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setComments([]);
          setPageMeta({ total: 0, totalPages: 0 });
          setErrorMessage(error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadComments();
    return () => controller.abort();
  }, [currentPage, region, status, submittedQuery, reloadKey]);

  const handleSearch = (event) => {
    event.preventDefault();
    setSubmittedQuery(query);
    setCurrentPage(1);
  };

  const handleDeleteConfirm = async () => {
    if (!deleteTarget || deleting) return;

    setDeleting(true);
    setDeleteErrorMessage('');
    try {
      await deleteAdminComment(deleteTarget.postId, deleteTarget.id);
      setDeleteTarget(null);
      setNotice({
        title: '댓글 삭제 완료',
        message: '댓글을 삭제했습니다.',
      });
      // AI작성: 서버의 삭제 상태와 페이징 개수를 반영하도록 현재 조건을 다시 조회한다.
      setReloadKey((current) => current + 1);
    } catch (error) {
      // AI작성: 삭제 대상을 유지해 일시적인 오류 후 바로 다시 시도할 수 있게 한다.
      setDeleteErrorMessage(error.message);
    } finally {
      setDeleting(false);
    }
  };

  const handleDeleteRequest = (comment) => {
    setDeleteErrorMessage('');
    setDeleteTarget(comment);
  };

  return (
    <Page>
      <Heading>
        <div>
          <h1>댓글 관리</h1>
          <p>검색과 상세 조회를 통해 서비스 운영 현황을 확인하세요.</p>
        </div>
        <AdminBadge>관리자 전용</AdminBadge>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="comments" />
        <Content>
          <AdminCommentFilterBar
            query={query}
            region={region}
            status={status}
            onQueryChange={setQuery}
            onRegionChange={(value) => {
              setRegion(value);
              setCurrentPage(1);
            }}
            onStatusChange={(value) => {
              setStatus(value);
              setCurrentPage(1);
            }}
            onSearch={handleSearch}
          />
          {errorMessage && <ErrorMessage role="alert">{errorMessage}</ErrorMessage>}
          <AdminCommentTable
            comments={comments}
            totalCount={pageMeta.total}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            onDelete={handleDeleteRequest}
            loading={loading}
            deleting={deleting}
          />
        </Content>
      </Grid>
      {deleteTarget && (
        <ConfirmModal
          title="댓글 삭제"
          description={
            deleteErrorMessage
              ? `삭제하지 못했습니다. ${deleteErrorMessage} 다시 시도하거나 취소하세요.`
              : '이 댓글을 삭제하시겠습니까? 삭제된 댓글은 내용이 숨겨집니다.'
          }
          confirmLabel={deleting ? '삭제 중...' : '댓글 삭제'}
          confirmVariant="danger"
          cancelFirst
          isBusy={deleting}
          onConfirm={handleDeleteConfirm}
          onCancel={() => {
            if (!deleting) {
              setDeleteErrorMessage('');
              setDeleteTarget(null);
            }
          }}
        />
      )}
      {notice && (
        <ConfirmModal
          title={notice.title}
          description={notice.message}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setNotice(null)}
          onCancel={() => setNotice(null)}
        />
      )}
    </Page>
  );
}

export default AdminCommentsPage;
