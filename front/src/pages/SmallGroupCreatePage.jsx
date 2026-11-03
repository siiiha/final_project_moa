import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import styled from 'styled-components';
import { createSmallGroup } from '../api/smallGroups';
import { getMemberRegions } from '../api/member';
import SmallGroupCreateForm from '../components/smallGroup/SmallGroupCreateForm';
import { smallGroupInterests } from '../mocks/smallGroups';

// AI작성: 페이지 최대 너비를 폼 카드와 맞춰 제목·설명·경로 안내의 왼쪽을 정렬한다.
const Page = styled.div`
  width: min(100% - 32px, 1040px);
  margin: 0 auto;
  padding: 24px 0 48px;
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
  width: min(100%, 1040px);
  margin: 18px auto 0;
`;

const ErrorNotice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.danger};
  background: ${({ theme }) => theme.colors.dangerSoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
`;

function SmallGroupCreatePage() {
  const navigate = useNavigate();
  const [submissionError, setSubmissionError] = useState('');
  const [regions, setRegions] = useState([]);
  const [isRegionLoading, setIsRegionLoading] = useState(true);
  const [regionLoadError, setRegionLoadError] = useState('');
  // AI작성: 다른 입력을 유지한 채 기존 지역 API 조회를 재시도한다.
  const [regionRetry, setRegionRetry] = useState(0);

  useEffect(() => {
    let isCurrentPage = true;

    // AI작성: 계모임·검색 화면과 같은 시·구·동 정보를 조회하고 빈 목록도 오류로 안내한다.
    const loadRegions = async () => {
      try {
        const loadedRegions = await getMemberRegions();
        if (!Array.isArray(loadedRegions) || loadedRegions.length === 0) {
          throw new Error('등록된 활동 지역이 없습니다. 다시 시도해 주세요.');
        }
        if (isCurrentPage) setRegions(loadedRegions);
      } catch {
        if (isCurrentPage) {
          setRegionLoadError('활동 지역을 불러오지 못했습니다. 다시 시도해 주세요.');
        }
      } finally {
        if (isCurrentPage) setIsRegionLoading(false);
      }
    };

    loadRegions();
    return () => {
      isCurrentPage = false;
    };
  }, [regionRetry]);

  // AI작성: 기존 폼 name을 백엔드 소모임 생성 DTO 필드명으로 변환한다.
  const handleSubmit = async (event) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    setSubmissionError('');

    try {
      const created = await createSmallGroup({
        groupName: formData.get('name'),
        regionName: formData.get('region'),
        likeCategory: formData.get('interest'),
        description: formData.get('description'),
        maxMembers: Number(formData.get('capacity')),
        meetDate: formData.get('meetingDate'),
        location: formData.get('place'),
      });
      // AI작성: 상세 API가 small_group.small_group_id를 경로 변수로 사용하므로 생성 응답의 식별값만 사용한다.
      if (created?.smallGroupId == null) {
        throw new Error('생성된 소모임 정보를 확인하지 못했습니다. 다시 시도해 주세요.');
      }
      // AI작성: 뒤로 가기로 빈 생성 폼을 다시 제출하지 않도록 상세 화면으로 현재 기록을 교체한다.
      navigate(`/small-groups/${encodeURIComponent(created.smallGroupId)}`, { replace: true });
    } catch (error) {
      setSubmissionError(error.message || '소모임 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.');
    }
  };

  return (
    <Page>
      <Breadcrumb>소모임 &gt; 소모임 목록 &gt; 소모임 만들기</Breadcrumb>
      <Heading>
        <h1>소모임 만들기</h1>
        <p>공통의 관심사를 이웃과 함께하는 모임을 만들어보세요.</p>
      </Heading>
      <Content>
        <SmallGroupCreateForm
          regions={regions}
          interests={smallGroupInterests}
          isRegionLoading={isRegionLoading}
          regionLoadError={regionLoadError}
          onRegionRetry={() => {
            setRegions([]);
            setRegionLoadError('');
            setIsRegionLoading(true);
            setRegionRetry((value) => value + 1);
          }}
          onSubmit={handleSubmit}
          onCancel={() => navigate('/small-groups')}
        />
        {submissionError && <ErrorNotice role="alert">{submissionError}</ErrorNotice>}
      </Content>
    </Page>
  );
}

export default SmallGroupCreatePage;
