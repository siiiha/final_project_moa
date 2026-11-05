package com.nightstudy.moa.member.service.identity;

import com.nightstudy.moa.common.config.PortOneProperties;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;


@Service
public class AccountRecoveryServiceImpl implements AccountRecoveryService {

    private final PortOneProperties portOneProperties;
    private final AccountRecoveryRequestStore requestStore;
    private final IdentityCiHasher identityCiHasher;
    private final PortOneApiClient portOneApiClient;
    private final MemberMapper memberMapper;

    public AccountRecoveryServiceImpl(
            PortOneProperties portOneProperties,
            AccountRecoveryRequestStore requestStore,
            IdentityCiHasher identityCiHasher,
            PortOneApiClient portOneApiClient,
            MemberMapper memberMapper
    ) {
        this.portOneProperties = portOneProperties;
        this.requestStore = requestStore;
        this.identityCiHasher = identityCiHasher;
        this.portOneApiClient = portOneApiClient;
        this.memberMapper = memberMapper;
    }

    @Override
    public StartResponse start() {
        if (!portOneProperties.isEnabled()
                || !StringUtils.hasText(portOneProperties.getStoreId())
                || !StringUtils.hasText(portOneProperties.getChannelKey())
                || !StringUtils.hasText(portOneProperties.getApiSecret())
                || !identityCiHasher.isConfigured()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ACCOUNT_RECOVERY_NOT_CONFIGURED",
                    "본인인증으로 아이디를 찾는 서비스를 준비 중입니다."
            );
        }

        String verificationId = "account-recovery-" + UUID.randomUUID();
        String requestToken = UUID.randomUUID().toString();

        requestStore.save(verificationId, requestToken);

        return new StartResponse(
                verificationId,
                requestToken,
                portOneProperties.getStoreId(),
                portOneProperties.getChannelKey()
        );
    }

    @Override
    public List<FoundAccount> complete(String verificationId, String requestToken) {
        if (!identityCiHasher.isConfigured() || !portOneProperties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "ACCOUNT_RECOVERY_NOT_CONFIGURED",
                    "본인인증으로 아이디를 찾는 서비스를 준비 중입니다."
            );
        }

        if (!requestStore.consume(verificationId, requestToken)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_RECOVERY_REQUEST_INVALID",
                    "인증 요청이 만료되었거나 이미 사용됐어요. 처음부터 다시 시도해 주세요."
            );
        }

        var result = portOneApiClient.getIdentityVerification(verificationId);
        var channel = result.getChannel();
        if (!verificationId.equals(result.getId())
                || !"VERIFIED".equals(result.getStatus())
                || channel == null
                || !"TEST".equals(channel.getType())
                || !java.util.Objects.equals(portOneProperties.getChannelKey(), channel.getKey())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_RECOVERY_VERIFICATION_INVALID",
                    "본인인증 결과를 확인할 수 없어요. 다시 시도해 주세요."
            );
        }

        var customer = result.getVerifiedCustomer();
        if (customer == null || !StringUtils.hasText(customer.getCi())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_RECOVERY_CI_UNAVAILABLE",
                    "이 인증 수단으로는 가입 계정을 확인할 수 없어요. 다른 인증 수단을 선택해 주세요."
            );
        }

        return memberMapper.findAccountsByIdentityCiHash(identityCiHasher.hash(customer.getCi()))
                .stream()
                .map(row -> new FoundAccount(
                        ((String) row.get("email")),
                        (String) row.get("provider")
                ))
                .distinct()
                .toList();
    }

}