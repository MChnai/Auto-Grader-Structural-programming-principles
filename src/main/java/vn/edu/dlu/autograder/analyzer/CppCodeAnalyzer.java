package vn.edu.dlu.autograder.analyzer;

import vn.edu.dlu.autograder.model.AnalysisResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class CppCodeAnalyzer {

    private static final List<String> BANNED_FUNCTIONS = Arrays.asList("system", "exec", "fork", "popen", "fopen", "freopen", "remove", "rename");
    private static final List<String> BANNED_HEADERS = Arrays.asList("bits/stdc++.h", "cstdlib", "process.h");

    private final AdvancedStructureAnalyzer structureAnalyzer = new AdvancedStructureAnalyzer();
    private final CodeStyleAnalyzer styleAnalyzer = new CodeStyleAnalyzer();
    private final PotentialBugAnalyzer bugAnalyzer = new PotentialBugAnalyzer();

    public AnalysisResult analyze(Path sourceFilePath) {
        return analyze(sourceFilePath, false, false);
    }

    public AnalysisResult analyze(Path sourceFilePath, boolean requireRecursion, boolean requireStruct) {
        AnalysisResult result = new AnalysisResult();

        if (sourceFilePath == null || !Files.exists(sourceFilePath)) {
            result.addViolation("File mã nguồn không tồn tại.");
            return result;
        }

        try {
            String rawCode = Files.readString(sourceFilePath);
            String normalizedCode = normalizeSourceCode(rawCode);

            // 1. Kiểm tra An ninh cơ bản
            checkBannedHeaders(normalizedCode, result);
            checkBannedFunctions(normalizedCode, result);

            // 2. Cập nhật các chỉ số cơ bản (loopCount, functionCount)
            result.setLoopCount(countMatches(normalizedCode, "\\b(for|while|do)\\b"));
            result.setFunctionCount(countMatches(normalizedCode, "\\b(int|void|long|double|float|bool|string)\\s+[a-zA-Z_][a-zA-Z0-9_]*\\s*\\("));

            // 3. THỐNG KÊ NAMESPACE & PACKAGE (Bổ sung thu thập dữ liệu trước)
            result.setNamespaceCount(countMatches(normalizedCode, "\\bnamespace\\s+[a-zA-Z_][a-zA-Z0-9_]*"));
            scanPackagesFromDirectory(sourceFilePath.getParent(), result);

            // 4. Phân tích Cấu trúc, Style & Potential Bugs
            structureAnalyzer.analyze(normalizedCode, result, requireRecursion, requireStruct);
            styleAnalyzer.analyze(rawCode, normalizedCode, result);
            bugAnalyzer.analyze(normalizedCode, result);
            
            // 5. ĐÁNH GIÁ ĐIỂM KIẾN TRÚC (Gọi sau khi đã có đủ dữ liệu ở Bước 3)
            result.evaluateArchitectureScore();

        } catch (IOException e) {
            result.addViolation("Không thể đọc file mã nguồn: " + e.getMessage());
        }

        return result;
    }

    private String normalizeSourceCode(String code) {
        if (code == null) return "";
        String noStrings = code.replaceAll("\"([^\"\\\\]|\\\\.)*\"", "\"\"").replaceAll("'([^'\\\\]|\\\\.)*'", "''");
        String noComments = noStrings.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//.*", "");
        String singleSpaced = noComments.replaceAll("\\s+", " ");
        return singleSpaced.replaceAll("(\\b[a-zA-Z_][a-zA-Z0-9_]*)\\s+\\(", "$1(");
    }

    private void checkBannedHeaders(String code, AnalysisResult result) {
        for (String header : BANNED_HEADERS) {
            if (Pattern.compile("#\\s*include\\s*[<\"]" + Pattern.quote(header) + "[>\"]").matcher(code).find()) {
                result.addViolation("Sử dụng thư viện bị cấm: <" + header + ">");
            }
        }
    }

    private void checkBannedFunctions(String code, AnalysisResult result) {
        for (String func : BANNED_FUNCTIONS) {
            if (Pattern.compile("\\b" + func + "\\(").matcher(code).find()) {
                result.addViolation("Sử dụng hàm hệ thống bị cấm: " + func + "()");
            }
        }
    }

    private int countMatches(String code, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(code);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private void scanPackagesFromDirectory(Path rootDir, AnalysisResult result) {
        if (rootDir == null || !Files.exists(rootDir)) return;
 
        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(Files::isDirectory)
                  .filter(p -> !p.equals(rootDir))
                  .forEach(p -> result.addDetectedPackage(rootDir.relativize(p).toString()));
        } catch (IOException ignored) {}
    }
}