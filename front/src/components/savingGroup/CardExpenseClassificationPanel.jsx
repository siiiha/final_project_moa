import { useMemo, useRef, useState } from 'react';
import styled from 'styled-components';
import Button from '../common/Button';
import { expenseCategories } from '../../constants/expenseCategories';
import { cardExpenseClassificationMock } from '../../mocks/cardExpenseClassification';
import { confirmCardExpenseCategories, uploadCardExpenses } from '../../api/savingGroups';

const Panel = styled.section`
  margin-top: 22px;
`;

const Header = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
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
  line-height: 1.6;
`;

const UploadButton = styled(Button)`
  min-height: 38px;
  padding: 8px 12px;
  font-size: 12px;
`;

const HeaderActions = styled.div`
  display: flex;
  gap: 8px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
    display: grid;
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
  display: block;
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.primary};
  font-size: 18px;
`;

const ListCard = styled.section`
  margin-top: 14px;
  padding: 18px;
  overflow-x: auto;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};
`;

const Table = styled.table`
  width: 100%;
  min-width: 880px;
  border-collapse: collapse;

  th,
  td {
    padding: 10px 8px;
    border-bottom: 1px solid ${({ theme }) => theme.colors.border};
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
    text-align: center;
    white-space: nowrap;
  }

  th {
    background: ${({ theme }) => theme.colors.background};
    font-size: 11px;
    font-weight: 700;
  }

  td:nth-child(2) {
    text-align: left;
  }

  tbody tr:last-child td {
    border-bottom: 0;
  }
`;

const Select = styled.select`
  min-width: 92px;
  height: 32px;
  padding: 0 7px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
`;

const Notice = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.background};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
  line-height: 1.6;
`;

const formatAmount = (amount, approvalStatus) => (approvalStatus === '취소' ? '취소' : `${amount.toLocaleString()}원`);

const categoryLabel = { FOOD: '식비', TRANSPORTATION: '교통비', RENTAL: '대관료', SUPPLIES: '물품비', ACTIVITY: '활동비', ACCOMMODATION: '숙박비', FEE: '수수료', OTHER: '기타', NEEDS_REVIEW: '확인 필요' };
const categoryCode = Object.fromEntries(Object.entries(categoryLabel).map(([code, label]) => [label, code]));

function CardExpenseClassificationPanel({ groupId }) {
  // AI작성: 서버 연동 전, 사용자가 추천 항목을 바꾸고 확정하는 흐름을 확인한다.
  const [expenses, setExpenses] = useState(() => cardExpenseClassificationMock.map((expense) => ({
    ...expense,
    finalCategory: expense.suggestedCategory || '확인 필요',
    isFinalized: false,
  })));
  const [isUploadPreparing, setIsUploadPreparing] = useState(false);
  const [isConfirming, setIsConfirming] = useState(false);
  const [uploadMessage, setUploadMessage] = useState('');
  const fileInputRef = useRef(null);

  const summary = useMemo(() => ({
    total: expenses.length,
    reviewRequired: expenses.filter((expense) => expense.finalCategory === '확인 필요').length,
    confirmed: expenses.filter((expense) => expense.isFinalized).length,
  }), [expenses]);

  const handleCategoryChange = (id, finalCategory) => {
    setExpenses((current) => current.map((expense) => (
      expense.id === id ? { ...expense, finalCategory, isFinalized: false } : expense
    )));
  };

  const handleConfirmAll = async () => {
    if (!expenses.length || isConfirming) return;
    const persistedItems = expenses.filter((expense) => expense.isPersisted).map((expense) => ({
      cardExpenseId: expense.id,
      finalCategory: categoryCode[expense.finalCategory],
    }));
    setIsConfirming(true);
    try {
      if (persistedItems.length) {
        await confirmCardExpenseCategories(groupId, persistedItems);
      }
      setExpenses((current) => current.map((expense) => ({ ...expense, isFinalized: true })));
      setUploadMessage('현재 선택한 항목을 모두 저장했습니다.');
    } catch (error) {
      setUploadMessage(error.message);
    } finally {
      setIsConfirming(false);
    }
  };

  const handleFileChange = async (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    setIsUploadPreparing(true);
    setUploadMessage('');
    try {
      const previewRows = await uploadCardExpenses(groupId, file);
      setExpenses(previewRows.map((row, index) => ({
        id: row.cardExpenseId || index + 1,
        isPersisted: Boolean(row.cardExpenseId),
        usedAt: row.usedDate,
        merchantName: row.merchantName,
        amount: Number(row.amount),
        approvalStatus: row.approvalStatus === 'CANCELED' ? '취소' : '승인',
        suggestedCategory: categoryLabel[row.suggestedCategory] || '',
        confidenceScore: row.confidence,
        finalCategory: categoryLabel[row.suggestedCategory] || '확인 필요',
        isFinalized: row.classificationStatus === 'CONFIRMED',
      })));
      setUploadMessage(`${file.name}의 분류 결과를 불러왔습니다.`);
    } catch (error) {
      setUploadMessage(error.message);
    } finally {
      setIsUploadPreparing(false);
      event.target.value = '';
    }
  };

  return (
    <Panel>
      <Header>
        <div>
          <Title>카드 지출 분류 검토</Title>
          <Description>추천 항목을 확인·수정한 뒤 확정하면, 확정된 지출만 통계에 반영됩니다.</Description>
        </div>
        <HeaderActions>
          <UploadButton type="button" $variant="secondary" disabled={isUploadPreparing} onClick={() => fileInputRef.current?.click()}>
            {isUploadPreparing ? '분류 중...' : '카드 내역 엑셀 업로드'}
          </UploadButton>
          <UploadButton type="button" disabled={isConfirming || !expenses.length} onClick={handleConfirmAll}>
            {isConfirming ? '저장 중...' : '확정하기'}
          </UploadButton>
          <input ref={fileInputRef} type="file" accept=".xls,.xlsx" hidden onChange={handleFileChange} />
        </HeaderActions>
      </Header>

      <Summary>
        <SummaryCard><SummaryLabel>불러온 카드 지출</SummaryLabel><SummaryValue>{summary.total}건</SummaryValue></SummaryCard>
        <SummaryCard><SummaryLabel>확인 필요</SummaryLabel><SummaryValue>{summary.reviewRequired}건</SummaryValue></SummaryCard>
        <SummaryCard><SummaryLabel>사용자 확정</SummaryLabel><SummaryValue>{summary.confirmed}건</SummaryValue></SummaryCard>
      </Summary>

      <ListCard>
        <Table>
          <thead>
            <tr><th scope="col">이용일시</th><th scope="col">가맹점명</th><th scope="col">금액</th><th scope="col">지출 항목</th></tr>
          </thead>
          <tbody>
            {expenses.map((expense) => (
              <tr key={expense.id}>
                <td>{expense.usedAt}</td>
                <td>{expense.merchantName}</td>
                <td>{formatAmount(expense.amount, expense.approvalStatus)}</td>
                <td>
                  <Select value={expense.finalCategory} disabled={expense.approvalStatus === '취소'} onChange={(event) => handleCategoryChange(expense.id, event.target.value)}>
                    <option value="">선택</option>
                    {expenseCategories.map((category) => <option key={category} value={category}>{category}</option>)}
                  </Select>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
        {uploadMessage && <Notice role="status">{uploadMessage}</Notice>}
      </ListCard>
    </Panel>
  );
}

export default CardExpenseClassificationPanel;
