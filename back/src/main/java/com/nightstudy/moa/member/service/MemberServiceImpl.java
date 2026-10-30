package com.nightstudy.moa.member.service;

import java.util.HashSet;
import java.util.ArrayList;

import com.nightstudy.moa.common.auth.JwtTokenProvider;
import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.*;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nightstudy.moa.member.dto.MemberRegionDto;

import java.util.List;
import com.nightstudy.moa.common.config.PortOneProperties;
import com.nightstudy.moa.member.dto.IdentityVerificationStartResponse;
import org.springframework.util.StringUtils;
import java.util.UUID;
import com.nightstudy.moa.member.dto.IdentityVerificationRequest;
import com.nightstudy.moa.member.dto.PortOneIdentityVerificationResponse;
import org.springframework.transaction.annotation.Isolation;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import java.nio.charset.StandardCharsets;

@Service
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailVerificationService emailVerificationService;
    private final AuthSessionService authSessionService;
    private final KakaoApiClient kakaoApiClient;
    private final PortOneProperties portOneProperties;
    private final PortOneApiClient portOneApiClient;

    public MemberServiceImpl(
            MemberMapper memberMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            EmailVerificationService emailVerificationService,
            AuthSessionService authSessionService,
            KakaoApiClient kakaoApiClient,
            PortOneProperties portOneProperties,
            PortOneApiClient portOneApiClient
    ) {
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.emailVerificationService = emailVerificationService;
        this.authSessionService = authSessionService;
        this.kakaoApiClient = kakaoApiClient;
        this.portOneProperties = portOneProperties;
        this.portOneApiClient = portOneApiClient;
    }

    // 기존 회원 Mapper를 재사용해 약관 목록 조회
    @Override
    @Transactional(readOnly = true)
    public List<MemberTermDto> getTerms() {
        return memberMapper.findTerms();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberRegionDto> getRegions() {
        return memberMapper.findRegions();
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
        String email = socialSignup ? null : request.getEmail().trim();
        String nickname = request.getNickname().trim();

        if (!socialSignup && memberMapper.countByEmail(email) > 0) {
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
        if (!socialSignup) {
            emailVerificationService.consumeVerificationToken(
                    email,
                    request.getEmailVerificationToken()
            );
        }

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

    // 위치 정확도와 가입 활동 지역을 확인한 뒤 인증 시각 저장
    @Override
    @Transactional
    public MemberProfileResponse verifyRegion(
            Long memberId,
            RegionVerificationRequest request
    ) {
        if (request == null
                || request.getLatitude() == null
                || request.getLongitude() == null
                || request.getAccuracy() == null
                || !Double.isFinite(request.getLatitude())
                || !Double.isFinite(request.getLongitude())
                || !Double.isFinite(request.getAccuracy())
                || Math.abs(request.getLatitude()) > 90
                || Math.abs(request.getLongitude()) > 180
                || request.getAccuracy() <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_LOCATION",
                    "현재 위치 정보를 다시 확인해주세요."
            );
        }

        if (request.getAccuracy() > 500) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "LOCATION_ACCURACY_LOW",
                    "위치가 정확하지 않습니다. 예상 오차 반경 500m 이하에서 다시 시도해주세요."
            );
        }

        // 외부 조회를 마친 후 회원 행을 잠궈 DB 잠금 시간 줄임
        KakaoRegionResponse.Region actualRegion = kakaoApiClient.getRegion(
                request.getLatitude(),
                request.getLongitude()
        );

        MemberLoginDto member = memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null || !"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_UNAVAILABLE",
                    "현재 계정 상태에서는 지역 인증을 진행할 수 없습니다."
            );
        }

        MemberProfileResponse profile = getMyProfile(memberId);

        // AI작성: 세종시는 하위 시·군·구 없이 시·도 일치 여부로 인증합니다.
        boolean isSejong =
                "세종특별자치시".equals(profile.getProvinceName());

        if (profile.getProvinceName() == null
                || profile.getProvinceName().isBlank()
                || (!isSejong && (
                profile.getDistrictName() == null
                        || profile.getDistrictName().isBlank()))) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ACTIVITY_REGION_INCOMPLETE",
                    "가입한 활동 지역의 시·군·구 정보가 없어 인증할 수 없습니다."
            );
        }

        if (!profile.getProvinceName().equals(actualRegion.getProvinceName())
                || (!isSejong && !profile.getDistrictName()
                .equals(actualRegion.getDistrictName()))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REGION_MISMATCH",
                    "현재 위치가 가입한 활동 지역과 다릅니다. 해당 지역에서 다시 시도해주세요."
            );
        }

        int updated = memberMapper.markRegionVerified(
                memberId,
                profile.getRegionId()
        );

        // 이미 인증된 회원이 같은 시각에 재요청한 경우도 정상 처리
        if (updated != 1 && !profile.isRegionVerified()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REGION_VERIFICATION_NOT_SAVED",
                    "인증 결과를 저장하지 못했습니다. 다시 시도해주세요."
            );
        }

        return getMyProfile(memberId);
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

    // AI작성: 이메일 인증 기반 비밀번호 재설정은 마이페이지 비밀번호 변경과 별도 메소드로 유지한다.
    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        String newPassword = request.getNewPassword();

        if (newPassword == null
                || newPassword.isBlank()
                || newPassword.length() < 8
                || newPassword.length() > 20
                || !newPassword.equals(request.getNewPasswordConfirm())
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_NEW_PASSWORD",
                    "새 비밀번호는 8~20자로 입력하고 확인 값도 동일하게 입력해주세요."
            );
        }

        // 비밀번호 재설정 목적으로 인증한 토큰만 허용
        emailVerificationService.consumeVerificationToken(
                request.getEmail(),
                request.getEmailVerificationToken(),
                "RESET_PASSWORD"
        );

        MemberLoginDto member =
                memberMapper.findByEmail(request.getEmail().trim());

        if (member == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ACCOUNT_NOT_FOUND",
                    "해당 이메일로 가입된 계정이 없습니다."
            );
        }

        if (!"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "비밀번호를 재설정할 수 없는 계정 상태입니다."
            );
        }

        // 비밀번호 없이 가입한 소셜 계정은 해당 제공사 로그인을 이용
        if (member.getPasswordHash() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SOCIAL_ACCOUNT_ONLY",
                    "소셜 로그인으로 가입한 계정입니다. 해당 간편 로그인을 이용해주세요."
            );
        }

        String newPasswordHash = passwordEncoder.encode(newPassword);

        int updatedRows = memberMapper.updatePasswordForReset(
                member.getMemberId(),
                member.getPasswordHash(),
                newPasswordHash
        );

        if (updatedRows != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PASSWORD_RESET_CONFLICT",
                    "계정 정보가 변경되었습니다. 다시 인증한 후 시도해주세요."
            );
        }

        // 비밀번호 변경 성공 시 해당 회원의 기존 로그인 세션 폐기
        authSessionService.revokeAllSessions(member.getMemberId());
    }

    // 인증 요청 번호를 서버에서 발급하고 활성 회원에게 연결
    @Override
    @Transactional
    public IdentityVerificationStartResponse startIdentityVerification(
            Long memberId
    ) {
        if (!portOneProperties.isEnabled()
                || !StringUtils.hasText(portOneProperties.getStoreId())
                || !StringUtils.hasText(portOneProperties.getChannelKey())
                || !StringUtils.hasText(portOneProperties.getApiSecret())) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "IDENTITY_VERIFICATION_NOT_CONFIGURED",
                    "본인인증 서비스를 준비 중입니다."
            );
        }

        String identityVerificationId =
                "identity-verification-" + UUID.randomUUID();

        int inserted = memberMapper.insertIdentityVerificationRequest(
                identityVerificationId,
                memberId
        );

        if (inserted != 1) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_UNAVAILABLE",
                    "현재 계정 상태에서는 본인인증을 진행할 수 없습니다."
            );
        }

        return new IdentityVerificationStartResponse(
                identityVerificationId,
                portOneProperties.getStoreId(),
                portOneProperties.getChannelKey()
        );
    }

    // 포트원 조회 결과를 확인한 뒤 인증 요청과 회원을 함께 갱신
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberProfileResponse completeIdentityVerification(
            Long memberId,
            IdentityVerificationRequest request
    ) {
        if (request == null
                || !StringUtils.hasText(request.getIdentityVerificationId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_IDENTITY_VERIFICATION_ID",
                    "본인인증 요청 번호가 필요합니다."
            );
        }

        String verificationId = request.getIdentityVerificationId();

        if (memberMapper.countAvailableIdentityVerificationRequest(
                verificationId, memberId) != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDENTITY_VERIFICATION_REQUEST_INVALID",
                    "인증 요청이 만료되었거나 사용할 수 없습니다. 다시 시작해주세요."
            );
        }

        // 외부 조회를 마친 뒤 회원 행을 잠가 잠금 시간을 줄임
        PortOneIdentityVerificationResponse result =
                portOneApiClient.getIdentityVerification(verificationId);

        if (!"VERIFIED".equals(result.getStatus())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "IDENTITY_VERIFICATION_NOT_COMPLETED",
                    "본인인증이 완료되지 않았습니다."
            );
        }

        // 현재는 설정된 테스트 채널의 결과만 허용
        if (result.getChannel() == null
                || !portOneProperties.getChannelKey()
                .equals(result.getChannel().getKey())
                || !"TEST".equals(result.getChannel().getType())
                || result.getVerifiedAt() == null
                || result.getVerifiedAt().isAfter(
                Instant.now().plusSeconds(60))) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "IDENTITY_VERIFICATION_CHANNEL_INVALID",
                    "설정된 테스트 채널의 인증 결과인지 확인해주세요."
            );
        }

        PortOneIdentityVerificationResponse.VerifiedCustomer customer =
                result.getVerifiedCustomer();

        if (customer == null
                || !StringUtils.hasText(customer.getName())
                || !StringUtils.hasText(customer.getBirthDate())
                || !StringUtils.hasText(customer.getPhoneNumber())) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "IDENTITY_VERIFICATION_CUSTOMER_INVALID",
                    "인증된 회원 정보를 확인할 수 없습니다."
            );
        }

        String birthDate;
        try {
            birthDate = LocalDate.parse(customer.getBirthDate()).toString();
        } catch (DateTimeParseException exception) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "IDENTITY_VERIFICATION_BIRTH_DATE_INVALID",
                    "인증된 생년월일 정보를 확인할 수 없습니다."
            );
        }

        String phoneNumber = customer.getPhoneNumber()
                .replaceAll("[\\s-]", "");

        // 국제전화 표기를 국내 번호 형식으로 맞춤
        if (phoneNumber.startsWith("+82")) {
            phoneNumber = "0" + phoneNumber.substring(3);
        }

        MemberLoginDto member =
                memberMapper.findByMemberIdForUpdate(memberId);

        if (member == null || !"ACTIVE".equals(member.getAccountStatus())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "MEMBER_UNAVAILABLE",
                    "현재 계정 상태에서는 본인인증을 진행할 수 없습니다."
            );
        }

        if (memberMapper.countMemberIdentityMatch(
                memberId,
                customer.getName().trim(),
                birthDate,
                phoneNumber
        ) != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "IDENTITY_VERIFICATION_MEMBER_MISMATCH",
                    "인증된 이름·생년월일·전화번호가 가입 정보와 다릅니다."
            );
        }

        if (memberMapper.consumeIdentityVerificationRequest(
                verificationId, memberId) != 1) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDENTITY_VERIFICATION_REQUEST_INVALID",
                    "인증 요청이 만료되었거나 이미 사용되었습니다."
            );
        }

        int updated = memberMapper.markIdentityVerified(memberId);
        MemberProfileResponse profile = getMyProfile(memberId);

        if (updated != 1 && !profile.isIdentityVerified()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "IDENTITY_VERIFICATION_SAVE_FAILED",
                    "본인인증 결과를 저장하지 못했습니다."
            );
        }

        return profile;
    }
}
