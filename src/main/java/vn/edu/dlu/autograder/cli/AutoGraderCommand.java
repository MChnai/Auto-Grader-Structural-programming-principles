package vn.edu.dlu.autograder.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import vn.edu.dlu.autograder.model.*;
import vn.edu.dlu.autograder.service.SubmissionGrader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "autograder", mixinStandardHelpOptions = true, version = "AutoGrader 1.0.0", description = "Hệ thống chấm điểm bài nộp tự động - Trường Đại Học Đà Lạt")
public class AutoGraderCommand implements Callable<Integer> {

	@Parameters(index = "0", description = "Đường dẫn tới file ZIP bài nộp (.zip)")
	private Path submissionZipPath;

	@Option(names = { "-t",
			"--testcases" }, description = "Thư mục chứa các file Testcase (.in và .out)", required = true)
	private Path testCasesDir;

	@Option(names = { "-s", "--max-score" }, description = "Thang điểm tối đa (Mặc định: 10.0)", defaultValue = "10.0")
	private double maxScore;

	@Option(names = {
			"--use-docker" }, description = "Bật chế độ cách ly an toàn trong Docker Sandbox", defaultValue = "false")
	private boolean useDocker;

	@Override
	public Integer call() throws Exception {
		System.out.println("=========================================");
		System.out.println("       DLU AUTOGRADER ENGINE v1.0        ");
		System.out.println("=========================================");

		// 1. Kiểm tra sự tồn tại của file/thư mục
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

		// 2. Load danh sách Testcase từ thư mục chỉ định
		List<TestCase> testCases = loadTestCasesFromDir(testCasesDir);
		if (testCases.isEmpty()) {
			System.err.println("[LỖI] Không tìm thấy testcase hợp lệ (.in/.out) trong thư mục!");
			return 1;
		}
		System.out.println("-> Đã tải thành công : " + testCases.size() + " testcases.");

		// 3. Khởi tạo SubmissionGrader và GradingRubric
		SubmissionGrader grader = new SubmissionGrader(useDocker);
		GradingRubric rubric = new GradingRubric(maxScore, 1.0, 0.0);

		// 4. Tiến hành chấm bài
		Path tempDir = Files.createTempDirectory("cli_grader_");
		StudentSubmission submission = new StudentSubmission("CLI_USER", "Sinh Vien", submissionZipPath);

		System.out.println("\n[+] Đang tiến hành biên dịch và chấm bài...");
		GradingReport report = grader.grade(submission, testCases, rubric, tempDir);

		// 5. In báo cáo kết quả
		printReport(report, rubric);

		return 0;
	}

	private List<TestCase> loadTestCasesFromDir(Path dir) throws Exception {
		List<TestCase> testCases = new ArrayList<>();

		// Quét và ghép cặp các file .in và .out
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
		System.out.println(
				"Trạng thái biên dịch: " + (report.isCompileSuccess() ? "THÀNH CÔNG" : "LỖI BIÊN DỊCH / GIẢI NÉN"));

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
				System.out.println("   └─ Thông báo: " + result.getDiffMessage().replace("\n", " "));
			}
		}

		System.out.println("----------------------------------------------");
		System.out.printf("Số testcase đúng : %d/%d\n", report.getPassedTestCount(), report.getTotalTestCount());
		System.out.printf("TỔNG ĐIỂM        : %.2f / %.2f\n", report.getFinalScore(), rubric.getMaxScore());
		System.out.println("Tóm tắt          : " + report.getSummary());
		System.out.println("==============================================");
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new AutoGraderCommand()).execute(args);
		System.exit(exitCode);
	}
}