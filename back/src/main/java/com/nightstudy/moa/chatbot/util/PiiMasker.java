package com.nightstudy.moa.chatbot.util;

import java.util.regex.Pattern;

public final class PiiMasker {

    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}"
    );
    private static final Pattern RESIDENT_NUMBER = Pattern.compile(
            "(?<!\\d)\\d{6}[- ]?[1-8]\\d{6}(?!\\d)"
    );
    private static final Pattern PHONE = Pattern.compile(
            "(?<!\\d)01[016789][- ]?\\d{3,4}[- ]?\\d{4}(?!\\d)"
    );
    private static final Pattern CARD = Pattern.compile(
            "(?<!\\d)\\d{4}(?:[- ]?\\d{4}){3}(?!\\d)"
    );
    private static final Pattern ACCOUNT = Pattern.compile(
            "(?<![\\d-])\\d{2,6}-\\d{2,6}-\\d{2,8}(?:-\\d{1,6})?(?![\\d-])"
    );

    private PiiMasker() {
    }

    public static String mask(String message) {
        String masked = EMAIL.matcher(message).replaceAll("[이메일]");
        masked = RESIDENT_NUMBER.matcher(masked).replaceAll("[주민등록번호]");
        masked = PHONE.matcher(masked).replaceAll("[전화번호]");
        masked = CARD.matcher(masked).replaceAll("[카드번호]");
        return ACCOUNT.matcher(masked).replaceAll("[계좌번호]");
    }
}