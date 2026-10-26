package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.SocialLoginMemberDto;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.mapper.MemberSocialAccountMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Set;

// 소셜 제공사 공통으로 회원 조회·상태 확인·JWT 발급을 처리
@Service
public class SocialAccountLoginServiceImpl
        implements SocialAccountLoginService {

    private static final Set<String> PROVIDERS =
            Set.of("KAKAO", "NAVER", "GOOGLE");

    private final MemberSocialAccountMapper socialAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public SocialAccountLoginServiceImpl(
            MemberSocialAccountMapper socialAccountMapper,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.socialAccountMapper = socialAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public SocialLoginResultDto login(
            String provider,
            String providerUserId
    ) {
        // 사용자 ID는 외부 인증 API에서 확인한 값만 전달받음
        if (provider == null
                || !PROVIDERS.contains(provider)
                || providerUserId == null
                || providerUserId.isBlank()
                || providerUserId.length() > 255) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SOCIAL_ACCOUNT",
                    "소셜 인증 정보가 올바르지 않습니다."
            );
        }

        SocialLoginMemberDto member =
                socialAccountMapper.findByProviderAndProviderUserId(
                        provider,
                        providerUserId
                );

        SocialLoginResultDto result = new SocialLoginResultDto();

        // 연결된 회원이 없으면 추가 가입을 안내
        if (member == null) {
            result.setSignupRequired(true);
            result.setProviderUserId(providerUserId);
            return result;
        }

        // 탈퇴·정지 회원은 로그인을 차단
        if (member.getDeletedAt() != null
                || !"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "로그인할 수 없는 계정 상태입니다."
            );
        }

        if (member.getMemberId() == null
                || member.getMemberId() <= 0
                || (!"USER".equals(member.getSystemRole())
                && !"ADMIN".equals(member.getSystemRole()))) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVALID_MEMBER_AUTH_INFO",
                    "회원 인증 정보를 확인할 수 없습니다."
            );
        }

        result.setSignupRequired(false);
        result.setToken(jwtTokenProvider.createToken(
                member.getMemberId(),
                member.getSystemRole()
        ));

        return result;
    }
}