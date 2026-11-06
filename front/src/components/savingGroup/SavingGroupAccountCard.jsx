import styled from 'styled-components';
import Button from '../common/Button';

// AI작성: 토스 공식 기관 코드의 가상계좌 지원 은행만 표시하며 알 수 없는 코드를 은행명으로 추정하지 않는다.
const bankNames = { '39': '경남은행', '34': '광주은행', '06': 'KB국민은행', '03': 'IBK기업은행',
  '11': 'NH농협은행', '31': 'iM뱅크', '32': '부산은행', '07': 'Sh수협은행',
  '88': '신한은행', '20': '우리은행', '71': '우체국예금보험', '37': '전북은행', '81': '하나은행',
  '004': 'KB국민은행', '003': 'IBK기업은행', '011': 'NH농협은행', '088': '신한은행',
  '020': '우리은행', '081': '하나은행', '039': '경남은행', '034': '광주은행', '031': 'iM뱅크',
  '032': '부산은행', '007': 'Sh수협은행', '071': '우체국예금보험', '037': '전북은행' };

const Card = styled.section`
  margin: 20px 0;
  padding: 24px;
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  background: ${({ theme }) => theme.colors.white};
  color: ${({ theme }) => theme.colors.text};
  h2 { margin: 0 0 16px; font-size: 22px; }
  p { font-size: 16px; line-height: 1.7; overflow-wrap: anywhere; }
  dl { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 24px; margin: 0; }
  dt { margin-bottom: 8px; font-size: 16px; }
  dd { margin: 0; font-size: 20px; font-weight: 700; overflow-wrap: anywhere; }
  @media (max-width: 600px) { padding: 20px; dl { grid-template-columns: 1fr; } }
`;

// AI작성: 계좌 조회와 테스트 입금 결과를 구분하여 잔액 재조회 실패가 입금 실패로 표시되지 않게 한다.
export default function SavingGroupAccountCard({ state, onRetry, children, depositMessage }) {
  return <Card aria-label="모임 테스트 계좌">
    <h2>모임 계좌 · 테스트</h2>
    {depositMessage && <p role="status">{depositMessage}</p>}
    {!state || state.status === 'loading' ? <p role="status">계좌 정보를 불러오는 중입니다.</p>
      : state.status === 'error' ? <div role="alert"><p>{state.message}</p>
        <Button type="button" onClick={onRetry}>계좌 정보 다시 조회</Button></div>
        : !state.data.issued ? <p role="status">아직 발급된 테스트 계좌가 없습니다.</p>
          : <dl><div><dt>{bankNames[state.data.bankCode] || '은행 정보 확인 필요'}</dt>
            <dd>{state.data.maskedAccountNumber}</dd></div>
            <div><dt>서비스 내부 장부 잔액</dt><dd>{state.data.balance.toLocaleString('ko-KR')}원</dd></div></dl>}
    <p>테스트 계좌이며 실제 입금할 수 없습니다.<br />장부 잔액은 은행 계좌의 실제 잔액과 다릅니다.</p>
    {state?.status === 'success' && state.data.issued && children}
  </Card>;
}
