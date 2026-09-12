package vn.edu.dlu.autograder.analyzer;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinnowingEngine {

    private final int kGramLength; // Tối ưu: k = 5 cho các bài tập ngắn
    private final int windowSize;  // Tối ưu: w = 4

    public WinnowingEngine(int kGramLength, int windowSize) {
        this.kGramLength = kGramLength;
        this.windowSize = windowSize;
    }

    public WinnowingEngine() {
        this(5, 4); // Cấu hình mặc định mới cho bài tập lập trình C++ nhỏ
    }

    // 1. Tiền xử lý & Tokenize: Bỏ comment, ẩn danh hóa chuỗi/biến và chuẩn hóa C++
    public String normalizeCppCode(String code) {
        if (code == null || code.isBlank()) return "";

        // a. Xóa comment single-line // ... và multi-line /* ... */
        String cleaned = code.replaceAll("//.*|/\\*.*?\\*/", "");

        // b. Xóa các thư viện #include, pragma
        cleaned = cleaned.replaceAll("#include\\s*<.*?>|#include\\s*\".*?\"", "");

        // c. Chuẩn hóa chuỗi hằng (String & Char Literal) về dạng đại diện ""
        cleaned = cleaned.replaceAll("\".*?\"|'\\\\?.'", "\"\"");

        // d. Tokenize: Thay thế tên biến, tên hàm, số nguyên/thực thành Token đại diện (VAR, NUM)
        // Điều này giúp chống kỹ thuật "Đổi tên biến/hàm" (Variable Renaming)
        cleaned = tokenizeIdentifiers(cleaned);

        // e. Xóa toàn bộ khoảng trắng và chuyển về chữ thường
        return cleaned.replaceAll("\\s+", "").toLowerCase();
    }

    /**
     * Thay thế các từ khóa không phải C++ Reserved Keywords thành token 'V'
     */
    private String tokenizeIdentifiers(String code) {
        // Tập hợp các từ khóa cố định trong C++ không bị thay thế
        Set<String> cppKeywords = new HashSet<>(Arrays.asList(
            "int", "long", "short", "float", "double", "char", "bool", "void", "auto",
            "if", "else", "for", "while", "do", "return", "switch", "case", "break",
            "continue", "using", "namespace", "std", "cin", "cout", "endl", "true", "false",
            "struct", "class", "public", "private", "protected", "new", "delete", "main"
        ));

        Pattern pattern = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");
        Matcher matcher = pattern.matcher(code);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String word = matcher.group();
            if (!cppKeywords.contains(word)) {
                matcher.appendReplacement(sb, "V"); // Đổi tất cả tên biến/hàm tùy chỉnh thành 'V'
            } else {
                matcher.appendReplacement(sb, word);
            }
        }
        matcher.appendTail(sb);

        // Đổi các hằng số nguyên/thực thành 'N'
        return sb.toString().replaceAll("\\b\\d+\\b", "N");
    }

    // 2. Tạo tập Fingerprints từ chuỗi mã nguồn
    public Set<Long> generateFingerprints(String code) {
        String normalized = normalizeCppCode(code);
        if (normalized.length() < kGramLength) {
            return Collections.emptySet();
        }

        // Tạo danh sách K-grams & Hash tương ứng
        List<Long> hashes = new ArrayList<>();
        for (int i = 0; i <= normalized.length() - kGramLength; i++) {
            String kgram = normalized.substring(i, i + kGramLength);
            hashes.add((long) kgram.hashCode());
        }

        // Thuật toán Winnowing với Cửa sổ trượt (Sliding Window)
        Set<Long> fingerprints = new HashSet<>();
        if (hashes.size() < windowSize) {
            fingerprints.addAll(hashes);
            return fingerprints;
        }

        for (int i = 0; i <= hashes.size() - windowSize; i++) {
            long minHash = hashes.get(i);
            // Sửa điều kiện <= : Nếu có nhiều giá trị bằng nhau trong window, ưu tiên lấy phần tử bên phải cùng
            for (int j = 1; j < windowSize; j++) {
                if (hashes.get(i + j) <= minHash) {
                    minHash = hashes.get(i + j);
                }
            }
            fingerprints.add(minHash);
        }

        return fingerprints;
    }

    // 3. Tính chỉ số Jaccard Similarity giữa 2 tập Fingerprints
    public double calculateSimilarity(Set<Long> fp1, Set<Long> fp2) {
        if (fp1 == null || fp2 == null || fp1.isEmpty() || fp2.isEmpty()) return 0.0;

        Set<Long> intersection = new HashSet<>(fp1);
        intersection.retainAll(fp2); // Lấy phần giao nhau

        Set<Long> union = new HashSet<>(fp1);
        union.addAll(fp2); // Lấy phần hợp

        return ((double) intersection.size() / union.size()) * 100.0;
    }
}