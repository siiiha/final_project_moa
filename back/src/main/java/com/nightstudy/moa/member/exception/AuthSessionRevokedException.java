package com.nightstudy.moa.member.exception;

import com.nightstudy.moa.common.exception.ApiException;
import org.springframework.http.HttpStatus;

// 인증 거절하면서 세션 폐기 기록은 유지해야 하는 경우 사용
public class AuthSessionRevokedException extends ApiException {

    public AuthSessionRevokedException(
            HttpStatus status,
            String code,
            String message
    ) {
        super(status, code, message);
    }
}
