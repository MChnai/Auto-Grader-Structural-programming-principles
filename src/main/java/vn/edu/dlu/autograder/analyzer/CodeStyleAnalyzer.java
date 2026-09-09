package vn.edu.dlu.autograder.analyzer;

import vn.edu.dlu.autograder.model.AnalysisResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeStyleAnalyzer {

    // Cấu hình quy tắc chấm Clean Code (Có thể load từ config YAML/JSON)
    public static class AnalyzerConfig {
        public int maxCyclomaticComplexity = 10;
        public double complexityPenalty = 10.0;
        public double badNamingPenalty = 5.0;
        public double missingCommentPenalty = 5.0;
        public double bonusCommentReward = 10.0;
    }

    private final AnalyzerConfig config;

    public CodeStyleAnalyzer() {
        this(new AnalyzerConfig());
    }

    public CodeStyleAnalyzer(AnalyzerConfig config) {
        this.config = config;
    }

    public void analyze(String rawCode, String normalizedCode, AnalysisResult result) {
        checkCyclomaticComplexity(normalizedCode, result);
        checkNamingConventions(rawCode, result);
        checkCommentDensity(rawCode, normalizedCode, result);
        checkForbiddenLibraries(rawCode, result);
        checkResourceLeaks(rawCode, result);
    }

    private void checkCyclomaticComplexity(String code, AnalysisResult result) {
        int decisionPoints = 1;
        Pattern pattern = Pattern.compile("\\b(if|else if|for|while|case|catch)\\b|&&|\\|\\|");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            decisionPoints++;
        }

        result.setCyclomaticComplexity(decisionPoints);
        if (decisionPoints > config.maxCyclomaticComplexity + 5) {
            result.addWarning("Độ phức tạp Cyclomatic rất cao (" + decisionPoints + "). Cần tách nhỏ hàm.", config.complexityPenalty * 2);
        } else if (decisionPoints > config.maxCyclomaticComplexity) {
            result.addWarning("Độ phức tạp Cyclomatic cao (" + decisionPoints + ").", config.complexityPenalty);
        }
    }

    private void checkNamingConventions(String rawCode, AnalysisResult result) {
        // Kiểm tra biến 1 ký tự không đúng quy chuẩn (trừ i, j, k, n, m, x, y, z)
        Pattern singleCharVar = Pattern.compile("\\b(int|double|float|long|char|auto)\\s+([a-zA-Z])\\s*(=|;|,|\\))");
        Matcher matcher = singleCharVar.matcher(rawCode);

        int badNames = 0;
        while (matcher.find()) {
            String var = matcher.group(2);
            if (!var.matches("[ijkmnxyzXYZ]")) {
                badNames++;
            }
        }

        if (badNames > 0) {
            result.addWarning("Phát hiện " + badNames + " biến đặt tên quá ngắn / không rõ nghĩa.", config.badNamingPenalty);
        }
    }

    private void checkCommentDensity(String rawCode, String normalizedCode, AnalysisResult result) {
        int rawLength = rawCode.length();
        int normalizedLength = normalizedCode.length();

        if (rawLength == 0) return;
        double commentRatio = (double) (rawLength - normalizedLength) / rawLength;

        if (commentRatio >= 0.15) {
            result.addWarning("[BONUS] Mã nguồn có chú thích thuật toán chi tiết (+10% Clean Code).", -config.bonusCommentReward);
        } else if (commentRatio < 0.03 && rawLength > 200) {
            result.addWarning("Thiếu chú thích (comment) giải thích luồng xử lý.", config.missingCommentPenalty);
        }
    }

    // Nâng cấp: Kiểm tra thư viện cấm/không an toàn (VD: system("pause"), conio.h)
    private void checkForbiddenLibraries(String rawCode, AnalysisResult result) {
        if (rawCode.contains("#include <conio.h>") || rawCode.contains("#include <windows.h>")) {
            result.addWarning("Sử dụng thư viện phụ thuộc HĐH không di động (<conio.h>/<windows.h>).", 10.0);
        }
        if (rawCode.contains("system(\"pause\")") || rawCode.contains("system(\"cls\")")) {
            result.addWarning("Sử dụng lệnh `system()` ảnh hưởng tới hiệu năng và bảo mật.", 10.0);
        }
    }

    // Nâng cấp: Phát hiện nguy cơ thất thoát bộ nhớ (Memory Leak) cơ bản
    private void checkResourceLeaks(String rawCode, AnalysisResult result) {
        int newCount = countOccurrences(rawCode, "\\bnew\\b");
        int deleteCount = countOccurrences(rawCode, "\\bdelete\\b");

        if (newCount > deleteCount) {
            result.addWarning("Cảnh báo Memory Leak: Số lần cấp phát (`new`: " + newCount + ") nhiều hơn số lần giải phóng (`delete`: " + deleteCount + ").", 10.0);
        }
    }

    private int countOccurrences(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}