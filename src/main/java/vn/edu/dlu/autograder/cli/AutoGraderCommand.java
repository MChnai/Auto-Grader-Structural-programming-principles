package vn.edu.dlu.autograder.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vn.edu.dlu.autograder.analyzer.PlagiarismReportExporter;
import vn.edu.dlu.autograder.model.*;
import vn.edu.dlu.autograder.model.PlagiarismResult;
import vn.edu.dlu.autograder.service.PlagiarismCheckerService;
import vn.edu.dlu.autograder.service.SubmissionGrader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
    name = "autograder",
    mixinStandardHelpOptions = true,
    version = "AutoGrader 1.0.0",
    description = "Hệ thống chấm điểm bài nộp & kiểm tra đạo văn tự động - Trường Đại Học Đà Lạt",
    subcommands = {
        AutoGraderCommand.GradeCommand.class,
        AutoGraderCommand.PlagiarismCommand.class
    }
)
public class AutoGraderCommand implements Runnable {

    @Override
    public void run() {
        // Mặc định in Hướng dẫn sử dụng nếu người dùng không truyền subcommand
        CommandLine.usage(this, System.out);
    }

    // =========================================================================
    // SUBCOMMAND 1: CHẤM ĐIỂM BÀI NỘP (autograder grade ...)
    // =========================================================================
    @Command(name = "grade", description = "Biên dịch và chấm điểm bài nộp ZIP")
    public static class GradeCommand implements Callable<Integer> {

        @Parameters(index = "0", description = "Đường dẫn tới file ZIP bài nộp (.zip)")
        private Path submissionZipPath;

        @Option(names = { "-t", "--testcases" }, description = "Thư mục chứa các file Testcase (.in và .out)", required = true)
        private Path testCasesDir;

        @Option(names = { "-s", "--max-score" }, description = "Thang điểm tối đa (Mặc định: 10.0)", defaultValue = "10.0")
        private double maxScore;

        @Option(names = { "--use-docker" }, description = "Bật chế độ cách ly an toàn trong Docker Sandbox", defaultValue = "false")
        private boolean useDocker;

        @Override
        public Integer call() throws Exception {
            System.out.println("=========================================");
            System.out.println("        DLU AUTOGRADER ENGINE v1.0       ");
            System.out.println("=========================================");

            if (!Files.exists(submissionZipPath)) {
                System.err.println("[LỖI] File bài nộp không tồn tại: " + submissionZipPath);
                return 1;
            }

            if (!Files.isDirectory(testCasesDir)) {
                System.err.println("[LỖI] Thư mục testcase không hợp lệ: " + testCasesDir);
                return 1;
            }

            System.out.println("-> Chế độ thực thi : " + (useDocker ? "DOCKER SANDBOX " : "LOCAL NATIVE "));
            System.out.println("-> File bài nộp    : " + submissionZipPath.getFileName());

            List<TestCase> testCases = loadTestCasesFromDir(testCasesDir);
            if (testCases.isEmpty()) {
                System.err.println("[LỖI] Không tìm thấy testcase hợp lệ (.in/.out) trong thư mục!");
                return 1;
            }
            System.out.println("-> Đã tải thành công : " + testCases.size() + " testcases.");

            SubmissionGrader grader = new SubmissionGrader(useDocker);
            GradingRubric rubric = new GradingRubric(maxScore, 1.0, 0.0);

            Path tempDir = Files.createTempDirectory("cli_grader_");
            StudentSubmission submission = new StudentSubmission("CLI_USER", "Sinh Vien", submissionZipPath);

            System.out.println("\n[+] Đang tiến hành biên dịch và chấm bài...");
            GradingReport report = grader.grade(submission, testCases, rubric, tempDir);

            printReport(report, rubric);
            return 0;
        }

        private List<TestCase> loadTestCasesFromDir(Path dir) throws Exception {
            List<TestCase> testCases = new ArrayList<>();
            try (var stream = Files.list(dir)) {
                List<Path> inFiles = stream.filter(p -> p.toString().endsWith(".in")).sorted().toList();
                for (int i = 0; i < inFiles.size(); i++) {
                    Path inPath = inFiles.get(i);
                    String baseName = inPath.getFileName().toString().replace(".in", "");
                    Path outPath = dir.resolve(baseName + ".out");

                    String input = Files.exists(inPath) ? Files.readString(inPath) : "";
                    String expectedOutput = Files.exists(outPath) ? Files.readString(outPath) : "";

                    testCases.add(new TestCase("TestCase_" + (i + 1), input, expectedOutput, 2));
                }
            }
            return testCases;
        }

        private void printReport(GradingReport report, GradingRubric rubric) {
            System.out.println("\n================ KẾT QUẢ CHẤM ================");
            System.out.println("Trạng thái biên dịch: " + (report.isCompileSuccess() ? "THÀNH CÔNG" : "LỖI BIÊN DỊCH / GIẢI NÉN"));

            if (!report.isCompileSuccess()) {
                System.out.println("\nChi tiết lỗi:");
                System.out.println(report.getCompileMessage());
                System.out.println("----------------------------------------------");
                System.out.printf("TỔNG ĐIỂM : %.2f / %.2f\n", report.getFinalScore(), rubric.getMaxScore());
                System.out.println("Tóm tắt   : " + report.getSummary());
                System.out.println("==============================================");
                return;
            }

            System.out.println("\nChi tiết từng Testcase:");
            for (TestCaseResult result : report.getTestCaseResults()) {
                String testName = result.getTestCase() != null ? result.getTestCase().getId() : "N/A";
                TestCaseResult.Status status = result.getStatus();
                long timeMs = result.getExecutionTimeMs();

                System.out.printf(" - [%-15s] : %-20s (%d ms)\n", testName, status, timeMs);

                if (status != TestCaseResult.Status.ACCEPTED && result.getDiffMessage() != null) {
                    System.out.println("    └─ Thông báo: " + result.getDiffMessage().replace("\n", " "));
                }
            }

            System.out.println("----------------------------------------------");
            System.out.printf("Số testcase đúng : %d/%d\n", report.getPassedTestCount(), report.getTotalTestCount());
            System.out.printf("TỔNG ĐIỂM         : %.2f / %.2f\n", report.getFinalScore(), rubric.getMaxScore());
            System.out.println("Tóm tắt          : " + report.getSummary());
            System.out.println("==============================================");
        }
    }

    // =========================================================================
    // SUBCOMMAND 2: KIỂM TRA ĐẠO VĂN (autograder check-plagiarism ...)
    // =========================================================================
    @Command(name = "check-plagiarism", description = "Kiểm tra độ tương đồng mã nguồn giữa danh sách file ZIP")
    public static class PlagiarismCommand implements Callable<Integer> {

        @Parameters(arity = "2..*", description = "Danh sách các file ZIP bài nộp (Tối thiểu 2 file)")
        private List<Path> zipFiles;

        @Option(names = { "--threshold" }, description = "Ngưỡng tỷ lệ tương đồng cảnh báo (%)", defaultValue = "40.0")
        private double threshold;

        @Option(names = { "-o", "--output" }, description = "Đường dẫn file HTML xuất báo cáo (Ví dụ: report.html)")
        private Path outputFile;

        @Override
        public Integer call() throws Exception {
            System.out.println("=========================================");
            System.out.println("    DLU PLAGIARISM CHECKER ENGINE v1.0   ");
            System.out.println("=========================================");
            System.out.println("-> Ngưỡng cảnh báo : " + threshold + "%");
            System.out.println("-> Số bài nộp (ZIP): " + zipFiles.size());

            for (Path zip : zipFiles) {
                if (!Files.exists(zip)) {
                    System.err.println("[LỖI] File không tồn tại: " + zip);
                    return 1;
                }
            }

            PlagiarismCheckerService checkerService = new PlagiarismCheckerService();
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));

            System.out.println("\n[+] Đang giải nén và trích xuất dấu vết mã nguồn (Winnowing Fingerprints)...");
            
            // Service bây giờ sẽ trả về TẤT CẢ các cặp so sánh
            List<PlagiarismResult> results = checkerService.checkZipSubmissions(zipFiles, tempDir, threshold);

            // 1. In toàn bộ danh sách ra Console
            System.out.println("\n================ KẾT QUẢ KIỂM TRA ================");
            if (results.isEmpty()) {
                System.out.println(" Không có dữ liệu so sánh.");
            } else {
                System.out.printf("%-25s %-25s %-15s %-10s\n", "Sinh viên A", "Sinh viên B", "Độ tương đồng", "Trạng thái");
                System.out.println("--------------------------------------------------------------------------------");

                for (PlagiarismResult res : results) {
                    boolean isWarning = res.getSimilarityPercentage() >= threshold;
                    String status = isWarning ? "[NGHI VẤN]" : "[AN TOÀN]";
                    System.out.printf("%-25s %-25s %-15.2f%% %-10s\n",
                            res.getStudentA(), res.getStudentB(), res.getSimilarityPercentage(), status);
                }
            }
            System.out.println("==================================================");

            // 2. Xuất toàn bộ dữ liệu ra báo cáo HTML
            if (outputFile != null) {
                try {
                    List<PlagiarismReportExporter.MatchResult> exportData = new ArrayList<>();
                    for (PlagiarismResult res : results) {
                        exportData.add(new PlagiarismReportExporter.MatchResult(
                                res.getStudentA(),
                                res.getStudentB(),
                                res.getSimilarityPercentage()
                        ));
                    }

                    PlagiarismReportExporter.exportToHtml(exportData, threshold, outputFile.toFile());
                    System.out.println("\n[+] Báo cáo HTML đã được xuất thành công tại: " + outputFile.toAbsolutePath());
                } catch (Exception e) {
                    System.err.println("\n[-] Lỗi khi xuất báo cáo HTML: " + e.getMessage());
                }
            }

            return 0;
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new AutoGraderCommand()).execute(args);
        System.exit(exitCode);
    }
}