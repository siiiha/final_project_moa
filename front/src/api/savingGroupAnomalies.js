import { savingGroupAnomalyFixtures } from '../mocks/savingGroupAnomalies';

// AI작성: 은행 입출금/임의 거래 입력이 아니라 서버에 정해 둔 다음 합성 거래만 수신한다.
// 같은 토큰을 재전송해도 다음 거래를 중복 소비하지 않는다. 사용 가능한 모드에서만 표시한다.
export async function receiveNextMockTransaction({ groupId, expectedNextToken, signal }) {
  if (!import.meta.env.DEV || Number(groupId) !== 12 || !/^[a-f0-9]{64}$/.test(expectedNextToken || '')) {
    throw new Error('새 합성 거래를 수신할 수 없는 상태입니다. 먼저 로컬 결과를 조회해 주세요.');
  }
  const response = await fetch('/__moa_test__/groups/12/replay-next', {
    method: 'POST', signal, headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ expectedNextToken }),
  });
  if (!response.ok) throw new Error('합성 거래 처리 실패 (HTTP ' + response.status + '). 다시 조회해 상태를 확인해 주세요.');
  const body = await response.json();
  if (body.received !== true || !['COMPLETED', 'FAILED'].includes(body.analysisStatus)) throw new Error('합성 거래 처리 응답을 확인해 주세요.');
  return body;
}


// AI작성: 저장 Mock과 분리된 로컬 합성 결과 조회다. 개발 서버 프록시만 거치며 실패 시 Mock으로 바꾸지 않는다.
export async function getLocalSavingGroupAnomalies({ groupId, signal }) {
  if (!import.meta.env.DEV) throw new Error('로컬 합성 서버 조회는 개발 모드에서만 사용할 수 있습니다.');
  if (!Number.isSafeInteger(Number(groupId)) || Number(groupId) <= 0) throw new Error('모임 번호를 확인해 주세요.');
  let response;
  try {
    response = await fetch('/__moa_test__/groups/' + encodeURIComponent(groupId) + '/transactions', {
      signal, cache: 'no-store', headers: { Accept: 'application/json' },
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new Error('로컬 합성 서버에 연결할 수 없습니다. 테스트 서버 실행 여부를 확인해 주세요.');
  }
  if (!response.ok) throw new Error('로컬 합성 결과 조회 실패 (HTTP ' + response.status + '). 저장 결과로 자동 대체하지 않습니다.');
  let body;
  try { body = await response.json(); }
  catch { throw new Error('로컬 서버의 JSON 응답을 확인해 주세요.'); }
  // AI작성: 일반 서버/HTML/잘못된 계좌 응답을 합성 분석 결과로 조용히 받아들이지 않는다.
  if (body.mode !== 'local-test' || body.meta?.syntheticOnly !== true || !Array.isArray(body.items)
    || !body.items.every((item) => item.groupId === Number(groupId) && item.sourceAccountId === 10013
      && Number.isSafeInteger(item.amount) && (item.balance === null || Number.isSafeInteger(item.balance))
      && ['입금', '출금'].includes(item.type) && typeof item.date === 'string' && typeof item.time === 'string'
      && typeof item.description === 'string' && item.analysis && Array.isArray(item.analysis.qualityFlags))) {
    throw new Error('로컬 합성 결과 형식 또는 모임 연결을 확인해 주세요.');
  }
  return body;
}


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
