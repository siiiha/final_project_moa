import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import styled from 'styled-components';
import Button from '../components/common/Button';
import AdminMemberDetail from '../components/admin/AdminMemberDetail';
import AdminSidebar from '../components/layout/AdminSidebar';
import { adminMenuItems } from '../mocks/admin';
import {
  getAdminMember,
  releaseAdminMemberRestriction,
  restrictAdminMember,
} from '../api/adminMembers';

const Page = styled.div`
  width: min(100% - 32px, ${({ theme }) => theme.maxWidth});
  margin: 0 auto;
  padding: 26px 0 48px;
`;

const Heading = styled.div`
  margin-left: 218px;

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

function AdminMemberDetailPage() {
  const navigate = useNavigate();
  const { id } = useParams();
  const [member, setMember] = useState(null);
  const [detail, setDetail] = useState(null);
  const [action, setAction] = useState('이용 제한');
  const [restrictionType, setRestrictionType] = useState('7_DAYS');
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    async function loadMember() {
      try {
        const result = await getAdminMember(id, controller.signal);
        setMember(result.member);
        setDetail(result.detail);
        setAction(result.member.accountStatus === 'SUSPENDED' ? '제한 해제' : '이용 제한');
      } catch (error) {
        if (error.code !== 'ERR_CANCELED') {
          setMember(null);
          setDetail(null);
          setErrorMessage(error.message);
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    loadMember();
    return () => controller.abort();
  }, [id, reloadKey]);

  const handleMenuSelect = (menuId) => {
    if (menuId === 'gatherings') {
      navigate('/admin/saving-groups');
      return;
    }

    if (menuId === 'gathering-review') {
      navigate('/admin/saving-groups/applications');
      return;
    }

    if (menuId === 'groups') {
      navigate('/admin/small-groups');
      return;
    }

    if (menuId === 'posts') {
      navigate('/admin/posts');
      return;
    }

    if (menuId === 'comments') {
      navigate('/admin/comments');
      return;
    }

    if (menuId === 'reports') {
      navigate('/admin/reposts');
      return;
    }

    // TODO: Spring Boot 관리자 기능 및 각 관리자 화면이 준비되면 선택한 메뉴의 라우트로 이동합니다.
  };

  const handleStatusSubmit = async (event) => {
    event.preventDefault();
    if (submitting) return;
    if (!reason.trim()) {
      setErrorMessage('처리 사유를 입력해주세요.');
      return;
    }

    setSubmitting(true);
    setMessage('');
    setErrorMessage('');
    try {
      if (action === '이용 제한') {
        await restrictAdminMember(id, restrictionType, reason.trim());
      } else {
        await releaseAdminMemberRestriction(id, reason.trim());
      }
      setReason('');
      setMessage(action === '이용 제한' ? '회원 이용을 제한했습니다.' : '회원 이용 제한을 해제했습니다.');
      // AI작성: 처리된 서버 상태와 제한 종료 시각을 화면에 반영하도록 상세 정보를 다시 조회한다.
      setLoading(true);
      setReloadKey((current) => current + 1);
    } catch (error) {
      setErrorMessage(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading || !member || !detail) {
    return (
      <Page>
        <EmptyCard>
          <p>{loading ? '회원 정보를 불러오는 중입니다.' : errorMessage || '존재하지 않는 회원입니다.'}</p>
          <Button type="button" $variant="secondary" onClick={() => navigate('/admin/members')}>
            회원 목록으로
          </Button>
        </EmptyCard>
      </Page>
    );
  }

  return (
    <Page>
      <Heading>
        <h1>회원 상세</h1>
        <p>{member.name} 회원의 정보와 이용 상태를 관리하세요.</p>
      </Heading>
      <Grid>
        <AdminSidebar items={adminMenuItems} activeItem="members" onSelect={handleMenuSelect} />
        <main>
          <AdminMemberDetail
            member={member}
            detail={detail}
            action={action}
            reason={reason}
            restrictionType={restrictionType}
            submitting={submitting}
            message={message}
            errorMessage={errorMessage}
            onActionChange={setAction}
            onReasonChange={setReason}
            onRestrictionTypeChange={setRestrictionType}
            onSubmit={handleStatusSubmit}
            onBack={() => navigate('/admin/members')}
          />
        </main>
      </Grid>
    </Page>
  );
}

export default AdminMemberDetailPage;
