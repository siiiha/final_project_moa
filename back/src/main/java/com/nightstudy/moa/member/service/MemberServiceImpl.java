package com.nightstudy.moa.member.service;

import java.util.HashSet;
import java.util.ArrayList;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberInsertDto;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.member.dto.MemberLoginDto;
import com.nightstudy.moa.member.dto.MemberLoginRequest;
import com.nightstudy.moa.member.dto.SocialSignupRequest;

import java.nio.charset.StandardCharsets;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;

    public MemberServiceImpl(
            MemberMapper memberMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailVerificationService emailVerificationService
    ) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationService = emailVerificationService;
    }

    @Override
    @Transactional
    public Long signup(MemberSignupRequest request) {
        return signupInternal(request, false);
    }


    // 소셜 가입도 기존 회원 정보·이메일·약관 검증을 재사용
    // 소셜 인증 검증은 이 메서드를 호출하는 가입 서비스에서 수행
    @Override
    @Transactional
    public Long signupSocial(SocialSignupRequest request) {
        MemberSignupRequest memberRequest = new MemberSignupRequest();

        memberRequest.setEmail(request.getEmail());
        memberRequest.setEmailVerificationToken(
                request.getEmailVerificationToken()
        );
        memberRequest.setName(request.getName());
        memberRequest.setNickname(request.getNickname());
        memberRequest.setPhone(request.getPhone());
        memberRequest.setBirthDate(request.getBirthDate());
        memberRequest.setGender(request.getGender());
        memberRequest.setRegionId(request.getRegionId());
        memberRequest.setAgreedTermIds(request.getAgreedTermIds());
        memberRequest.setInterestIds(request.getInterestIds());

        return signupInternal(memberRequest, true);
    }

    // 두 가입 방식의 공통 검증과 회원 저장을 처리
    private Long signupInternal(
            MemberSignupRequest request,
            boolean socialSignup
    ) {
        String email = request.getEmail().trim();
        String nickname = request.getNickname().trim();

        if (memberMapper.countByEmail(email) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "이미 가입된 이메일입니다."
            );
        }

        if (memberMapper.countByNickname(nickname) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "NICKNAME_ALREADY_EXISTS",
                    "이미 사용 중인 닉네임입니다."
            );
        }

        if (memberMapper.countRegionById(request.getRegionId()) != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REGION",
                    "존재하지 않는 지역입니다."
            );
        }

        // 비밀번호를 사용하는 일반 가입에만 적용
        if (!socialSignup && request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_LONG",
                    "비밀번호가 너무 깁니다. 더 짧게 입력해주세요."
            );
        }
        HashSet<Long> interestIds = new HashSet<>
                (request.getInterestIds());

        for (Long interestId : interestIds) {
            if (memberMapper.countInterestById(interestId) != 1) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_INTEREST",
                        "존재하지 않는 관심사입니다."
                );
            }
        }
        ArrayList<Long> termIds =
                new ArrayList<>(new HashSet<>(request.getAgreedTermIds()));

        for (Long termId : termIds) {
            if (memberMapper.countTermById(termId) != 1) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_TERM",
                        "존재하지 않는 약관입니다."
                );
            }
        }

        int requiredCount = memberMapper.countRequiredTerms();
        int agreedRequiredCount =
                memberMapper.countRequiredAgreedTerms(termIds);

        if (requiredCount != agreedRequiredCount) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REQUIRED_TERMS_NOT_AGREED",
                    "필수 약관에 모두 동의해주세요."
            );
        }

        emailVerificationService.consumeVerificationToken(
                email,
                request.getEmailVerificationToken()
        );

        MemberInsertDto member = new MemberInsertDto();
        member.setRegionId(request.getRegionId());
        member.setEmail(email);
        member.setPasswordHash(
                socialSignup
                        ? null
                        : passwordEncoder.encode(request.getPassword())
        );
        member.setName(request.getName().trim());
        member.setNickname(nickname);
        member.setPhone(request.getPhone().trim());
        member.setBirthDate(request.getBirthDate());
        member.setGender(request.getGender());

        try {
            int savedRows = memberMapper.insertMember(member);

            if (savedRows != 1 || member.getMemberId() == null) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "SIGNUP_FAILED",
                        "회원가입 처리에 실패했습니다."
                );
            }
        } catch (DuplicateKeyException e) {
            // 동시에 같은 이메일로 가입하는 요청도 DB에서 차단
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MEMBER_ALREADY_EXISTS",
                    "이미 사용 중인 회원 정보입니다."
            );
        }

        // 회원이 선택한 관심사 저장
        for (Long interestId : interestIds) {
            int savedRows = memberMapper.insertMemberInterest(
                    member.getMemberId(),
                    interestId
            );

            if (savedRows != 1) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "SIGNUP_FAILED",
                        "관심사 저장에 실패했습니다."
                );
            }
        }
        for (Long termId : termIds) {
            int saveRows = memberMapper.insertTermAgreement(
                    member.getMemberId(),
                    termId
            );

            if (saveRows != 1) {
                throw new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "SIGNUP_FAILED",
                        "약관 동의 저장에 실패했습니다."
                );
            }
        }
        return member.getMemberId();
    }

    // 이메일 사용 가능 여부 확인
    @Override
    public boolean isEmailAvailable(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INPUT",
                    "이메일을 입력해주세요."
            );
        }

        return memberMapper.countByEmail(email.trim()) == 0;
    }

    // 닉네임 사용 가능 여부 확인
    @Override
    public boolean isNicknameAvailable(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_INPUT",
                    "닉네임을 입력해주세요."
            );
        }

        return memberMapper.countByNickname(nickname.trim()) == 0;
    }

    // 이메일과 비밀번호 확인 후 로그인 토큰 발급
    @Override
    public String login(MemberLoginRequest request) {
        String email = request.getEmail().trim();

        MemberLoginDto member = memberMapper.findByEmail(email);

        if (member == null
                || member.getPasswordHash() == null
                || request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72
                || !passwordEncoder.matches(
                request.getPassword(),
                member.getPasswordHash()
        )) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "LOGIN_FAILED",
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        if (!"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "로그인할 수 없는 계정 상태입니다."
            );
        }

        return jwtTokenProvider.createToken(
                member.getMemberId(),
                member.getSystemRole()
        );
    }
}