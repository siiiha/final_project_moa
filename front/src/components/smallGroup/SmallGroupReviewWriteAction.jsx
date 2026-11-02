import { useEffect, useState } from 'react';
import styled from 'styled-components';
import { hasWrittenSmallGroupReview } from '../../api/smallGroups';
import Button from '../common/Button';

// AI작성: 작성 여부 확인 상태와 재시도를 작성 버튼 위치에서 안내한다.
const Status = styled.div`
  font-size: 14px;
  line-height: 1.6;
  color: ${({ theme }) => theme.colors.secondaryText};
`;

// AI작성: 후기 탭 진입 시 새로 마운트하며 확인 전·실패 시에는 작성 버튼을 제공하지 않는다.
function SmallGroupReviewWriteAction({ smallGroupId, onWrite }) {
  const [retry, setRetry] = useState(0);
  const [result, setResult] = useState(null);
  const current = result?.retry === retry ? result : null;

  useEffect(() => {
    const controller = new AbortController();
    hasWrittenSmallGroupReview(smallGroupId, { signal: controller.signal })
      .then((written) => {
        if (!controller.signal.aborted) setResult({ retry, written });
      })
      .catch((error) => {
        if (!controller.signal.aborted) setResult({ retry, error: error.message || '후기 작성 여부를 확인하지 못했습니다.' });
      });
    return () => controller.abort();
  }, [smallGroupId, retry]);

  if (!current) return <Status role="status">후기 작성 여부를 확인하고 있습니다.</Status>;
  if (current.error) {
    return (
      <Status role="alert">
        <p>{current.error}</p>
        <Button type="button" onClick={() => setRetry((value) => value + 1)}>다시 확인</Button>
      </Status>
    );
  }
  if (current.written) return <Status role="status">이미 후기를 작성했습니다.</Status>;
  return <Button type="button" onClick={onWrite}>후기 작성</Button>;
}

export default SmallGroupReviewWriteAction;
