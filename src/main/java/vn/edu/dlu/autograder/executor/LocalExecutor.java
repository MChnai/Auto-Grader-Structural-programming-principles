package vn.edu.dlu.autograder.executor;

import vn.edu.dlu.autograder.model.ExecutionResult;
import vn.edu.dlu.autograder.model.TestCase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

public class LocalExecutor implements IExecutor {
    @Override
    public ExecutionResult execute(Path exePath, TestCase testCase) {
    	if (exePath == null || !Files.exists(exePath)) {
            // Exit code -1 đại diện cho lỗi hệ thống không thực thi được
            return new ExecutionResult(-1, "", "Lỗi: File thực thi không tồn tại!", 0, false);
        }
        if (!Files.isExecutable(exePath)) {
            exePath.toFile().setExecutable(true);
        }

        long startTime = System.currentTimeMillis();
        ProcessBuilder pb = new ProcessBuilder(exePath.toAbsolutePath().toString());
        ExecutorService ioThreadPool = Executors.newFixedThreadPool(2);

        try {
            Process process = pb.start();

            Future<String> stdOutFuture = ioThreadPool.submit(() -> readStream(process.getInputStream()));
            Future<String> stdErrFuture = ioThreadPool.submit(() -> readStream(process.getErrorStream()));

            String normalizedInput = testCase.getInput() != null 
                    ? testCase.getInput().replace("\r\n", "\n").replace("\r", "") 
                    : "";

            if (!normalizedInput.isEmpty()) {
                try (OutputStream os = process.getOutputStream()) {
                    os.write(normalizedInput.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
            }

            long timeLimitSec = testCase.getTimeLimitSeconds() > 0 ? testCase.getTimeLimitSeconds() : 2;
            boolean completed = process.waitFor(timeLimitSec, TimeUnit.SECONDS);
            long executionTime = System.currentTimeMillis() - startTime;

            // Xử lý Timeout
            if (!completed) {
                killProcessTree(process);
                return new ExecutionResult(
                        -1,
                        "",
                        "Time Limit Exceeded (" + timeLimitSec + "s)",
                        executionTime,
                        true // isTimedOut = true
                );
            }

            String stdOut = stdOutFuture.get(1, TimeUnit.SECONDS);
            String stdErr = stdErrFuture.get(1, TimeUnit.SECONDS);

            // Trả về exitCode thực tế từ Process và isTimedOut = false
            return new ExecutionResult(process.exitValue(), stdOut, stdErr, executionTime, false);

        } catch (IOException e) {
            return new ExecutionResult(-1, "", "Lỗi I/O / Quyền thực thi: " + e.getMessage(), System.currentTimeMillis() - startTime, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecutionResult(-1, "", "Tiến trình bị ngắt giữa chừng!", System.currentTimeMillis() - startTime, false);
        } catch (ExecutionException | TimeoutException e) {
            return new ExecutionResult(-1, "", "Lỗi khi đọc luồng I/O: " + e.getMessage(), System.currentTimeMillis() - startTime, false);
        } finally {
            ioThreadPool.shutdownNow();
        }
    }
    

    private String readStream(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}