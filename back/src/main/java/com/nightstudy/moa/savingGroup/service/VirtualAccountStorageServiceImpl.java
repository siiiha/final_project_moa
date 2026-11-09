package com.nightstudy.moa.savingGroup.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.savingGroup.dto.*;
import com.nightstudy.moa.savingGroup.mapper.VirtualAccountStorageMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// AI작성: 계좌번호는 암호화 후 Mapper로 전달하고 일반 응답으로 반환하지 않는다.
@Service
public class VirtualAccountStorageServiceImpl implements VirtualAccountStorageService {
    private final VirtualAccountStorageMapper mapper;
    private final AccountNumberCipher cipher;
    public VirtualAccountStorageServiceImpl(VirtualAccountStorageMapper mapper, AccountNumberCipher cipher) {
        this.mapper = mapper; this.cipher = cipher;
    }
    @Override public void requireConfigured() { cipher.requireConfigured(); }
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void save(Long approvalId, Long groupId, VirtualAccountIssueResult result, String holder) {
        if (result == null || result.getBankCode() == null || !result.getBankCode().matches("[0-9]{2,3}")
                || result.getDueDate() == null || !result.getDueDate().isAfter(java.time.OffsetDateTime.now())
                || holder == null || holder.isBlank() || holder.length() > 100) throw failed();
        var account = new EncryptedGroupAccountRow();
        account.setGroupId(groupId); account.setBankCode(result.getBankCode());
        account.setAccountNumber(cipher.encrypt(groupId, result.getAccountNumber()));
        account.setAccountHolderName(holder);
        if (mapper.insertAccount(account) != 1 || account.getAccountId() == null
                || mapper.completeIssue(approvalId, account.getAccountId()) != 1) throw failed();
    }
    private ApiException failed() {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "ACCOUNT_STORAGE_FAILED", "계좌 정보를 저장하지 못했습니다.");
    }
}
