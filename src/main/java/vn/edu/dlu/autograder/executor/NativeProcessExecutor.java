package vn.edu.dlu.autograder.executor;

import vn.edu.dlu.autograder.model.ExecutionResult;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class NativeProcessExecutor {

    /**
     * Thực thi file nhị phân của sinh viên với cơ chế Timeout
     */
    public static ExecutionResult runWithTimeout(Path executablePath, String inputData, long timeoutSeconds) {
        ProcessBuilder pb = new ProcessBuilder(executablePath.toAbsolutePath().toString());
        long startTime = System.currentTimeMillis();

        try {
            Process process = pb.start();

            // 1. Ghi dữ liệu testcase vào stdin
            if (inputData != null && !inputData.isEmpty()) {
                try (var os = process.getOutputStream()) {
                    os.write(inputData.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            // 2. Chờ tiến trình trong timeoutSeconds
            boolean completedInTime = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long executionTimeMs = System.currentTimeMillis() - startTime;

            // 3. Xử lý khi bị Timeout (Lặp vô tận)
            if (!completedInTime) {
                process.destroyForcibly();
                process.waitFor();

                return new ExecutionResult(
                    -1, 
                    "", 
                    "Lỗi: Chương trình chạy quá thời gian cho phép (" + timeoutSeconds + "s) - Có thể do lặp vô tận!", 
                    executionTimeMs, 
                    true // isTimedOut = true
                );
            }

            // 4. Nếu chạy xong đúng giờ
            int exitCode = process.exitValue();
            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

            return new ExecutionResult(exitCode, stdout, stderr, executionTimeMs, false);

        } catch (Exception e) {
            long executionTimeMs = System.currentTimeMillis() - startTime;
            return new ExecutionResult(-1, "", "Lỗi hệ thống: " + e.getMessage(), executionTimeMs, false);
        }
    }
}