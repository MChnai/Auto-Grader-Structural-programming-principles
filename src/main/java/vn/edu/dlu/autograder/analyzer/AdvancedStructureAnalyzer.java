package vn.edu.dlu.autograder.analyzer;

import vn.edu.dlu.autograder.model.AnalysisResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AdvancedStructureAnalyzer {

    private static final long MAX_STACK_ARRAY_SIZE = 1_000_000;

    public void analyze(String normalizedCode, AnalysisResult result, boolean requireRecursion, boolean requireStruct) {
        checkRecursion(normalizedCode, result, requireRecursion);
        checkLargeStackArrays(normalizedCode, result);
        checkStructDefinition(normalizedCode, result, requireStruct);
    }

    private void checkRecursion(String code, AnalysisResult result, boolean requireRecursion) {
        Pattern funcPattern = Pattern.compile("\\b(int|void|long|double|float|bool|string)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)\\s*\\{");
        Matcher matcher = funcPattern.matcher(code);

        boolean isRecursive = false;
        while (matcher.find()) {
            String funcName = matcher.group(2);
            if (funcName.equals("main")) continue;

            int openBraces = 1;
            int startIdx = matcher.end();
            int endIdx = startIdx;

            for (int i = startIdx; i < code.length(); i++) {
                if (code.charAt(i) == '{') openBraces++;
                else if (code.charAt(i) == '}') openBraces--;

                if (openBraces == 0) {
                    endIdx = i;
                    break;
                }
            }

            String body = code.substring(startIdx, endIdx);
            if (Pattern.compile("\\b" + funcName + "\\s*\\(").matcher(body).find()) {
                isRecursive = true;
                break;
            }
        }

        result.setRecursive(isRecursive);
        if (requireRecursion && !isRecursive) {
            result.addViolation("Yêu cầu bài toán: Phải sử dụng Đệ quy (Recursion) nhưng không phát hiện.");
        }
    }

    private void checkLargeStackArrays(String code, AnalysisResult result) {
        Pattern arrayPattern = Pattern.compile("\\b(int|long|double|float|char|bool)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\[\\s*(\\d+)\\s*\\]");
        Matcher matcher = arrayPattern.matcher(code);

        while (matcher.find()) {
            long size = Long.parseLong(matcher.group(2));
            if (size > MAX_STACK_ARRAY_SIZE) {
                result.addViolation("Khai báo mảng tĩnh quá lớn trên Stack [" + size + " phần tử]. Rủi ro gây Stack Overflow!");
            }
        }
    }

    private void checkStructDefinition(String code, AnalysisResult result, boolean requireStruct) {
        boolean hasStruct = Pattern.compile("\\b(struct|class)\\s+[a-zA-Z_][a-zA-Z0-9_]*").matcher(code).find();
        result.setStructDefined(hasStruct);

        if (requireStruct && !hasStruct) {
            result.addViolation("Yêu cầu bài toán: Phải định nghĩa Cấu trúc dữ liệu (struct/class Node) nhưng không tìm thấy.");
        }
    }
}