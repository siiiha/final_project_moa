import { savingGroupAnomalyFixtures } from '../mocks/savingGroupAnomalies';

// AI작성: 실제 요청과 분리된 450ms Mock 조회. 취소 시 타이머·리스너를 정리한다.
export async function getMockSavingGroupAnomalies({ groupId, signal, scenario = 'ready' }) {
  await new Promise((resolve, reject) => {
    const abort = () => {
      clearTimeout(timer);
      signal?.removeEventListener('abort', abort);
      reject(new DOMException('조회 취소', 'AbortError'));
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener('abort', abort);
      resolve();
    }, 450);
    if (signal?.aborted) abort();
    else signal?.addEventListener('abort', abort, { once: true });
  });
  if (scenario === 'error') throw new Error('분석 결과를 불러오지 못했습니다. 다시 시도해 주세요.');
  if (scenario === 'forbidden') {
    const error = new Error('모임장 또는 승인된 운영진만 분석 결과를 조회할 수 있습니다.');
    error.status = 403;
    throw error;
  }
  return {
    mode: 'mock',
    items: scenario === 'empty' ? [] : structuredClone(savingGroupAnomalyFixtures[groupId] || []),
  };
}
