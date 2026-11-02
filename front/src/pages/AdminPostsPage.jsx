import { useEffect, useState } from 'react';
import styled from 'styled-components';
import AdminPostFilterBar from '../components/admin/AdminPostFilterBar';
import AdminPostTable from '../components/admin/AdminPostTable';
import Badge from '../components/common/Badge';
import ConfirmModal from '../components/common/ConfirmModal';
import AdminPageLayout from '../components/layout/AdminPageLayout';
import { deleteAdminPost, getAdminPosts } from '../api/adminPosts';

const PAGE_SIZE = 10;

const AdminBadge = styled(Badge)`
  flex: 0 0 auto;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.border};
`;

function AdminPostsPage() {
  const [query, setQuery] = useState('');
  const [submittedQuery, setSubmittedQuery] = useState('');
  const [posts, setPosts] = useState([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageMeta, setPageMeta] = useState({ total: 0, totalPages: 0 });
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState('');
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteErrorMessage, setDeleteErrorMessage] = useState('');
  const [deleting, setDeleting] = useState(false);
  const [notice, setNotice] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();

    async function loadPosts() {
      setLoading(true);
      setErrorMessage('');
      try {
        const result = await getAdminPosts({
          keyword: submittedQuery,
          page: currentPage,
          size: PAGE_SIZE,
          signal: controller.signal,
        });
        setPosts(result.items);
        setPageMeta(result.meta);
        // AI작성: 삭제 후 현재 페이지가 없어지면 유효한 마지막 페이지를 다시 조회한다.
        if (result.meta.totalPages > 0 && currentPage > result.meta.totalPages) {
          setCurrentPage(result.meta.totalPages);
        }
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setPosts([]);
          setPageMeta({ total: 0, totalPages: 0 });
          setErrorMessage(error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }

    loadPosts();
    return () => controller.abort();
  }, [currentPage, reloadKey, submittedQuery]);

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
      await deleteAdminPost(deleteTarget);
      setDeleteTarget(null);
      setNotice('게시글을 삭제했습니다.');
      // AI작성: 삭제 결과와 페이징 정보를 서버 기준으로 다시 조회한다.
      setReloadKey((current) => current + 1);
    } catch (error) {
      // AI작성: 삭제 대상을 유지해 일시적인 오류 뒤 바로 다시 시도할 수 있게 한다.
      setDeleteErrorMessage(error.message);
    } finally {
      setDeleting(false);
    }
  };

  return (
    <>
      <AdminPageLayout
        title="게시글 관리"
        description="검색과 상세 조회를 통해 서비스 운영 현황을 확인하세요."
        activeItem="posts"
        headingAddon={<AdminBadge>관리자 전용</AdminBadge>}
        contentPaddingTop
      >
          <AdminPostFilterBar
            query={query}
            onQueryChange={setQuery}
            onSearch={handleSearch}
          />
          {errorMessage && <p role="alert">{errorMessage}</p>}
          <AdminPostTable
            posts={posts}
            totalCount={pageMeta.total}
            currentPage={currentPage}
            totalPages={pageMeta.totalPages}
            onPageChange={setCurrentPage}
            onDelete={(postId) => {
              setDeleteErrorMessage('');
              setDeleteTarget(postId);
            }}
            loading={loading}
          />
      </AdminPageLayout>
      {deleteTarget && (
        <ConfirmModal
          title="게시글 삭제"
          description={
            deleteErrorMessage
              ? `삭제하지 못했습니다. ${deleteErrorMessage} 다시 시도하거나 취소하세요.`
              : '이 게시글을 삭제하시겠습니까? 삭제된 게시글은 복구할 수 없습니다.'
          }
          confirmLabel={deleting ? '삭제 중...' : '게시글 삭제'}
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
          title="삭제 완료"
          description={notice}
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setNotice('')}
          onCancel={() => setNotice('')}
        />
      )}
    </>
  );
}

export default AdminPostsPage;
