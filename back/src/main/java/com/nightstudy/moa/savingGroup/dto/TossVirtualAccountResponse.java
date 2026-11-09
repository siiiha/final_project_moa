package com.nightstudy.moa.savingGroup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

// AI작성: 필요한 응답만 역직렬화하고 원문이 있는 DTO에 toString을 생성하지 않는다.
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TossVirtualAccountResponse {
    private String orderId;
    private String status;
    private String currency;
    private Integer totalAmount;
    private VirtualAccount virtualAccount;
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VirtualAccount {
        private String accountType;
        private String accountNumber;
        private String bankCode;
        private String dueDate;
        private Boolean expired;
    }
}
