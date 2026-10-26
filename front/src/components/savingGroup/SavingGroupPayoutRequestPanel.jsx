import { useState } from 'react';
import { FiFileText, FiImage } from 'react-icons/fi';
import styled from 'styled-components';
import Button from '../common/Button';
import Textarea from '../common/Textarea';

const Panel = styled.section`
  margin-top: 22px;
`;

const Intro = styled.div`
  h2 {
    color: ${({ theme }) => theme.colors.text};
    font-size: 20px;
    font-weight: 800;
    letter-spacing: -0.9px;
  }

  p {
    margin-top: 8px;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 12px;
  }
`;

const Card = styled.section`
  margin-top: 16px;
  padding: 22px;
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.md};
  box-shadow: ${({ theme }) => theme.shadow.card};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    padding: 18px;
  }
`;

const CardTitle = styled.h3`
  color: ${({ theme }) => theme.colors.text};
  font-size: 15px;
  font-weight: 800;
`;

const ScheduledPayout = styled.div`
  margin-top: 14px;
  padding: 16px;
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};

  span {
    display: block;
    color: ${({ theme }) => theme.colors.secondaryText};
    font-size: 11px;
  }

  strong {
    margin-top: 5px;
    display: block;
    color: ${({ theme }) => theme.colors.text};
    font-size: 15px;
  }
`;

const PayoutAmount = styled.strong`
  color: ${({ theme }) => theme.colors.primary} !important;
  font-size: 19px !important;
  letter-spacing: -0.7px;
`;

const InfoNotice = styled.p`
  margin-top: 8px;
  padding: 10px 12px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 11px;
  line-height: 1.55;
`;

const Form = styled.form`
  margin-top: 16px;
`;

const FieldGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;

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

const FullField = styled(Field)`
  margin-top: 14px;
`;

const Input = styled.input`
  width: 100%;
  height: 42px;
  padding: 0 12px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ $readOnly, theme }) => ($readOnly ? theme.colors.background : theme.colors.white)};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 13px;

  &::placeholder {
    color: ${({ theme }) => theme.colors.muted};
  }

  &:focus {
    border-color: ${({ theme }) => theme.colors.primary};
    outline: 0;
    box-shadow: 0 0 0 3px rgba(23, 107, 91, 0.12);
  }
`;

const UploadArea = styled.div`
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: ${({ theme }) => theme.colors.background};
  border: 1px dashed ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    align-items: stretch;
    flex-direction: column;
  }
`;

const FileInput = styled.input`
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
`;

const UploadButton = styled.label`
  min-height: 36px;
  padding: 8px 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: ${({ theme }) => theme.colors.text};
  background: ${({ theme }) => theme.colors.white};
  border: 1px solid ${({ theme }) => theme.colors.border};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;

  &:hover {
    background: ${({ theme }) => theme.colors.primarySoft};
  }
`;

const FileName = styled.p`
  min-width: 0;
  overflow: hidden;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const Analysis = styled.div`
  margin-top: 12px;
  padding: 13px 14px;
  display: flex;
  align-items: center;
  gap: 9px;
  color: ${({ theme }) => theme.colors.secondaryText};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};

  div {
    min-width: 0;
  }

  span,
  strong {
    display: block;
  }

  span {
    font-size: 11px;
  }

  strong {
    margin-top: 3px;
    color: ${({ theme }) => theme.colors.primary};
    font-size: 16px;
  }
`;

const HelpText = styled.p`
  margin-top: 7px;
  color: ${({ theme }) => theme.colors.muted};
  font-size: 11px;
`;

const ActionRow = styled.div`
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
`;

const RequestButton = styled(Button)`
  min-height: 40px;
  padding: 9px 16px;
  font-size: 13px;

  @media (max-width: ${({ theme }) => theme.breakpoints.mobile}) {
    width: 100%;
  }
`;

const SuccessMessage = styled.p`
  margin-top: 14px;
  padding: 11px 12px;
  color: ${({ theme }) => theme.colors.primary};
  background: ${({ theme }) => theme.colors.primarySoft};
  border-radius: ${({ theme }) => theme.radius.sm};
  font-size: 12px;
  font-weight: 700;
`;

function SavingGroupPayoutRequestPanel({ groupType }) {
  const [requestGroupType] = useState(groupType || '순번계');
  const [receiptName, setReceiptName] = useState('선택된 영수증이 없습니다.');
  const [analyzedAmount, setAnalyzedAmount] = useState('');
  const [isRequested, setIsRequested] = useState(false);
  const isRotationGroup = requestGroupType === '순번계';

  const handleReceiptChange = (event) => {
    const [receipt] = event.target.files;
    setReceiptName(receipt ? receipt.name : '선택된 영수증이 없습니다.');
    // TODO: Spring Boot 서버의 영수증 OCR 분석 API로 실제 지출 금액을 조회합니다.
    setAnalyzedAmount(receipt ? '32,000원' : '');
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    // TODO: Spring Boot 서버에 계금 요청 API와 계모임 유형별 요청 정보를 전송합니다.
    setIsRequested(true);
  };

  return (
    <Panel>
      <Intro>
        <h2>계금 요청</h2>
        <p>{isRotationGroup ? '이번 회차의 수령 대상과 지급 금액을 확인해주세요.' : '선결제한 지출의 영수증을 첨부해 계금을 요청할 수 있습니다.'}</p>
      </Intro>

      <Card>
        <CardTitle>{isRotationGroup ? '수령 정보' : '지출 증빙 정보'}</CardTitle>
        {isRotationGroup ? (
          <>
            <ScheduledPayout>
              <span>이번 회차 수령 예정 계원</span>
              <strong>김영희</strong>
              <PayoutAmount>1,200,000원</PayoutAmount>
              <span>2026년 10월 · 10회차</span>
            </ScheduledPayout>
            <InfoNotice>순번계에서는 계금 수령을 요청할 수 있습니다. 회차와 납부 상태를 먼저 확인해주세요.</InfoNotice>
          </>
        ) : (
          <>
            <FullField as="div">
              <span>영수증 사진</span>
              <UploadArea>
                <FileInput id="payout-receipt" type="file" accept="image/*" onChange={handleReceiptChange} />
                <UploadButton htmlFor="payout-receipt">
                  <FiImage size={15} aria-hidden="true" /> 영수증 첨부
                </UploadButton>
                <FileName>{receiptName}</FileName>
              </UploadArea>
            </FullField>
            {analyzedAmount && (
              <Analysis aria-live="polite">
                <FiFileText size={19} aria-hidden="true" />
                <div>
                  <span>영수증 분석 금액</span>
                  <strong>{analyzedAmount}</strong>
                </div>
              </Analysis>
            )}
            <HelpText>JPG, PNG, WEBP 형식의 영수증 이미지를 첨부할 수 있습니다.</HelpText>
          </>
        )}

        <Form onSubmit={handleSubmit}>
          <FieldGrid>
            <Field>
              {isRotationGroup ? '수령 은행' : '지급 은행'}
              <Input name="bank" required defaultValue="모아은행" />
            </Field>
            <Field>
              {isRotationGroup ? '수령 계좌' : '지급 계좌'}
              <Input name="account" required defaultValue="123-456-7890" />
            </Field>
          </FieldGrid>
          {!isRotationGroup && (
            <FullField>
              분석된 요청 금액
              <Input name="amount" $readOnly readOnly value={analyzedAmount} placeholder="영수증을 첨부하면 금액이 표시됩니다." required />
            </FullField>
          )}
          <FullField>
            요청 메모
            <Textarea
              name="memo"
              placeholder={isRotationGroup ? '이번 회차 계금 지급을 요청합니다.' : '지출 내역과 지급 요청 내용을 입력해주세요.'}
              defaultValue={isRotationGroup ? '이번 회차 계금 지급을 요청합니다.' : ''}
            />
          </FullField>
          <ActionRow>
            <RequestButton type="submit" disabled={!isRotationGroup && !analyzedAmount}>
              계금 요청하기
            </RequestButton>
          </ActionRow>
        </Form>
        {isRequested && <SuccessMessage role="status">계금 요청이 접수되었습니다.</SuccessMessage>}
      </Card>
    </Panel>
  );
}

export default SavingGroupPayoutRequestPanel;
