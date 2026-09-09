package vn.edu.dlu.autograder.executor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CompilerEngine {

    public static class CompileResult {
        private final boolean success;
        private final String compilerMessage;
        private final Path executablePath;

        public CompileResult(boolean success, String compilerMessage, Path executablePath) {
            this.success = success;
            this.compilerMessage = compilerMessage;
            this.executablePath = executablePath;
        }

        public boolean isSuccess() { return success; }
        public String getCompilerMessage() { return compilerMessage; }
        public Path getExecutablePath() { return executablePath; }
    }

    public CompileResult compile(Path sourceDir, Path outputDir) {
        // 1. Kiểm tra tồn tại thư mục nguồn (Khắc phục NoSuchFileException)
        if (sourceDir == null || !Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
            return new CompileResult(false, "Lỗi: Thư mục mã nguồn không tồn tại hoặc không phải thư mục!", null);
        }

        try {
            // 2. Tự động tạo thư mục đầu ra nếu chưa có (Khắc phục lỗi thiếu outputDir)
            Files.createDirectories(outputDir);

            // 3. Quét tìm tất cả file .cpp
            List<Path> cppFiles;
            try (Stream<Path> stream = Files.walk(sourceDir)) {
                cppFiles = stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".cpp"))
                        .collect(Collectors.toList());
            }

            if (cppFiles.isEmpty()) {
                return new CompileResult(false, "Lỗi: Không tìm thấy file .cpp nào trong dự án!", null);
            }

            String os = System.getProperty("os.name").toLowerCase();
            String exeName = os.contains("win") ? "program.exe" : "program.out";
            Path exePath = outputDir.resolve(exeName);

            // 4. Tạo câu lệnh g++
            List<String> command = new ArrayList<>();
            command.add("g++");
            for (Path cpp : cppFiles) {
                command.add(cpp.toAbsolutePath().toString());
            }
            command.add("-o");
            command.add(exePath.toAbsolutePath().toString());

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();

            // Đọc stderr đồng thời để tránh đầy buffer biên dịch
            StringBuilder errorLog = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorLog.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                killProcessTree(process);
                return new CompileResult(false, "Lỗi: Quá thời gian biên dịch (Compile Timeout - 30s)", null);
            }

            if (process.exitValue() == 0) {
                return new CompileResult(true, "Biên dịch thành công!", exePath);
            } else {
                return new CompileResult(false, "Lỗi biên dịch (Compile Error):\n" + errorLog, null);
            }

        } catch (IOException e) {
            // Khắc phục IOException khi g++ không có trong PATH hoặc thiếu quyền
            return new CompileResult(false, "Lỗi I/O (Chưa cài g++ hoặc không có quyền truy cập): " + e.getMessage(), null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            return new CompileResult(false, "Biên dịch bị ngắt giữa chừng (InterruptedException)", null);
        } catch (Exception e) {
            return new CompileResult(false, "Ngoại lệ hệ thống không xác định: " + e.getMessage(), null);
        }
    }

    private void killProcessTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
    }
}