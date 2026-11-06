package com.nightstudy.moa.savingGroup.cardExpense;

import com.nightstudy.moa.common.exception.ApiException;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI작성: 신한카드 명세서처럼 HTML 표를 .xls 확장자로 내려받는 파일을 읽는다.
 * 카드번호·할부·포인트 등 분류에 불필요한 금융정보는 읽어 저장하지 않는다.
 */
@Component
public class CardExpenseStatementParser {
    private static final String INVALID_STATEMENT_CODE = "CARD_EXPENSE_STATEMENT_INVALID";

    public List<CardExpenseParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidStatement("카드 명세서 파일을 선택해 주세요.");
        }

        try {
            Document document = Jsoup.parse(file.getInputStream(), StandardCharsets.UTF_8.name(), "");
            List<CardExpenseParsedRow> rows = new ArrayList<>();
            rows.addAll(parseRows(document, List.of("이용일", "이용가맹점", "이용금액"), "이용금액", CardExpenseApprovalStatus.APPROVED));
            rows.addAll(parseRows(document, List.of("이용일", "이용가맹점", "취소금액"), "취소금액", CardExpenseApprovalStatus.CANCELED));

            if (rows.isEmpty()) {
                throw invalidStatement("카드 사용 또는 취소 내역 표를 찾을 수 없습니다.");
            }
            return List.copyOf(rows);
        } catch (IOException exception) {
            throw invalidStatement("카드 명세서 파일을 읽을 수 없습니다.");
        }
    }

    private List<CardExpenseParsedRow> parseRows(
            Document document,
            List<String> requiredHeaders,
            String amountHeader,
            CardExpenseApprovalStatus approvalStatus
    ) {
        Element table = findTable(document, requiredHeaders);
        if (table == null) {
            return List.of();
        }

        List<String> headers = getHeaders(table);
        int usedDateIndex = headers.indexOf("이용일");
        int merchantNameIndex = headers.indexOf("이용가맹점");
        int amountIndex = headers.indexOf(amountHeader);
        List<CardExpenseParsedRow> rows = new ArrayList<>();

        for (Element row : getBodyRows(table)) {
            List<Element> cells = getDirectChildren(row, "td");
            if (cells.size() <= Math.max(usedDateIndex, Math.max(merchantNameIndex, amountIndex))) {
                continue;
            }

            String merchantName = cells.get(merchantNameIndex).text().trim();
            if (merchantName.isBlank()) {
                continue;
            }

            try {
                rows.add(new CardExpenseParsedRow(
                        parseDate(cells.get(usedDateIndex).text()),
                        merchantName,
                        parseAmount(cells.get(amountIndex).text()),
                        approvalStatus
                ));
            } catch (IllegalArgumentException ignored) {
                // AI작성: 소계·합계 행처럼 날짜·금액 형식이 다른 행은 카드 지출로 저장하지 않는다.
            }
        }
        return rows;
    }

    private Element findTable(Document document, List<String> requiredHeaders) {
        for (Element table : document.select("table")) {
            List<String> headers = getHeaders(table);
            if (headers.containsAll(requiredHeaders)) {
                return table;
            }
        }
        return null;
    }

    private List<String> getHeaders(Element table) {
        Element thead = getDirectChild(table, "thead");
        if (thead == null) {
            return List.of();
        }
        Element firstRow = getDirectChild(thead, "tr");
        if (firstRow == null) {
            return List.of();
        }
        return getDirectChildren(firstRow, "th").stream().map(Element::text).toList();
    }

    private List<Element> getBodyRows(Element table) {
        Element tbody = getDirectChild(table, "tbody");
        return tbody == null ? List.of() : getDirectChildren(tbody, "tr");
    }

    private Element getDirectChild(Element parent, String tagName) {
        return parent.children().stream()
                .filter(child -> child.tagName().equals(tagName))
                .findFirst()
                .orElse(null);
    }

    private List<Element> getDirectChildren(Element parent, String tagName) {
        return parent.children().stream()
                .filter(child -> child.tagName().equals(tagName))
                .toList();
    }

    private LocalDate parseDate(String value) {
        String date = value.replaceAll("[^0-9]", "");
        if (date.length() != 8) {
            throw new IllegalArgumentException("Invalid date");
        }
        try {
            return LocalDate.parse(date, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Invalid date", exception);
        }
    }

    private BigDecimal parseAmount(String value) {
        String amount = value.replaceAll("[^0-9-]", "");
        if (amount.isBlank() || "-".equals(amount)) {
            throw new IllegalArgumentException("Invalid amount");
        }
        return new BigDecimal(amount);
    }

    private ApiException invalidStatement(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, INVALID_STATEMENT_CODE, message);
    }
}
