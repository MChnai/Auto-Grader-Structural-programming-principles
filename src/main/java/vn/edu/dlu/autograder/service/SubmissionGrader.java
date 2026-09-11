package vn.edu.dlu.autograder.service;

import vn.edu.dlu.autograder.analyzer.CppCodeAnalyzer;
import vn.edu.dlu.autograder.archive.ZipExtractor;
import vn.edu.dlu.autograder.checker.OutputChecker;
import vn.edu.dlu.autograder.executor.CompilerEngine;
import vn.edu.dlu.autograder.executor.ExecutorEngine;
import vn.edu.dlu.autograder.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SubmissionGrader {

    private final ZipExtractor zipExtractor;
    private final CompilerEngine compilerEngine;
    private final ExecutorEngine executorEngine;
    private final OutputChecker outputChecker;
    private final CppCodeAnalyzer cppCodeAnalyzer;
    private final ExecutorService threadPool;
    private final boolean useDocker; // Bổ sung cờ nhận biết môi trường

    public SubmissionGrader(ZipExtractor zipExtractor,
                            CompilerEngine compilerEngine,
                            ExecutorEngine executorEngine,
                            OutputChecker outputChecker,
                            boolean useDocker) {
        this.zipExtractor = zipExtractor;
        this.compilerEngine = compilerEngine;
        this.executorEngine = executorEngine;
        this.outputChecker = outputChecker;
        this.cppCodeAnalyzer = new CppCodeAnalyzer();
        this.useDocker = useDocker;
        // Cấu hình Thread Pool chạy các testcase song song
        this.threadPool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public SubmissionGrader() {
        this(false);
    }

    public SubmissionGrader(boolean useDocker) {
        this(new ZipExtractor(),  
             new CompilerEngine(), 
             new ExecutorEngine(useDocker), 
             new OutputChecker(),
             useDocker);
    }

    public GradingReport grade(StudentSubmission submission,
                               List<TestCase> testCases,
                               GradingRubric rubric,
                               Path parentTempDir) {

        Path workDir = null;
        AnalysisResult analysisResult = null;

        try {
            workDir = Files.createTempDirectory(parentTempDir, "sub_" + submission.getStudentId() + "_");

            // 1. Giải nén
            Path extractTarget = workDir.resolve("extracted");
            ZipExtractor.ExtractResult extractResult = zipExtractor.extract(submission.getZipFilePath(), extractTarget);

            if (!extractResult.isSuccess()) {
                return GradingReport.extractionError(submission, extractResult.getMessage());
            }
            
            Path sourceDir = extractResult.getSourceDir();
            Path mainCppPath = findMainCppSourceFile(sourceDir);

            // 2. TẦNG 1: STATIC ANALYSIS (CLEAN CODE & STYLE - 15%)
            if (mainCppPath != null) {
                analysisResult = cppCodeAnalyzer.analyze(mainCppPath);

                if (!analysisResult.isValid()) {
                    String errorMessage = "LỖI PHÂN TÍCH MÃ NGUỒN (STATIC ANALYSIS):\n - " 
                            + String.join("\n - ", analysisResult.getViolations());
                    
                    GradingReport errorReport = GradingReport.compileError(submission, errorMessage);
                    errorReport.setCleanCodeScore(analysisResult.getCleanCodeScore());
                    errorReport.setStyleWarnings(analysisResult.getWarnings());

                    return errorReport;
                }
            }

            // 3. Biên dịch
            CompilerEngine.CompileResult compileResult = compilerEngine.compile(sourceDir, workDir);

            if (!compileResult.isSuccess()) {
                return GradingReport.compileError(submission, compileResult.getCompilerMessage());
            }

            Path exePath = compileResult.getExecutablePath();

            // 4. TẦNG 2, 3 & 4: DYNAMIC EXECUTION, PERFORMANCE & MEMORY SAFETY (Chạy song song)
            List<CompletableFuture<TestCaseResult>> futures = new ArrayList<>();
            for (TestCase tc : testCases) {
                futures.add(CompletableFuture.supplyAsync(() -> runSingleTestCase(exePath, tc), threadPool));
            }

            // Chờ tất cả TestCase chạy xong
            List<TestCaseResult> testResults = futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

            // --- THU THẬP KẾT QUẢ THỰC THI ---
            int passedCount = 0;
            long totalExecutionTimeMs = 0;
            boolean detectedMemoryLeak = false;

            for (TestCaseResult tr : testResults) {
                if (tr.isPassed()) {
                    passedCount++;
                }
                
                // NẾU DÙNG DOCKER: Trừ bớt 150ms Overhead ảo hóa mỗi TestCase để tính Performance công bằng
                long adjustedTime = useDocker ? Math.max(0, tr.getExecutionTimeMs() - 150) : tr.getExecutionTimeMs();
                totalExecutionTimeMs += adjustedTime;
                
                // Bắt cờ Memory Leak từ kết quả thực thi Sandbox
                if (tr.hasMemoryLeak()) {
                    detectedMemoryLeak = true;
                }
            }

            double maxScore = rubric.getMaxScore();

            // 1. Correctness (60%)
            double correctnessRatio = testCases.isEmpty() ? 0.0 : (double) passedCount / testCases.size();
            double correctnessScore = correctnessRatio * (maxScore * 0.60);

            // 2. Clean Code (15%)
            double cleanCodeScoreRatio = (analysisResult != null) ? (analysisResult.getCleanCodeScore() / 100.0) : 1.0;
            double cleanCodeScore = cleanCodeScoreRatio * (maxScore * 0.15);

            // 3. Performance (15%) - So sánh với Benchmark đề bài
            double avgTimeMs = testCases.isEmpty() ? 0.0 : (double) totalExecutionTimeMs / testCases.size();
            double benchmarkTimeMs = rubric.getBenchmarkTimeMs() > 0 ? rubric.getBenchmarkTimeMs() : 200.0;
            
            double performanceRatio;
            if (avgTimeMs <= benchmarkTimeMs) {
                performanceRatio = 1.0;
            } else {
                performanceRatio = Math.max(0.0, 1.0 - ((avgTimeMs - benchmarkTimeMs) / benchmarkTimeMs));
            }
            // Điều kiện ăn điểm Performance: Phải làm đúng ít nhất 1 testcase
            double performanceScore = (passedCount > 0) ? (performanceRatio * (maxScore * 0.15)) : 0.0;

            // 4. Memory Safety (10%) - Điều kiện: Đúng ít nhất 1 testcase & Không dính Memory Leak
            double memorySafetyScore = (passedCount > 0 && !detectedMemoryLeak) ? (maxScore * 0.10) : 0.0;

            // TỔNG ĐIỂM
            double finalScore = correctnessScore + cleanCodeScore + performanceScore + memorySafetyScore;
            finalScore = Math.min(maxScore, Math.round(finalScore * 100.0) / 100.0);

            double cleanCodePercent = (analysisResult != null) ? analysisResult.getCleanCodeScore() : 100.0;
            String summary = String.format("Correctness: %.2fp (%d/%d) | Clean Code: %.2fp (%.0f%%) | Perf: %.2fp | MemSafety: %.2fp | Tổng: %.2f/%.2f",
                    correctnessScore, passedCount, testCases.size(), 
                    cleanCodeScore, cleanCodePercent,
                    performanceScore, memorySafetyScore,
                    finalScore, maxScore);

            GradingReport report = new GradingReport(
                    submission,
                    true,
                    "Biên dịch & Chạy thành công",
                    testResults,
                    finalScore,
                    summary
            );

            if (analysisResult != null) {
                report.setCleanCodeScore(analysisResult.getCleanCodeScore());
                report.setStyleWarnings(analysisResult.getWarnings());
            }

            return report;

        } catch (IOException e) {
            return GradingReport.extractionError(submission, "Lỗi I/O hệ thống: " + e.getMessage());
        } finally {
            cleanUpTempDirectory(workDir);
        }
    }

    // Helper: Xử lý 1 Testcase đơn lẻ
    private TestCaseResult runSingleTestCase(Path exePath, TestCase tc) {
        ExecutionResult execResult = executorEngine.execute(exePath, tc);

        TestCaseResult.Status status;
        String diffMsg;

        if (execResult.isTimedOut()) {
            status = TestCaseResult.Status.TIME_LIMIT_EXCEEDED;
            diffMsg = execResult.getStderr();
        } else if (execResult.getExitCode() != 0) {
            status = TestCaseResult.Status.RUNTIME_ERROR;
            diffMsg = "Runtime Error (Exit Code " + execResult.getExitCode() + "):\n" + execResult.getStderr();
        } else {
            OutputChecker.CheckResult checkResult = outputChecker.check(
                    execResult.getStdout(),
                    tc.getExpectedOutput(),
                    OutputChecker.MatchMode.IGNORE_TRAILING_WHITESPACE
            );

            if (checkResult.isMatched()) {
                status = TestCaseResult.Status.ACCEPTED;
                diffMsg = "Correct Answer";
            } else {
                status = TestCaseResult.Status.WRONG_ANSWER;
                diffMsg = checkResult.getDiffMessage();
            }
        }

        TestCaseResult result = new TestCaseResult(tc, status, execResult.getStdout(), diffMsg, execResult.getExecutionTimeMs());
        
        // Cập nhật thông số Memory Leak từ ExecutionResult sang TestCaseResult nếu có
        result.setHasMemoryLeak(execResult.hasMemoryLeak());
        
        return result;
    }

    private Path findMainCppSourceFile(Path dir) {
        if (dir == null || !Files.exists(dir)) return null;
        try (Stream<Path> stream = Files.walk(dir)) {
            List<Path> cppFiles = stream.filter(p -> p.toString().endsWith(".cpp") || p.toString().endsWith(".cc"))
                                       .collect(Collectors.toList());

            for (Path p : cppFiles) {
                String content = Files.readString(p);
                if (content.contains("int main") || content.contains("void main")) {
                    return p;
                }
            }
            return cppFiles.isEmpty() ? null : cppFiles.get(0);
        } catch (IOException e) {
            return null;
        }
    }

    private void cleanUpTempDirectory(Path dir) {
        if (dir != null && Files.exists(dir)) {
            try (var stream = Files.walk(dir)) {
                stream.sorted((p1, p2) -> p2.compareTo(p1))
                      .forEach(path -> {
                          try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                      });
            } catch (IOException ignored) {}
        }
    }
}