package com.nightstudy.moa.savingGroup.mapper;

import com.nightstudy.moa.savingGroup.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// AI작성: 발급 이력과 암호화 계좌만 관리하며 잔액 변경·거래 생성은 수행하지 않는다.
@Mapper
public interface VirtualAccountStorageMapper {
    VirtualAccountIssueRecord selectIssue(@Param("approvalId") Long approvalId);
    int insertIssue(VirtualAccountIssueRecord issue);
    int completeIssue(@Param("approvalId") Long approvalId, @Param("accountId") Long accountId);
    int failIssue(@Param("approvalId") Long approvalId, @Param("status") String status,
                  @Param("errorCode") String errorCode);
    int insertAccount(EncryptedGroupAccountRow account);
    EncryptedGroupAccountRow selectAccount(@Param("groupId") Long groupId);
}
