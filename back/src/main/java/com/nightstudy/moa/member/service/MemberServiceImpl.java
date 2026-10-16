package com.nightstudy.moa.member.service;

import java.util.HashSet;
import java.util.ArrayList;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberInsertDto;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(
            MemberMapper memberMapper,
            PasswordEncoder passwordEncoder
    ){
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Long signup(MemberSignupRequest request) {
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

        if (memberMapper.countRegionById(request.getRegionId()) == 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_REGION",
                    "존재하지 않는 지역입니다."
            );
        }

        // Bcrypt가 처리할 수 있는 비밀번호 길이 확인
        if (request.getPassword()
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
            if (memberMapper.countInterestById(interestId) == 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_INTEREST",
                        "존재하지 않는 관심사입니다."
                );
            }
        }
        ArrayList<Long> termIds =
                new ArrayList<>(new HashSet<>(request.getAgreedTermIds()));

        for  (Long termId : termIds) {
            if (memberMapper.countTermById(termId) == 0) {
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

        MemberInsertDto member = new MemberInsertDto();
        member.setRegionId(request.getRegionId());
        member.setEmail(email);
        member.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
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
}