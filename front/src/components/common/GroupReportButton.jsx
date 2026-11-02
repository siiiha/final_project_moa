import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { createReport, REPORT_TYPES } from '../../api/reports';
import useAuthStore from '../../store/authStore';
import Button from './Button';
import ConfirmModal from './ConfirmModal';
import ReportModal from './ReportModal';

const ReportButton = styled(Button)`
  min-height: 38px;
  padding: 8px 14px;
  flex: 0 0 auto;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

// AI작성: 계모임과 소모임에서 로그인 확인·모달·API 호출·결과 안내를 중복 구현하지 않도록 공통 처리한다.
function GroupReportButton({ groupId, groupLabel }) {
  const navigate = useNavigate();
  const location = useLocation();
  const token = useAuthStore((state) => state.token);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState('');
  const [isComplete, setIsComplete] = useState(false);

  const handleOpen = () => {
    if (!token) {
      const redirect = `${location.pathname}${location.search}`;
      navigate(`/member/login?redirect=${encodeURIComponent(redirect)}`);
      return;
    }
    setErrorMessage('');
    setIsModalOpen(true);
  };

  const handleSubmit = async (reason, selectedTarget) => {
    if (isSubmitting) return;

    setIsSubmitting(true);
    setErrorMessage('');
    try {
      await createReport({
        reportType: selectedTarget.reportType,
        targetId: selectedTarget.targetId,
        reason,
      });
      setIsModalOpen(false);
      setIsComplete(true);
    } catch (error) {
      setErrorMessage(error.message || '신고를 접수하지 못했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <ReportButton type="button" $variant="secondary" onClick={handleOpen}>
        신고하기
      </ReportButton>
      {isModalOpen && (
        <ReportModal
          targets={[{
            reportType: REPORT_TYPES.GROUP,
            targetId: groupId,
            label: `${groupLabel} 신고`,
            description: '모임 운영 및 정보를 신고합니다.',
          }]}
          submitting={isSubmitting}
          errorMessage={errorMessage}
          onSubmit={handleSubmit}
          onCancel={() => {
            if (!isSubmitting) setIsModalOpen(false);
          }}
        />
      )}
      {isComplete && (
        <ConfirmModal
          title="신고 접수 완료"
          description="신고가 접수되었습니다. 관리자가 내용을 확인합니다."
          confirmLabel="확인"
          showCancel={false}
          onConfirm={() => setIsComplete(false)}
          onCancel={() => setIsComplete(false)}
        />
      )}
    </>
  );
}

export default GroupReportButton;
