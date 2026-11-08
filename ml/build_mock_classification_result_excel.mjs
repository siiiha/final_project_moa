import fs from "node:fs/promises";
import { createRequire } from "node:module";
import path from "node:path";

// AI작성: 프로젝트 의존성을 추가하지 않고, Codex에 제공된 스프레드시트 런타임만 사용합니다.
const require = createRequire("C:/Users/user1/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/artifact-loader.cjs");
const { SpreadsheetFile, Workbook } = require("@oai/artifact-tool");

const outputDir = path.resolve("ml", "outputs");
const outputPath = path.join(outputDir, "card_expense_mock_classification_result_v1.xlsx");

// AI작성: 가상 입력 20건에 현재 규칙 우선·ML 보조 결과를 붙인 검토용 결과입니다.
const records = [
  [new Date(2026, 9, 1, 12, 10), "사강식당", 58000, "승인", "식비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 1, 15, 30), "왕조2동공영주차장", 3000, "승인", "교통비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 2, 10, 0), "6층 공용 회의실", 15000, "승인", "대관료", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 2, 14, 20), "동네 미술재료", 42000, "승인", "물품비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 3, 19, 0), "대덕문화전당", 18000, "승인", "활동비", "ML", 0.6063, "검토 대기", "", "아니오"],
  [new Date(2026, 9, 4, 16, 10), "그린호텔", 140000, "승인", "숙박비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 5, 9, 30), "카드 연회비", 12000, "승인", "수수료", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 5, 17, 40), "동네꽃방", 25000, "승인", "기타", "ML", 0.8901, "검토 대기", "", "아니오"],
  [new Date(2026, 9, 6, 11, 15), "캠핑장비 판매", 89000, "승인", "물품비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 6, 13, 20), "행복 공예체험", 22000, "승인", "활동비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 7, 12, 35), "서울역 KTX", 59800, "승인", "교통비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 7, 18, 10), "네이버페이 주문", 36400, "승인", "", "결제대행 규칙", "", "확인 필요", "", "아니오"],
  [new Date(2026, 9, 8, 10, 45), "보드게임 카페", 18000, "승인", "식비", "ML", 0.2539, "확인 필요", "", "아니오"],
  [new Date(2026, 9, 8, 15, 5), "삼각대팩", 37000, "승인", "물품비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 9, 12, 0), "따뜻한찻집", 12000, "승인", "활동비", "ML", 0.1729, "확인 필요", "", "아니오"],
  [new Date(2026, 9, 9, 20, 15), "국립서울과학관", 6000, "승인", "교통비", "ML", 0.5161, "검토 대기", "", "아니오"],
  [new Date(2026, 9, 10, 9, 50), "리페어룸", 30000, "승인", "기타", "ML", 0.4183, "확인 필요", "", "아니오"],
  [new Date(2026, 9, 10, 14, 0), "메가박스 상암", 28000, "승인", "활동비", "ML", 0.7134, "검토 대기", "", "아니오"],
  [new Date(2026, 9, 11, 16, 20), "러닝화 아울렛", 99000, "승인", "물품비", "키워드 규칙", "", "검토 대기", "", "아니오"],
  [new Date(2026, 9, 11, 19, 30), "사계절", 45000, "승인", "식비", "ML", 0.2695, "확인 필요", "", "아니오"],
];

const workbook = Workbook.create();
const sheet = workbook.worksheets.add("분류 결과 검토");
sheet.showGridLines = false;
sheet.getRange("A2:J2").merge();
sheet.getRange("A2").values = [["가상 카드 지출 분류 결과 검토"]];
sheet.getRange("A3:J3").merge();
sheet.getRange("A3").values = [["노란색 열에서 사용자가 최종 분류를 입력하고 확정 여부를 변경하는 가상 흐름입니다."]];
sheet.getRange("A5:J5").values = [["이용일시", "가맹점명", "이용금액", "승인상태", "추천 항목", "분류 방식", "ML 확신도", "검토 상태", "사용자 최종 분류", "확정 여부"]];
sheet.getRange(`A6:J${5 + records.length}`).values = records;

sheet.getRange("A2:J2").format = { font: { name: "Arial", size: 14, bold: true, color: "#1F2937" }, horizontalAlignment: "left", verticalAlignment: "center" };
sheet.getRange("A3:J3").format = { font: { name: "Arial", size: 10, italic: true, color: "#6B7280" }, horizontalAlignment: "left" };
sheet.getRange("A5:J5").format = { fill: "#1F4E78", font: { name: "Arial", size: 10, bold: true, color: "#FFFFFF" }, horizontalAlignment: "center", verticalAlignment: "center", borders: { preset: "all", style: "thin", color: "#D9E2F3" } };
sheet.getRange(`A6:J${5 + records.length}`).format = { font: { name: "Arial", size: 10, color: "#1F2937" }, verticalAlignment: "center", borders: { insideHorizontal: { style: "thin", color: "#E5E7EB" } } };
sheet.getRange(`A6:A${5 + records.length}`).format.numberFormat = "yyyy-mm-dd hh:mm";
sheet.getRange(`C6:C${5 + records.length}`).format.numberFormat = "#,##0\"원\"";
sheet.getRange(`G6:G${5 + records.length}`).format.numberFormat = "0.0%";
sheet.getRange(`C6:C${5 + records.length}`).format.horizontalAlignment = "right";
sheet.getRange(`G6:G${5 + records.length}`).format.horizontalAlignment = "right";
sheet.getRange(`I6:J${5 + records.length}`).format = { fill: "#FFF2CC", font: { name: "Arial", size: 10, color: "#1F2937" }, verticalAlignment: "center", borders: { insideHorizontal: { style: "thin", color: "#E5E7EB" } } };
sheet.getRange(`I6:I${5 + records.length}`).dataValidation = { rule: { type: "list", values: ["식비", "교통비", "대관료", "물품비", "활동비", "숙박비", "수수료", "기타"] } };
sheet.getRange(`J6:J${5 + records.length}`).dataValidation = { rule: { type: "list", values: ["예", "아니오"] } };
sheet.getRange(`H6:H${5 + records.length}`).conditionalFormats.add("containsText", { text: "확인 필요", format: { fill: "#FDE2E2", font: { bold: true, color: "#B91C1C" } } });
sheet.getRange("A2:J2").format.rowHeight = 24;
sheet.getRange("A5:J5").format.rowHeight = 22;
const widths = [18, 23, 13, 10, 12, 14, 11, 12, 16, 10];
for (let index = 0; index < widths.length; index += 1) {
  sheet.getRangeByIndexes(4, index, 21, 1).format.columnWidth = widths[index];
}
sheet.freezePanes.freezeRows(5);
sheet.tables.add(`A5:J${5 + records.length}`, true, "MockClassificationResultTable");

workbook.recalculate();
await fs.mkdir(outputDir, { recursive: true });
const output = await SpreadsheetFile.exportXlsx(workbook);
await output.save(outputPath);
console.log(outputPath);
