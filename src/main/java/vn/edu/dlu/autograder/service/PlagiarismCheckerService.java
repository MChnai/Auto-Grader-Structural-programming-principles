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
}