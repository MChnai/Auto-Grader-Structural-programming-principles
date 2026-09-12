package vn.edu.dlu.autograder.service;

import vn.edu.dlu.autograder.analyzer.WinnowingEngine;
import vn.edu.dlu.autograder.model.PlagiarismResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class PlagiarismCheckerService {

    private final WinnowingEngine engine;

    public PlagiarismCheckerService() {
        this.engine = new WinnowingEngine(10, 5);
    }

    /**
     * So sánh tất cả các file mã nguồn C++ trong thư mục bài nộp của lớp học
     */
    public List<PlagiarismResult> checkAllSubmissions(Map<String, Path> studentCodeMap, double threshold) throws IOException {
        List<PlagiarismResult> results = new ArrayList<>();
        Map<String, Set<Long>> studentFingerprints = new HashMap<>();

        // Buớc 1: Trích xuất Fingerprints cho từng sinh viên
        for (Map.Entry<String, Path> entry : studentCodeMap.entrySet()) {
            String studentId = entry.getKey();
            String code = Files.readString(entry.getValue());
            studentFingerprints.put(studentId, engine.generateFingerprints(code));
        }

        List<String> studentIds = new ArrayList<>(studentCodeMap.keySet());

        // Bước 2: So sánh chéo từng cặp (Pairwise Comparison)
        for (int i = 0; i < studentIds.size(); i++) {
            for (int j = i + 1; j < studentIds.size(); j++) {
                String studentA = studentIds.get(i);
                String studentB = studentIds.get(j);

                Set<Long> fpA = studentFingerprints.get(studentA);
                Set<Long> fpB = studentFingerprints.get(studentB);

                double similarity = engine.calculateSimilarity(fpA, fpB);

                if (similarity >= threshold) {
                    results.add(new PlagiarismResult(studentA, studentB, Math.round(similarity * 100.0) / 100.0));
                }
            }
        }

        // Sắp xếp giảm dần theo tỷ lệ phần trăm nghi vấn đạo văn
        results.sort((r1, r2) -> Double.compare(r2.getSimilarityPercentage(), r1.getSimilarityPercentage()));
        return results;
    }
    /**
     * BỔ SUNG MỚI: Giải nén danh sách file ZIP bài nộp và thực hiện so sánh chéo tất cả các cặp
     */
    public List<PlagiarismResult> checkZipSubmissions(List<Path> zipFiles, Path tempBaseDir, double threshold) throws IOException {
        List<PlagiarismResult> results = new ArrayList<>();
        Map<String, Set<Long>> studentFingerprints = new HashMap<>();

        // Loại bỏ các file ZIP trùng lặp nếu người dùng truyền lặp lại trên CLI
        List<Path> uniqueZipFiles = zipFiles.stream().distinct().toList();

        // Bước 1: Giải nén từng file ZIP và trích xuất Fingerprints
        for (Path zipPath : uniqueZipFiles) {
            String studentName = getFileNameWithoutExtension(zipPath.getFileName().toString());
            Path extractedDir = Files.createTempDirectory(tempBaseDir, "plag_" + studentName + "_");

            try {
                // Giải nén file ZIP
                vn.edu.dlu.autograder.archive.ZipExtractor.extract(zipPath, extractedDir);

                // Đọc và ghép toàn bộ mã nguồn C++
                String fullCode = aggregateAllCppCode(extractedDir);

                System.out.println("DEBUG [" + studentName + "] Code đọc được:\n" + fullCode);

                Set<Long> fingerprints = engine.generateFingerprints(fullCode);
                System.out.println("DEBUG [" + studentName + "] Số lượng Fingerprints: " + fingerprints.size());

                studentFingerprints.put(studentName, fingerprints);
            } finally {
                // Dọn dẹp thư mục tạm sau khi xử lý xong
                vn.edu.dlu.autograder.archive.ZipExtractor.cleanUpDirectory(extractedDir);
            }
        }

        List<String> studentNames = new ArrayList<>(studentFingerprints.keySet());

        // Bước 2: So sánh chéo tất cả các cặp (Giữ đầy đủ danh sách để in ra Console và xuất HTML)
        for (int i = 0; i < studentNames.size(); i++) {
            for (int j = i + 1; j < studentNames.size(); j++) {
                String studentA = studentNames.get(i);
                String studentB = studentNames.get(j);

                Set<Long> fpA = studentFingerprints.get(studentA);
                Set<Long> fpB = studentFingerprints.get(studentB);

                double similarity = engine.calculateSimilarity(fpA, fpB);

                results.add(new PlagiarismResult(
                    studentA, 
                    studentB, 
                    Math.round(similarity * 100.0) / 100.0
                ));
            }
        }

        // Sắp xếp kết quả giảm dần theo tỷ lệ phần trăm
        results.sort((r1, r2) -> Double.compare(r2.getSimilarityPercentage(), r1.getSimilarityPercentage()));
        return results;
    }

    /**
     * BỔ SUNG MỚI: Gom toàn bộ nội dung file mã nguồn C++ trong thư mục giải nén
     */
    private String aggregateAllCppCode(Path sourceDir) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (java.util.stream.Stream<Path> stream = Files.walk(sourceDir)) {
            List<Path> codeFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".cpp") || name.endsWith(".cc") || name.endsWith(".h") || name.endsWith(".hpp");
                    })
                    .toList();

            for (Path codeFile : codeFiles) {
                sb.append(Files.readString(codeFile)).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * BỔ SUNG MỚI: Lấy tên file không kèm đuôi mở rộng (.zip)
     */
    private String getFileNameWithoutExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
}