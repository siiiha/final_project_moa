package com.nightstudy.moa.anomaly.mapper;
import com.nightstudy.moa.anomaly.dto.BusinessContextTransaction;
import com.nightstudy.moa.anomaly.dto.PayoutContextRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 기존 테이블의 명시적 transaction_id 연결만 조회한다. 금액·시각 유사 매칭 금지.
@Mapper
public interface AnomalyContextMapper {
    BusinessContextTransaction findTransaction(@Param("groupId") long groupId, @Param("transactionId") long transactionId);
    List<PayoutContextRecord> findPayoutLinks(@Param("transactionId") long transactionId);
}
