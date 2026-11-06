import { useEffect, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { getSavingGroupAccount, getSavingGroupTransactions } from '../../api/savingGroups.js';

// AI작성: 거래내역은 큰 글씨·명확한 입력 단위와 모바일 목록으로 제공한다.
const Panel = styled.section
`
  margin-top: 22px;
  color: ${({ theme }) => theme.colors.text};
  h2 { font-size: 24px; margin-bottom: 12px; }
  p { font-size: 16px; line-height: 1.6; }
  table { width: 100%; border-collapse: collapse; margin-top: 20px; }
  th, td { padding: 16px 10px; text-align: left; border-bottom: 1px solid ${({ theme }) => theme.colors.border}; font-size: 16px; }
  th { background: ${({ theme }) => theme.colors.background}; }
  @media (max-width: 720px) { table { display: none; } }
`;
const FilterForm = styled.form
`
  display: flex; align-items: end; gap: 16px; flex-wrap: wrap;
  margin-top: 20px; padding: 20px;
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.md};
  label { display: grid; gap: 8px; font-size: 16px; flex: 1 1 150px; }
  input, select { width: 100%; min-height: 48px; padding: 10px; font-size: 16px;
    color: ${({ theme }) => theme.colors.text}; background: ${({ theme }) => theme.colors.white};
    border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: 8px; }
  button { min-height: 48px; font-size: 16px; }
`;
const MobileList = styled.div
`
  display: none;
  @media (max-width: 720px) { display: grid; gap: 14px; margin-top: 20px; }
  article { padding: 18px; border: 1px solid ${({ theme }) => theme.colors.border}; border-radius: 12px; }
  dl { display: grid; grid-template-columns: 80px 1fr; gap: 12px; margin: 0; font-size: 16px; }
  dd { margin: 0; overflow-wrap: anywhere; }
`;
const Pagination = styled.nav
`
  display: flex; justify-content: center; flex-wrap: wrap; gap: 8px; margin-top: 24px;
  button { min-width: 44px; min-height: 44px; font-size: 16px; }
`;

const INITIAL_FILTERS = { sort: 'LATEST', transactionType: '', minAmount: '', maxAmount: '' };
// AI작성: 금액 부호는 유형으로 표시하고 저장되지 않은 거래 후 잔액은 추정하지 않는다.
const money = value => value === null ? '확인 불가' : value.toLocaleString('ko-KR') + '원';
const typeLabel = row => row.transactionType === 'DEPOSIT' ? '입금' : '출금';
const amountLabel = row => (row.transactionType === 'DEPOSIT' ? '+' : '-') + money(row.amount);
const dateLabel = row => row.transactedAt.replace('T', ' ').slice(0, 19);

// AI작성: 상태별 화면을 분리해 로딩·권한 오류에서 이전 거래가 노출되지 않도록 한다.
export function SavingGroupTransactionHistoryView({ state, draftFilters, onFilterChange, onSearch, onPageChange,
  onRetry, validationMessage }) {
  const successful = state.status === 'success';
  const items = successful ? state.items : [];
  const meta = successful ? state.meta : null;
  const firstPage = meta ? Math.max(1, Math.min(meta.page - 2, meta.totalPages - 4)) : 1;
  const pages = meta ? Array.from({ length: Math.min(5, meta.totalPages) }, (_, index) => firstPage + index) : [];
  return <Panel aria-label="공용 통장 거래내역" aria-busy={state.status === 'loading'}>
    <h2>공용 통장 거래내역</h2>
    <p>서비스 내부에 기록된 거래입니다. 은행의 실시간 거래내역과 다를 수 있습니다.</p>
    {successful && <p>현재 내부 장부 잔액: {state.account ? money(state.account.balance) : '확인 불가'}</p>}
    {successful && state.balanceError && <p role="alert">현재 잔액을 불러오지 못했습니다.
      <Button type="button" onClick={onRetry}>다시 조회</Button></p>}
    <FilterForm onSubmit={onSearch}>
      <label>정렬
        <select value={draftFilters.sort} onChange={event => onFilterChange('sort', event.target.value)}>
          <option value="LATEST">최신순</option><option value="OLDEST">과거순</option>
        </select>
      </label>
      <label>입출금 구분
        <select value={draftFilters.transactionType} onChange={event => onFilterChange('transactionType', event.target.value)}>
          <option value="">전체</option><option value="DEPOSIT">입금</option><option value="WITHDRAWAL">출금</option>
        </select>
      </label>
      <label>최소 금액 (원)
        <input type="number" min="0" max="9999999999999.99" step="0.01" inputMode="decimal" placeholder="예: 10000"
          value={draftFilters.minAmount} onChange={event => onFilterChange('minAmount', event.target.value)} />
      </label>
      <label>최대 금액 (원)
        <input type="number" min="0" max="9999999999999.99" step="0.01" inputMode="decimal" placeholder="예: 40000"
          value={draftFilters.maxAmount} onChange={event => onFilterChange('maxAmount', event.target.value)} />
      </label>
      <Button type="submit">조회</Button>
    </FilterForm>
    <p>최소·최대 금액을 포함해 조회합니다. 금액을 비우면 해당 범위를 제한하지 않습니다.</p>
    {validationMessage && <p role="alert">{validationMessage}</p>}
    {state.status === 'loading' ? <p role="status">거래내역을 불러오는 중입니다.</p>
      : state.status === 'error' ? <div role="alert"><p>{state.message}</p>
        <Button type="button" onClick={onRetry}>거래내역 다시 조회</Button></div>
      : items.length === 0 ? <p role="status">조회 조건에 맞는 거래내역이 없습니다.</p>
      : <>
        <p>총 {meta.total.toLocaleString('ko-KR')}건 · {meta.page}페이지</p>
        <table><caption>공용 계좌 입출금 목록</caption><thead><tr>
          <th scope="col">거래일시</th><th scope="col">거래 유형</th><th scope="col">금액</th><th scope="col">거래 후 잔액</th>
        </tr></thead><tbody>{items.map((row, index) => <tr key={index}>
          <td>{dateLabel(row)}</td><td>{typeLabel(row)}</td><td>{amountLabel(row)}</td><td>{money(row.balanceAfter)}</td>
        </tr>)}</tbody></table>
        <MobileList>{items.map((row, index) => <article key={index}><dl>
          <dt>거래일시</dt><dd>{dateLabel(row)}</dd><dt>거래 유형</dt><dd>{typeLabel(row)}</dd>
          <dt>금액</dt><dd>{amountLabel(row)}</dd><dt>거래 후 잔액</dt><dd>{money(row.balanceAfter)}</dd>
        </dl></article>)}</MobileList>
      </>}
    {meta?.totalPages > 1 && <Pagination aria-label="거래내역 페이지 이동">
      <Button type="button" disabled={meta.page <= 1} onClick={() => onPageChange(meta.page - 1)}>이전</Button>
      {pages.map(page => <Button key={page} type="button" $variant={page === meta.page ? 'primary' : 'secondary'}
        aria-current={page === meta.page ? 'page' : undefined} onClick={() => onPageChange(page)}>{page}</Button>)}
      <Button type="button" disabled={meta.page >= meta.totalPages} onClick={() => onPageChange(meta.page + 1)}>다음</Button>
    </Pagination>}
  </Panel>;
}

// AI작성: 조건·모임 변경 시 이전 요청을 취소하고 동일 요청의 응답만 화면에 반영한다.
export default function SavingGroupTransactionHistoryPanel({ groupId }) {
  const [draftFilters, setDraftFilters] = useState(INITIAL_FILTERS);
  const [query, setQuery] = useState({ filters: INITIAL_FILTERS, page: 1, revision: 0 });
  const [state, setState] = useState({ status: 'loading' });
  const [validationMessage, setValidationMessage] = useState('');
  const requestKey = JSON.stringify([groupId, query]);
  useEffect(() => {
    const controller = new AbortController();
    const options = { ...query.filters, page: query.page, signal: controller.signal };
    Promise.allSettled([getSavingGroupTransactions(groupId, options), getSavingGroupAccount(groupId, { signal: controller.signal })])
      .then(([transactions, account]) => {
        if (controller.signal.aborted) return;
        if (transactions.status === 'rejected') {
          const error = transactions.reason;
          const message = error.status === 401 ? '로그인 후 거래내역을 확인해 주세요.'
            : error.status === 403 ? '현재 승인된 모임원만 거래내역을 확인할 수 있습니다.'
              : error.status === 404 ? '계모임 또는 공용 계좌를 찾을 수 없습니다.' : error.message;
          setState({ key: requestKey, status: 'error', message });
        } else {
          setState({ key: requestKey, status: 'success', ...transactions.value,
            account: account.status === 'fulfilled' && account.value.issued ? account.value : null,
            balanceError: account.status === 'rejected' });
        }
      });
    return () => controller.abort();
  }, [groupId, query, requestKey]);
  const onSearch = event => {
    event.preventDefault();
    if (draftFilters.minAmount !== '' && draftFilters.maxAmount !== ''
      && Number(draftFilters.minAmount) > Number(draftFilters.maxAmount)) {
      setValidationMessage('최소 금액은 최대 금액보다 클 수 없습니다.'); return;
    }
    setValidationMessage('');
    setQuery(current => ({ filters: { ...draftFilters }, page: 1, revision: current.revision + 1 }));
  };
  return <SavingGroupTransactionHistoryView state={state.key === requestKey ? state : { status: 'loading' }}
    draftFilters={draftFilters} validationMessage={validationMessage} onSearch={onSearch}
    onFilterChange={(name, value) => setDraftFilters(current => ({ ...current, [name]: value }))}
    onPageChange={page => setQuery(current => ({ ...current, page }))}
    onRetry={() => setQuery(current => ({ ...current, revision: current.revision + 1 }))} />;
}
