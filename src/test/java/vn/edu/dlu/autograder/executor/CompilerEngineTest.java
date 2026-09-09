package vn.edu.dlu.autograder.executor;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import vn.edu.dlu.autograder.model.ExecutionResult;
import vn.edu.dlu.autograder.model.TestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Hệ thống Biên dịch & Thực thi Autograder")
public class CompilerEngineTest {

    private CompilerEngine compiler;
    private ExecutorEngine executor;

    @BeforeEach
    void setUp() {
        compiler = new CompilerEngine();
        executor = new ExecutorEngine();
    }

    @Test
    @DisplayName("1. Biên dịch thành công & Chạy đúng Testcase (Happy Path)")
    void testSuccessExecution(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("src_success");
        Files.createDirectories(sourceDir);
        
        // Code C++ tính tổng 2 số
        String code = "#include <iostream>\n" +
                      "using namespace std;\n" +
                      "int main() {\n" +
                      "    int a, b; cin >> a >> b;\n" +
                      "    cout << (a + b);\n" +
                      "    return 0;\n" +
                      "}";
        Files.writeString(sourceDir.resolve("main.cpp"), code);

        // Biên dịch
        CompilerEngine.CompileResult compileResult = compiler.compile(sourceDir, tempDir);
        assertTrue(compileResult.isSuccess(), "Biên dịch phải thành công");
        assertNotNull(compileResult.getExecutablePath());

        // Thực thi
        TestCase tc = new TestCase("TC1", "10 20\n", "30", 2);
        ExecutionResult execResult = executor.execute(compileResult.getExecutablePath(), tc);

        assertFalse(execResult.isTimedOut(), "Không được bị Timeout");
        assertEquals(0, execResult.getExitCode(), "Exit code phải bằng 0");
        assertEquals("30\n", execResult.getStdout());
    }

    @Test
    @DisplayName("2. Lỗi biên dịch (Compile Error)")
    void testCompileError(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("src_ce");
        Files.createDirectories(sourceDir);
        
        // Code C++ thiếu dấu chấm phẩy
        String badCode = "#include <iostream>\n" +
                         "int main() {\n" +
                         "    std::cout << \"Hello World\"\n" + // Thiếu ';'
                         "    return 0;\n" +
                         "}";
        Files.writeString(sourceDir.resolve("main.cpp"), badCode);

        CompilerEngine.CompileResult compileResult = compiler.compile(sourceDir, tempDir);

        assertFalse(compileResult.isSuccess());
        assertNull(compileResult.getExecutablePath());
        assertTrue(compileResult.getCompilerMessage().contains("error:"), "Phải chứa thông báo lỗi từ g++");
    }

    @Test
    @DisplayName("3. Vòng lặp vô hạn (Time Limit Exceeded - TLE)")
    void testTimeLimitExceeded(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("src_tle");
        Files.createDirectories(sourceDir);
        
        // Code C++ lặp vô tận
        String code = "int main() { while(true); return 0; }";
        Files.writeString(sourceDir.resolve("main.cpp"), code);

        CompilerEngine.CompileResult compileResult = compiler.compile(sourceDir, tempDir);
        assertTrue(compileResult.isSuccess());

        // Chạy với giới hạn 1 giây
        TestCase tc = new TestCase("TC_TLE", "", "", 1);
        ExecutionResult execResult = executor.execute(compileResult.getExecutablePath(), tc);

        assertTrue(execResult.isTimedOut(), "Trạng thái phải là TimedOut");
        assertTrue(execResult.getExecutionTimeMs() >= 1000, "Thời gian chạy phải xấp xỉ hoặc lớn hơn 1000ms");
    }

    @Test
    @DisplayName("4. Lỗi thực thi / Chia cho 0 (Runtime Error - RTE)")
    void testRuntimeError(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("src_rte");
        Files.createDirectories(sourceDir);
        
        // Code C++ gây lỗi chia cho 0
        String code = "#include <iostream>\n" +
                      "int main() {\n" +
                      "    int x = 0;\n" +
                      "    int y = 10 / x;\n" +
                      "    std::cout << y;\n" +
                      "    return 0;\n" +
                      "}";
        Files.writeString(sourceDir.resolve("main.cpp"), code);

        CompilerEngine.CompileResult compileResult = compiler.compile(sourceDir, tempDir);
        assertTrue(compileResult.isSuccess());

        TestCase tc = new TestCase("TC_RTE", "", "", 2);
        ExecutionResult execResult = executor.execute(compileResult.getExecutablePath(), tc);

        assertFalse(execResult.isTimedOut());
        assertNotEquals(0, execResult.getExitCode(), "Lỗi Runtime Error phải trả về exitCode khác 0");
    }

    @Test
    @DisplayName("5. Đầu ra dữ liệu lớn - Chống Deadlock I/O Buffer")
    void testOutputOverflowBuffer(@TempDir Path tempDir) throws IOException {
        Path sourceDir = tempDir.resolve("src_overflow");
        Files.createDirectories(sourceDir);

        // Code in liên tục 100,000 dòng để lấp đầy OS Stream Buffer
        String code = "#include <iostream>\n" +
                      "using namespace std;\n" +
                      "int main() {\n" +
                      "    for(int i = 0; i < 100000; i++) {\n" +
                      "        cout << \"Dong dư lieu kiem thu buffer: \" << i << \"\\n\";\n" +
                      "    }\n" +
                      "    return 0;\n" +
                      "}";
        Files.writeString(sourceDir.resolve("main.cpp"), code);

        CompilerEngine.CompileResult compileResult = compiler.compile(sourceDir, tempDir);
        assertTrue(compileResult.isSuccess());

        TestCase tc = new TestCase("TC_BUF", "", "", 3);
        ExecutionResult execResult = executor.execute(compileResult.getExecutablePath(), tc);

        assertFalse(execResult.isTimedOut());
        assertEquals(0, execResult.getExitCode());
        assertFalse(execResult.getStdout().isEmpty(), "Stdout không được rỗng");
    }

    @Test
    @DisplayName("6. Thư mục rỗng / Không tồn tại file .cpp")
    void testEmptySourceDirectory(@TempDir Path tempDir) throws IOException {
        Path emptyDir = tempDir.resolve("empty_dir");
        Files.createDirectories(emptyDir);

        CompilerEngine.CompileResult compileResult = compiler.compile(emptyDir, tempDir);

        assertFalse(compileResult.isSuccess());
        assertTrue(compileResult.getCompilerMessage().contains("Không tìm thấy file .cpp"));
    }
}