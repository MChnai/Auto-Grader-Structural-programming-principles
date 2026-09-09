package vn.edu.dlu.autograder.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Luồng Chấm Bài Toàn Diện (SubmissionGraderTest)")
public class SubmissionGraderTest {

    private SubmissionGrader defaultGrader; // Chạy ở chế độ Local mặc định
    private GradingRubric rubric;

    @BeforeEach
    void setUp() {
        // Mặc định chạy LocalExecutor cho các bài test đơn vị thông thường
        defaultGrader = new SubmissionGrader(false);
        // Cấu hình Rubric: Thang điểm 10, Tỷ trọng Testcase 100% (1.0), Tỷ trọng Style/AST 0%
        rubric = new GradingRubric(10.0, 1.0, 0.0);
    }

    // =========================================================================
    // 1. CÁC BÀI TEST CHẠY CHẾ ĐỘ LOCAL EXECUTOR
    // =========================================================================

    @Test
    @DisplayName("1. Tất cả Testcase đúng (All Accepted) -> 10/10 điểm")
    void testGradeSuccessAllPassed(@TempDir Path tempDir) throws IOException {
        String cppCode = "#include <iostream>\n" +
                         "using namespace std;\n" +
                         "int main() {\n" +
                         "    int a, b; if(cin >> a >> b) cout << (a + b);\n" +
                         "    return 0;\n" +
                         "}";

        Path zipFile = createZipFile(tempDir, "2112345_Correct.zip", Map.of("main.cpp", cppCode));
        StudentSubmission submission = new StudentSubmission("2112345", "Nguyen Van A", zipFile);

        List<TestCase> testCases = List.of(
                new TestCase("TC1", "5 10\n", "15\n", 2),
                new TestCase("TC2", "100 200\n", "300\n", 2)
        );

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(10.0, report.getFinalScore(), 0.01);
        assertEquals(2, report.getTestCaseResults().size());

        for (TestCaseResult tcResult : report.getTestCaseResults()) {
            assertEquals(TestCaseResult.Status.ACCEPTED, tcResult.getStatus());
        }
    }

    @Test
    @DisplayName("2. Điểm một phần (Partial Score) -> Đúng 1/2 Testcase")
    void testGradePartialScore(@TempDir Path tempDir) throws IOException {
        String cppCode = "#include <iostream>\n" +
                         "using namespace std;\n" +
                         "int main() {\n" +
                         "    int a, b; cin >> a >> b;\n" +
                         "    cout << (a + b);\n" +
                         "    return 0;\n" +
                         "}";

        Path zipFile = createZipFile(tempDir, "2112346_Partial.zip", Map.of("main.cpp", cppCode));
        StudentSubmission submission = new StudentSubmission("2112346", "Tran Van B", zipFile);

        List<TestCase> testCases = List.of(
                new TestCase("TC1 - P.Cộng", "3 4\n", "7\n", 2),   // Đúng -> Pass
                new TestCase("TC2 - P.Nhân", "3 4\n", "12\n", 2)   // Sai -> Fail (In ra 7)
        );

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(5.0, report.getFinalScore(), 0.01);
        assertEquals(TestCaseResult.Status.ACCEPTED, report.getTestCaseResults().get(0).getStatus());
        assertEquals(TestCaseResult.Status.WRONG_ANSWER, report.getTestCaseResults().get(1).getStatus());
    }

    @Test
    @DisplayName("3. Tất cả Testcase đều sai (All Wrong Answer) -> 0 điểm")
    void testGradeAllWrongAnswer(@TempDir Path tempDir) throws IOException {
        String cppCode = "#include <iostream>\n" +
                         "using namespace std;\n" +
                         "int main() {\n" +
                         "    cout << \"Sai Hoan Toan\";\n" +
                         "    return 0;\n" +
                         "}";

        Path zipFile = createZipFile(tempDir, "2112347_AllWA.zip", Map.of("main.cpp", cppCode));
        StudentSubmission submission = new StudentSubmission("2112347", "Le Van C", zipFile);

        List<TestCase> testCases = List.of(
                new TestCase("TC1", "1 1\n", "2\n", 2),
                new TestCase("TC2", "2 2\n", "4\n", 2)
        );

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);
        for (TestCaseResult tcResult : report.getTestCaseResults()) {
            assertEquals(TestCaseResult.Status.WRONG_ANSWER, tcResult.getStatus());
        }
    }

    @Test
    @DisplayName("4. Lỗi biên dịch (Compile Error - CE) -> 0 điểm")
    void testGradeCompileError(@TempDir Path tempDir) throws IOException {
        String badCppCode = "#include <iostream>\n" +
                            "int main() {\n" +
                            "    std::cout << \"Lỗi biên dịch\"\n" + // Thiếu ';'
                            "    return 0;\n" +
                            "}";

        Path zipFile = createZipFile(tempDir, "2112348_CE.zip", Map.of("main.cpp", badCppCode));
        StudentSubmission submission = new StudentSubmission("2112348", "Pham Van D", zipFile);

        List<TestCase> testCases = List.of(new TestCase("TC1", "", "10", 2));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertFalse(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);
        assertNotNull(report.getCompileMessage());
        assertTrue(report.getCompileMessage().contains("error:"));
    }

    @Test
    @DisplayName("5. Lỗi thực thi (Runtime Error - RTE) bằng std::vector::at()")
    void testGradeRuntimeError(@TempDir Path tempDir) throws IOException {
        String rteCode = "#include <iostream>\n" +
                         "#include <vector>\n" +
                         "using namespace std;\n" +
                         "int main() {\n" +
                         "    vector<int> v = {1, 2, 3};\n" +
                         "    cout << v.at(100);\n" + 
                         "    return 0;\n" +
                         "}";

        Path zipFile = createZipFile(tempDir, "2112349_RTE.zip", Map.of("main.cpp", rteCode));
        StudentSubmission submission = new StudentSubmission("2112349", "Hoang Van E", zipFile);

        List<TestCase> testCases = List.of(new TestCase("TC_RTE", "", "", 2));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);
        assertEquals(TestCaseResult.Status.RUNTIME_ERROR, report.getTestCaseResults().get(0).getStatus());
    }

    @Test
    @DisplayName("6. Vòng lặp vô hạn (Time Limit Exceeded - TLE)")
    void testGradeTimeLimitExceeded(@TempDir Path tempDir) throws IOException {
        String infiniteLoopCode = "int main() { while(true); return 0; }";

        Path zipFile = createZipFile(tempDir, "2112350_TLE.zip", Map.of("main.cpp", infiniteLoopCode));
        StudentSubmission submission = new StudentSubmission("2112350", "Vo Van F", zipFile);

        List<TestCase> testCases = List.of(new TestCase("TC_TLE", "", "", 1));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);
        assertEquals(TestCaseResult.Status.TIME_LIMIT_EXCEEDED, report.getTestCaseResults().get(0).getStatus());
    }

    @Test
    @DisplayName("7. Lỗi file Zip bị hỏng (Corrupted ZIP)")
    void testGradeCorruptedZip(@TempDir Path tempDir) throws IOException {
        Path invalidZip = tempDir.resolve("corrupted.zip");
        Files.writeString(invalidZip, "Đây không phải định dạng file ZIP!");

        StudentSubmission submission = new StudentSubmission("2112351", "Dang Van G", invalidZip);
        List<TestCase> testCases = List.of(new TestCase("TC1", "", "", 2));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertFalse(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);
    }

    @Test
    @DisplayName("8. File ZIP hợp lệ nhưng không chứa file .cpp nào")
    void testGradeValidZipNoCppFiles(@TempDir Path tempDir) throws IOException {
        Path zipFile = createZipFile(tempDir, "2112352_NoCpp.zip", Map.of("README.txt", "Em nộp nhầm file btl"));
        StudentSubmission submission = new StudentSubmission("2112352", "Bui Van H", zipFile);

        List<TestCase> testCases = List.of(new TestCase("TC1", "", "", 2));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertFalse(report.isCompileSuccess());
        assertEquals(0.0, report.getFinalScore(), 0.01);

        String msg = report.getCompileMessage();
        assertNotNull(msg, "Thông báo lỗi biên dịch không được null");
        assertTrue(msg.toLowerCase().contains(".cpp") || msg.contains("Không tìm thấy"), 
                "Thông báo thực tế là: [" + msg + "]");
    }

    @Test
    @DisplayName("9. File ZIP lồng trong nhiều thư mục con (Nested Folders)")
    void testGradeZipWithNestedFolders(@TempDir Path tempDir) throws IOException {
        String cppCode = "#include <iostream>\n" +
                         "using namespace std;\n" +
                         "int main() { cout << \"Nested Success\"; return 0; }";

        Map<String, String> entries = Map.of("src/project/main.cpp", cppCode);
        Path zipFile = createZipFile(tempDir, "2112353_Nested.zip", entries);

        StudentSubmission submission = new StudentSubmission("2112353", "Ngo Van I", zipFile);
        List<TestCase> testCases = List.of(new TestCase("TC1", "", "Nested Success", 2));

        GradingReport report = defaultGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess());
        assertEquals(10.0, report.getFinalScore(), 0.01);
        assertEquals(TestCaseResult.Status.ACCEPTED, report.getTestCaseResults().get(0).getStatus());
    }

    // =========================================================================
    // 2. BÀI TEST CHẠY CHẾ ĐỘ DOCKER SANDBOX MODE
    // =========================================================================

    @Test
    @DisplayName("10. Chấm bài cách ly an toàn trong Docker Sandbox Mode")
    @EnabledIf("isDockerAvailable") // Tự động bỏ qua nếu máy chưa bật Docker
    void testGradeWithDockerSandbox(@TempDir Path tempDir) throws IOException {
        SubmissionGrader dockerGrader = new SubmissionGrader(true);

        String cppCode = "#include <iostream>\n" +
                         "using namespace std;\n" +
                         "int main() { cout << \"Hello Docker Sandbox\"; return 0; }";

        Path zipFile = createZipFile(tempDir, "2112399_Docker.zip", Map.of("main.cpp", cppCode));
        StudentSubmission submission = new StudentSubmission("2112399", "Tran Docker", zipFile);

        List<TestCase> testCases = List.of(
                new TestCase("TC_Docker", "", "Hello Docker Sandbox\n", 2)
        );

        GradingReport report = dockerGrader.grade(submission, testCases, rubric, tempDir);

        assertTrue(report.isCompileSuccess(), "Biên dịch thành công trong container Docker");
        assertEquals(10.0, report.getFinalScore(), 0.01);
        assertEquals(1, report.getTestCaseResults().size());
        assertEquals(TestCaseResult.Status.ACCEPTED, report.getTestCaseResults().get(0).getStatus());
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Hàm điều kiện cho @EnabledIf: Kiểm tra xem Docker Daemon có sẵn sàng không
     */
    static boolean isDockerAvailable() {
        try {
            // Kiểm tra Docker daemon có chạy và có thể mount volume, chạy lệnh gcc hay không
            Process process = new ProcessBuilder(
                "docker", "run", "--rm", "gcc:latest", "g++", "--version"
            ).start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tạo file .zip phục vụ kiểm thử
     */
    private Path createZipFile(Path tempDir, String zipFileName, Map<String, String> files) throws IOException {
        Path zipPath = tempDir.resolve(zipFileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        Files.write(zipPath, baos.toByteArray());
        return zipPath;
    }
}