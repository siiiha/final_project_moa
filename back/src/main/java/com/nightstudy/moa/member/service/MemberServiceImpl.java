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
import com.nightstudy.moa.member.dto.MemberProfileResponse;
import com.nightstudy.moa.member.dto.MemberProfileUpdateRequest;
import com.nightstudy.moa.member.dto.MemberPasswordUpdateRequest;
import com.nightstudy.moa.member.dto.MemberWithdrawalRequest;

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

    @Override
    @Transactional(readOnly = true) // 데이터를 조회하는 트랜잭션임을 표시
    public MemberProfileResponse getMyProfile(Long memberId) {

        MemberProfileResponse profile =
                memberMapper.findProfileByMemberId(memberId);

        if (profile == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원 정보를 찾을 수 없습니다."
            );
        }

        profile.setInterests(
                memberMapper.findInterestsByMemberId(memberId)
        );

        return profile;
    }

    // 비밀번호와 입력값 검증 후 회원정보 수정
    @Override
    @Transactional
    public MemberProfileResponse updateMyProfile(
            Long memberId,
            MemberProfileUpdateRequest request
    ) {
        MemberLoginDto member =
                memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원 정보를 찾을 수 없습니다."
            );
        }

        if (!"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "회원정보를 수정할 수 없는 계정 상태입니다."
            );
        }

        String currentPassword = request.getCurrentPassword();

        if (currentPassword == null
                || currentPassword.isBlank()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || member.getPasswordHash() == null
                || !passwordEncoder.matches(
                currentPassword,
                member.getPasswordHash()
        )) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENT_PASSWORD",
                    "현재 비밀번호가 올바르지 않습니다."
            );
        }

        String nickname = request.getNickname();

        if (nickname != null) {
            nickname = nickname.trim();

            if (nickname.length() < 2 || nickname.length() > 12) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_NICKNAME",
                        "닉네임은 2~12자로 입력해주세요."
                );
            }

            if (memberMapper.countByNicknameExcludingMember(
                    nickname, memberId
            ) > 0) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "NICKNAME_ALREADY_EXISTS",
                        "이미 사용 중인 닉네임입니다."
                );
            }
        }

        // null은 기존 관심사 유지, 빈 목록은 전체 해제
        HashSet<Long> interestIds = null;

        if (request.getInterestIds() != null) {
            interestIds = new HashSet<>(request.getInterestIds());

            for (Long interestId : interestIds) {
                if (interestId == null
                        || interestId <= 0
                        || memberMapper.countInterestById(interestId) != 1) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "INVALID_INTEREST",
                            "존재하지 않는 관심사입니다."
                    );
                }
            }
        }

        if (nickname != null) {
            try {
                memberMapper.updateNickname(memberId, nickname);
            } catch (DuplicateKeyException e) {
                // 동시 요청으로 발생한 닉네임 중복도 처리
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "NICKNAME_ALREADY_EXISTS",
                        "이미 사용 중인 닉네임입니다."
                );
            }
        }

        if (interestIds != null) {
            memberMapper.deleteMemberInterests(memberId);

            for (Long interestId : interestIds) {
                int savedRows = memberMapper.insertMemberInterest(
                        memberId, interestId
                );

                if (savedRows != 1) {
                    throw new ApiException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "PROFILE_UPDATE_FAILED",
                            "회원정보 수정에 실패했습니다."
                    );
                }
            }
        }

        // 두 칸 모두 비어 있으면 기존 비밀번호를 유지
        String newPassword = request.getNewPassword();
        String confirmNewPassword = request.getConfirmNewPassword();

        boolean passwordChangeRequested =
                (newPassword != null && !newPassword.isEmpty())
                        || (confirmNewPassword != null
                        && !confirmNewPassword.isEmpty());

        if (passwordChangeRequested) {
            MemberPasswordUpdateRequest passwordRequest =
                    new MemberPasswordUpdateRequest();

            passwordRequest.setCurrentPassword(request.getCurrentPassword());
            passwordRequest.setNewPassword(newPassword);
            passwordRequest.setConfirmNewPassword(confirmNewPassword);

            updatePassword(memberId, passwordRequest);
        }

        return getMyProfile(memberId);
    }

    // 현재 비밀번호를 확인 후 새 비밀번호를 암호화해 저장
    @Override
    @Transactional
    public void updatePassword(
            Long memberId,
            MemberPasswordUpdateRequest request
    ) {
        MemberLoginDto member =
                memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원 정보를 찾을 수 없습니다."
            );
        }

        if (!"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "비밀번호를 수정할 수 없는 계정 상태입니다."
            );
        }

        String currentPassword = request.getCurrentPassword();

        if (currentPassword == null
                || currentPassword.isBlank()
                || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
                || member.getPasswordHash() == null
                || !passwordEncoder.matches(
                currentPassword,
                member.getPasswordHash()
        )) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_CURRENT_PASSWORD",
                    "현재 비밀번호가 올바르지 않습니다."
            );
        }

        String newPassword = request.getNewPassword();

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.length() < 8) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_NEW_PASSWORD",
                    "새 비밀번호는 8자 이상 입력해주세요."
            );
        }

        if (newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_TOO_LONG",
                    "비밀번호가 너무 깁니다. 더 짧게 입력해주세요."
            );
        }

        if (!newPassword.equals(request.getConfirmNewPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PASSWORD_CONFIRM_MISMATCH",
                    "새 비밀번호와 확인 입력값이 일치하지 않습니다."
            );
        }
        if (passwordEncoder.matches(newPassword, member.getPasswordHash())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "NEW_PASSWORD_SAME_AS_CURRENT",
                    "새 비밀번호는 현재 비밀번호와 다르게 입력해주세요."
            );
        }

        String passwordHash = passwordEncoder.encode(newPassword);

        int updatedRows =
                memberMapper.updatePassword(memberId, passwordHash);

        if (updatedRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "PASSWORD_UPDATE_FAILED",
                    "비밀번호 수정에 실패했습니다."
            );
        }
    }

    @Override
    @Transactional
    public void withdrawMember(
            Long memberId,
            MemberWithdrawalRequest request
    ) {
        if (!request.isAgreed()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "WITHDRAWAL_AGREEMENT_REQUIRED",
                    "회원탈퇴 안내사항에 동의해주세요."
            );
        }

        MemberLoginDto member =
                memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "MEMBER_NOT_FOUND",
                    "회원 정보를 찾을 수 없습니다."
            );
        }

        String currentPassword = request.getCurrentPassword();

        if (currentPassword == null
            || currentPassword.isBlank()
            || currentPassword.getBytes(StandardCharsets.UTF_8).length > 72
            || member.getPasswordHash() == null
            || !passwordEncoder.matches(
                    currentPassword,
                    member.getPasswordHash()
        )) {
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
                "INVALID_CURRENT_PASSWORD",
                "현재 비밀번호가 올바르지 않습니다."
    );
        }

        int blockingGroupCount =
                memberMapper.countWithdrawalBlockingGroups(memberId);

        if (blockingGroupCount > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "WITHDRAWAL_BLOCKED_BY_GROUP",
                    "진행 중이거나 정지된 계모임에 참여하고 있어 "
                            + "지금은 탈퇴할 수 없습니다."
            );
        }

        int updateRows = memberMapper.withdrawMember(memberId);

        if (updateRows != 1) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MEMBER_WITHDRAWAL_FAILED",
                    "회원탈퇴를 처리하지 못했습니다. 다시 시도해주세요."
            );
        }
    }
}