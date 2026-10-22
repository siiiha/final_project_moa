import { useEffect, useMemo, useRef, useState } from 'react';
import styled from 'styled-components';
import Badge from '../common/Badge';
import Button from '../common/Button';
import { getMockSavingGroupAnomalies } from '../../api/savingGroupAnomalies';
// AI작성: 계좌·기간 표시는 저장 결과 스냅샷 메타데이터와 같은 값을 사용한다.
import { savingGroupReplayInfo } from '../../mocks/savingGroupAnomalies';

const Panel = styled.section`
  margin-top: 22px;
`;

const Header = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const Title = styled.h2`
  color: ${({ theme }) => theme.colors.text};
  font-size: 20px;
  font-weight: 800;
  letter-spacing: -0.9px;
`;

const Description = styled.p`
  margin-top: 8px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 12px;
`;

const DownloadButton = styled(Button)`
  min-height: 36px;
  padding: 7px 12px;
  font-size: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const Summary = styled.div`
  margin-top: 16px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const SummaryCard = styled.section`
  min-height: 76px;
  padding: 14px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const SummaryLabel = styled.p`
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;
`;

const SummaryValue = styled.strong`
  margin-top: 7px;
  display: block;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.7px;
`;

const FilterForm = styled.form`
  margin-top: 16px;
  display: grid;
  grid-template-columns: minmax(140px, 1fr) minmax(140px, 1fr) minmax(120px, 1fr) auto;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    grid-template-columns: 1fr;
  }
`;

const Field = styled.label`
  display: grid;
  gap: 7px;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
  font-weight: 700;
`;

const Input = styled.input`
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const Select = styled.select`
  width: 100%;
  height: 40px;
  padding: 0 11px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const SearchButton = styled(Button)`
  min-height: 40px;
  padding: 8px 17px;
  align-self: end;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    grid-column: 1 / -1;
  }
`;

const ListCard = styled.section`
  margin-top: 14px;
  padding: 18px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 14px;
  }
`;

const ListTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const TableWrap = styled.div`
  margin-top: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: none;
  }
`;

const Table = styled.table`
  width: 100%;
  overflow: hidden;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-collapse: separate;
  border-spacing: 0;
  border-radius: ${({ theme }) => theme.radius.sm};
  table-layout: fixed;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    text-align: center;
    vertical-align: middle;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(1),
  td:nth-child(3) {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const TypeBadge = styled(Badge)`
  min-height: 22px;
  padding: 3px 7px;
  color: ${({ $type, theme }) => ($type === '출금' ? theme.colors.danger : theme.colors.primary)};
  background: ${({ $type, theme }) => ($type === '출금' ? theme.colors.dangerSoft : theme.colors.primarySoft)};
  font-size: 10px;
`;

const Amount = styled.strong`
  color: ${({ $type, theme }) => ($type === '출금' ? theme.colors.danger : theme.colors.primary)};
  font-size: 12px;
`;

const MobileList = styled.div`
  margin-top: 14px;
  display: none;
  gap: 10px;

  @media (max-width: ${({ theme }) => theme.breakpoints.tablet}) {
    display: grid;
  }
`;

const MobileItem = styled.article`
  padding: 14px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
`;

const MobileTop = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
`;

const MobileDescription = styled.strong`
  margin-top: 10px;
  display: block;
  color: ${({ theme }) => theme.colors.text};
  font-size: 12px;
`;

const MobileInfo = styled.dl`
  margin-top: 10px;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 7px 10px;
  color: ${({ theme }) => theme.colors.secondaryText};
  font-size: 11px;

  dt {
    color: ${({ theme }) => theme.colors.muted};
    font-weight: 700;
  }
`;

const Empty = styled.p`
  padding: 42px 16px;
  color: ${({ theme }) => theme.colors.muted};
  text-align: center;
  font-size: 13px;
`;

const Pagination = styled.nav`
  margin-top: 18px;
  display: flex;
  justify-content: center;
  gap: 4px;
`;

const PageButton = styled(Button)`
  min-width: 30px;
`;

const Notice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

// AI작성: 모델 경고와 이력·품질 확인을 색상뿐 아니라 서로 다른 문구로 구분한다.
const AnalysisBadge = styled.span`
  display: inline-flex;
  padding: 7px 9px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  color: ${({ $warning, theme }) => ($warning ? theme.colors.danger : theme.colors.text)};
  background: ${({ $warning, theme }) => ($warning ? theme.colors.dangerSoft : theme.colors.background)};
`;
const AnalysisTools = styled.div`
  display: flex;
  align-items: end;
  flex-wrap: wrap;
  gap: 12px;
  margin: 16px 0;
  label { min-width: 180px; }
  select, button { min-height: 44px; font-size: 15px; }
`;
const AnalysisTable = styled(Table)`
  th, td { font-size: 14px; overflow-wrap: anywhere; }
`;
const AnalysisDialog = styled.dialog`
  width: min(620px, calc(100% - 32px));
  max-height: 85vh;
  overflow-y: auto;
  margin: auto;
  padding: 24px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: 14px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  &::backdrop { background: rgba(20, 37, 32, 0.4); }
  h3 { font-size: 22px; margin-bottom: 16px; }
  p { margin: 14px 0; font-size: 16px; line-height: 1.7; }
  dl { display: grid; grid-template-columns: 110px minmax(0, 1fr); gap: 12px; margin: 20px 0; font-size: 15px; }
  dd { margin: 0; overflow-wrap: anywhere; }
  button { min-height: 44px; font-size: 16px; }
`;
const AccessibleNotice = styled(Notice)`
  font-size: 15px;
  line-height: 1.7;
`;
const PAGE_SIZE = 10;

// AI작성: 미분석·실패·알 수 없는 응답을 정상 거래로 오인하지 않는다.
export function getAnalysisLabel(analysis) {
  if (!analysis) return '분석 결과 없음';
  if (analysis.status === 'PENDING') return '분석 대기';
  if (analysis.status === 'FAILED') return '분석 실패';
  if (analysis.status !== 'COMPLETED' || typeof analysis.isAnomaly !== 'boolean'
    || !Number.isFinite(analysis.anomalyScore) || !Number.isFinite(analysis.threshold)) return '결과 확인 필요';
  return analysis.isAnomaly ? '모델 경고' : '모델 경고 없음';
}

// AI작성: 기술 플래그를 사용자 설명으로 바꾸되 이상·횡령 판정으로 표현하지 않는다.
const qualityLabels = {
  bank_balance_missing: '거래 후 잔액 정보가 부족합니다.',
  bank_balance_arithmetic_mismatch: '은행 제공 잔액의 정합성 확인이 필요합니다.',
  ledger_balance_mismatch: '내부 장부와 은행 잔액 비교가 필요합니다.',
  same_timestamp_order_unknown: '같은 시각 거래의 발생 순서를 확인할 수 없습니다.',
  source_sequence_missing: '원천 거래 순번이 없습니다.',
  same_direction_history_under_5: '비교할 같은 방향의 과거 거래가 적습니다.',
  balance_before_nonpositive: '거래 직전 잔액을 확인해야 합니다.',
  withdrawal_balance_unavailable: '출금 직전 잔액을 알 수 없습니다.',
  withdrawal_exceeds_balance_before: '출금액과 직전 잔액의 정합성 확인이 필요합니다.',
};

const formatAmount = (amount) => `${amount > 0 ? '+' : '-'}${Math.abs(amount).toLocaleString()}원`;

// AI작성: 실제 모델의 저장 결과 재생 화면이다. 합성 계좌이며 실제 모임 권한 검사를 대신하지 않는다.
function SavingGroupTransactionHistoryPanel({ groupId }) {
  // AI작성: 저장 결과 기간을 기본 조회 범위로 사용한다.
  const [draftFilters, setDraftFilters] = useState({ startDate: savingGroupReplayInfo.periodStart, endDate: savingGroupReplayInfo.periodEnd, type: '전체' });
  const [appliedFilters, setAppliedFilters] = useState(draftFilters);
  const [currentPage, setCurrentPage] = useState(1);
  const [isDownloadPreparing, setIsDownloadPreparing] = useState(false);
  const [analysisFilter, setAnalysisFilter] = useState('전체');
  const [scenario, setScenario] = useState('ready');
  const [requestVersion, setRequestVersion] = useState(0);
  const [query, setQuery] = useState({ status: 'loading', items: [], error: '' });
  const [selected, setSelected] = useState(null);
  const [filterError, setFilterError] = useState('');
  const detailRef = useRef(null);

  // AI작성: 모임 변경·재조회 시 이전 요청을 취소하고 오래된 결과 표시를 차단한다.
  useEffect(() => {
    const controller = new AbortController();
    setQuery({ status: 'loading', items: [], error: '' });
    setSelected(null);
    setCurrentPage(1);
    getMockSavingGroupAnomalies({ groupId, signal: controller.signal, scenario })
      .then(({ items }) => {
        if (!controller.signal.aborted) setQuery({ status: 'ready', items, error: '' });
      })
      .catch((error) => {
        if (!controller.signal.aborted) setQuery({ status: error.status === 403 ? 'forbidden' : 'error', items: [], error: error.message });
      });
    return () => controller.abort();
  }, [groupId, scenario, requestVersion]);

  // AI작성: native dialog의 포커스 잠금·Escape 닫기를 활용한다.
  useEffect(() => {
    if (selected && !detailRef.current?.open) detailRef.current?.showModal();
  }, [selected]);

  const filteredTransactions = useMemo(() => query.items.filter((transaction) => {
    const matchesStartDate = !appliedFilters.startDate || transaction.date >= appliedFilters.startDate;
    const matchesEndDate = !appliedFilters.endDate || transaction.date <= appliedFilters.endDate;
    const matchesType = appliedFilters.type === '전체' || transaction.type === appliedFilters.type;
    return matchesStartDate && matchesEndDate && matchesType;
  }), [appliedFilters, query.items]);
  const analysisTransactions = filteredTransactions.filter((transaction) => {
    if (analysisFilter === '전체') return true;
    if (analysisFilter === '이력·품질 확인') return transaction.analysis?.requiresReview === true;
    return getAnalysisLabel(transaction.analysis) === analysisFilter;
  });
  const totalPages = Math.max(1, Math.ceil(analysisTransactions.length / PAGE_SIZE));
  const visibleTransactions = analysisTransactions.slice((currentPage - 1) * PAGE_SIZE, currentPage * PAGE_SIZE);
  const sumAmount = (type) => filteredTransactions.filter((item) => item.type === type).reduce((total, item) => total + Math.abs(item.amount), 0);
  const latestTransaction = [...query.items].sort((a, b) => `${b.date}T${b.time}`.localeCompare(`${a.date}T${a.time}`))[0];

  const handleSearch = (event) => {
    event.preventDefault();
    // AI작성: 잘못된 날짜 범위는 적용하지 않고 기존 조회 조건을 유지한다.
    if (draftFilters.startDate && draftFilters.endDate && draftFilters.startDate > draftFilters.endDate) {
      setFilterError('종료일은 시작일보다 빠를 수 없습니다.');
      return;
    }
    setFilterError('');
    setAppliedFilters(draftFilters);
    setCurrentPage(1);
  };

  const handleDownload = () => {
    // AI작성: 다운로드 미연동 상태는 실제 파일 저장 성공으로 표시하지 않는다.
    setIsDownloadPreparing(true);
  };

  const handlePageChange = (page) => {
    setCurrentPage(Math.min(Math.max(page, 1), totalPages));
  };

  return (
    <Panel>
      <Header>
        <div>
          <Title>공용 통장 거래내역</Title>
          <Description>모임 자금의 입출금 내역입니다. 잔액과 변동 내역을 확인할 수 있습니다.</Description>
          {/* AI작성: 실제 추론 결과와 실시간 API 연결을 혼동하지 않도록 출처·임시 매핑을 표시한다. */}
          <AccessibleNotice>합성 계좌 {savingGroupReplayInfo.sourceAccountId}의 {savingGroupReplayInfo.periodStart} ~ {savingGroupReplayInfo.periodEnd} 저장된 모델 결과를 재생합니다. 현재 모임은 시연 위치이며 실제 계좌 연결이 아닙니다. 실시간 분석·은행·권한 서버는 미연결입니다.</AccessibleNotice>
        </div>
        <DownloadButton type="button" $variant="secondary" onClick={handleDownload}>내역 다운로드 안내</DownloadButton>
      </Header>

      <Summary>
        <SummaryCard>
          <SummaryLabel>재생 계좌의 마지막 거래 후 잔액</SummaryLabel>
          <SummaryValue>{query.status !== 'ready' || !latestTransaction ? '—' : `${latestTransaction.balance.toLocaleString('ko-KR')}원`}</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>조회 입금</SummaryLabel>
          <SummaryValue>{query.status === 'ready' ? `${sumAmount('입금').toLocaleString('ko-KR')}원` : '—'}</SummaryValue>
        </SummaryCard>
        <SummaryCard>
          <SummaryLabel>조회 출금</SummaryLabel>
          <SummaryValue>{query.status === 'ready' ? `${sumAmount('출금').toLocaleString('ko-KR')}원` : '—'}</SummaryValue>
        </SummaryCard>
      </Summary>

      <FilterForm onSubmit={handleSearch}>
        <Field>
          시작일
          <Input
            type="date"
            value={draftFilters.startDate}
            onChange={(event) => setDraftFilters((current) => ({ ...current, startDate: event.target.value }))}
          />
        </Field>
        <Field>
          종료일
          <Input
            type="date"
            value={draftFilters.endDate}
            onChange={(event) => setDraftFilters((current) => ({ ...current, endDate: event.target.value }))}
          />
        </Field>
        <Field>
          구분
          <Select value={draftFilters.type} onChange={(event) => setDraftFilters((current) => ({ ...current, type: event.target.value }))}>
            <option value="전체">전체</option>
            <option value="입금">입금</option>
            <option value="출금">출금</option>
          </Select>
        </Field>
        <SearchButton type="submit">조회</SearchButton>
      </FilterForm>
      {filterError && <AccessibleNotice role="alert">{filterError}</AccessibleNotice>}
      <AnalysisTools>
        <Field>분석 상태
          <Select value={analysisFilter} onChange={(event) => { setAnalysisFilter(event.target.value); setCurrentPage(1); }}>
            {/* AI작성: 저장된 완료 결과만 재생하므로 임의 대기·실패 행을 만들지 않는다. */}
            {['전체', '모델 경고', '모델 경고 없음', '이력·품질 확인'].map((value) => <option key={value}>{value}</option>)}
          </Select>
        </Field>
        <Field>조회 상태 시연 (실제 장애 아님)
          <Select value={scenario} onChange={(event) => setScenario(event.target.value)}>
            <option value="ready">저장 모델 결과</option><option value="empty">빈 목록</option>
            <option value="error">조회 오류</option><option value="forbidden">권한 없음</option>
          </Select>
        </Field>
        <Button type="button" $variant="secondary" onClick={() => setRequestVersion((value) => value + 1)}>다시 조회</Button>
      </AnalysisTools>
      <AccessibleNotice>모델 경고는 평소와 다른 패턴을 뜻하며 사고 판정이 아닙니다. 이력·품질 확인은 모델 경고와 별개입니다.</AccessibleNotice>

      <ListCard>
        <ListTitle>입출금·분석 목록 {query.status === 'ready' ? `(${analysisTransactions.length}건)` : ''}</ListTitle>
        {query.status === 'loading' ? <Empty role="status">거래와 분석 결과를 불러오는 중입니다.</Empty>
          : query.status === 'error' || query.status === 'forbidden' ? <Empty role="alert">{query.error} 위의 다시 조회 버튼으로 재시도할 수 있습니다.</Empty>
          : visibleTransactions.length ? (
          <>
            <TableWrap>
              <AnalysisTable>
                <colgroup>
                  <col width="17%" /><col width="8%" /><col width="17%" />
                  <col width="14%" /><col width="14%" /><col width="30%" />
                </colgroup>
                <thead>
                  <tr>
                    <th scope="col">일시</th>
                    <th scope="col">구분</th>
                    <th scope="col">내용</th>
                    <th scope="col">금액</th>
                    <th scope="col">잔액</th>
                    <th scope="col">분석 상태·상세</th>
                  </tr>
                </thead>
                <tbody>
                  {visibleTransactions.map((transaction) => (
                    <tr key={transaction.id}>
                      <td>{`${transaction.date.slice(5).replace('-', '.')} ${transaction.time}`}</td>
                      <td><TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge></td>
                      <td>{transaction.description}</td>
                      <td><Amount $type={transaction.type}>{formatAmount(transaction.amount)}</Amount></td>
                      <td>{`${transaction.balance.toLocaleString()}원`}</td>
                      <td>
                        <AnalysisBadge $warning={getAnalysisLabel(transaction.analysis) === '모델 경고'}>{getAnalysisLabel(transaction.analysis)}</AnalysisBadge>
                        {transaction.analysis?.requiresReview && <p>이력·품질 확인</p>}
                        <Button type="button" $variant="secondary" onClick={() => setSelected(transaction)} aria-label={`${transaction.date} ${transaction.time} 거래 분석 상세`}>상세</Button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </AnalysisTable>
            </TableWrap>

            <MobileList>
              {visibleTransactions.map((transaction) => (
                <MobileItem key={transaction.id}>
                  <MobileTop>
                    <TypeBadge $type={transaction.type}>{transaction.type}</TypeBadge>
                    <Amount $type={transaction.type}>{formatAmount(transaction.amount)}</Amount>
                  </MobileTop>
                  <MobileDescription>{transaction.description}</MobileDescription>
                  <MobileInfo>
                    <dt>일시</dt>
                    <dd>{`${transaction.date} ${transaction.time}`}</dd>
                    <dt>잔액</dt>
                    <dd>{`${transaction.balance.toLocaleString()}원`}</dd>
                  </MobileInfo>
                  <AnalysisBadge $warning={getAnalysisLabel(transaction.analysis) === '모델 경고'}>{getAnalysisLabel(transaction.analysis)}</AnalysisBadge>
                  {transaction.analysis?.requiresReview && <p>이력·품질 확인</p>}
                  <Button type="button" $variant="secondary" onClick={() => setSelected(transaction)}>분석 상세</Button>
                </MobileItem>
              ))}
            </MobileList>
          </>
        ) : (
          <Empty>조회 조건에 맞는 거래 내역이 없습니다.</Empty>
        )}

        {query.status === 'ready' && totalPages > 1 && (
          <Pagination aria-label="거래 내역 페이지 이동">
            <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === 1} onClick={() => handlePageChange(currentPage - 1)}>
              이전
            </PageButton>
            {Array.from({ length: totalPages }, (_, index) => index + 1).map((page) => (
              <PageButton
                key={page}
                type="button"
                $variant={page === currentPage ? 'primary' : 'secondary'}
                $size="small"
                aria-current={page === currentPage ? 'page' : undefined}
                onClick={() => handlePageChange(page)}
              >
                {page}
              </PageButton>
            ))}
            <PageButton type="button" $variant="secondary" $size="small" disabled={currentPage === totalPages} onClick={() => handlePageChange(currentPage + 1)}>
              다음
            </PageButton>
          </Pagination>
        )}
        {isDownloadPreparing && <AccessibleNotice role="status">파일 다운로드는 아직 연결되지 않았습니다. 현재 화면은 합성 계좌의 저장 모델 결과를 조회합니다.</AccessibleNotice>}
      </ListCard>
      <AnalysisDialog ref={detailRef} aria-labelledby="transaction-analysis-title" onClose={() => setSelected(null)}>
        {selected && <>
          <h3 id="transaction-analysis-title">거래 분석 상세</h3>
          <AnalysisBadge $warning={getAnalysisLabel(selected.analysis) === '모델 경고'}>{getAnalysisLabel(selected.analysis)}</AnalysisBadge>
          <dl>
            <dt>거래일시</dt><dd>{selected.date} {selected.time}</dd>
            <dt>금액</dt><dd>{formatAmount(selected.amount)}</dd>
            <dt>내용</dt><dd>{selected.description}</dd>
            <dt>분석 차수</dt><dd>{selected.analysis?.revisionNo ?? '저장 결과에 미기록'}</dd>
            <dt>모델 점수</dt><dd>{selected.analysis?.status === 'COMPLETED' && Number.isFinite(selected.analysis.anomalyScore) ? selected.analysis.anomalyScore.toFixed(6) : '미산출'}</dd>
            <dt>경고 기준</dt><dd>{selected.analysis?.status === 'COMPLETED' && Number.isFinite(selected.analysis.threshold) ? `${selected.analysis.threshold.toFixed(6)} 초과` : '미산출'}</dd>
            <dt>분석 시각</dt><dd>{selected.analysis?.analyzedAt ? new Date(selected.analysis.analyzedAt).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' }) : '저장 결과에 미기록'}</dd>
            <dt>합성 계좌</dt><dd>{selected.sourceAccountId}</dd>
            <dt>모델 버전</dt><dd>{selected.analysis?.modelVersion || '—'}</dd>
          </dl>
          {selected.analysis?.status === 'PENDING' && <p>아직 분석하지 않았습니다. 경고 없음으로 판단할 수 없습니다.</p>}
          {selected.analysis?.status === 'FAILED' && <p>분석 서비스 응답을 받지 못했습니다. 운영 서버 연결 후 재처리가 필요합니다. 원래 거래는 유지됩니다.</p>}
          {selected.analysis?.coldStart && <p>비교할 과거 거래가 부족합니다. 결과 해석에 주의해 주세요.</p>}
          {selected.analysis?.qualityFlags?.map((flag) => <p key={flag}>{qualityLabels[flag] || '거래 정보의 추가 확인이 필요합니다.'}</p>)}
          {selected.analysis?.requiresReview && !selected.analysis?.coldStart && !selected.analysis?.qualityFlags?.length && <p>거래 정보의 추가 확인이 필요합니다.</p>}
          <p>합성 거래를 실제 모델이 분석해 저장한 결과입니다. 현재 화면에서 새로 분석한 결과는 아닙니다. 점수는 확률이 아니며 연체·횡령 판단이나 실제 계좌의 안전 보장에 사용할 수 없습니다.</p>
          <Button type="button" onClick={() => detailRef.current?.close()}>닫기</Button>
        </>}
      </AnalysisDialog>
    </Panel>
  );
}

export default SavingGroupTransactionHistoryPanel;
