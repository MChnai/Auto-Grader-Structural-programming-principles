package vn.edu.dlu.autograder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.dlu.autograder.model.*;
import vn.edu.dlu.autograder.service.SubmissionGrader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/grader")
public class GraderController {

    private final SubmissionGrader submissionGrader = new SubmissionGrader(false);

    @PostMapping(value = "/grade", consumes = "multipart/form-data")
    public ResponseEntity<?> gradeSubmission(
            @RequestParam("file") MultipartFile file,
            @RequestParam("studentId") String studentId,
            @RequestParam("studentName") String studentName) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File ZIP bài nộp không được rỗng!");
        }

        try {
            // 1. Lưu file ZIP tạm
            Path tempZip = Files.createTempFile("sub_", "_" + file.getOriginalFilename());
            file.transferTo(tempZip.toFile());

            StudentSubmission submission = new StudentSubmission(studentId, studentName, tempZip);
            GradingRubric rubric = new GradingRubric(10.0, 200.0); // 10 điểm, benchmark 200ms
            Path tempWorkDir = Files.createTempDirectory("grader_work_");

            // 2. Gọi Service chấm điểm (Sử dụng danh sách testcase mặc định hoặc truyền từ ngoài vào)
            GradingReport report = submissionGrader.grade(submission, Collections.emptyList(), rubric, tempWorkDir);

            // 3. Dọn dẹp file ZIP tạm
            Files.deleteIfExists(tempZip);

            return ResponseEntity.ok(report);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi xử lý file bài nộp: " + e.getMessage());
        }
    }
}