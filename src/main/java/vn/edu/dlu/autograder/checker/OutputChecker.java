package vn.edu.dlu.autograder.checker;

public class OutputChecker {

    public enum MatchMode {
        EXACT,                     // Khớp chính xác 100%
        IGNORE_TRAILING_WHITESPACE // Bỏ qua khoảng trắng cuối dòng & dòng trống cuối file (Tiêu chuẩn)
    }

    public static class CheckResult {
        private final boolean matched;
        private final String diffMessage;

        public CheckResult(boolean matched, String diffMessage) {
            this.matched = matched;
            this.diffMessage = diffMessage;
        }

        public boolean isMatched() { return matched; }
        public String getDiffMessage() { return diffMessage; }
    }

    /**
     * So sánh Output thực tế thu được từ C++ với Expected Output chuẩn
     */
    public CheckResult check(String actualOutput, String expectedOutput, MatchMode mode) {
        if (actualOutput == null) actualOutput = "";
        if (expectedOutput == null) expectedOutput = "";

        if (mode == MatchMode.EXACT) {
            boolean isMatch = actualOutput.equals(expectedOutput);
            return new CheckResult(isMatch, isMatch ? "Khớp hoàn toàn" : "Khác biệt ký tự/xuống dòng");
        }

        // Chế độ IGNORE_TRAILING_WHITESPACE (Chuẩn hóa trước khi so sánh)
        String normalizedActual = normalizeText(actualOutput);
        String normalizedExpected = normalizeText(expectedOutput);

        boolean isMatch = normalizedActual.equals(normalizedExpected);
        
        String diff = isMatch ? "Kết quả đúng (Accepted)" : 
                "Kết quả sai (Wrong Answer).\n- Kỳ vọng:\n" + normalizedExpected + "\n- Thực tế:\n" + normalizedActual;

        return new CheckResult(isMatch, diff);
    }

    /**
     * Hàm chuẩn hóa chuỗi:
     * 1. Xóa ký tự \r của Windows (\r\n -> \n, \r đơn lẻ -> xóa)
     * 2. Bỏ khoảng trắng ở cuối mỗi dòng
     * 3. Bỏ các ký tự xuống dòng/khoảng trắng dư thừa ở cuối văn bản
     */
    private String normalizeText(String input) {
        if (input == null || input.isEmpty()) return "";
        
        // 1. Loại bỏ triệt để ký tự \r của Windows, ép về chuẩn \n duy nhất
        String text = input.replace("\r\n", "\n").replace("\r", "");
        
        // 2. Tách từng dòng để loại bỏ khoảng trắng thừa ở cuối dòng (stripTrailing)
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i].stripTrailing());
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        
        // 3. Loại bỏ toàn bộ xuống dòng & khoảng trắng dư thừa ở cuối file
        return sb.toString().stripTrailing();
    }
}