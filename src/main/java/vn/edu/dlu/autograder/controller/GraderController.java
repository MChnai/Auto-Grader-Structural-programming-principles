package vn.edu.dlu.autograder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.dlu.autograder.model.*;
import vn.edu.dlu.autograder.service.SubmissionGrader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/v1/grader")
public class GraderController {

    private final SubmissionGrader submissionGrader = new SubmissionGrader(false);

    @PostMapping(value = "/grade", consumes = "multipart/form-data")
    public ResponseEntity<?> gradeSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") String studentId,
            @RequestParam("studentName") String studentName,
            @RequestParam(value = "inFiles", required = false) List<MultipartFile> inFiles,
            @RequestParam(value = "outFiles", required = false) List<MultipartFile> outFiles) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File ZIP bài nộp không được rỗng!");
        }

        try {
            // 1. Ghép các cặp file .in và .out thành List<TestCase>
            List<TestCase> testCases = parseTestCasesFromFiles(inFiles, outFiles);

            // 2. Lưu file ZIP tạm
            Path tempZip = Files.createTempFile("sub_", "_" + file.getOriginalFilename());
            file.transferTo(tempZip.toFile());

            StudentSubmission submission = new StudentSubmission(studentId, studentName, tempZip);
            GradingRubric rubric = new GradingRubric(10.0, 200.0);
            Path tempWorkDir = Files.createTempDirectory("grader_work_");

            // 3. Gọi Service chấm điểm với testCases vừa ghép
            GradingReport report = submissionGrader.grade(submission, testCases, rubric, tempWorkDir);

            // 4. Dọn dẹp file ZIP tạm
            Files.deleteIfExists(tempZip);

            return ResponseEntity.ok(report);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý file: " + e.getMessage());
        }
    }

    // Helper: Ghép file .in và .out có cùng tên (VD: test1.in ghép với test1.out)
    private List<TestCase> parseTestCasesFromFiles(List<MultipartFile> inFiles, List<MultipartFile> outFiles) throws IOException {
        if (inFiles == null || inFiles.isEmpty()) return Collections.emptyList();

        Map<String, String> outputsMap = new HashMap<>();
        if (outFiles != null) {
            for (MultipartFile outFile : outFiles) {
                String baseName = getBaseName(outFile.getOriginalFilename());
                String content = new String(outFile.getBytes(), StandardCharsets.UTF_8);
                outputsMap.put(baseName, content);
            }
        }

        List<TestCase> testCases = new ArrayList<>();
        for (MultipartFile inFile : inFiles) {
            String baseName = getBaseName(inFile.getOriginalFilename());
            String inputContent = new String(inFile.getBytes(), StandardCharsets.UTF_8);
            String expectedOutput = outputsMap.getOrDefault(baseName, "");

            testCases.add(new TestCase(baseName, inputContent, expectedOutput, 5));
        }

        return testCases;
    }

    private String getBaseName(String fileName) {
        if (fileName == null) return "";
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
    }
}