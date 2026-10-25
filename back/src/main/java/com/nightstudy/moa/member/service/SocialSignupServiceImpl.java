package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.auth.SocialSignupSessionManager.PendingSignup;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.SocialLoginResultDto;
import com.nightstudy.moa.member.dto.SocialSignupRequest;
import com.nightstudy.moa.member.mapper.MemberSocialAccountMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// 회원 생성과 소셜 계정 연결을 하나의 트랜잭션으로 처리
@Service
public class SocialSignupServiceImpl implements SocialSignupService {

    private final MemberService memberService;
    private final MemberSocialAccountMapper socialAccountMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public SocialSignupServiceImpl(
            MemberService memberService,
            MemberSocialAccountMapper socialAccountMapper,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.memberService = memberService;
        this.socialAccountMapper = socialAccountMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public SocialLoginResultDto signup(
            SocialSignupRequest request,
            PendingSignup pending
    ) {
        // Controller에서 검증한 서버 세션 정보를 전달받음
        if (pending == null
                || !Instant.now().isBefore(pending.expiresAt())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_SOCIAL_SIGNUP",
                    "소셜 가입 요청이 만료되었습니다. 다시 로그인해주세요."
            );
        }

        // 탈퇴 회원을 포함해 이미 연결된 계정이면 신규 가입을 막음
        if (socialAccountMapper.findByProviderAndProviderUserId(
                pending.provider(),
                pending.providerUserId()
        ) != null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SOCIAL_ACCOUNT_ALREADY_EXISTS",
                    "이미 가입된 소셜 계정입니다. 다시 로그인해주세요."
            );
        }

        // 기존 검증을 재사용해 회원·약관·관심사를 저장
        Long memberId = memberService.signupSocial(request);

        try {
            int savedRows = socialAccountMapper.insertSocialAccount(
                    memberId,
                    pending.provider(),
                    pending.providerUserId()
            );

            if (savedRows != 1) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "SOCIAL_SIGNUP_FAILED",
                        "소셜 계정 연결에 실패했습니다."
                );
            }
        } catch (DuplicateKeyException exception) {
            // 동시 가입으로 중복이 발생해도 회원 생성까지 취소
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SOCIAL_ACCOUNT_ALREADY_EXISTS",
                    "이미 가입된 소셜 계정입니다. 다시 로그인해주세요."
            );
        }

        // 신규 회원의 기본 권한인 USER로 모아 JWT를 발급
        SocialLoginResultDto result = new SocialLoginResultDto();
        result.setSignupRequired(false);
        result.setToken(jwtTokenProvider.createToken(memberId, "USER"));

        return result;
    }
}