package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.MemberInsertDto;
import com.nightstudy.moa.member.dto.MemberSignupRequest;
import com.nightstudy.moa.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;

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

        // BCrypt가 처리할 수 있는 비밀번호 길이 확인
        if (request.getPassword()
                .getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_LONG",
                    "비밀번호는 UTF-8 기준 72바이트 이내여야 합니다."
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

        return member.getMemberId();
    }
}