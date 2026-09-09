package vn.edu.dlu.autograder.checker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Module 2: Output Checker (So sánh đầu ra)")
public class OutputCheckerTest {

    private OutputChecker checker;

    @BeforeEach
    void setUp() {
        checker = new OutputChecker();
    }

    @Test
    @DisplayName("1. Khớp hoàn toàn (Happy Path)")
    void testExactMatch() {
        String actual = "Hello World\n30\n";
        String expected = "Hello World\n30\n";

        OutputChecker.CheckResult result = checker.check(actual, expected, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);

        assertTrue(result.isMatched(), "Hai chuỗi giống hệt nhau phải trả về True");
    }

    @Test
    @DisplayName("2. Khác biệt định dạng xuống dòng Windows (\\r\\n) vs Linux (\\n)")
    void testLineEndingDifferences() {
        String actualWin = "Dong 1\r\nDong 2\r\n";
        String expectedLinux = "Dong 1\nDong 2\n";

        OutputChecker.CheckResult result = checker.check(actualWin, expectedLinux, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);

        assertTrue(result.isMatched(), "Phải chấp nhận sự khác biệt giữa \\r\\n và \\n");
    }

    @Test
    @DisplayName("3. Khoảng trắng thừa ở cuối mỗi dòng (Trailing Spaces)")
    void testTrailingWhitespaceOnLines() {
        String actual = "Ket qua: 100    \nHoan thanh   \n"; // Thừa khoảng trắng ở cuối
        String expected = "Ket qua: 100\nHoan thanh\n";

        OutputChecker.CheckResult result = checker.check(actual, expected, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);

        assertTrue(result.isMatched(), "Phải bỏ qua khoảng trắng dư thừa ở cuối mỗi dòng");
    }

    @Test
    @DisplayName("4. Các dòng trống dư thừa ở cuối bài nộp (Trailing Newlines)")
    void testMultipleTrailingNewlines() {
        String actual = "30\n\n\n\n"; // Sinh viên cout << endl quá nhiều ở cuối
        String expected = "30\n";

        OutputChecker.CheckResult result = checker.check(actual, expected, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);

        assertTrue(result.isMatched(), "Phải bỏ qua các dòng xuống dòng trống dư thừa ở cuối văn bản");
    }

    @Test
    @DisplayName("5. Kết quả sai (Wrong Answer - WA)")
    void testWrongAnswer() {
        String actual = "Ket qua: 25\n";
        String expected = "Ket qua: 30\n";

        OutputChecker.CheckResult result = checker.check(actual, expected, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);

        assertFalse(result.isMatched(), "Kết quả khác nhau phải báo Sai (Wrong Answer)");
        assertTrue(result.getDiffMessage().contains("Wrong Answer"));
    }

    @Test
    @DisplayName("6. Xử lý giá trị NULL hoặc chuỗi rỗng an toàn")
    void testNullAndEmptyInputs() {
        OutputChecker.CheckResult result1 = checker.check(null, "", OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);
        assertTrue(result1.isMatched(), "Null và rỗng phải được coi là khớp nhau");

        OutputChecker.CheckResult result2 = checker.check("  \n  ", null, OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE);
        assertTrue(result2.isMatched(), "Chuỗi chỉ chứa khoảng trắng/xuống dòng so với null phải khớp nhau sau khi chuẩn hóa");
    }

    @Test
    @DisplayName("7. Chế độ EXACT - Yêu cầu chính xác 100%")
    void testExactModeFailsOnTrailingSpace() {
        String actual = "Hello \n"; // Thừa 1 khoảng trắng
        String expected = "Hello\n";

        OutputChecker.CheckResult result = checker.check(actual, expected, OutputChecker.MatchMode.EXACT);

        assertFalse(result.isMatched(), "Chế độ EXACT phải phân biệt được khoảng trắng thừa");
    }
}