package vn.edu.dlu.autograder.analyzer;

import java.util.*;

public class WinnowingEngine {

    private final int kGramLength; // Độ dài K-gram (thường từ 8 - 12)
    private final int windowSize;  // Kích thước cửa sổ trượt W (thường từ 4 - 8)

    public WinnowingEngine(int kGramLength, int windowSize) {
        this.kGramLength = kGramLength;
        this.windowSize = windowSize;
    }

    public WinnowingEngine() {
        this(10, 5); // Cấu hình mặc định tối ưu cho bài tập C++
    }

    // 1. Tiền xử lý: Bỏ comment, khoảng trắng và chuẩn hóa code C++
    public String normalizeCppCode(String code) {
        // Xóa comment single-line // ... và multi-line /* ... */
        String noComments = code.replaceAll("//.*|/\\*.*?\\*/", "");
        // Xóa include, pragma và khoảng trắng dư thừa
        String cleaned = noComments.replaceAll("#include\\s*<.*?>|#include\\s*\".*?\"", "")
                                   .replaceAll("\\s+", "");
        return cleaned.toLowerCase();
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
            hashes.add((long) kgram.hashCode()); // Có thể đổi sang Rabin-Karp Hash
        }

        // Thuật toán Winnowing với Cửa sổ trượt (Sliding Window)
        Set<Long> fingerprints = new HashSet<>();
        if (hashes.size() < windowSize) {
            fingerprints.addAll(hashes);
            return fingerprints;
        }

        for (int i = 0; i <= hashes.size() - windowSize; i++) {
            long minHash = hashes.get(i);
            // Tìm giá trị hash nhỏ nhất trong cửa sổ hiện tại
            for (int j = 1; j < windowSize; j++) {
                if (hashes.get(i + j) < minHash) {
                    minHash = hashes.get(i + j);
                }
            }
            fingerprints.add(minHash);
        }

        return fingerprints;
    }

    // 3. Tính chỉ số Jaccard Similarity giữa 2 tập Fingerprints
    public double calculateSimilarity(Set<Long> fp1, Set<Long> fp2) {
        if (fp1.isEmpty() || fp2.isEmpty()) return 0.0;

        Set<Long> intersection = new HashSet<>(fp1);
        intersection.retainAll(fp2); // Lấy phần giao nhau

        Set<Long> union = new HashSet<>(fp1);
        union.addAll(fp2); // Lấy phần hợp

        return ((double) intersection.size() / union.size()) * 100.0;
    }
}