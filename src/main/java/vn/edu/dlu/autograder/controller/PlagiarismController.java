package vn.edu.dlu.autograder.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.edu.dlu.autograder.dto.PlagiarismRequestDTO;
import vn.edu.dlu.autograder.model.PlagiarismResult;
import vn.edu.dlu.autograder.service.PlagiarismCheckerService;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/plagiarism")
public class PlagiarismController {

    private final PlagiarismCheckerService plagiarismService =
            new PlagiarismCheckerService();

    @PostMapping("/check")
    public ResponseEntity<?> checkPlagiarism(
            @RequestBody PlagiarismRequestDTO request) {

        if (request.getZipFilePaths() == null
                || request.getZipFilePaths().size() < 2) {

            return ResponseEntity.badRequest().body(
                    "Cần ít nhất 2 file ZIP để tiến hành so sánh đạo văn!"
            );
        }

        try {

            List<Path> zipFiles = new ArrayList<>();

            for (String pathStr : request.getZipFilePaths()) {

                Path path = convertToPath(pathStr);

                if (!Files.exists(path)) {
                    return ResponseEntity.badRequest().body(
                            "Không tìm thấy file: " + path
                    );
                }

                if (!Files.isRegularFile(path)) {
                    return ResponseEntity.badRequest().body(
                            "Đường dẫn không phải là file: " + path
                    );
                }

                String fileName =
                        path.getFileName()
                                .toString()
                                .toLowerCase();

                if (!fileName.endsWith(".zip")) {
                    return ResponseEntity.badRequest().body(
                            "File phải có định dạng ZIP: " + path
                    );
                }

                zipFiles.add(path);
            }

            double threshold =
                    request.getThreshold() > 0
                            ? request.getThreshold()
                            : 0.7;

            Path tempBaseDir =
                    Files.createTempDirectory("plagiarism_work_");

            try {

                List<PlagiarismResult> results =
                        plagiarismService.checkZipSubmissions(
                                zipFiles,
                                tempBaseDir,
                                threshold
                        );

                return ResponseEntity.ok(results);

            } finally {

                deleteDirectory(tempBaseDir);

            }

        } catch (IOException e) {

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Lỗi khi xử lý file ZIP: "
                                    + e.getMessage()
                    );

        } catch (Exception e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Dữ liệu không hợp lệ: "
                                    + e.getMessage()
                    );
        }
    }

    private Path convertToPath(String pathStr) {

        if (pathStr == null || pathStr.isBlank()) {

            throw new IllegalArgumentException(
                    "Đường dẫn file không được để trống!"
            );
        }

        if (pathStr.startsWith("file:")) {

            return Paths.get(
                    URI.create(pathStr)
            );
        }

        return Paths.get(pathStr);
    }

    private void deleteDirectory(Path directory)
            throws IOException {

        if (directory == null
                || !Files.exists(directory)) {

            return;
        }

        try (var stream = Files.walk(directory)) {

            stream
                    .sorted(
                            java.util.Comparator.reverseOrder()
                    )
                    .forEach(path -> {

                        try {

                            Files.deleteIfExists(path);

                        } catch (IOException e) {

                            throw new RuntimeException(e);

                        }
                    });
        }
    }
}