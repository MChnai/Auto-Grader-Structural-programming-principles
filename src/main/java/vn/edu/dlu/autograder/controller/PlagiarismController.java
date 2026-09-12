package vn.edu.dlu.autograder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.dlu.autograder.dto.PlagiarismRequestDTO;
import vn.edu.dlu.autograder.model.PlagiarismResult;
import vn.edu.dlu.autograder.service.PlagiarismCheckerService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/plagiarism")
public class PlagiarismController {

    private final PlagiarismCheckerService plagiarismService = new PlagiarismCheckerService();

    @PostMapping("/check")
    public ResponseEntity<?> checkPlagiarism(@RequestBody PlagiarismRequestDTO request) {
        if (request.getZipFilePaths() == null || request.getZipFilePaths().size() < 2) {
            return ResponseEntity.badRequest().body("Cần ít nhất 2 file để tiến hành so sánh đạo văn!");
        }

        try {
            Map<String, Path> studentCodeMap = new HashMap<>();
            for (int i = 0; i < request.getZipFilePaths().size(); i++) {
                String pathStr = request.getZipFilePaths().get(i);
                studentCodeMap.put("Student_" + (i + 1), Paths.get(pathStr));
            }

            double threshold = request.getThreshold() > 0 ? request.getThreshold() : 0.7; // Mặc định 70%

            List<PlagiarismResult> results = plagiarismService.checkAllSubmissions(studentCodeMap, threshold);

            return ResponseEntity.ok(results);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Lỗi khi đọc file mã nguồn: " + e.getMessage());
        }
    }
}