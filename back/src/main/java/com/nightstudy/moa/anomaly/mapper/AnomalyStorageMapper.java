package com.nightstudy.moa.anomaly.mapper;
import com.nightstudy.moa.anomaly.dto.StoredTransaction;
import com.nightstudy.moa.anomaly.dto.AnomalyResult;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 제안 스키마 전용. 모든 값은 바인딩하며 계좌번호·거래 원문을 조회하지 않는다.
@Mapper
public interface AnomalyStorageMapper {
    Long lockAccount(@Param("accountId") long accountId);
    StoredTransaction findSource(StoredTransaction transaction);
    int insertTransaction(StoredTransaction transaction);
    StoredTransaction findTransaction(@Param("transactionId") long transactionId);
    List<StoredTransaction> selectHistory(StoredTransaction target);
    int insertPending(@Param("transactionId") long transactionId, @Param("fingerprint") String fingerprint);
    int complete(@Param("transactionId") long transactionId, @Param("fingerprint") String fingerprint,
        @Param("result") AnomalyResult result, @Param("qualityFlags") String qualityFlags);
    int fail(@Param("transactionId") long transactionId, @Param("fingerprint") String fingerprint,
        @Param("errorCode") String errorCode);
}
