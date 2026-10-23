package com.nightstudy.moa.anomaly.service;
import com.nightstudy.moa.anomaly.dto.TransactionBusinessContext;
import com.nightstudy.moa.anomaly.dto.TransactionBusinessContext.State;
import com.nightstudy.moa.anomaly.mapper.AnomalyContextMapper;
import com.nightstudy.moa.common.exception.ApiException;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 현재 저장된 지급 기록의 정합성만 확인한다. 정상 거래 보증·모델 점수 보정·경고 해제는 하지 않는다.
@Service
@ConditionalOnProperty(name="moa.anomaly.context.enabled",havingValue="true")
public class AnomalyContextServiceImpl implements AnomalyContextService {
    private final AnomalyContextMapper mapper;
    public AnomalyContextServiceImpl(AnomalyContextMapper mapper) { this.mapper=mapper; }

    @Override
    @Transactional(readOnly=true)
    public TransactionBusinessContext findContext(long groupId,long transactionId) {
        if(groupId<1 || transactionId<1)
            throw new ApiException(HttpStatus.BAD_REQUEST,"ANOMALY_CONTEXT_INVALID_ID","조회 조건이 올바르지 않습니다.");
        var transaction=mapper.findTransaction(groupId,transactionId);
        if(transaction==null)
            throw new ApiException(HttpStatus.NOT_FOUND,"ANOMALY_CONTEXT_NOT_FOUND","해당 모임의 거래를 찾을 수 없습니다.");
        var links=mapper.findPayoutLinks(transactionId);
        if(links.isEmpty()) return new TransactionBusinessContext(State.NO_LINK,null,
            "직접 연결된 지급 요청 기록이 없습니다. 정상·이상 여부를 뜻하지 않습니다.");
        if(links.size()!=1) return review();
        var payout=links.get(0);
        // AI작성: FK 연결, 동일 모임·금액, 출금, 완료 상태와 검토 시각을 모두 확인한다.
        if(!Objects.equals(transaction.getGroupId(),payout.getGroupId())
                || !"WITHDRAWAL".equals(transaction.getTransactionType())
                || !"COMPLETED".equals(payout.getStatus()) || payout.getPayoutRequestId()==null
                || transaction.getAmount()==null || payout.getAmount()==null
                || payout.getAmount().signum()<=0 || transaction.getAmount().compareTo(payout.getAmount())!=0
                || payout.getReviewerId()==null || payout.getRequestedAt()==null
                || payout.getReviewedAt()==null || payout.getCompletedAt()==null
                || payout.getReviewedAt().isBefore(payout.getRequestedAt())
                || payout.getCompletedAt().isBefore(payout.getReviewedAt())) return review();
        return new TransactionBusinessContext(State.LINKED_COMPLETED_PAYOUT,payout.getPayoutRequestId(),
            "완료·검토 정보가 있는 지급 요청이 이 거래에 연결되어 있습니다. 모델 경고는 별도로 확인하세요.");
    }
    private TransactionBusinessContext review() {
        // AI작성: 모임 불일치 시 다른 모임의 요청 ID·금액·검토자 정보를 노출하지 않는다.
        return new TransactionBusinessContext(State.LINK_REQUIRES_REVIEW,null,
            "지급 요청 연결 정보가 미완료·불일치·중복 상태입니다. 담당자 확인이 필요합니다.");
    }
}
