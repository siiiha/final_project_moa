package com.nightstudy.moa.member.service;

import com.nightstudy.moa.common.exception.ApiException;
import com.nightstudy.moa.member.dto.EmailVerificationDto;
import com.nightstudy.moa.member.mapper.EmailVerificationMapper;
import com.nightstudy.moa.member.mapper.MemberMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.time.ZoneOffset;


// 이메일 인증번호 발송, 검증, 회원가입용 토큰 사용 처리
@Service
public class EmailVerificationServiceImpl
        implements EmailVerificationService {

    private final EmailVerificationMapper verificationMapper;
    private final MemberMapper memberMapper;
    private final PasswordEncoder passwordEncoder;
    private final VerificationCodeSender codeSender;
    private final EmailVerificationTokenProvider tokenProvider;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationServiceImpl(
            EmailVerificationMapper verificationMapper,
            MemberMapper memberMapper,
            PasswordEncoder passwordEncoder,
            VerificationCodeSender codeSender,
            EmailVerificationTokenProvider tokenProvider
    ) {
        this.verificationMapper = verificationMapper;
        this.memberMapper = memberMapper;
        this.passwordEncoder = passwordEncoder;
        this.codeSender = codeSender;
        this.tokenProvider = tokenProvider;
    }

    // 인증번호를 저장한 뒤 이메일로 발송
    @Override
    @Transactional
    public Long sendVerificationCode(String email) {
        String normalizedEmail = email.trim();

        if (memberMapper.countByEmail(normalizedEmail) > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_EXISTS",
                    "이미 가입된 이메일입니다."
            );
        }

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        EmailVerificationDto latest =
                verificationMapper.findLatestByEmail(normalizedEmail);

        // 같은 이메일에는 1분 간격으로 재발송 허용
        if (latest != null
                && latest.getCreatedAt().plusMinutes(1).isAfter(now)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "EMAIL_SEND_TOO_FREQUENT",
                    "인증번호는 1분 후 다시 요청해주세요."
            );
        }

        // 앞자리가 0이어도 여섯 자리를 유지
        String code = String.format(
                Locale.ROOT,
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        EmailVerificationDto verification =
                new EmailVerificationDto();

        verification.setEmail(normalizedEmail);
        verification.setCodeHash(passwordEncoder.encode(code));
        verification.setExpiresAt(now.plusMinutes(3));

        int savedRows =
                verificationMapper.insertVerification(verification);

        if (savedRows != 1 || verification.getVerificationId() == null) {
            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EMAIL_VERIFICATION_SAVE_FAILED",
                    "이메일 인증 요청을 저장하지 못했습니다."
            );
        }

        codeSender.send(normalizedEmail, code);

        return verification.getVerificationId();
    }

    // 인증 실패 예외가 발생해도 실패 횟수 증가는 저장
    @Override
    @Transactional(noRollbackFor = ApiException.class)
    public String verifyCode(Long verificationRequestId, String code) {
        EmailVerificationDto verification =
                verificationMapper.findById(verificationRequestId);

        if (verification == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_REQUEST",
                    "이메일 인증 요청을 찾을 수 없습니다."
            );
        }

        // 가장 최근에 발급한 인증번호인지 확인
        EmailVerificationDto latest =
                verificationMapper.findLatestByEmail(
                        verification.getEmail()
                );

        if (latest == null
                || !verificationRequestId.equals(
                latest.getVerificationId()
        )) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OLD_VERIFICATION_REQUEST",
                    "가장 최근에 요청한 인증번호를 입력해주세요."
            );
        }

        if (verification.getVerifiedAt() != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "EMAIL_ALREADY_VERIFIED",
                    "이미 인증이 완료된 요청입니다."
            );
        }

        if (!verification.getExpiresAt().isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw new ApiException(
                    HttpStatus.GONE,
                    "VERIFICATION_EXPIRED",
                    "인증번호가 만료되었습니다. 다시 요청해주세요."
            );
        }

        if (verification.getFailedAttempts() >= 5) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "VERIFICATION_ATTEMPTS_EXCEEDED",
                    "인증번호 입력 횟수를 초과했습니다. 다시 요청해주세요."
            );
        }

        // 번호가 틀리면 실패 횟수를 저장하고 인증 거절
        if (!passwordEncoder.matches(code, verification.getCodeHash())) {
            int updatedRows =
                    verificationMapper.incrementFailedAttempts(
                            verificationRequestId
                    );

            if (updatedRows != 1) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "VERIFICATION_NOT_AVAILABLE",
                        "인증을 진행할 수 없습니다. 다시 요청해주세요."
                );
            }

            if (verification.getFailedAttempts() + 1 >= 5) {
                throw new ApiException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "VERIFICATION_ATTEMPTS_EXCEEDED",
                        "인증번호 입력 횟수를 초과했습니다. 다시 요청해주세요."
                );
            }

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_CODE",
                    "인증번호가 일치하지 않습니다."
            );
        }

        // 인증번호가 맞으면 5분짜리 회원가입용 토큰 생성
        String token = tokenProvider.createToken();
        String tokenHash = tokenProvider.hashToken(token);
        LocalDateTime tokenExpiresAt =
                LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5);

        // 원본 토큰 대신 해시를 인증 완료 시간과 함께 저장
        int updatedRows = verificationMapper.markVerifiedWithToken(
                verificationRequestId,
                tokenHash,
                tokenExpiresAt
        );

        if (updatedRows != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VERIFICATION_NOT_AVAILABLE",
                    "인증을 완료할 수 없습니다. 다시 요청해주세요."
            );
        }

        return token;
    }

    // 회원가입할 이메일과 토큰을 확인하고 한 번만 사용 처리
    @Override
    @Transactional
    public void consumeVerificationToken(String email, String token) {
        if (email == null || email.isBlank()
                || token == null
                || !token.matches("^[A-Za-z0-9_-]{43}$")) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_TOKEN",
                    "이메일 인증 정보가 올바르지 않습니다."
            );
        }

        String tokenHash = tokenProvider.hashToken(token);

        // SQL에서 이메일 일치, 인증 완료, 만료, 사용 여부를 확인
        int updatedRows = verificationMapper.consumeVerificationToken(
                email.trim(),
                tokenHash
        );

        if (updatedRows != 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_VERIFICATION_TOKEN",
                    "인증 정보가 유효하지 않습니다. 이메일 인증을 다시 진행해주세요."
            );
        }
    }
}
