package vn.edu.dlu.autograder.analyzer;

import vn.edu.dlu.autograder.model.AnalysisResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PotentialBugAnalyzer {

    public void analyze(String normalizedCode, AnalysisResult result) {
        checkNullPointerDereference(normalizedCode, result);
        checkMissingReturnInNonVoid(normalizedCode, result);
        checkOutOfBoundsArrayAccess(normalizedCode, result);
    }

    private void checkNullPointerDereference(String code, AnalysisResult result) {
        Pattern uninitPtrPattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\s*\\*\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*;\\s*\\*\\1");
        if (uninitPtrPattern.matcher(code).find()) {
            result.addViolation("Lỗi con trỏ: Sử dụng con trỏ chưa được khởi tạo (Uninitialized Pointer Dereference).");
        }
    }

    private void checkMissingReturnInNonVoid(String code, AnalysisResult result) {
        Pattern nonVoidFuncPattern = Pattern.compile("\\b(int|long|double|float|bool|string)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\([^)]*\\)\\s*\\{");
        Matcher matcher = nonVoidFuncPattern.matcher(code);

        while (matcher.find()) {
            String funcName = matcher.group(2);
            if (funcName.equals("main")) continue; // Bỏ qua main() vì C++11 cho phép ẩn return 0

            int startIdx = matcher.end();
            int openBraces = 1;
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
            if (!body.contains("return ")) {
                result.addViolation("Hàm trả về giá trị [" + funcName + "] không có câu lệnh 'return'.");
            }
        }
    }

    private void checkOutOfBoundsArrayAccess(String code, AnalysisResult result) {
        Pattern declPattern = Pattern.compile("\\bint\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\[\\s*(\\d+)\\s*\\]");
        Matcher declMatcher = declPattern.matcher(code);

        while (declMatcher.find()) {
            String arrName = declMatcher.group(1);
            int size = Integer.parseInt(declMatcher.group(2));

            Pattern accessPattern = Pattern.compile("\\b" + arrName + "\\s*\\[\\s*(\\d+)\\s*\\]");
            Matcher accessMatcher = accessPattern.matcher(code);

            while (accessMatcher.find()) {
                int index = Integer.parseInt(accessMatcher.group(1));
                if (index >= size) {
                    result.addViolation("Lỗi truy cập mảng: " + arrName + "[" + index + "] vượt quá kích thước mảng [" + size + "].");
                }
            }
        }
    }
}